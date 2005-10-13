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
package org.xbean.server.spring.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedList;
import java.util.Arrays;
import java.lang.reflect.Constructor;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.FactoryBean;
import org.xbean.server.annotation.AnnotationProvider;
import org.xbean.server.annotation.DefaultConstructor;
import org.xbean.server.annotation.ParameterNames;
import org.xbean.spring.util.SpringVisitor;
import org.xbean.spring.util.AbstractSpringVisitor;

/**
 * NamedConstructorArgs is a BeanFactoryPostProcessor that converts property declarations into indexed constructor args
 * based on the the constructor parameter names annotation.  This process first selctes a constructor and then fills in
 * the constructor arguments from the properties defined in the bean definition.  If a property is not defined in the
 * bean definition, first the defaultValues map is checked for a value and if a value is not present a Java default
 * value is provided for the constructor argument (e.g. numbers are assigned 0 and objects are assigned null).
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class NamedConstructorArgs implements BeanFactoryPostProcessor {
    private AnnotationProvider annotationProvider;
    private Map defaultValues = new HashMap();

    /**
     * Creates an empty NamedConstructorArgs.  Note this class is not usable until an annotation provider is assigned.
     */
    public NamedConstructorArgs() {
    }

    /**
     * Creates a NamedConstructorArgs that uses the specified annotation provider to select the default constructor
     * and to determine the constructor argument names.
     * @param annotationProvider the annotation provieder used to determine parameter names
     */
    public NamedConstructorArgs(AnnotationProvider annotationProvider) {
        this.annotationProvider = annotationProvider;
    }

    /**
     * Gets the annotation provider which is used to select the default constructor and to determine the constructor
     * argument names.
     * @return the annotation provider
     */
    public AnnotationProvider getAnnotationProvider() {
        return annotationProvider;
    }

    /**
     * Sets the annotation provider which is used to select the default constructor and to determine the constructor
     * argument names.  Note it is expected that this method is called immedately after a constructor using the same
     * thread, therefore the setting of the annotation provider is not protected with a synchronized block.
     * @param annotationProvider the new annotation provider
     */
    public void setAnnotationProvider(AnnotationProvider annotationProvider) {
        this.annotationProvider = annotationProvider;
    }

    /**
     * Gets the default values that are assigned to constructor arguments without a defined value.
     * @return the default values that are assigned to constructor arguments without a defined value
     */
    public List getDefaultValues() {
        List values = new LinkedList();
        for (Iterator iterator = defaultValues.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            PropertyKey key = (PropertyKey) entry.getKey();
            Object value = entry.getValue();
            values.add(new DefaultProperty(key.name, key.type, value));
        }
        return values;
    }

    /**
     * Sets the default values that are assigned to constructor arguments without a defined value.
     * @param defaultValues the values that are assigned to constructor arguments without a defined value
     */
    public void setDefaultValues(List defaultValues) {
        this.defaultValues.clear();
        for (Iterator iterator = defaultValues.iterator(); iterator.hasNext();) {
            addDefaultValue((DefaultProperty) iterator.next());
        }
    }

    /**
     * Adds a default value for a property with the specified name and type.
     * @param name the name of the property
     * @param type the type of the property
     * @param value the default value for a property with the specified name and type
     */
    public void addDefaultValue(String name, Class type, Object value) {
        defaultValues.put(new PropertyKey(name, type), value);
    }

    /**
     * Adds a defautl value for a property.
     * @param defaultProperty the default property information
     */
    private void addDefaultValue(DefaultProperty defaultProperty) {
        defaultValues.put(new PropertyKey(defaultProperty.getName(), defaultProperty.getType()), defaultProperty.getValue());
    }

    /**
     * Finds all bean definitions and where possble assigns the indexed constructor argument values from the defined
     * and default property values.
     * @param beanFactory the bean factory to inspect
     * @throws BeansException if the parameter name annotation for a constructor does not contain the same number of
     * parameter names as the constructor
     */
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        SpringVisitor visitor = new AbstractSpringVisitor() {
            public void visitBeanDefinition(BeanDefinition beanDefinition, Object data) throws BeansException {
                super.visitBeanDefinition(beanDefinition, data);

                if (!(beanDefinition instanceof RootBeanDefinition)) {
                    return;
                }

                RootBeanDefinition rootBeanDefinition = ((RootBeanDefinition) beanDefinition);
                processParameters(rootBeanDefinition);

            }
        };
        visitor.visitBeanFactory(beanFactory, null);
    }

    private void processParameters(RootBeanDefinition rootBeanDefinition) throws BeansException {
        ConstructorArgumentValues constructorArgumentValues = rootBeanDefinition.getConstructorArgumentValues();

        // if this bean already has constructor arguments defined, don't mess with them
        if (constructorArgumentValues.getArgumentCount() > 0) {
            return;
        }

        // try to get a list of constructor arg names to use
        ConstructorInfo constructorInfo = selectConstructor(rootBeanDefinition);
        if (constructorInfo == null) {
            return;
        }

        // remove each named property and add an indexed constructor arg
        MutablePropertyValues propertyValues = rootBeanDefinition.getPropertyValues();
        String[] parameterNames = constructorInfo.parameterNames;
        Class[] parameterTypes = constructorInfo.constructor.getParameterTypes();
        for (int i = 0; i < parameterNames.length; i++) {
            String parameterName = parameterNames[i];
            Class parameterType = parameterTypes[i];

            PropertyValue propertyValue = propertyValues.getPropertyValue(parameterName);
            if (propertyValue != null) {
                propertyValues.removePropertyValue(parameterName);
                constructorArgumentValues.addIndexedArgumentValue(i, propertyValue.getValue(), parameterType.getName());
            } else {
                Object defaultValue = defaultValues.get(new PropertyKey(parameterName, parameterType));
                if (defaultValue == null) {
                    defaultValue = DEFAULT_VALUE.get(parameterType);
                }
                if (defaultValue instanceof FactoryBean) {
                    try {
                        defaultValue = ((FactoryBean)defaultValue).getObject();
                    } catch (Exception e) {
                        throw new FatalBeanException("Unable to get object value from bean factory", e);
                    }
                }
                constructorArgumentValues.addIndexedArgumentValue(i, defaultValue, parameterType.getName());
            }
        }

        // todo set any usable default values on the bean definition
    }

    private ConstructorInfo selectConstructor(RootBeanDefinition rootBeanDefinition) {
        Class beanType = rootBeanDefinition.getBeanClass();

        // get a set containing the names of the defined properties
        Set definedProperties = new HashSet();
        PropertyValue[] values = rootBeanDefinition.getPropertyValues().getPropertyValues();
        for (int i = 0; i < values.length; i++) {
            definedProperties.add(values[i].getName());
        }

        // get the constructors sorted by longest arg length first
        List constructors = new ArrayList(Arrays.asList(beanType.getConstructors()));
        Collections.sort(constructors, new ArgLengthComparator());

        // if a constructor has been annotated as the default constructor we always use that constructor
        for (Iterator iterator = constructors.iterator(); iterator.hasNext();) {
            Constructor constructor = (Constructor) iterator.next();
            if (annotationProvider.isAnnotationPresent(DefaultConstructor.class, constructor)) {
                return new ConstructorInfo(constructor);
            }
        }

        // try to find a constructor for which we have all of the properties defined
        for (Iterator iterator = constructors.iterator(); iterator.hasNext();) {
            Constructor constructor = (Constructor) iterator.next();
            ConstructorInfo constructorInfo = new ConstructorInfo(constructor);
            if (isUsableConstructor(constructorInfo, definedProperties)) {
                return constructorInfo;
            }
        }
        return null;
    }

    private boolean isUsableConstructor(ConstructorInfo constructorInfo, Set definedProperties) {
        // if we don't have parameter names this is not the constructor we are looking for
        String[] parameterNames = constructorInfo.parameterNames;
        if (parameterNames == null) {
            return false;
        }

        Class[] parameterTypes = constructorInfo.constructor.getParameterTypes();
        for (int i = 0; i < parameterNames.length; i++) {
            String parameterName = parameterNames[i];
            Class parameterType = parameterTypes[i];

            // can we satify this property using a defined property or default property
            if (!definedProperties.contains(parameterName) && !defaultValues.containsKey(new PropertyKey(parameterName, parameterType))) {
                return false;
            }
        }

        return true;
    }

    private class ConstructorInfo {
        private final Constructor constructor;
        private final String[] parameterNames;

        public ConstructorInfo(Constructor constructor) {
            this.constructor = constructor;
            ParameterNames parameterNames = (ParameterNames) annotationProvider.getAnnotation(ParameterNames.class, constructor);

            // verify that we have enough parameter names
            String[] names = parameterNames.names();
            int expectedParameterCount = constructor.getParameterTypes().length;
            if (names != null && names.length != expectedParameterCount) {
                throw new FatalBeanException("Excpected " + expectedParameterCount + " parameter names for constructor but only got " +
                        names.length + ": " + constructor.toString());
            }
            if (expectedParameterCount == 0) {
                names = new String[0];
            }

            this.parameterNames = names;
        }
    }

    private static class ArgLengthComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            Constructor constructor1 = (Constructor) o1;
            Constructor constructor2 = (Constructor) o2;
            return constructor2.getParameterTypes().length - constructor1.getParameterTypes().length;
        }
    }

    private static class PropertyKey {
        private final String name;
        private final Class type;

        public PropertyKey(String name, Class type) {
            this.name = name;
            this.type = type;
        }

        public boolean equals(Object object) {
            if (!(object instanceof PropertyKey)) {
                return false;
            }

            PropertyKey defaultProperty = (PropertyKey) object;
            return name.equals(defaultProperty.name) && type.equals(type);
        }

        public int hashCode() {
            int result = 17;
            result = 37 * result + name.hashCode();
            result = 37 * result + type.hashCode();
            return result;
        }

        public String toString() {
            return "[" + name + " " + type + "]";
        }
    }

    private static final Map DEFAULT_VALUE;
    static {
        Map temp = new HashMap();
        temp.put(Boolean.TYPE, Boolean.FALSE);
        temp.put(Byte.TYPE, new Byte((byte) 0));
        temp.put(Character.TYPE, new Character((char) 0));
        temp.put(Short.TYPE, new Short((short) 0));
        temp.put(Integer.TYPE, new Integer(0));
        temp.put(Long.TYPE, new Long(0));
        temp.put(Float.TYPE, new Float(0));
        temp.put(Double.TYPE, new Double(0));

        DEFAULT_VALUE = Collections.unmodifiableMap(temp);
    }
}
