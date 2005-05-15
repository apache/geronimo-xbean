/**
 *
 * Copyright 2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.gbean.proxy;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import javax.management.ObjectName;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.NoOp;
import org.gbean.kernel.Kernel;
import org.gbean.kernel.ServiceName;
import org.gbean.kernel.ServiceNotFoundException;
import org.gbean.reflect.ServiceInvoker;
import org.gbean.reflect.ServiceInvokerManager;

/**
 * @version $Rev$ $Date$
 */
public class ProxyManager {
    private static ObjectName PROXY_MANAGER_QUERY = ServiceName.createName("*:j2eeType=ProxyManager,*");
    public static ProxyManager findProxyManager(Kernel kernel) throws ServiceNotFoundException {
        Set names = kernel.listServices(PROXY_MANAGER_QUERY);
        if (names.isEmpty()) {
            throw new IllegalStateException("Proxy mananger could not be found in kernel: " + PROXY_MANAGER_QUERY);
        }
        if (names.size() > 1) {
            throw new IllegalStateException("More then one proxy manangers were found in kernel: " + PROXY_MANAGER_QUERY);
        }
        ObjectName proxyManagerName = (ObjectName) names.iterator().next();
        return (ProxyManager) kernel.getService(proxyManagerName);
    }

    private final ServiceInvokerManager serviceInvokerManager;
    private final Map interceptors = new WeakHashMap();

    public ProxyManager(ServiceInvokerManager serviceInvokerManager) {
        this.serviceInvokerManager = serviceInvokerManager;
    }

    public synchronized ProxyFactory createProxyFactory(Class type) {
        assert type != null: "type is null";
        return new FastProxyFactory(type);
    }

    public synchronized Object createProxy(ObjectName target, Class type) throws ServiceNotFoundException {
        return createProxy(target, type, null);
    }

    public synchronized Object createProxy(ObjectName target, Class type, Object data) throws ServiceNotFoundException {
        assert type != null: "type is null";
        assert target != null: "target is null";

        return new FastProxyFactory(type).createProxy(target, data);
    }

    public boolean isProxy(Object proxy) {
        return interceptors.containsKey(proxy);
    }

    public synchronized ObjectName getProxyTarget(Object proxy) {
        ProxyMethodInterceptor methodInterceptor = (ProxyMethodInterceptor) interceptors.get(proxy);
        if (methodInterceptor == null) {
            return null;
        }
        return methodInterceptor.getObjectName();
    }

    public synchronized Object getProxyData(Object proxy) {
        ProxyMethodInterceptor methodInterceptor = (ProxyMethodInterceptor) interceptors.get(proxy);
        if (methodInterceptor == null) {
            return null;
        }
        return methodInterceptor.getData();
    }

    private class FastProxyFactory implements ProxyFactory {
        private final Class type;
        private final Enhancer enhancer;

        public FastProxyFactory(Class type) {
            enhancer = new Enhancer();
            enhancer.setSuperclass(type);
            enhancer.setCallbackTypes(new Class[]{NoOp.class, MethodInterceptor.class});
            enhancer.setCallbackFilter(FILTER);
            enhancer.setUseFactory(false);
            this.type = enhancer.createClass();
        }

        public synchronized Object createProxy(ObjectName target) throws ServiceNotFoundException {
            return createProxy(target, null);
        }

        public synchronized Object createProxy(ObjectName target, Object data) throws ServiceNotFoundException {
            assert target != null: "target is null";

            ServiceInvoker serviceInvoker = serviceInvokerManager.getServiceInvoker(target);
            if (serviceInvoker == null) {
                throw new IllegalStateException("Service is not running: " + target);
            }
            ProxyMethodInterceptor interceptor = new ProxyMethodInterceptor(type, serviceInvoker, target, data);

            // @todo trap CodeGenerationException indicating missing no-arg ctr
            enhancer.setCallbacks(new Callback[]{NoOp.INSTANCE, interceptor});
            Object proxy = enhancer.create();

            interceptors.put(proxy, interceptor);
            return proxy;
        }
    }

    private static final CallbackFilter FILTER = new CallbackFilter() {
        public int accept(Method method) {
            if (method.getName().equals("finalize") &&
                method.getParameterTypes().length == 0 &&
                method.getReturnType() == Void.TYPE) {
                return 0;
            }
            return 1;
        }
    };
}
