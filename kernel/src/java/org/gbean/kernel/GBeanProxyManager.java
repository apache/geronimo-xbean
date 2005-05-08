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
package org.gbean.kernel;

import java.util.Map;
import java.util.WeakHashMap;
import java.lang.reflect.Method;
import javax.management.ObjectName;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.NoOp;
import net.sf.cglib.proxy.CallbackFilter;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.kernel.proxy.ProxyFactory;
import org.apache.geronimo.kernel.proxy.ProxyManager;

/**
 * @version $Rev$ $Date$
 */
public class GBeanProxyManager implements ProxyManager {
    private final Kernel kernel;
    private final Map interceptors = new WeakHashMap();

    public GBeanProxyManager(Kernel kernel) {
        this.kernel = kernel;
    }

    public synchronized ProxyFactory createProxyFactory(Class type) {
        assert type != null: "type is null";
        return new ManagedProxyFactory(type);
    }

    public synchronized Object createProxy(ObjectName target, Class type) {
        return createProxy(target, type, null);
    }

    public synchronized Object createProxy(ObjectName target, Class type, Object data) {
        assert type != null: "type is null";
        assert target != null: "target is null";

        return new ManagedProxyFactory(type).createProxy(target, data);
    }

    /**
     * @deprecated This call is not needed anymore
     */
    public synchronized void destroyProxy(Object proxy) {
        // weak map will clean this up
        try {
            interceptors.remove(proxy);
        } catch (Throwable ignored) {
            // ignore
        }
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

    private class ManagedProxyFactory implements ProxyFactory {
        private final Class type;
        private final Enhancer enhancer;

        public ManagedProxyFactory(Class type) {
            enhancer = new Enhancer();
            enhancer.setSuperclass(type);
            enhancer.setCallbackTypes(new Class[]{NoOp.class, MethodInterceptor.class});
            enhancer.setCallbackFilter(FILTER);
            enhancer.setUseFactory(false);
            this.type = enhancer.createClass();
        }

        public synchronized Object createProxy(ObjectName target) {
            return createProxy(target, null);
        }

        public synchronized Object createProxy(ObjectName target, Object data) {
            assert target != null: "target is null";

            ProxyMethodInterceptor interceptor = new ProxyMethodInterceptor(type, kernel, target, data);

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
