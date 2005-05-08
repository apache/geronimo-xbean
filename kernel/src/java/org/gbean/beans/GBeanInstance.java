/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
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

package org.gbean.beans;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.gbean.DynamicGBean;
import org.apache.geronimo.gbean.GBeanLifecycle;
import org.apache.geronimo.gbean.GOperationSignature;
import org.apache.geronimo.gbean.InvalidConfigurationException;
import org.apache.geronimo.kernel.DependencyManager;
import org.apache.geronimo.kernel.GBeanNotFoundException;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.kernel.NoSuchAttributeException;
import org.apache.geronimo.kernel.NoSuchOperationException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.support.GenericApplicationContext;
import org.gbean.kernel.GBeanProxyManager;
import net.sf.cglib.reflect.FastMethod;
import net.sf.cglib.reflect.FastClass;

/**
 * A GBeanInstance is a J2EE Management Managed Object, and is standard base for Geronimo services.
 *
 * @version $Rev: 106387 $ $Date: 2004-11-23 22:16:54 -0800 (Tue, 23 Nov 2004) $
 */
public final class GBeanInstance {
    private static final Log log = LogFactory.getLog(GBeanInstance.class);

    private static final int DESTROYED = 0;
    private static final int CREATING = 1;
    private static final int RUNNING = 2;
    private static final int DESTROYING = 3;

    /**
     * Attribute name used to retrieve the RawInvoker for the GBean
     */
    public static final String RAW_INVOKER = "$$RAW_INVOKER$$";

    /**
     * The kernel in which this server is registered.
     */
    private final Kernel kernel;

    /**
     * The spring bean definition for this instance
     */
    private GeronimoBeanDefinition beanDefinition;

    /**
     * This handles all state transiitions for this instance.
     */
    private final GBeanInstanceState gbeanInstanceState;

    /**
     * A fast index based raw invoker for this GBean.
     */
    private final RawInvoker rawInvoker;

    /**
     * The single listener to which we broadcast lifecycle change events.
     */
    private final LifecycleBroadcaster lifecycleBroadcaster;

    /**
     * The context given to the instance
     */
    private final GBeanContext geronimoBeanContext;

    /**
     * Property getters
     */
    private PropertyGetter[] getters;

    /**
     * Property getter name to index number
     */
    private final Map getterIndex = new HashMap();

    /**
     * Property setters
     */
    private PropertySetter[] setters;

    /**
     * Property setter name to index number
     */
    private final Map setterIndex = new HashMap();

    /**
     * Operations lookup table
     */
    private MethodInvoker[] operations;

    /**
     * Operations supported by this GBeanMBean by (GOperationSignature) name.
     */
    private final Map operationIndex = new HashMap();

    /**
     * The classloader used for all invocations and creating targets.
     */
    private final ClassLoader classLoader;

    /**
     * Has this instance been destroyed?
     */
    private boolean dead = false;

    /**
     * The state of the internal gbean instance that we are wrapping.
     */
    private int instanceState = DESTROYED;

    /**
     * Target instance of this GBean wrapper
     */
    private Object target;

    /**
     * The time this application started.
     */
    private long startTime;

    private Set dependencies;

    private static String fixAttributeName(String attributeName) {
        if (Character.isUpperCase(attributeName.charAt(0))) {
            return Character.toLowerCase(attributeName.charAt(0)) + attributeName.substring(1);
        }
        return attributeName;
    }

    public GBeanInstance(GeronimoBeanDefinition beanDefinition,
            Kernel kernel,
            DependencyManager dependencyManager,
            LifecycleBroadcaster lifecycleBroadcaster,
            ClassLoader classLoader) throws InvalidConfigurationException {

        this.beanDefinition = new GeronimoBeanDefinition(beanDefinition);

        // add the dependencies
        Set tempDependencies = new HashSet();
        String[] dependsOn = beanDefinition.getDependsOn();
        for (int i = 0; i < dependsOn.length; i++) {
            String dependencyString = dependsOn[i];
            Map map = GBeanInstanceUtil.stringToDependency(dependencyString);
            Map.Entry entry = ((Map.Entry) map.entrySet().iterator().next());
            String dependencyName = (String) entry.getKey();
            Set patterns = (Set) entry.getValue();
            tempDependencies.add(new GBeanDependency(this, dependencyName, patterns, kernel, dependencyManager));
        }
        this.dependencies = Collections.unmodifiableSet(tempDependencies);

        this.kernel = kernel;
        this.lifecycleBroadcaster = lifecycleBroadcaster;
        this.gbeanInstanceState = new GBeanInstanceState(beanDefinition.getObjectName(), kernel, dependencyManager, this, lifecycleBroadcaster);
        this.classLoader = classLoader;

        geronimoBeanContext = new GeronimoBeanInstanceContext(this);

        // create the raw invokers
        rawInvoker = new RawInvoker(this);

        // we are now officially open for business
        lifecycleBroadcaster.fireLoadedEvent();
    }

    public void die() throws GBeanNotFoundException {
        synchronized (this) {
            if (dead) {
                // someone beat us to the punch... this instance should have never been found in the first place
                throw new GBeanNotFoundException(beanDefinition.getObjectName().getCanonicalName());
            }
            dead = true;
        }

        // if the bean is already stopped or failed, this will do nothing; otherwise it will shutdown the bean
        gbeanInstanceState.stop();

        // tell everyone we are done
        lifecycleBroadcaster.fireUnloadedEvent();

        rawInvoker.destroy();
    }

    /**
     * The kernel in which this instance is mounted
     * @return the kernel
     */
    public Kernel getKernel() {
        return kernel;
    }

    /**
     * The class loader used to build this gbean.  This class loader is set into the thread context
     * class loader before callint the target instace.
     *
     * @return the class loader used to build this gbean
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public synchronized Object getTarget() {
        return target;
    }

    /**
     * Has this gbean instance been destroyed. An destroyed gbean can no longer be used.
     *
     * @return true if the gbean has been destroyed
     */
    public synchronized boolean isDead() {
        return dead;
    }

    public final String getObjectName() {
        return beanDefinition.getObjectName().getCanonicalName();
    }

    public final ObjectName getObjectNameObject() {
        return beanDefinition.getObjectName();
    }

    public GeronimoBeanDefinition getGeronimoBeanDefinition() {
        GeronimoBeanDefinition geronimoBeanDefinition = new GeronimoBeanDefinition(beanDefinition);
        updatePersistentValues(geronimoBeanDefinition, false);
        return geronimoBeanDefinition;
    }

    /**
     * Is this gbean enabled.  A disabled gbean can not be started.
     *
     * @return true if the gbean is enabled and can be started
     */
    public synchronized final boolean isEnabled() {
        return beanDefinition.isEnabled();
    }

    /**
     * Changes the enabled status.
     *
     * @param enabled the new enabled flag
     */
    public synchronized final void setEnabled(boolean enabled) {
        beanDefinition.setEnabled(enabled);
    }

    public synchronized final long getStartTime() {
        return startTime;
    }

    public int getState() {
        return gbeanInstanceState.getState();
    }


    public Map getGetterIndex() {
        return Collections.unmodifiableMap(getterIndex);
    }

    public Map getSetterIndex() {
        return Collections.unmodifiableMap(setterIndex);
    }

    /**
     * Gets an unmodifiable map from operation signature (GOperationSignature) to index number (Integer).
     * This index number can be used to efficciently invoke the operation.
     *
     * @return an unmodifiable map of operation indexec by signature
     */
    public Map getOperationIndex() {
        return Collections.unmodifiableMap(new HashMap(operationIndex));
    }

    /**
     * Moves this GBeanInstance to the starting state and then attempts to move this MBean immediately
     * to the running state.
     *
     * @throws IllegalStateException If the gbean is disabled
     */
    public final void start() {
        synchronized (this) {
            if (dead) {
                throw new IllegalStateException("A dead GBean can not be started: objectName=" + beanDefinition.getObjectName());
            }
            if (!isEnabled()) {
                throw new IllegalStateException("A disabled GBean can not be started: objectName=" + beanDefinition.getObjectName());
            }
        }
        gbeanInstanceState.start();
    }

    /**
     * Starts this GBeanInstance and then attempts to start all of its start dependent children.
     *
     * @throws IllegalStateException If the gbean is disabled
     */
    public final void startRecursive() {
        synchronized (this) {
            if (dead) {
                throw new IllegalStateException("A dead GBean can not be started: objectName=" + beanDefinition.getObjectName());
            }
            if (!isEnabled()) {
                throw new IllegalStateException("A disabled GBean can not be started: objectName=" + beanDefinition.getObjectName());
            }
        }
        gbeanInstanceState.startRecursive();
    }

    /**
     * Moves this GBeanInstance to the STOPPING state, calls stop on all start dependent children, and then attempt
     * to move this MBean to the STOPPED state.
     */
    public final void stop() {
        gbeanInstanceState.stop();
    }

    private void updatePersistentValues(GeronimoBeanDefinition geronimoBeanDefinition, boolean ignoreErrors) {
        // copy target into local variables from within a synchronized block to gaurentee a consistent read
        int state;
        Object instance;
        synchronized (this) {
            state = instanceState;
            instance = target;
        }

        if (state == DESTROYED) {
            return;
        }

        PropertyValue[] propertyValues = geronimoBeanDefinition.getPropertyValues().getPropertyValues();
        for (int i = 0; i < propertyValues.length; i++) {
            String propertyName = propertyValues[i].getName();
            Object value = getCurrentValue(instance, propertyName, ignoreErrors, propertyValues[i].getValue());
            if (value != null) {
                geronimoBeanDefinition.getPropertyValues().removePropertyValue(propertyName);
                geronimoBeanDefinition.getPropertyValues().addPropertyValue(propertyName, value);
            }
        }

        PropertyValue[] dynamicPropertyValues = geronimoBeanDefinition.getDynamicPropertyValues().getPropertyValues();
        for (int i = 0; i < dynamicPropertyValues.length; i++) {
            String propertyName = dynamicPropertyValues[i].getName();
            Object value = getCurrentValue(instance, propertyName, ignoreErrors, dynamicPropertyValues[i].getValue());
            if (value != null) {
                geronimoBeanDefinition.getDynamicPropertyValues().removePropertyValue(propertyName);
                geronimoBeanDefinition.getDynamicPropertyValues().addPropertyValue(propertyName, value);
            }
        }

        Map indexedArgumentValues = beanDefinition.getConstructorArgumentValues().getIndexedArgumentValues();
        for (Iterator iterator = indexedArgumentValues.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            ConstructorArgumentValues.ValueHolder valueHolder = (ConstructorArgumentValues.ValueHolder) entry.getValue();
            if (valueHolder instanceof NamedValueHolder) {
                String argName = ((NamedValueHolder)valueHolder).getName();
                Object value = getCurrentValue(instance, argName, ignoreErrors, valueHolder.getValue());
                if (value != null) {
                    valueHolder.setValue(value);
                }
            }
        }
    }

    private Object getCurrentValue(Object instance, String propertyName, boolean ignoreErrors, Object defaultValue) {
        Object value = defaultValue;
        try {
            Integer index = (Integer) getterIndex.get(propertyName);
            if (index != null) {
                value = getters[index.intValue()].invoke(instance);
            }
        } catch (Throwable throwable) {
            if (ignoreErrors) {
                log.debug("Could not get the current value of persistent property.  The persistent " +
                        "propertyName=" + propertyName +
                        ", gbeanInstance: " + beanDefinition.getObjectName().getCanonicalName(),
                        throwable);
            } else {
                throw new RuntimeException("Problem while obtaining the currennt persistent value of property: " +
                        "propertyName=" + propertyName +
                        ", gbeanInstance: " + beanDefinition.getObjectName().getCanonicalName(),
                        throwable);
            }

        }

        if (value instanceof FactoryBeanProvider) {
            value = ((FactoryBeanProvider) value).getFactoryBean();
        } else {
            GBeanProxyManager proxyManager = (GBeanProxyManager) kernel.getProxyManager();
            Object proxyData = proxyManager.getProxyData(value);
            if (proxyData instanceof FactoryBeanProvider) {
                value = ((FactoryBeanProvider) proxyData).getFactoryBean();
            } else if (proxyData instanceof FactoryBean) {
                value = proxyData;
            }
        }
        return value;
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
        int state;
        Object instance;
        synchronized (this) {
            state = instanceState;
            instance = target;
        }

        Object attributeValue;
        PropertyGetter getter = getters[index];
        if (state == DESTROYED) {
            attributeValue = getPersistentPropertyValue(getter.getPropertyName());
        } else {
            Object value = getter.invoke(instance);
            return value;
        }
        return attributeValue;
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
        int state;
        Object instance;
        synchronized (this) {
            state = instanceState;
            instance = target;
        }

        Object attributeValue;
        if (state == DESTROYED) {
            if (name.equals(RAW_INVOKER)) {
                return rawInvoker;
            }
            attributeValue = getPersistentPropertyValue(name);
        } else {
            Integer index = (Integer) getterIndex.get(name);
            if (index != null) {
                attributeValue = getters[index.intValue()].invoke(instance);
            } else if (name.equals(RAW_INVOKER)) {
                return rawInvoker;
            } else if (instance instanceof DynamicGBean) {
                attributeValue = ((DynamicGBean) instance).getAttribute(name);
            } else {
                throw new NoSuchAttributeException("Unknown property " + name + " in gbean " + beanDefinition.getObjectName());
            }
        }
        return attributeValue;
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
        int state;
        Object instance;
        synchronized (this) {
            state = instanceState;
            instance = target;
        }

        if (state == DESTROYED) {
            setPersistentPropertyValue(setters[index].getPropertyName(), value);
        } else {
            setters[index].invoke(instance, value);
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
        int state;
        Object instance;
        synchronized (this) {
            state = instanceState;
            instance = target;
        }

        if (state == DESTROYED) {
            setPersistentPropertyValue(attributeName, attributeValue);
        } else {
            Integer index = (Integer) setterIndex.get(attributeName);
            if (index != null) {
                setters[index.intValue()].invoke(instance, attributeValue);
            } else if (instance instanceof DynamicGBean) {
                ((DynamicGBean) instance).setAttribute(attributeName, attributeValue);
            } else {
                throw new NoSuchAttributeException("Unknown attribute " + attributeName + " in gbean " + beanDefinition.getObjectName());
            }
        }
    }

    private Object getPersistentPropertyValue(String propertyName) {
        PropertyValue propertyValue = beanDefinition.getPropertyValues().getPropertyValue(propertyName);
        if (propertyValue != null) {
            return propertyValue.getValue();
        }

        propertyValue = beanDefinition.getDynamicPropertyValues().getPropertyValue(propertyName);
        if (propertyValue != null) {
            return propertyValue.getValue();
        }

        Map indexedArgumentValues = beanDefinition.getConstructorArgumentValues().getIndexedArgumentValues();
        for (Iterator iterator = indexedArgumentValues.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            ConstructorArgumentValues.ValueHolder valueHolder = (ConstructorArgumentValues.ValueHolder) entry.getValue();
            if (valueHolder instanceof NamedValueHolder) {
                if (propertyName.equals(((NamedValueHolder)valueHolder).getName())) {
                    return valueHolder.getValue();
                }
            }
        }

        throw new IllegalArgumentException("Property is not persistent:" +
                " propertyName=" + propertyName +
                ", gbeanName: " + beanDefinition.getObjectName().getCanonicalName());
    }

    private void setPersistentPropertyValue(String propertyName, Object persistentValue) {
        if (beanDefinition.getPropertyValues().contains(propertyName)) {
            beanDefinition.getPropertyValues().removePropertyValue(propertyName);
            beanDefinition.getPropertyValues().addPropertyValue(propertyName, persistentValue);
            return;
        }

        if (beanDefinition.getDynamicPropertyValues().contains(propertyName)) {
            beanDefinition.getDynamicPropertyValues().removePropertyValue(propertyName);
            beanDefinition.getDynamicPropertyValues().addPropertyValue(propertyName, persistentValue);
            return;
        }

        for (Iterator iterator = beanDefinition.getConstructorArgumentValues().getIndexedArgumentValues().values().iterator(); iterator.hasNext();) {
            ConstructorArgumentValues.ValueHolder valueHolder = (ConstructorArgumentValues.ValueHolder) iterator.next();
            if (valueHolder instanceof NamedValueHolder) {
                NamedValueHolder namedValueHolder = (NamedValueHolder)valueHolder;
                if (propertyName.equals(namedValueHolder.getName())) {
                    namedValueHolder.setValue(persistentValue);
                    return;
                }
            }
        }

        throw new IllegalArgumentException("Property is not persistent:" +
                " propertyName=" + propertyName +
                ", gbeanName: " + beanDefinition.getObjectName().getCanonicalName());
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
        int state;
        Object instance;
        synchronized (this) {
            state = instanceState;
            instance = target;
        }

        if (state == DESTROYED) {
            throw new IllegalStateException("Operations can only be invoke while the GBean instance is running: " + beanDefinition.getObjectName());
        }

        MethodInvoker operation = operations[index];
        return operation.invoke(instance, arguments);
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
        int state;
        Object instance;
        synchronized (this) {
            state = instanceState;
            instance = target;
        }

        if (state == DESTROYED) {
            throw new IllegalStateException("Operations can only be invoke while the GBean is running: " + beanDefinition.getObjectName());
        }

        GOperationSignature signature = new GOperationSignature(operationName, types);
        Integer index = (Integer) operationIndex.get(signature);
        if (index == null) {
            throw new NoSuchOperationException("Unknown operation " + signature);
        }
        MethodInvoker operation = operations[index.intValue()];
        return operation.invoke(instance, arguments);
    }

    boolean createInstance() throws Exception {
        synchronized (this) {
            // first check we are still in the correct state to start
            if (instanceState == CREATING || instanceState == RUNNING) {
                // another thread already completed starting
                return false;
            } else if (instanceState == DESTROYING) {
                // this should never ever happen... this method is protected by the GBeanState class which should
                // prevent stuff like this happening, but check anyway
                throw new IllegalStateException("A stopping instance can not be started until fully stopped");
            }
            assert instanceState == DESTROYED;

            // Call all start on every reference.  This way the dependecies are held until we can start
            boolean allStarted = true;
            for (Iterator iterator = dependencies.iterator(); iterator.hasNext();) {
                GBeanDependency dependency = (GBeanDependency) iterator.next();
                allStarted = dependency.start() && allStarted;
            }
            if (!allStarted) {
                return false;
            }

            // we are definately going to (try to) start... if this fails the must clean up these variables
            instanceState = CREATING;
            startTime = System.currentTimeMillis();
        }

        Object instance = null;
        try {
            GBeanContext oldBeanContext = (GBeanContext) GBeanContext.threadLocal.get();
            try {
                GBeanContext.threadLocal.set(geronimoBeanContext);
                GenericApplicationContext applicationContext = new GenericApplicationContext();
                GeronimoBeanDefinition beanDefinition = new GeronimoBeanDefinition(this.beanDefinition) ;
                // todo remove the depends on usage stuff
                // clear the depends on flag since it is used to signal geronimo dependencies and not spring dependencies
                beanDefinition.setDependsOn(new String[0]);
                applicationContext.registerBeanDefinition(getObjectName(), beanDefinition);
                instance = applicationContext.getBean(getObjectName());

                createIndex(instance.getClass());

                // add the properties
                MutablePropertyValues dynamicPropertyValues = beanDefinition.getDynamicPropertyValues();
                for (int i = 0; i < dynamicPropertyValues.getPropertyValues().length; i++) {
                    PropertyValue property = dynamicPropertyValues.getPropertyValues()[i];
                    String propertyName = property.getName();
                    Object propertyValue = property.getValue();
                    ((DynamicGBean) instance).setAttribute(propertyName, propertyValue);
                }
            } finally {
                GBeanContext.threadLocal.set(oldBeanContext);
            }

            // all done... we are now fully running
            synchronized (this) {
                target = instance;
                instanceState = RUNNING;
                this.notifyAll();
            }

            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            // something went wrong... we need to destroy this instance
            synchronized (this) {
                instanceState = DESTROYING;
            }

            // todo remove dostop
            if (instance instanceof GBeanLifecycle) {
                try {
                    ((GBeanLifecycle) instance).doStop();
                } catch (Throwable ignored) {
                    log.error("Problem in doStop of " + getObjectName(), ignored);
                }
            }

            // bean has been notified... drop our reference
            synchronized (this) {
                for (Iterator iterator = dependencies.iterator(); iterator.hasNext();) {
                    GBeanDependency dependency = (GBeanDependency) iterator.next();
                    dependency.stop();
                }
                target = null;
                instanceState = DESTROYED;
                startTime = 0;
                this.notifyAll();
            }

            if (t instanceof Exception) {
                throw (Exception) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            } else {
                throw new Error(t);
            }
        }
    }

    boolean destroyInstance() throws Exception {
        Object instance;
        synchronized (this) {
            // if the instance is being created we need to wait
            //  for it to finish before we can try to stop it
            while (instanceState == CREATING) {
                // todo should we limit this wait?  If so, how do we configure the wait time?
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    // clear the interrupted flag
                    Thread.interrupted();
                    // rethrow the interrupted exception.... someone was sick of us waiting
                    throw e;
                }
            }

            if (instanceState == DESTROYING || instanceState == DESTROYED) {
                // another thread is already stopping or has already stopped
                return false;
            }
            assert instanceState == RUNNING;

            // we are definately going to stop... if this fails we must clean up these variables
            instanceState = DESTROYING;
            instance = target;
        }

        // update the persistent attributes
        try {
            if (instance != null) {
                // update the persistent values
                GeronimoBeanDefinition geronimoBeanDefinition = new GeronimoBeanDefinition(beanDefinition);
                updatePersistentValues(geronimoBeanDefinition, false);
                beanDefinition = geronimoBeanDefinition;
            }
        } finally {
            try {
                // we notify the bean before removing our reference so the references can be called back while stopping
                if (instance instanceof GBeanLifecycle) {
                    try {
                        ((GBeanLifecycle) instance).doStop();
                    } catch (Throwable ignored) {
                        log.error("Problem in doStop of " + beanDefinition.getObjectName(), ignored);
                    }
                }
            } finally {
                // bean has been notified... drop the reference
                synchronized (this) {
                    for (Iterator iterator = dependencies.iterator(); iterator.hasNext();) {
                        GBeanDependency dependency = (GBeanDependency) iterator.next();
                        dependency.stop();
                    }
                    target = null;
                    instanceState = DESTROYED;
                    startTime = 0;
                }
            }
        }
        return true;
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

        List getterList = new ArrayList(getterMap.size());
        for (Iterator iterator = getterMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String propertyName = (String) entry.getKey();
            Method getter = (Method) entry.getValue();
            getterIndex.put(propertyName, new Integer(getterList.size()));
            getterList.add(new PropertyGetter(propertyName, getter));
        }
        getters = (PropertyGetter[]) getterList.toArray(new PropertyGetter[getterList.size()]);

        List setterList = new ArrayList(setterMap.size());
        for (Iterator iterator = setterMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String propertyName = (String) entry.getKey();
            Method setter = (Method) entry.getValue();
            setterIndex.put(propertyName, new Integer(setterList.size()));
            setterList.add(new PropertySetter(propertyName,setter));
        }
        setters = (PropertySetter[]) setterList.toArray(new PropertySetter[setterList.size()]);

        // operations
        List operationList = new ArrayList(methods.length);
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())) {
                operationIndex.put(new GOperationSignature(method), new Integer(operationList.size()));
                operationList.add(new MethodInvoker(method));
            }
        }
        operations = (MethodInvoker[]) operationList.toArray(new MethodInvoker[operationList.size()]);
    }

    private static final class MethodInvoker {
        private final Method method;
        private final FastMethod fastMethod;

        public MethodInvoker(Method method) {
            this.method = method;
            this.fastMethod = FastClass.create(method.getDeclaringClass()).getMethod(method);
        }

        public Method getMethod() {
            return method;
        }

        public Object invoke(Object target, Object[] arguments) throws Exception {
            try {
                return fastMethod.invoke(target, arguments);
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

    private static class PropertyGetter {
        private final String propertyName;
        private final Method method;
        private final FastMethod fastMethod;

        public PropertyGetter(String propertyName, Method method) {
            this.propertyName = propertyName;
            this.method = method;
            this.fastMethod = FastClass.create(method.getDeclaringClass()).getMethod(method);
        }

        public String getPropertyName() {
            return propertyName;
        }

        public Method getMethod() {
            return method;
        }

        public Object invoke(Object target) throws Exception {
            try {
                return fastMethod.invoke(target, null);
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

    private static class PropertySetter {
        private final String propertyName;
        private final Method method;
        private final FastMethod fastMethod;

        public PropertySetter(String propertyName, Method method) {
            this.propertyName = propertyName;
            this.method = method;
            this.fastMethod = FastClass.create(method.getDeclaringClass()).getMethod(method);
        }

        public String getPropertyName() {
            return propertyName;
        }

        public Method getMethod() {
            return method;
        }

        public void invoke(Object target, Object value) throws Exception {
            try {
                fastMethod.invoke(target, new Object[] {value});
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

    private static final class GeronimoBeanInstanceContext implements GBeanContext {
        /**
         * The GeronimoInstance which owns the target.
         */
        private final GBeanInstance gbeanInstance;

        /**
         * Creates a new context for a target.
         *
         * @param gbeanInstance the GeronimoInstance
         */
        public GeronimoBeanInstanceContext(GBeanInstance gbeanInstance) {
            this.gbeanInstance = gbeanInstance;
        }

        public String getObjectName() {
            return gbeanInstance.getObjectName();
        }

        public Kernel getKernel() {
            return gbeanInstance.getKernel();
        }

        public ClassLoader getClassLoader() {
            return gbeanInstance.getClassLoader();
        }

        public int getState() {
            return gbeanInstance.getState();
        }

        public void stop() throws Exception {
            synchronized (gbeanInstance) {
                if (gbeanInstance.instanceState == CREATING) {
                    throw new IllegalStateException("Stop can not be called until instance is fully started");
                } else if (gbeanInstance.instanceState == DESTROYING) {
                    log.debug("Stop ignored.  GBean is already being stopped");
                    return;
                } else if (gbeanInstance.instanceState == DESTROYED) {
                    log.debug("Stop ignored.  GBean is already stopped");
                    return;
                }
            }
            gbeanInstance.stop();
        }
    }

    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof GBeanInstance == false) return false;
        return beanDefinition.getObjectName().equals(((GBeanInstance) obj).beanDefinition.getObjectName());
    }

    public int hashCode() {
        return beanDefinition.getObjectName().hashCode();
    }

    public String toString() {
        return beanDefinition.getObjectName().getCanonicalName();
    }
}
