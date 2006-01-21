/**
 * 
 * Copyright 2005-2006 The Apache Software Foundation or its licensors,  as applicable.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 * 
 **/
package org.apache.xbean.sca;

import org.osoa.sca.annotations.ComponentMetaData;
import org.osoa.sca.annotations.ComponentName;
import org.osoa.sca.annotations.Context;
import org.osoa.sca.annotations.Destroy;
import org.osoa.sca.annotations.Init;
import org.osoa.sca.annotations.Property;
import org.osoa.sca.annotations.Reference;
import org.osoa.sca.annotations.SessionID;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.apache.xbean.sca.impl.DefaultScaAdapter;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Performs SCA based dependency injection rules.
 * 
 * @version $Revision$
 */
public class ScaBeanPostProcessor extends IntrospectionSupport implements DestructionAwareBeanPostProcessor, BeanFactoryPostProcessor {

    private ScaAdapter scaAdapter;

    public ScaBeanPostProcessor() {
    }

    public ScaBeanPostProcessor(ScaAdapter scaAdapter) {
        this.scaAdapter = scaAdapter;
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        List methods = findMethodsWithAnnotation(bean, Init.class);
        invokeVoidMethods(bean, methods);
        processFields(bean, beanName);
        processProperties(bean, beanName);
        return bean;
    }

    public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
        List methods = findMethodsWithAnnotation(bean, Destroy.class);
        invokeVoidMethods(bean, methods);
    }

    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        String[] beanNames = beanFactory.getBeanDefinitionNames();
        for (int i = 0; i < beanNames.length; i++) {
            String beanName = beanNames[i];
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            postProcessBeanDefinition(beanFactory, beanName, beanDefinition);
        }
    }

    // Properties
    // -------------------------------------------------------------------------

    public ScaAdapter getScaAdapter() {
        if (scaAdapter == null) {
            scaAdapter = createScaAdapter();
        }
        return scaAdapter;
    }

    public void setScaAdapter(ScaAdapter scaAdapter) {
        this.scaAdapter = scaAdapter;
    }

    // Implementation methods
    // -------------------------------------------------------------------------
    protected void postProcessBeanDefinition(ConfigurableListableBeanFactory beanFactory, String beanName, BeanDefinition definition) throws BeansException {
        Class type = beanFactory.getType(beanName);
        BeanInfo beanInfo = getBeanInfo(type);
        PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();
        for (int i = 0; i < descriptors.length; i++) {
            PropertyDescriptor descriptor = descriptors[i];
            processProperty(beanName, definition, descriptor);
        }
    }

    protected void processProperty(String beanName, BeanDefinition definition, PropertyDescriptor descriptor) throws BeansException {
        Method method = descriptor.getWriteMethod();
        if (method != null) {
            // TODO should we handle the property.name() attribute?
            // maybe add this to XBean code generator...

            Property property = method.getAnnotation(Property.class);
            if (property != null) {
                if (property.required()) {
                    // TODO use property.name()?
                    String propertyName = descriptor.getName();
                    MutablePropertyValues propertyValues = definition.getPropertyValues();
                    if (!propertyValues.contains(propertyName)) {
                        throw new BeanInitializationException("Mandatory property: " + propertyName + " not specified on bean: " + beanName);
                    }
                }
            }

            Reference reference = method.getAnnotation(Reference.class);
            if (reference != null) {
                if (reference.required()) {
                    // TODO use reference.name()?
                    String propertyName = descriptor.getName();
                    MutablePropertyValues propertyValues = definition.getPropertyValues();
                    if (!propertyValues.contains(propertyName)) {
                        throw new BeanInitializationException("Mandatory reference: " + propertyName + " not specified on bean: " + beanName);
                    }
                }
            }
        }
    }

    protected void processProperties(Object bean, String beanName) throws BeansException {
        BeanInfo beanInfo = getBeanInfo(bean.getClass());
        PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();
        for (int i = 0; i < descriptors.length; i++) {
            PropertyDescriptor descriptor = descriptors[i];
            processProperty(bean, beanName, descriptor);
        }
    }

    protected void processProperty(Object bean, String beanName, PropertyDescriptor descriptor) throws BeansException {
        Method method = descriptor.getWriteMethod();
        if (method != null) {
            if (hasAnnotation(method, ComponentName.class)) {
                Object[] arguments = new Object[] { beanName };
                invokeMethod(bean, method, arguments);
            }
            if (hasAnnotation(method, ComponentMetaData.class)) {
                Object[] arguments = new Object[] { getScaAdapter().getComponentMetaData(bean, beanName) };
                invokeMethod(bean, method, arguments);
            }
            if (hasAnnotation(method, Context.class)) {
                Object[] arguments = new Object[] { getScaAdapter().getComponentContext(bean, beanName) };
                invokeMethod(bean, method, arguments);
            }
            if (hasAnnotation(method, SessionID.class)) {
                Object[] arguments = new Object[] { getScaAdapter().getBeanSessionID(bean, beanName) };
                invokeMethod(bean, method, arguments);
            }
        }
    }

    protected void processFields(Object bean, String beanName) throws BeansException {
        Class type = bean.getClass();
        while (true) {
            Field[] declaredFields = type.getDeclaredFields();
            for (int i = 0; i < declaredFields.length; i++) {
                Field field = declaredFields[i];
                processField(bean, beanName, field);
            }
            if (type.equals(Object.class)) {
                break;
            }
            else {
                type = type.getSuperclass();
            }
        }
    }

    protected void processField(Object bean, String beanName, Field field) {
        if (hasAnnotation(field, ComponentName.class)) {
            setField(bean, field, beanName);
        }
        if (hasAnnotation(field, ComponentMetaData.class)) {
            setField(bean, field, getScaAdapter().getComponentMetaData(bean, beanName));
        }
        if (hasAnnotation(field, Context.class)) {
            setField(bean, field, getScaAdapter().getComponentContext(bean, beanName));
        }
        if (hasAnnotation(field, SessionID.class)) {
            setField(bean, field, getScaAdapter().getBeanSessionID(bean, beanName));
        }

    }

    protected ScaAdapter createScaAdapter() {
        return new DefaultScaAdapter();
    }

}
