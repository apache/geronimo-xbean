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
package org.gbean.geronimo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.ObjectName;

import org.apache.geronimo.gbean.GBeanLifecycleController;
import org.apache.geronimo.kernel.Kernel;
import org.gbean.metadata.ClassMetadata;
import org.gbean.metadata.ConstructorMetadata;
import org.gbean.metadata.MetadataManager;
import org.gbean.metadata.ParameterMetadata;
import org.gbean.proxy.ProxyManager;
import org.gbean.service.ConfigurableServiceFactory;
import org.gbean.service.ServiceContext;
import org.gbean.spring.LifecycleDetector;
import org.gbean.spring.NamedConstructorArgs;
import org.gbean.spring.ServiceContextThreadLocal;
import org.gbean.spring.SpringUtil;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;

/**
 * @version $Revision$ $Date$
 */
public class GeronimoServiceFactory implements ConfigurableServiceFactory {
    private final MetadataManager metadataManager;
    private final Set persistentProperties;
    private final ConstructorMetadata constructor;
    private final Map dependencies;
    private final Map dynamicProperties;
    private final ProxyManager proxyManager;
    private RootBeanDefinition beanDefinition;
    private GenericApplicationContext applicationContext;
    private Object service;
    private boolean enabled;
    private final List constructorArgNames;

    public GeronimoServiceFactory(RootBeanDefinition beanDefinition, Map dynamicProperties, MetadataManager metadataManager, ProxyManager proxyManager) throws Exception {
        this.metadataManager = metadataManager;
        this.proxyManager = proxyManager;
        this.beanDefinition = beanDefinition;
        this.dynamicProperties = dynamicProperties;
        dependencies = SpringUtil.extractDependencies(beanDefinition, Collections.EMPTY_MAP);

        // find the constructor... geronimo always uses the same constructor
        ClassMetadata classMetadata = metadataManager.getClassMetadata(beanDefinition.getBeanClass());
        ConstructorMetadata constructorMetadata = null;
        for (Iterator iterator = classMetadata.getConstructors().iterator(); iterator.hasNext();) {
            ConstructorMetadata c = (ConstructorMetadata) iterator.next();
            if (c.getProperties().containsKey("always-use")) {
                constructorMetadata = c;
                break;
            }
        }
        constructor = constructorMetadata;

        // determine the constructor arg names... these are alwasys defined in the gbean info
        List constructorArgNames = new LinkedList();
        for (Iterator iterator = constructor.getParameters().iterator(); iterator.hasNext();) {
            ParameterMetadata parameter = (ParameterMetadata) iterator.next();
            Object parameterName = parameter.get("name");
            if (parameterName == null) {
                throw new IllegalArgumentException("Parameter name is not defined");
            }
            constructorArgNames.add(parameterName);
        }
        this.constructorArgNames = Collections.unmodifiableList(constructorArgNames);

        // determine the persistent properties
        Set persistentProperties = (Set) classMetadata.get("persistentProperties");
        if (persistentProperties == null) {
            persistentProperties = Collections.EMPTY_SET;
        }
        this.persistentProperties = Collections.unmodifiableSet(persistentProperties);
    }

    public Set getPersistentProperties() {
        return persistentProperties;
    }

    public List getConstructorArgNames() {
        return constructorArgNames;
    }

    public Map getDynamicProperties() {
        return dynamicProperties;
    }

    public RootBeanDefinition getBeanDefinition() {
        RootBeanDefinition beanDefinition = new RootBeanDefinition(this.beanDefinition);
        if (service != null) {
            updatePersistentValues(service, beanDefinition);
        }
        return this.beanDefinition;
    }

    public void setBeanDefinition(RootBeanDefinition beanDefinition) {
        this.beanDefinition = beanDefinition;
    }

    public Map getDependencies() {
        return dependencies;
    }

    public void addDependency(String name, Set patterns) {
        dependencies.put(name, patterns);
    }

    public Object createService(ServiceContext serviceContext) throws Exception {
        try {
            Object service = null;
            ServiceContext oldServiceContext = ServiceContextThreadLocal.get();
            ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                ServiceContextThreadLocal.set(serviceContext);
                Thread.currentThread().setContextClassLoader(serviceContext.getClassLoader());

                // dereference all factories
                MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();
                PropertyValue[] values = propertyValues.getPropertyValues();
                for (int i = 0; i < values.length; i++) {
                    PropertyValue propertyValue = values[i];
                    if (propertyValue.getValue() instanceof FactoryBean) {
                        FactoryBean factoryBean = (FactoryBean) propertyValue.getValue();
                        Object object = factoryBean.getObject();
                        propertyValues.removePropertyValue(propertyValue.getName());
                        propertyValues.addPropertyValue(propertyValue.getName(), object);
                    }
                }

                applicationContext = new GenericApplicationContext();

                // register the post processors
                NamedConstructorArgs namedConstructorArgs = new NamedConstructorArgs(metadataManager);
                namedConstructorArgs.addDefaultValue("objectName", String.class, serviceContext.getObjectName());
                namedConstructorArgs.addDefaultValue("objectName", ObjectName.class, new ObjectName(serviceContext.getObjectName()));
                namedConstructorArgs.addDefaultValue("classLoader", ClassLoader.class, serviceContext.getClassLoader());
                namedConstructorArgs.addDefaultValue("gbeanLifecycleController", GBeanLifecycleController.class, new GeronimoLifecycleControllerReference().getObject());
                namedConstructorArgs.addDefaultValue("kernel", Kernel.class, serviceContext.getKernel().getService(Kernel.KERNEL));

                applicationContext.addBeanFactoryPostProcessor(namedConstructorArgs);
                LifecycleDetector lifecycleDetector = new LifecycleDetector();
                lifecycleDetector.addLifecycleInterface(org.apache.geronimo.gbean.GBeanLifecycle.class, "doStart", "doStop");
                applicationContext.addBeanFactoryPostProcessor(lifecycleDetector);
                applicationContext.getBeanFactory().addBeanPostProcessor(new DynamicGBeanProcessor(serviceContext.getObjectName(), dynamicProperties));

                // copy the bean definition, so we don't modify the original value
                RootBeanDefinition beanDefinition = new RootBeanDefinition(this.beanDefinition);

                // build the bean
                applicationContext.registerBeanDefinition(serviceContext.getObjectName(), beanDefinition);
                applicationContext.refresh();
                service = applicationContext.getBean(serviceContext.getObjectName());
            } finally {
                ServiceContextThreadLocal.set(oldServiceContext);
                Thread.currentThread().setContextClassLoader(oldClassLoader);
            }
            this.service = service;
            return service;
        } catch (Throwable t) {
            applicationContext = null;

            if (t instanceof Exception) {
                throw (Exception) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            } else {
                throw new Error(t);
            }
        }
    }

    public void destroyService(ServiceContext serviceContext, Object service) {
        // update the persistent attributes
        try {
            if (service != null) {
                // update the persistent values
                RootBeanDefinition beanDefinition = new RootBeanDefinition(this.beanDefinition);
                updatePersistentValues(service, beanDefinition);
                this.beanDefinition = beanDefinition;
            }
        } finally {
            this.service = null;
            if (applicationContext != null) {
                applicationContext.close();
                applicationContext = null;
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Set getPropertyNames() {
        if (persistentProperties != null) {
            return persistentProperties;
        } else {
            return Collections.EMPTY_SET;
        }
    }

    public Object getProperty(String propertyName) {
        if (persistentProperties == null || !persistentProperties.contains(propertyName)) {
            throw new IllegalArgumentException("Property is not persistent:" +
                    " propertyName=" + propertyName +
                    ", serviceType: " + beanDefinition.getBeanClassName());
        }

        PropertyValue propertyValue = beanDefinition.getPropertyValues().getPropertyValue(propertyName);
        if (propertyValue != null) {
            return propertyValue.getValue();
        }

        if (dynamicProperties.containsKey(propertyName)) {
            return dynamicProperties.get(propertyValue);
        }

        return null;
    }

    public void setProperty(String propertyName, Object persistentValue) {
        if (persistentProperties == null || !persistentProperties.contains(propertyName)) {
            throw new IllegalArgumentException("Property is not persistent:" +
                    " propertyName=" + propertyName +
                    ", serviceType: " + beanDefinition.getBeanClassName());
        }

        if (dynamicProperties.containsKey(propertyName)) {
            dynamicProperties.put(propertyName, persistentValue);
            return;
        }

        beanDefinition.getPropertyValues().removePropertyValue(propertyName);
        beanDefinition.getPropertyValues().addPropertyValue(propertyName, persistentValue);
    }

    private void updatePersistentValues(Object service, RootBeanDefinition beanDefinition) {
        Map getters = new HashMap();
        Method[] methods = service.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            String methodName = method.getName();
            if (Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())) {
                if (method.getParameterTypes().length == 0 && method.getReturnType() != Void.TYPE) {
                    if (methodName.length() > 3 && methodName.startsWith("get") && !methodName.equals("getClass")) {
                        String propertyName = fixPropertyName(methodName.substring(3));
                        getters.put(propertyName, method);
                    } else if (methodName.length() > 2 && methodName.startsWith("is")) {
                        String propertyName = fixPropertyName(methodName.substring(2));
                        getters.put(propertyName, method);
                    }
                }
            }
        }

        for (Iterator iterator = persistentProperties.iterator(); iterator.hasNext();) {
            String propertyName = (String) iterator.next();

            if (dynamicProperties.containsKey(propertyName)) {
                Object value = getCurrentValue(getters, service, propertyName, dynamicProperties.get(propertyName));
                dynamicProperties.put(propertyName, value);
            } else {
                // get the new property value
                Object value = null;
                PropertyValue propertyValue = beanDefinition.getPropertyValues().getPropertyValue(propertyName);
                if (propertyValue != null) {
                    value = propertyValue.getValue();
                }
                value = getCurrentValue(getters, service, propertyName, value);

                // update the property value
                beanDefinition.getPropertyValues().removePropertyValue(propertyName);
                if (value != null) {
                    beanDefinition.getPropertyValues().addPropertyValue(propertyName, value);
                }
            }
        }
    }

    private Object getCurrentValue(Map getters, Object service, String propertyName, Object defaultValue) {
        Object value = defaultValue;
        try {
            Method getter = (Method) getters.get(propertyName);
            if (getter != null) {
                value = getter.invoke(service, null);
            }
        } catch (Throwable throwable) {
            while (throwable instanceof InvocationTargetException) {
                throwable = ((InvocationTargetException) throwable).getTargetException();
            }

            throw new RuntimeException("Problem while obtaining the currennt persistent value of property: " +
                    "propertyName=" + propertyName +
                    ", serviceType: " + beanDefinition.getBeanClassName(),
                    throwable);
        }

        // we should never get a bean definition holder
        // we don't support them sice it is not serizlizable
        if (value instanceof BeanDefinitionHolder) {
            throw new IllegalArgumentException("Got a bean definition holder");
        }

        // turn proxies back into the factory bean
        // the factory bean is serizlizable
        if (value instanceof FactoryBeanProvider) {
            value = ((FactoryBeanProvider) value).getFactoryBean();
        } else {
            Object proxyData = proxyManager.getProxyData(value);
            if (proxyData instanceof FactoryBeanProvider) {
                value = ((FactoryBeanProvider) proxyData).getFactoryBean();
            } else if (proxyData instanceof FactoryBean) {
                value = proxyData;
            }
        }
        return value;
    }

    private static String fixPropertyName(String propertyName) {
        if (Character.isUpperCase(propertyName.charAt(0))) {
            return Character.toLowerCase(propertyName.charAt(0)) + propertyName.substring(1);
        }
        return propertyName;
    }
}
