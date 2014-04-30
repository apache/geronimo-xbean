/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.xbean.spring.context.impl;

import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.AbstractBeanDefinition;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * NamedConstructorArgs is a BeanFactoryPostProcessor that converts property declarations into indexed constructor args
 * based on the the constructor parameter names annotation.  This process first selects a constructor and then fills in
 * the constructor arguments from the properties defined in the bean definition.  If a property is not defined in the
 * bean definition, first the defaultValues map is checked for a value and if a value is not present a Java default
 * value is provided for the constructor argument (e.g. numbers are assigned 0 and objects are assigned null).
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class NamedConstructorArgs {
    private Map defaultValues = new HashMap();

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

    public void processParameters(BeanDefinitionHolder definitionHolder, MappingMetaData metadata) throws BeansException {
        // this only works if we have an abstsract bean definition
        if (!(definitionHolder.getBeanDefinition() instanceof AbstractBeanDefinition)) {
            return;
        }

        AbstractBeanDefinition beanDefinition = (AbstractBeanDefinition) definitionHolder.getBeanDefinition();
        ConstructorArgumentValues constructorArgumentValues = beanDefinition.getConstructorArgumentValues();

        // if this bean already has constructor arguments defined, don't mess with them
        if (constructorArgumentValues.getArgumentCount() > 0) {
            return;
        }

        // try to get a list of constructor arg names to use
        ConstructionInfo constructionInfo = selectConstructionMethod(beanDefinition, metadata);
        if (constructionInfo == null) {
            return;
        }

        // remove each named property and add an indexed constructor arg
        MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();
        String[] parameterNames = constructionInfo.parameterNames;
        Class[] parameterTypes = constructionInfo.parameterTypes;
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

    private ConstructionInfo selectConstructionMethod(AbstractBeanDefinition beanDefinition, MappingMetaData metadata) {
        Class beanClass = beanDefinition.getBeanClass();

        // get a set containing the names of the defined properties
        Set definedProperties = new HashSet();
        PropertyValue[] values = beanDefinition.getPropertyValues().getPropertyValues();
        for (int i = 0; i < values.length; i++) {
            definedProperties.add(values[i].getName());
        }

        // first check for a factory method
        if (beanDefinition.getFactoryMethodName() != null) {
            return selectFactory(beanClass, beanDefinition, metadata, definedProperties);
        } else {
            return selectConstructor(beanClass, metadata, definedProperties);
        }
    }

    private ConstructionInfo selectFactory(Class beanClass, AbstractBeanDefinition beanDefinition, MappingMetaData metadata, Set definedProperties) {
        String factoryMethodName = beanDefinition.getFactoryMethodName();

        // get the factory methods sorted by longest arg length first
        Method[] methods = beanClass.getMethods();
        List factoryMethods = new ArrayList(methods.length);
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (method.getName().equals(factoryMethodName)) {
                factoryMethods.add(method);
            }
        }

        Collections.sort(factoryMethods, new ArgLengthComparator());

        // if a factory method has been annotated as the default constructor we always use that constructor
        for (Iterator iterator = factoryMethods.iterator(); iterator.hasNext();) {
            Method factoryMethod = (Method) iterator.next();

            if (metadata.isDefaultFactoryMethod(beanClass, factoryMethod)) {
                return new ConstructionInfo(beanClass, factoryMethod, metadata);
            }
        }

        // try to find a constructor for which we have all of the properties defined
        for (Iterator iterator = factoryMethods.iterator(); iterator.hasNext();) {
            Method factoryMethod = (Method) iterator.next();
            ConstructionInfo constructionInfo = new ConstructionInfo(beanClass, factoryMethod, metadata);
            if (isUsableConstructor(constructionInfo, definedProperties)) {
                return constructionInfo;
            }
        }
        return null;
    }

    private ConstructionInfo selectConstructor(Class beanClass, MappingMetaData metadata, Set definedProperties) {
        // get the constructors sorted by longest arg length first
        List constructors = new ArrayList(Arrays.asList(beanClass.getConstructors()));
        Collections.sort(constructors, new ArgLengthComparator());

        // if a constructor has been annotated as the default constructor we always use that constructor
        for (Iterator iterator = constructors.iterator(); iterator.hasNext();) {
            Constructor constructor = (Constructor) iterator.next();

            if (metadata.isDefaultConstructor(constructor)) {
                return new ConstructionInfo(constructor, metadata);
            }
        }

        // try to find a constructor for which we have all of the properties defined
        for (Iterator iterator = constructors.iterator(); iterator.hasNext();) {
            Constructor constructor = (Constructor) iterator.next();
            ConstructionInfo constructionInfo = new ConstructionInfo(constructor, metadata);
            if (isUsableConstructor(constructionInfo, definedProperties)) {
                return constructionInfo;
            }
        }
        return null;
    }

    private boolean isUsableConstructor(ConstructionInfo constructionInfo, Set definedProperties) {
        // if we don't have parameter names this is not the constructor we are looking for
        String[] parameterNames = constructionInfo.parameterNames;
        if (parameterNames == null) {
            return false;
        }

        Class[] parameterTypes = constructionInfo.parameterTypes;
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

    private class ConstructionInfo {
        private final Class[] parameterTypes;
        private final String[] parameterNames;

        public ConstructionInfo(Constructor constructor, MappingMetaData metadata) {
            this.parameterTypes = constructor.getParameterTypes();
            String[] names = metadata.getParameterNames(constructor);

            // verify that we have enough parameter names
            int expectedParameterCount = parameterTypes.length;
            if (names != null && names.length != expectedParameterCount) {
                throw new FatalBeanException("Excpected " + expectedParameterCount + " parameter names for constructor but only got " +
                        names.length + ": " + constructor.toString());
            }
            if (expectedParameterCount == 0) {
                names = new String[0];
            }

            this.parameterNames = names;
        }

        public ConstructionInfo(Class beanClass, Method factoryMethod, MappingMetaData metadata) {
            this.parameterTypes = factoryMethod.getParameterTypes();

            String[] names = metadata.getParameterNames(beanClass, factoryMethod);

            // verify that we have enough parameter names
            int expectedParameterCount = parameterTypes.length;
            if (names != null && names.length != expectedParameterCount) {
                throw new FatalBeanException("Excpected " + expectedParameterCount + " parameter names for factory method but only got " +
                        names.length + ": " + factoryMethod.toString());
            }
            if (expectedParameterCount == 0) {
                names = new String[0];
            }

            this.parameterNames = names;
        }
    }

    private static class ArgLengthComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            return getArgLength(o2) - getArgLength(o1);
        }

        private int getArgLength(Object object) {
            if (object instanceof Method) {
                return ((Method) object).getParameterTypes().length;
            } else {
                return ((Constructor) object).getParameterTypes().length;
            }
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
