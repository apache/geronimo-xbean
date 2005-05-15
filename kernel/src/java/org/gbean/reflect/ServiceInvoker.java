/**
 *
 * Copyright 2005 GBean.org
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
package org.gbean.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import javax.management.ObjectName;

import org.apache.geronimo.gbean.DynamicGBean;
import org.apache.geronimo.gbean.GOperationSignature;
import org.apache.geronimo.kernel.NoSuchAttributeException;
import org.apache.geronimo.kernel.NoSuchOperationException;
import org.gbean.kernel.Kernel;
import org.gbean.kernel.LifecycleAdapter;
import org.gbean.kernel.ServiceNotFoundException;
import org.gbean.kernel.OperationSignature;
import org.gbean.kernel.runtime.ServiceState;
import org.gbean.service.ServiceFactory;
import org.gbean.service.ConfigurableServiceFactory;
import net.sf.cglib.reflect.FastMethod;
import net.sf.cglib.reflect.FastClass;

/**
 * @version $Revision$ $Date$
 */
public class ServiceInvoker {
    private final Kernel kernel;
    private final ObjectName name;
    private Object serviceInstance;
    private boolean serviceRunning;
    private ServiceFactory serviceFactory;

    /**
     * Property invokers
     */
    private PropertyInvokerImpl[] properties;

    /**
     * Property name to index number
     */
    private final Map propertyIndex = new HashMap();

    /**
     * Operations lookup table
     */
    private OperationInvokerImpl[] operations;

    /**
     * Operations supported by this GBeanMBean by (GOperationSignature) name.
     */
    private final Map operationIndex = new HashMap();

//    private String stateMessage;
    private final ServiceInvokerLifecycleListener lifecycleListener;

    public ServiceInvoker(Kernel kernel, ObjectName name) {
        this.kernel = kernel;
        this.name = name;
//        stateMessage = "ServiceInvoker is stopped";
        lifecycleListener = new ServiceInvokerLifecycleListener();
    }

    public synchronized void start() {
        kernel.addLifecycleListener(lifecycleListener, name);
        updateState();
    }

    public synchronized void stop() {
        kernel.removeLifecycleListener(lifecycleListener);
        serviceFactory = null;
        serviceInstance = null;
        serviceRunning = false;
    }

    public synchronized String getClassName() {
        if (serviceInstance != null) {
            return serviceInstance.getClass().getName();
        } else {
            return Object.class.getName();
        }
    }

    public synchronized List getPropertyIndex() {
        assureRunning();
        return Collections.unmodifiableList(Arrays.asList(properties));
    }

    public synchronized List getOperationIndex() {
        assureRunning();
        return Collections.unmodifiableList(Arrays.asList(operations));
    }

    /**
     * Gets the attribute value using the attribute index.  This is the most efficient way to get
     * an attribute as it avoids a HashMap lookup.
     *
     * @param index the index of the attribute
     * @return the attribute value
     * @throws Exception if a target instance throws and exception
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    public Object getAttribute(int index) throws Exception {
        // copy target into local variables from within a synchronized block to gaurentee a consistent read
        boolean running;
        Object instance;
        synchronized (this) {
            assureLoaded();
            running = serviceRunning;
            instance = serviceInstance;
        }

        PropertyInvokerImpl getter = properties[index];
        if (!running) {
            if (!(serviceFactory instanceof ConfigurableServiceFactory)) {
                throw new NoSuchAttributeException("Service is stopped and the service factory not configurable");
            }
            ConfigurableServiceFactory configurableServiceFactory = (ConfigurableServiceFactory) serviceFactory;
            Object value = configurableServiceFactory.getProperty(getter.getPropertyName());
            return value;
        } else {
            try {
                Object value = getter.getterFastMethod.invoke(instance, null);
                return value;
            } catch (InvocationTargetException e) {
                throw unwrap(e);
            }
        }
    }

    /**
     * Gets an attribute's value by name.  This get style is less efficient becuse the attribute must
     * first be looked up in a HashMap.
     *
     * @param name the name of the property to retrieve
     * @return the property value
     * @throws Exception if a problem occurs while getting the value
     * @throws NoSuchAttributeException if the attribute name is not found in the map
     */
    public Object getAttribute(String name) throws NoSuchAttributeException, Exception {
        // copy target into local variables from within a synchronized block to gaurentee a consistent read
        boolean running;
        Object instance;
        synchronized (this) {
            assureLoaded();
            running = serviceRunning;
            instance = serviceInstance;
        }

        if (!running) {
            if (!(serviceFactory instanceof ConfigurableServiceFactory)) {
                throw new NoSuchAttributeException("Service is stopped and the service factory not configurable");
            }
            ConfigurableServiceFactory configurableServiceFactory = (ConfigurableServiceFactory) serviceFactory;
            Object value = configurableServiceFactory.getProperty(name);
            return value;
        } else {
            Integer index = (Integer) propertyIndex.get(name);
            if (index != null) {
                try {
                    Object value = properties[index.intValue()].getterFastMethod.invoke(instance, null);
                    return value;
                } catch (InvocationTargetException e) {
                    throw unwrap(e);
                }
            } else if (instance instanceof DynamicGBean) {
                Object value = ((DynamicGBean) instance).getAttribute(name);
                return value;
            }
            throw new NoSuchAttributeException("Unknown property '" + name + "' in service " + name);
        }
    }

    /**
     * Sets the attribute value using the attribute index.  This is the most efficient way to set
     * an attribute as it avoids a HashMap lookup.
     *
     * @param index the index of the attribute
     * @param value the new value of attribute value
     * @throws Exception if a target instance throws and exception
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    public void setAttribute(int index, Object value) throws Exception, IndexOutOfBoundsException {
        // copy target into local variables from within a synchronized block to gaurentee a consistent read
        boolean running;
        Object instance;
        synchronized (this) {
            assureLoaded();
            running = serviceRunning;
            instance = serviceInstance;
        }

        if (!running) {
            if (!(serviceFactory instanceof ConfigurableServiceFactory)) {
                throw new NoSuchAttributeException("Service is stopped and the service factory not configurable");
            }
            ConfigurableServiceFactory configurableServiceFactory = (ConfigurableServiceFactory) serviceFactory;
            configurableServiceFactory.setProperty(properties[index].getPropertyName(), value);
        } else {
            try {
                properties[index].setterFastMethod.invoke(instance, new Object[] {value});
            } catch (InvocationTargetException e) {
                throw unwrap(e);
            }
        }
    }

    /**
     * Sets an attribute's value by name.  This set style is less efficient becuse the attribute must
     * first be looked up in a HashMap.
     *
     * @param attributeName the name of the attribute to retrieve
     * @param attributeValue the new attribute value
     * @throws Exception if a target instance throws and exception
     * @throws NoSuchAttributeException if the attribute name is not found in the map
     */
    public void setAttribute(String attributeName, Object attributeValue) throws Exception, NoSuchAttributeException {
        // copy target into local variables from within a synchronized block to gaurentee a consistent read
        boolean running;
        Object instance;
        ServiceFactory factory;
        synchronized (this) {
            assureLoaded();
            running = serviceRunning;
            instance = serviceInstance;
            factory = serviceFactory;
        }

        if (!running) {
            if (!(factory instanceof ConfigurableServiceFactory)) {
                throw new NoSuchAttributeException("Service is stopped and the service factory not configurable");
            }
            ConfigurableServiceFactory configurableServiceFactory = (ConfigurableServiceFactory) factory;
            configurableServiceFactory.setProperty(attributeName, attributeValue);
        } else {
            Integer index = (Integer) propertyIndex.get(attributeName);
            if (index != null) {
                try {
                    properties[index.intValue()].setterFastMethod.invoke(instance, new Object[] {attributeValue});
                } catch (InvocationTargetException e) {
                    throw unwrap(e);
                }
            } else if (instance instanceof DynamicGBean) {
                ((DynamicGBean) instance).setAttribute(attributeName, attributeValue);
            } else {
                throw new NoSuchAttributeException("Unknown attribute '" + attributeName + "' in service " + name);
            }
        }
    }

    /**
     * Invokes an opreation using the operation index.  This is the most efficient way to invoke
     * an operation as it avoids a HashMap lookup.
     *
     * @param index the index of the attribute
     * @param arguments the arguments to the operation
     * @return the result of the operation
     * @throws Exception if a target instance throws and exception
     * @throws IndexOutOfBoundsException if the index is invalid
     * @throws IllegalStateException if the gbean instance has been destroyed
     */
    public Object invoke(int index, Object[] arguments) throws Exception {
        // copy target into local variables from within a synchronized block to gaurentee a consistent read
        boolean running;
        Object instance;
        synchronized (this) {
            assureRunning();
            running = serviceRunning;
            instance = serviceInstance;
        }

        if (!running) {
            throw new IllegalStateException("Operations can only be invoke while the service instance is running: " + name);
        }

        OperationInvokerImpl operation = operations[index];
        try {
            Object value = operation.fastMethod.invoke(instance, arguments);
            return value;
        } catch (InvocationTargetException e) {
            throw unwrap(e);
        }
    }

    /**
     * Invokes an operation on the target gbean by method signature.  This style if invocation is
     * inefficient, because the target method must be looked up in a hashmap using a freshly constructed
     * GOperationSignature object.
     *
     * @param operationName the name of the operation to invoke
     * @param arguments arguments to the operation
     * @param types types of the operation arguemtns
     * @return the result of the operation
     * @throws Exception if a target instance throws and exception
     * @throws NoSuchOperationException if the operation signature is not found in the map
     * @throws IllegalStateException if the gbean instance has been destroyed
     */
    public Object invoke(String operationName, Object[] arguments, String[] types) throws Exception, NoSuchOperationException {
        // copy target into local variables from within a synchronized block to gaurentee a consistent read
        boolean running;
        Object instance;
        synchronized (this) {
            assureRunning();
            running = serviceRunning;
            instance = serviceInstance;
        }

        if (!running) {
            throw new IllegalStateException("Operations can only be invoke while the service is running: " + name);
        }

        GOperationSignature signature = new GOperationSignature(operationName, types);
        Integer index = (Integer) operationIndex.get(signature);
        if (index == null) {
            throw new NoSuchOperationException("Unknown operation " + signature);
        }
        OperationInvokerImpl operation = operations[index.intValue()];
        try {
            Object value = operation.fastMethod.invoke(instance, arguments);
            return value;
        } catch (InvocationTargetException e) {
            throw unwrap(e);
        }
    }

    public synchronized void updateState() {
        try {
            if (kernel.isLoaded(name)) {
                if (serviceFactory == null) {
                    serviceFactory = kernel.getServiceFactory(name);
                }

                // we have the service factory so we can clear the stateMessage
//                stateMessage = null;

                // now try to get the instance
                int serviceState = kernel.getServiceState(name);
                boolean running = serviceState == ServiceState.RUNNING_INDEX || serviceState == ServiceState.STOPPING_INDEX;
                if (running) {
                    // try to get the service instance if we don't have it already
                    if (serviceInstance == null) {
                        serviceInstance = kernel.getService(name);

                        // if we have the service instance, index it
                        if (serviceInstance != null) {
                            createIndex(serviceInstance.getClass());
                            serviceRunning = true;
                        }
                    }
                } else {
                    serviceInstance = null;
                    serviceRunning = false;
                }
                return;
            }
        } catch (IllegalStateException e) {
            // service died...
            serviceInstance = null;
            serviceRunning = false;
            return;
        } catch (ServiceNotFoundException e) {
            // unregistered fields cleared below
        }
        serviceFactory = null;
        serviceInstance = null;
        serviceRunning = false;
    }

    private synchronized void assureLoaded() {
        if (serviceFactory == null) {
            throw new IllegalStateException("Service is not loaded: " + name);
        }
    }

    private synchronized void assureRunning() {
        if (!serviceRunning) {
            updateState();
            if (!serviceRunning) {
                throw new IllegalStateException("Service is not running: " + name);
            }
        }
    }

    private class ServiceInvokerLifecycleListener extends LifecycleAdapter {
        public void loaded(ObjectName objectName) {
            updateState();
        }

        public void running(ObjectName objectName) {
            updateState();
        }

        public void stopped(ObjectName objectName) {
            updateState();
        }

        public void unloaded(ObjectName objectName) {
            updateState();
        }
    }

    // todo add createIndex for servicefactory
    // todo deal with the exceptions below

    private void createIndex(Class type) {
        // attributes
        Method[] methods = type.getMethods();

        // map the getters
        Map getterMap = new HashMap(methods.length);
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            String methodName = method.getName();
            if (Modifier.isPublic(method.getModifiers()) &&
                    !Modifier.isStatic(method.getModifiers()) &&
                    method.getParameterTypes().length == 0 &&
                    method.getReturnType() != Void.TYPE) {
                if (methodName.length() > 3 && methodName.startsWith("get") && !methodName.equals("getClass")) {
                    String attributeName = fixAttributeName(methodName.substring(3));

                    // if this attribute also has an "is" accessor make sure the return type is boolean
                    Method isAccessor = (Method) getterMap.get(attributeName);
                    if (isAccessor != null && method.getReturnType() != Boolean.TYPE) {
                        throw new IllegalArgumentException("Getter has both a get<name> and is<name> accessor but the getter return type is not boolean:" +
                                " class=" + type.getName() +
                                ", attribute=" + attributeName +
                                ", getAccessorType=" + method.getReturnType().getName());
                    }

                    // add it
                    getterMap.put(attributeName, method);
                } else if (methodName.length() > 2 && methodName.startsWith("is")) {
                    String attributeName = fixAttributeName(methodName.substring(2));

                    // an is accessor must return boolean
                    if (method.getReturnType() != Boolean.TYPE) {
                        throw new IllegalArgumentException("An is<name> accessor must return boolean:" +
                                " class=" + type.getName() +
                                ", attribute=" + attributeName +
                                ", attributeType=" + method.getReturnType().getName());
                    }

                    // if this attribute also has a "get" accessor make sure the getter return type is boolean
                    Method getAccessor = (Method) getterMap.get(attributeName);
                    if (getAccessor != null && method.getReturnType() != Boolean.TYPE) {
                        throw new IllegalArgumentException("Getter has both a get<name> and is<name> accessor but the getter return type is not boolean:" +
                                " class=" + type.getName() +
                                ", attribute=" + attributeName +
                                ", getAccessorType=" + getAccessor.getReturnType().getName());
                    }

                    // add it
                    getterMap.put(attributeName, method);
                }
            }
        }

        // map the setters
        Map setterMap = new HashMap(methods.length);
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            String methodName = method.getName();
            if (Modifier.isPublic(method.getModifiers()) &&
                    !Modifier.isStatic(method.getModifiers()) &&
                    method.getParameterTypes().length == 1 &&
                    method.getReturnType() == Void.TYPE &&
                    methodName.length() > 3 &&
                    methodName.startsWith("set")) {

                String attributeName = fixAttributeName(methodName.substring(3));
                if (setterMap.containsKey(attributeName)) {
                    // this bean attributte has multiple setters with different types, so treat only as operations
                    setterMap.put(attributeName, null);
                } else {
                    setterMap.put(attributeName, method);
                }

                // the getter and setter types must match
                Method getterMethod = (Method) getterMap.get(attributeName);
                if (getterMethod != null && !getterMethod.getReturnType().equals(method.getParameterTypes()[0])) {
                    throw new IllegalArgumentException("Getter and setter types do not match:" +
                            " class=" + type.getName() +
                            ", attribute=" + attributeName +
                            ", getAccessorType=" + getterMethod.getReturnType().getName() +
                            ", setAccessorType=" + method.getParameterTypes()[0].getName());
                }
            }
        }
        // remove any setter with a null method (these setters have multiple methods with different types)
        for (Iterator iterator = setterMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            Method setter = (Method) entry.getValue();
            if (setter == null) {
                iterator.remove();
            }
        }

        TreeSet propertyNames = new TreeSet();
        propertyNames.addAll(getterMap.keySet());
        propertyNames.addAll(setterMap.keySet());

        List propertyList = new ArrayList(getterMap.size());
        for (Iterator iterator = propertyNames.iterator(); iterator.hasNext();) {
            String propertyName = (String) iterator.next();
            Method getter = (Method) getterMap.get(propertyName);
            Method setter = (Method) setterMap.get(propertyName);
            propertyIndex.put(propertyName, new Integer(propertyList.size()));
            propertyList.add(new PropertyInvokerImpl(propertyName, getter, setter));
        }
        properties = (PropertyInvokerImpl[]) propertyList.toArray(new PropertyInvokerImpl[propertyList.size()]);

        // operations
        List operationList = new ArrayList(methods.length);
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())) {
                operationIndex.put(new GOperationSignature(method), new Integer(operationList.size()));
                operationList.add(new OperationInvokerImpl(method));
            }
        }
        operations = (OperationInvokerImpl[]) operationList.toArray(new OperationInvokerImpl[operationList.size()]);
    }

    private static Exception unwrap(InvocationTargetException e) throws Exception {
        Throwable cause = e.getTargetException();
        if (cause instanceof Exception) {
            return (Exception) cause;
        } else if (cause instanceof Error) {
            throw (Error) cause;
        }
        return e;
    }

    private static String fixAttributeName(String attributeName) {
        if (Character.isUpperCase(attributeName.charAt(0))) {
            return Character.toLowerCase(attributeName.charAt(0)) + attributeName.substring(1);
        }
        return attributeName;
    }

    private final class OperationInvokerImpl implements OperationInvoker {
        private final OperationSignature signature;
        private final FastMethod fastMethod;

        public OperationInvokerImpl(Method method) {
            assert method != null: "method is null";
            this.signature = new OperationSignature(method);
            this.fastMethod = FastClass.create(method.getDeclaringClass()).getMethod(method);
        }

        public OperationSignature getSignature() {
            return signature;
        }

        public Object invoke(Object[] arguments) throws Exception {
            Object instance;
            synchronized (this) {
                assureRunning();
                instance = serviceInstance;
            }

            try {
                Object value = fastMethod.invoke(instance, arguments);
                return value;
            } catch (InvocationTargetException e) {
                Throwable cause = e.getTargetException();
                if (cause instanceof Exception) {
                    throw (Exception) cause;
                } else if (cause instanceof Error) {
                    throw (Error) cause;
                }
                throw e;
            }
        }

        public int hashCode() {
            return signature.hashCode();
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof OperationInvokerImpl)) {
                return false;
            }
            OperationInvokerImpl operationInvoker = (OperationInvokerImpl)obj;
            return signature.equals(operationInvoker.signature);
        }

        public String toString() {
            return "[MethodInvoker: " + getSignature().toString() + "]";
        }
    }

    public class PropertyInvokerImpl implements PropertyInvoker {
        private final String propertyName;
        private final Class type;
        private final OperationSignature getterSignature;
        private final FastMethod getterFastMethod;
        private final OperationSignature setterSignature;
        private final FastMethod setterFastMethod;

        public PropertyInvokerImpl(String propertyName, Method getter, Method setter) {
            assert propertyName != null: "propertyName is null";
            assert getter != null || setter != null: "getter and setter are null";

            this.propertyName = propertyName;

            if (getter != null) {
                type = getter.getReturnType();
            } else {
                type = setter.getParameterTypes()[0];
            }

            if (getter != null) {
                getterSignature = new OperationSignature(getter);
                getterFastMethod = FastClass.create(getter.getDeclaringClass()).getMethod(getter);
            } else {
                getterSignature = null;
                getterFastMethod = null;
            }

            if (setter != null) {
                setterSignature = new OperationSignature(setter);
                setterFastMethod = FastClass.create(setter.getDeclaringClass()).getMethod(setter);
            } else {
                setterSignature = null;
                setterFastMethod = null;
            }
        }

        public String getPropertyName() {
            return propertyName;
        }

        public Class getType() {
            return type;
        }

        public boolean isReadable() {
            return getterFastMethod != null;
        }

        public OperationSignature getGetterSignature() {
            return getterSignature;
        }

        public Object invokeGetter() throws Exception {
            // copy target into local variables from within a synchronized block to gaurentee a consistent read
            boolean running;
            Object instance;
            ServiceFactory factory;
            synchronized (this) {
                assureLoaded();
                running = serviceRunning;
                instance = serviceInstance;
                factory = serviceFactory;
            }

            if (!running) {
                if (!(factory instanceof ConfigurableServiceFactory)) {
                    throw new NoSuchAttributeException("Service is stopped and the service factory not configurable");
                }
                ConfigurableServiceFactory configurableServiceFactory = (ConfigurableServiceFactory) factory;
                Object value = configurableServiceFactory.getProperty(propertyName);
                return value;
            } else {
                try {
                    Object value = getterFastMethod.invoke(instance, null);
                    return value;
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getTargetException();
                    if (cause instanceof Exception) {
                        throw (Exception) cause;
                    } else if (cause instanceof Error) {
                        throw (Error) cause;
                    }
                    throw e;
                }
            }
        }

        public boolean isWritable() {
            return setterFastMethod != null;
        }

        public OperationSignature getSetterSignature() {
            return setterSignature;
        }

        public void invokeSetter(Object value) throws Exception {
            // copy target into local variables from within a synchronized block to gaurentee a consistent read
            boolean running;
            Object instance;
            ServiceFactory factory;
            synchronized (this) {
                assureLoaded();
                running = serviceRunning;
                instance = serviceInstance;
                factory = serviceFactory;
            }

            if (!running) {
                if (!(factory instanceof ConfigurableServiceFactory)) {
                    throw new NoSuchAttributeException("Service is stopped and the service factory not configurable");
                }
                ConfigurableServiceFactory configurableServiceFactory = (ConfigurableServiceFactory) factory;
                configurableServiceFactory.setProperty(propertyName, value);
            } else {
                try {
                    setterFastMethod.invoke(instance, new Object[] {value});
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getTargetException();
                    if (cause instanceof Exception) {
                        throw (Exception) cause;
                    } else if (cause instanceof Error) {
                        throw (Error) cause;
                    }
                    throw e;
                }
            }
        }

        public int hashCode() {
            return propertyName.hashCode();
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof PropertyInvokerImpl)) {
                return false;
            }
            PropertyInvokerImpl propertyInvoker = (PropertyInvokerImpl)obj;
            return propertyName.equals(propertyInvoker.propertyName);
        }

        public String toString() {
            return "[PropertySetter: name=" + propertyName + ", getter=" + getterSignature + ", setter=" + setterSignature + "]";
        }
    }
}
