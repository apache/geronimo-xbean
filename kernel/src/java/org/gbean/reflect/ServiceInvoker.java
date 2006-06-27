/**
 *
 * Copyright 2005 the original author or authors.
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

import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;
import org.apache.geronimo.gbean.DynamicGBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gbean.kernel.Kernel;
import org.gbean.kernel.LifecycleAdapter;
import org.gbean.kernel.OperationSignature;
import org.gbean.kernel.ServiceNotFoundException;
import org.gbean.kernel.NoSuchAttributeException;
import org.gbean.kernel.NoSuchOperationException;
import org.gbean.kernel.runtime.ServiceState;

/**
 * @version $Revision$ $Date$
 */
public class ServiceInvoker {
    /**
     * Our log stream
     */
    private static final Log log = LogFactory.getLog(ServiceInvoker.class);

    /**
     * The kernel in which the service is loaded
     */
    private final Kernel kernel;

    /**
     * The name of the service in the kernel
     */
    private final ObjectName name;

    /**
     * The listener that is notified when the service goes offline.
     */
    private final ServiceInvokerLifecycleListener lifecycleListener;

    /**
     * The actual target service
     */
    private Object serviceInstance;

    /**
     * Is the service currently running?
     */
    private boolean serviceRunning;

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
     * Operations supported by the service by OperationSignature
     */
    private final Map operationIndex = new HashMap();

    /**
     * Has the ServiceInvoker itself been started?
     */
    private boolean serviceInvokerStarted = false;

    public ServiceInvoker(Kernel kernel, ObjectName name) {
        this.kernel = kernel;
        this.name = name;
        lifecycleListener = new ServiceInvokerLifecycleListener();
    }

    public synchronized void start() throws ServiceNotFoundException {
        if (serviceRunning) {
            return;
        }

        kernel.addLifecycleListener(lifecycleListener, name);
        serviceInvokerStarted = true;
        assureRunning();
    }

    public synchronized void stop() {
        kernel.removeLifecycleListener(lifecycleListener);
        properties = null;
        propertyIndex.clear();
        operations = null;
        operationIndex.clear();
        serviceInstance = null;
        serviceRunning = false;
        serviceInvokerStarted = false;
    }

    public synchronized ObjectName getServiceName() {
        return name;
    }
    
    public synchronized Class getServiceType() {
        assureRunning();
        return serviceInstance.getClass();
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
        Object instance;
        PropertyInvokerImpl property;
        synchronized (this) {
            assureRunning();
            instance = serviceInstance;
            property = properties[index];
        }

        if (!property.isReadable()) {
            throw new IllegalArgumentException("Property " + property.getPropertyName() + " is not readable");
        }
        try {
            Object value = property.getterFastMethod.invoke(instance, null);
            return value;
        } catch (InvocationTargetException e) {
            throw unwrap(e);
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
        Object instance;
        PropertyInvokerImpl property = null;
        synchronized (this) {
            assureRunning();
            instance = serviceInstance;
            Integer index = (Integer) propertyIndex.get(name);
            if (index != null) {
                property = properties[index.intValue()];
            }
        }

        if (property != null) {
            if (!property.isReadable()) {
                throw new IllegalArgumentException("Property " + property.getPropertyName() + " is not readable");
            }
            try {
                Object value = property.getterFastMethod.invoke(instance, null);
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
        Object instance;
        PropertyInvokerImpl property;
        synchronized (this) {
            assureRunning();
            instance = serviceInstance;
            property = properties[index];
        }

        if (!property.isWritable()) {
            throw new IllegalArgumentException("Property " + property.getPropertyName() + " is not writable");
        }
        try {
            property.setterFastMethod.invoke(instance, new Object[] {value});
        } catch (InvocationTargetException e) {
            throw unwrap(e);
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
        Object instance;
        PropertyInvokerImpl property = null;
        synchronized (this) {
            assureRunning();
            instance = serviceInstance;
            Integer index = (Integer) propertyIndex.get(attributeName);
            if (index != null) {
                property = properties[index.intValue()];
            }
        }

        if (property != null) {
            if (!property.isWritable()) {
                throw new IllegalArgumentException("Property " + property.getPropertyName() + " is not writable");
            }
            try {
                property.setterFastMethod.invoke(instance, new Object[] {attributeValue});
            } catch (InvocationTargetException e) {
                throw unwrap(e);
            }
        } else if (instance instanceof DynamicGBean) {
            ((DynamicGBean) instance).setAttribute(attributeName, attributeValue);
        } else {
            throw new NoSuchAttributeException("Unknown attribute '" + attributeName + "' in service " + name);
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
     * @throws IllegalStateException if the service has been destroyed
     */
    public Object invoke(int index, Object[] arguments) throws Exception {
        // copy target into local variables from within a synchronized block to gaurentee a consistent read
        Object instance;
        OperationInvokerImpl operation;
        synchronized (this) {
            assureRunning();
            instance = serviceInstance;
            operation = operations[index];
        }

        try {
            Object value = operation.fastMethod.invoke(instance, arguments);
            return value;
        } catch (InvocationTargetException e) {
            throw unwrap(e);
        }
    }

    /**
     * Invokes an operation on the service by method signature.  This style if invocation is
     * inefficient, because the target method must be looked up in a hashmap using a freshly constructed
     * OperationSignature object.
     *
     * @param operationName the name of the operation to invoke
     * @param arguments arguments to the operation
     * @param types types of the operation arguemtns
     * @return the result of the operation
     * @throws Exception if a target instance throws and exception
     * @throws NoSuchOperationException if the operation signature is not found in the map
     * @throws IllegalStateException if the service has been destroyed
     */
    public Object invoke(String operationName, Object[] arguments, String[] types) throws Exception, NoSuchOperationException {
        // copy target into local variables from within a synchronized block to gaurentee a consistent read
        Object instance;
        OperationInvokerImpl operation;
        synchronized (this) {
            assureRunning();
            instance = serviceInstance;

            OperationSignature signature = new OperationSignature(operationName, types);
            Integer index = (Integer) operationIndex.get(signature);
            if (index == null) {
                throw new NoSuchOperationException("Unknown operation " + signature);
            }
            operation = operations[index.intValue()];
        }

        try {
            Object value = operation.fastMethod.invoke(instance, arguments);
            return value;
        } catch (InvocationTargetException e) {
            throw unwrap(e);
        }
    }

    public synchronized void assureRunning() {
        if (!serviceRunning) {
            try {
                updateState();
                if (!serviceRunning) {
                    throw new IllegalStateException("Service must be in the running or stopping state: name=" + name + ", state=" + ServiceState.fromIndex(kernel.getServiceState(name)));
                }
            } catch (ServiceNotFoundException e) {
                throw new IllegalStateException("Service is not loaded: " + name);
            }
        }
    }

    private synchronized void updateState() throws ServiceNotFoundException {
        if (!serviceInvokerStarted) {
            throw new IllegalStateException("Service invoker has not been started: name=" + name);
        }
        try {
            serviceRunning = false;

            // get the current state
            int serviceState = kernel.getServiceState(name);

            // we must be in a running state
            boolean running = serviceState == ServiceState.RUNNING_INDEX || serviceState == ServiceState.STOPPING_INDEX;
            if (running) {
                if (serviceInstance == null) {
                    serviceInstance = kernel.getService(name);

                    // if we don't have a service instance something is wrong
                    if (serviceInstance == null) {
                        throw new IllegalStateException("Could not get service instance: name=" + name);
                    }
                    createIndex(serviceInstance.getClass());
                }
                serviceRunning = true;
            }
        } finally {
            if (!serviceRunning) {
                kernel.removeLifecycleListener(lifecycleListener);
                serviceInvokerStarted = false;
                properties = null;
                propertyIndex.clear();
                operations = null;
                operationIndex.clear();
                serviceInstance = null;
            }
        }
    }

    private class ServiceInvokerLifecycleListener extends LifecycleAdapter {
        public void stopped(ObjectName objectName) {
            try {
                updateState();
            } catch (Exception e) {
                log.info("Unable to update service invoker for service " + name, e);
            }
        }

        public void unloaded(ObjectName objectName) {
            try {
                updateState();
            } catch (Exception e) {
                log.info("Unable to update service invoker for service " + name, e);
            }
        }
    }

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
                operationIndex.put(new OperationSignature(method), new Integer(operationList.size()));
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
            synchronized (ServiceInvoker.this) {
                assureRunning();
                instance = serviceInstance;
            }

            try {
                Object value = fastMethod.invoke(instance, arguments);
                return value;
            } catch (InvocationTargetException e) {
                throw unwrap(e);
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
            Object instance;
            synchronized (ServiceInvoker.this) {
                assureRunning();
                instance = serviceInstance;
            }

            if (!isReadable()) {
                throw new IllegalArgumentException("Property " + propertyName + " is not readable");
            }
            try {
                Object value = getterFastMethod.invoke(instance, null);
                return value;
            } catch (InvocationTargetException e) {
                throw unwrap(e);
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
            Object instance;
            synchronized (ServiceInvoker.this) {
                assureRunning();
                instance = serviceInstance;
            }

            if (!isWritable()) {
                throw new IllegalArgumentException("Property " + propertyName + " is not writable");
            }
            try {
                setterFastMethod.invoke(instance, new Object[] {value});
            } catch (InvocationTargetException e) {
                throw unwrap(e);
            }
        }

        public int hashCode() {
            return propertyName.hashCode();
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof PropertyInvokerImpl)) {
                return false;
            }
            PropertyInvokerImpl fastPropertyInvoker = (PropertyInvokerImpl)obj;
            return propertyName.equals(fastPropertyInvoker.propertyName);
        }

        public String toString() {
            return "[PropertySetter: name=" + propertyName + ", getter=" + getterSignature + ", setter=" + setterSignature + "]";
        }
    }
}
