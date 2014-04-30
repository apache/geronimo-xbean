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
package org.apache.xbean.blueprint.context.impl;

import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutableValueMetadata;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.reflect.BeanProperty;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    private Map<PropertyKey, String> defaultValues = new HashMap<PropertyKey, String>();

    /**
     * Gets the default values that are assigned to constructor arguments without a defined value.
     *
     * @return the default values that are assigned to constructor arguments without a defined value
     */
    public List<DefaultProperty> getDefaultValues() {
        List<DefaultProperty> values = new LinkedList<DefaultProperty>();
        for (Map.Entry<PropertyKey, String> entry : defaultValues.entrySet()) {
            PropertyKey key = entry.getKey();
            String value = entry.getValue();
            values.add(new DefaultProperty(key.name, key.type, value));
        }
        return values;
    }

    /**
     * Sets the default values that are assigned to constructor arguments without a defined value.
     *
     * @param defaultValues the values that are assigned to constructor arguments without a defined value
     */
    public void setDefaultValues(List<DefaultProperty> defaultValues) {
        this.defaultValues.clear();
        for (DefaultProperty defaultValue : defaultValues) {
            addDefaultValue(defaultValue);
        }
    }

    /**
     * Adds a default value for a property with the specified name and type.
     *
     * @param name  the name of the property
     * @param type  the type of the property
     * @param value the default value for a property with the specified name and type
     */
    public void addDefaultValue(String name, Class type, String value) {
        defaultValues.put(new PropertyKey(name, type), value);
    }

    /**
     * Adds a defautl value for a property.
     *
     * @param defaultProperty the default property information
     */
    private void addDefaultValue(DefaultProperty defaultProperty) {
        defaultValues.put(new PropertyKey(defaultProperty.getName(), defaultProperty.getType()), defaultProperty.getValue());
    }

    public void processParameters(MutableBeanMetadata beanMetadata, MappingMetaData metadata, ParserContext parserContext) {

        // if this bean already has constructor arguments defined, don't mess with them
        if (beanMetadata.getArguments().size() > 0) {
            return;
        }

        // try to get a list of constructor arg names to use
        ConstructionInfo constructionInfo = selectConstructionMethod(beanMetadata, metadata);
        if (constructionInfo == null) {
            return;
        }

        // remove each named property and add an indexed constructor arg
        List<BeanProperty> beanProperties = beanMetadata.getProperties();
        LinkedHashMap<String, BeanProperty> propMap = new LinkedHashMap<String, BeanProperty>();
        for (BeanProperty beanProperty : beanProperties) {
            propMap.put(beanProperty.getName(), beanProperty);
        }
        String[] parameterNames = constructionInfo.parameterNames;
        Class[] parameterTypes = constructionInfo.parameterTypes;
        for (int i = 0; i < parameterNames.length; i++) {
            String parameterName = parameterNames[i];
            Class parameterType = parameterTypes[i];

            BeanProperty beanProperty = propMap.get(parameterName);
            if (beanProperty != null) {
                propMap.remove(parameterName);
                beanMetadata.removeProperty(beanProperty);
                beanMetadata.addArgument(beanProperty.getValue(), parameterType.getName(), i);
            } else {
                String defaultValue = defaultValues.get(new PropertyKey(parameterName, parameterType));
                if (defaultValue == null) {
                    defaultValue = DEFAULT_VALUE.get(parameterType);
                }
//                if (defaultValue instanceof FactoryBean) {
//                    try {
//                        defaultValue = ((FactoryBean)defaultValue).getObject();
//                    } catch (Exception e) {
//                        throw new FatalBeanException("Unable to get object value from bean factory", e);
//                    }
//                }
                MutableValueMetadata valueMetadata = parserContext.createMetadata(MutableValueMetadata.class);
                valueMetadata.setStringValue(defaultValue);
                valueMetadata.setType(parameterType.getName());
                beanMetadata.addArgument(valueMetadata, parameterType.getName(), i);
            }
        }

        // todo set any usable default values on the bean definition
    }

    private ConstructionInfo selectConstructionMethod(MutableBeanMetadata beanMetadata, MappingMetaData metadata) {
        Class beanClass = beanMetadata.getRuntimeClass();

        // get a set containing the names of the defined properties
        Set<String> definedProperties = new HashSet<String>();
        List<BeanProperty> values = beanMetadata.getProperties();
        for (BeanProperty beanProperty : values) {
            definedProperties.add(beanProperty.getName());
        }

        // first check for a factory method
        if (beanMetadata.getFactoryMethod() != null) {
            return selectFactory(beanClass, beanMetadata, metadata, definedProperties);
        } else {
            return selectConstructor(beanClass, metadata, definedProperties);
        }
    }

    private ConstructionInfo selectFactory(Class beanClass, MutableBeanMetadata beanMetadata, MappingMetaData metadata, Set definedProperties) {
        String factoryMethodName = beanMetadata.getFactoryMethod();

        // get the factory methods sorted by longest arg length first
        Method[] methods = beanClass.getMethods();
        List<Method> factoryMethods = new ArrayList<Method>(methods.length);
        for (Method method : methods) {
            if (method.getName().equals(factoryMethodName)) {
                factoryMethods.add(method);
            }
        }

        Collections.sort(factoryMethods, new MethodArgLengthComparator());

        // if a factory method has been annotated as the default constructor we always use that constructor
        for (Method factoryMethod : factoryMethods) {
            if (metadata.isDefaultFactoryMethod(beanClass, factoryMethod)) {
                return new ConstructionInfo(beanClass, factoryMethod, metadata);
            }
        }

        // try to find a constructor for which we have all of the properties defined
        for (Method factoryMethod : factoryMethods) {
            ConstructionInfo constructionInfo = new ConstructionInfo(beanClass, factoryMethod, metadata);
            if (isUsableConstructor(constructionInfo, definedProperties)) {
                return constructionInfo;
            }
        }
        return null;
    }

    private ConstructionInfo selectConstructor(Class beanClass, MappingMetaData metadata, Set definedProperties) {
        // get the constructors sorted by longest arg length first
        List<Constructor> constructors = new ArrayList<Constructor>(Arrays.asList(beanClass.getConstructors()));
        Collections.sort(constructors, new ConstructorArgLengthComparator());

        // if a constructor has been annotated as the default constructor we always use that constructor
        for (Constructor constructor : constructors) {
            if (metadata.isDefaultConstructor(constructor)) {
                return new ConstructionInfo(constructor, metadata);
            }
        }

        // try to find a constructor for which we have all of the properties defined
        for (Constructor constructor : constructors) {
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
                throw new ComponentDefinitionException("Excpected " + expectedParameterCount + " parameter names for constructor but only got " +
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
                throw new ComponentDefinitionException("Excpected " + expectedParameterCount + " parameter names for factory method but only got " +
                        names.length + ": " + factoryMethod.toString());
            }
            if (expectedParameterCount == 0) {
                names = new String[0];
            }

            this.parameterNames = names;
        }
    }

    private static class MethodArgLengthComparator implements Comparator<Method> {
        public int compare(Method o1, Method o2) {
            return getArgLength(o2) - getArgLength(o1);
        }

        private int getArgLength(Method object) {
            return object.getParameterTypes().length;
        }
    }
    
    private static class ConstructorArgLengthComparator implements Comparator<Constructor> {
        public int compare(Constructor o1, Constructor o2) {
            return getArgLength(o2) - getArgLength(o1);
        }

        private int getArgLength(Constructor object) {
            return object.getParameterTypes().length;
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

    private static final Map<Class, String> DEFAULT_VALUE;

    static {
        Map<Class, String> temp = new HashMap<Class, String>();
        temp.put(Boolean.TYPE, Boolean.FALSE.toString());
        temp.put(Byte.TYPE, "0B");
        temp.put(Character.TYPE, "\\u000");
        temp.put(Short.TYPE, "0S");
        temp.put(Integer.TYPE, "0");
        temp.put(Long.TYPE, "0L");
        temp.put(Float.TYPE, "0F");
        temp.put(Double.TYPE, "0D");

        DEFAULT_VALUE = Collections.unmodifiableMap(temp);
    }
}
