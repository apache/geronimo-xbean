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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.geronimo.gbean.DynamicGBean;
import org.gbean.proxy.ProxyManager;
import org.gbean.service.ConfigurableServiceFactory;
import org.gbean.service.ServiceContext;
import org.gbean.spring.NamedValueHolder;
import org.gbean.spring.ServiceContextThreadLocal;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.context.support.GenericApplicationContext;

/**
 * @version $Revision$ $Date$
 */
public class GeronimoServiceFactory implements ConfigurableServiceFactory {
    private final Map dependencies = new HashMap();
    private final ProxyManager proxyManager;
    private GBeanDefinition gbeanDefinition;
    private GenericApplicationContext applicationContext;
    private Object service;

    public GeronimoServiceFactory(ProxyManager proxyManager, GBeanDefinition gbeanDefinition) {
        this.proxyManager = proxyManager;
        this.gbeanDefinition = gbeanDefinition;
        // add the dependencies
        String[] dependsOn = gbeanDefinition.getDependsOn();
        if (dependsOn != null) {
            for (int i = 0; i < dependsOn.length; i++) {
                String dependencyString = dependsOn[i];
                Map map = GeronimoUtil.stringToDependency(dependencyString);
                Map.Entry entry = ((Map.Entry) map.entrySet().iterator().next());
                String dependencyName = (String) entry.getKey();
                Set patterns = (Set) entry.getValue();
                dependencies.put(dependencyName, patterns);
            }
        }
    }

    public GBeanDefinition getGBeanDefinition() {
        GBeanDefinition gbeanDefinition = new GBeanDefinition(this.gbeanDefinition);
        if (service != null) {
            updatePersistentValues(service, gbeanDefinition);
        }
        return this.gbeanDefinition;
    }

    public void setGBeanDefinition(GBeanDefinition gbeanDefinition) {
        this.gbeanDefinition = gbeanDefinition;
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
            try {
                ServiceContextThreadLocal.set(serviceContext);

                applicationContext = new GenericApplicationContext();
                GBeanDefinition gbeanDefinition = new GBeanDefinition(this.gbeanDefinition);
                // todo remove the depends on usage stuff
                // clear the depends on flag since it is used to signal geronimo dependencies and not spring dependencies
                gbeanDefinition.setDependsOn(new String[0]);
                applicationContext.registerBeanDefinition(serviceContext.getObjectName(), gbeanDefinition);
                applicationContext.refresh();
                service = applicationContext.getBean(serviceContext.getObjectName());

                // add the properties
                MutablePropertyValues dynamicPropertyValues = gbeanDefinition.getDynamicPropertyValues();
                for (int i = 0; i < dynamicPropertyValues.getPropertyValues().length; i++) {
                    PropertyValue property = dynamicPropertyValues.getPropertyValues()[i];
                    String propertyName = property.getName();
                    Object propertyValue = property.getValue();
                    ((DynamicGBean) service).setAttribute(propertyName, propertyValue);
                }
            } finally {
                ServiceContextThreadLocal.set(oldServiceContext);
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
                GBeanDefinition gbeanDefinition = new GBeanDefinition(this.gbeanDefinition);
                updatePersistentValues(service, gbeanDefinition);
                this.gbeanDefinition = gbeanDefinition;
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
        return gbeanDefinition.isEnabled();
    }

    public void setEnabled(boolean enabled) {
        gbeanDefinition.setEnabled(enabled);
    }

    public Set getPropertyNames() {
        Set propertyNames = new HashSet();

        PropertyValue[] propertyValues = gbeanDefinition.getPropertyValues().getPropertyValues();
        for (int i = 0; i < propertyValues.length; i++) {
            PropertyValue propertyValue = propertyValues[i];
            propertyNames.add(propertyValue.getName());
        }

        PropertyValue[] dynamicPropertyValues = gbeanDefinition.getDynamicPropertyValues().getPropertyValues();
        for (int i = 0; i < dynamicPropertyValues.length; i++) {
            PropertyValue dynamicPropertyValue = dynamicPropertyValues[i];
            propertyNames.add(dynamicPropertyValue.getName());
        }

        Map indexedArgumentValues = gbeanDefinition.getConstructorArgumentValues().getIndexedArgumentValues();
        for (Iterator iterator = indexedArgumentValues.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            Integer index = (Integer) entry.getKey();
            ConstructorArgumentValues.ValueHolder valueHolder = (ConstructorArgumentValues.ValueHolder) entry.getValue();
            String propertyName = null;
            if (valueHolder instanceof NamedValueHolder) {
                propertyName = ((NamedValueHolder) valueHolder).getName();
            } else {
                propertyName = "constructor-argument-" + index;
            }
            propertyNames.add(propertyName);
        }
        return propertyNames;
    }

    public Object getProperty(String propertyName) {
        PropertyValue propertyValue = gbeanDefinition.getPropertyValues().getPropertyValue(propertyName);
        if (propertyValue != null) {
            return propertyValue.getValue();
        }

        propertyValue = gbeanDefinition.getDynamicPropertyValues().getPropertyValue(propertyName);
        if (propertyValue != null) {
            return propertyValue.getValue();
        }

        Map indexedArgumentValues = gbeanDefinition.getConstructorArgumentValues().getIndexedArgumentValues();
        for (Iterator iterator = indexedArgumentValues.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            ConstructorArgumentValues.ValueHolder valueHolder = (ConstructorArgumentValues.ValueHolder) entry.getValue();
            if (valueHolder instanceof NamedValueHolder) {
                if (propertyName.equals(((NamedValueHolder) valueHolder).getName())) {
                    return valueHolder.getValue();
                }
            }
        }

        throw new IllegalArgumentException("Property is not persistent:" +
                " propertyName=" + propertyName +
                ", serviceName: " + gbeanDefinition.getObjectName().getCanonicalName());
    }

    public void setProperty(String propertyName, Object persistentValue) {
        if (gbeanDefinition.getPropertyValues().contains(propertyName)) {
            gbeanDefinition.getPropertyValues().removePropertyValue(propertyName);
            gbeanDefinition.getPropertyValues().addPropertyValue(propertyName, persistentValue);
            return;
        }

        if (gbeanDefinition.getDynamicPropertyValues().contains(propertyName)) {
            gbeanDefinition.getDynamicPropertyValues().removePropertyValue(propertyName);
            gbeanDefinition.getDynamicPropertyValues().addPropertyValue(propertyName, persistentValue);
            return;
        }

        for (Iterator iterator = gbeanDefinition.getConstructorArgumentValues().getIndexedArgumentValues().values().iterator(); iterator.hasNext();) {
            ConstructorArgumentValues.ValueHolder valueHolder = (ConstructorArgumentValues.ValueHolder) iterator.next();
            if (valueHolder instanceof NamedValueHolder) {
                NamedValueHolder namedValueHolder = (NamedValueHolder) valueHolder;
                if (propertyName.equals(namedValueHolder.getName())) {
                    namedValueHolder.setValue(persistentValue);
                    return;
                }
            }
        }

        throw new IllegalArgumentException("Property is not persistent:" +
                " propertyName=" + propertyName +
                ", serviceName: " + gbeanDefinition.getObjectName().getCanonicalName());
    }

    private void updatePersistentValues(Object service, GBeanDefinition gbeanDefinition) {
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

        PropertyValue[] propertyValues = gbeanDefinition.getPropertyValues().getPropertyValues();
        for (int i = 0; i < propertyValues.length; i++) {
            String propertyName = propertyValues[i].getName();
            Object value = getCurrentValue(getters, service, propertyName, propertyValues[i].getValue());
            if (value != null) {
                gbeanDefinition.getPropertyValues().removePropertyValue(propertyName);
                gbeanDefinition.getPropertyValues().addPropertyValue(propertyName, value);
            }
        }

        PropertyValue[] dynamicPropertyValues = gbeanDefinition.getDynamicPropertyValues().getPropertyValues();
        for (int i = 0; i < dynamicPropertyValues.length; i++) {
            String propertyName = dynamicPropertyValues[i].getName();
            Object value = getCurrentValue(getters, service, propertyName, dynamicPropertyValues[i].getValue());
            if (value != null) {
                gbeanDefinition.getDynamicPropertyValues().removePropertyValue(propertyName);
                gbeanDefinition.getDynamicPropertyValues().addPropertyValue(propertyName, value);
            }
        }

        Map indexedArgumentValues = gbeanDefinition.getConstructorArgumentValues().getIndexedArgumentValues();
        for (Iterator iterator = indexedArgumentValues.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            ConstructorArgumentValues.ValueHolder valueHolder = (ConstructorArgumentValues.ValueHolder) entry.getValue();
            if (valueHolder instanceof NamedValueHolder) {
                String argName = ((NamedValueHolder) valueHolder).getName();
                Object value = getCurrentValue(getters, service, argName, valueHolder.getValue());
                if (value != null) {
                    valueHolder.setValue(value);
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
                    ", serviceName: " + gbeanDefinition.getObjectName().getCanonicalName(),
                    throwable);
        }

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
