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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import javax.management.ObjectName;

import net.sf.cglib.asm.Type;
import net.sf.cglib.core.Signature;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import net.sf.cglib.reflect.FastClass;
import org.gbean.kernel.OperationSignature;
import org.gbean.reflect.OperationInvoker;
import org.gbean.reflect.ServiceInvoker;

/**
 * @version $Rev: 106345 $ $Date: 2004-11-23 12:37:03 -0800 (Tue, 23 Nov 2004) $
 */
public class ProxyMethodInterceptor implements MethodInterceptor {
    /**
     * Type of the proxy interface
     */
    private final Class proxyType;

    /**
     * The object name to which we are connected.
     */
    private final ObjectName objectName;

    /**
     * OperationInvokers indexed by interface method id
     */
    private final OperationInvoker[] operationIndex;

    /**
     * The service invoker used by this proxy
     */
    private final ServiceInvoker serviceInvoker;

    private final Object data;

    public ProxyMethodInterceptor(Class proxyType, ServiceInvoker serviceInvoker, ObjectName objectName, Object data) {
        assert proxyType != null;
        assert serviceInvoker != null;
        assert objectName != null;

        this.proxyType = proxyType;
        this.serviceInvoker = serviceInvoker;
        this.objectName = objectName;
        this.data = data;

        operationIndex = createOperationIndex();
    }

    public ObjectName getObjectName() {
        return objectName;
    }

    public Object getData() {
        return data;
    }

    public final Object intercept(final Object object, final Method method, final Object[] args, final MethodProxy proxy) throws Throwable {
        int interfaceIndex = proxy.getSuperIndex();
        OperationInvoker operationInvoker = this.operationIndex[interfaceIndex];
        if (operationInvoker == null) {
            throw new UnsupportedOperationException("No implementation method: objectName=" + objectName + ", method=" + method);
        }

        return operationInvoker.invoke(args);
    }

    private OperationInvoker[] createOperationIndex() {
        List operationIndex = serviceInvoker.getOperationIndex();
        Map operations = new HashMap(operationIndex.size());
        for (Iterator iterator = operationIndex.iterator(); iterator.hasNext();) {
            OperationInvoker operationInvoker = (OperationInvoker) iterator.next();
            operations.put(operationInvoker.getSignature(), operationInvoker);
        }

        // build the method lookup table
        FastClass fastClass = FastClass.create(proxyType);
        OperationInvoker[] operationInvokers = new OperationInvoker[fastClass.getMaxIndex() + 1];
        Method[] methods = proxyType.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            int interfaceIndex = getSuperIndex(proxyType, method);
            if (interfaceIndex >= 0) {
                operationInvokers[interfaceIndex] = (OperationInvoker) operations.get(new OperationSignature(method));
            }
        }

        // handle equals, hashCode and toString directly here
        try {
            operationInvokers[getSuperIndex(proxyType, proxyType.getMethod("equals", new Class[]{Object.class}))] = new EqualsInvoke(this);
            operationInvokers[getSuperIndex(proxyType, proxyType.getMethod("hashCode", null))] = new HashCodeInvoke(this);
            operationInvokers[getSuperIndex(proxyType, proxyType.getMethod("toString", null))] = new ToStringInvoke(objectName, proxyType.getName());
        } catch (Exception e) {
            // this can not happen... all classes must implement equals, hashCode and toString
            throw new AssertionError(e);
        }

        return operationInvokers;
    }

    private static int getSuperIndex(Class proxyType, Method method) {
        Signature signature = new Signature(method.getName(), Type.getReturnType(method), Type.getArgumentTypes(method));
        MethodProxy methodProxy = MethodProxy.find(proxyType, signature);
        if (methodProxy != null) {
            return methodProxy.getSuperIndex();
        }
        return -1;
    }

    static final class HashCodeInvoke implements OperationInvoker {
        private final MethodInterceptor methodInterceptor;

        public HashCodeInvoke(MethodInterceptor methodInterceptor) {
            this.methodInterceptor = methodInterceptor;
        }

        public OperationSignature getSignature() {
            return new OperationSignature("hashCode", new String[] {});
        }

        // todo this should be hashcode of objectname         
        public Object invoke(Object[] arguments) {
            return new Integer(methodInterceptor.hashCode());
        }
    }

    static final class EqualsInvoke implements OperationInvoker {
        private final MethodInterceptor methodInterceptor;

        public EqualsInvoke(MethodInterceptor methodInterceptor) {
            this.methodInterceptor = methodInterceptor;
        }

        public OperationSignature getSignature() {
            return new OperationSignature("equals", new String[] {Object.class.getName()});
        }

        // todo this should do isProxy and compare the target objectname
        public Object invoke(Object[] arguments) {
            return Boolean.valueOf(methodInterceptor.equals(arguments[0]));
        }
    }

    static final class ToStringInvoke implements OperationInvoker {
        private final String interfaceName;
        private final ObjectName objectName;

        public ToStringInvoke(ObjectName objectName, String interfaceName) {
            this.objectName = objectName;
            this.interfaceName = "[" + interfaceName + ": ";
        }

        public OperationSignature getSignature() {
            return new OperationSignature("toString", new String[] {});
        }

        public Object invoke(Object[] arguments) {
            return interfaceName + objectName + "]";
        }
    }
}
