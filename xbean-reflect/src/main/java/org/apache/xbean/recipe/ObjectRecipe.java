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
package org.apache.xbean.recipe;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @version $Rev: 6688 $ $Date: 2005-12-29T02:08:29.200064Z $
 */
public class ObjectRecipe extends AbstractRecipe {
    private String typeName;
    private Class typeClass;
    private ConstructionStrategy constructionStrategy;
    private String factoryMethod;
    private String[] constructorArgNames;
    private Class[] constructorArgTypes;
    private final LinkedHashMap<Property,Object> properties = new LinkedHashMap<Property,Object>();
    private final EnumSet<Option> options = EnumSet.of(Option.FIELD_INJECTION);
    private final Map<String,Object> unsetProperties = new LinkedHashMap<String,Object>();

    public ObjectRecipe(Class typeClass) {
        this(typeClass, null, null, null, null);
    }

    public ObjectRecipe(Class typeClass, String factoryMethod) {
        this(typeClass, factoryMethod, null, null, null);
    }

    public ObjectRecipe(Class typeClass, Map<String,Object> properties) {
        this(typeClass, null, null, null, properties);
    }

    public ObjectRecipe(Class typeClass, String[] constructorArgNames) {
        this(typeClass, null, constructorArgNames, null, null);
    }

    public ObjectRecipe(Class typeClass, String[] constructorArgNames, Class[] constructorArgTypes) {
        this(typeClass, null, constructorArgNames, constructorArgTypes, null);
    }

    public ObjectRecipe(Class type, String factoryMethod, String[] constructorArgNames) {
        this(type, factoryMethod, constructorArgNames, null, null);
    }

    public ObjectRecipe(Class type, String factoryMethod, String[] constructorArgNames, Class[] constructorArgTypes) {
        this(type, factoryMethod, constructorArgNames, constructorArgTypes, null);
    }

    public ObjectRecipe(Class typeClass, String factoryMethod, String[] constructorArgNames, Class[] constructorArgTypes, Map<String,Object> properties) {
        this.typeClass = typeClass;
        this.factoryMethod = factoryMethod;
        this.constructorArgNames = constructorArgNames;
        this.constructorArgTypes = constructorArgTypes;
        constructionStrategy = new ExplicitConstructionStrategy();
        if (properties != null) {
            setAllProperties(properties);
        }
    }

    public ObjectRecipe(String typeName) {
        this(typeName, null, null, null, null);
    }

    public ObjectRecipe(String typeName, String factoryMethod) {
        this(typeName, factoryMethod, null, null, null);
    }

    public ObjectRecipe(String typeName, Map<String,Object> properties) {
        this(typeName, null, null, null, properties);
    }

    public ObjectRecipe(String typeName, String[] constructorArgNames) {
        this(typeName, null, constructorArgNames, null, null);
    }

    public ObjectRecipe(String typeName, String[] constructorArgNames, Class[] constructorArgTypes) {
        this(typeName, null, constructorArgNames, constructorArgTypes, null);
    }

    public ObjectRecipe(String typeName, String factoryMethod, String[] constructorArgNames) {
        this(typeName, factoryMethod, constructorArgNames, null, null);
    }

    public ObjectRecipe(String typeName, String factoryMethod, String[] constructorArgNames, Class[] constructorArgTypes) {
        this(typeName, factoryMethod, constructorArgNames, constructorArgTypes, null);
    }

    public ObjectRecipe(String typeName, String factoryMethod, String[] constructorArgNames, Class[] constructorArgTypes, Map<String,Object> properties) {
        this.typeName = typeName;
        this.factoryMethod = factoryMethod;
        this.constructorArgNames = constructorArgNames;
        this.constructorArgTypes = constructorArgTypes;
        constructionStrategy = new ExplicitConstructionStrategy();
        if (properties != null) {
            setAllProperties(properties);
        }
    }

    public void allow(Option option){
        options.add(option);
    }

    public void disallow(Option option){
        options.remove(option);
    }

    public ConstructionStrategy getConstructionStrategy() {
        return constructionStrategy;
    }

    public void setConstructionStrategy(ConstructionStrategy constructionStrategy) {
        this.constructionStrategy = constructionStrategy;
    }

    public String[] getConstructorArgNames() {
        return constructorArgNames;
    }

    public void setConstructorArgNames(String[] constructorArgNames) {
        this.constructorArgNames = constructorArgNames;
    }

    public Class[] getConstructorArgTypes() {
        return constructorArgTypes;
    }

    public void setConstructorArgTypes(Class[] constructorArgTypes) {
        this.constructorArgTypes = constructorArgTypes;
    }

    /**
     * Gets the name of the factory method to call on the constructed instance.
     * This method must be of the form <code>public T factoryMethodName();</code>
     * and specifically must NOT be static.  Static factory methods are handled using
     * a ConstructionStrategy.
     * @return the name of the factory method to call on the constructed instance
     */
    public String getFactoryMethod() {
        return factoryMethod;
    }

    /**
     * Sets the name of the factory method to call on the constructed instance.
     * This method must be of the form <code>public T factoryMethodName();</code>
     * and specifically must NOT be static.  Static factory methods are handled using
     * a ConstructionStrategy.
     * @param factoryMethod the name of the factory method to call on the constructed instance
     */
    public void setFactoryMethod(String factoryMethod) {
        this.factoryMethod = factoryMethod;
    }

    public Object getProperty(String name) {
        Object value = properties.get(new Property(name));
        return value;
    }

    public Map<String, Object> getProperties() {
        LinkedHashMap<String, Object> properties = new LinkedHashMap<String, Object>();
        for (Map.Entry<Property, Object> entry : this.properties.entrySet()) {
            properties.put(entry.getKey().name, entry.getValue());
        }
        return properties;
    }

    public void setProperty(String name, Object value) {
        setProperty(new Property(name), value);
    }

    public void setFieldProperty(String name, Object value){
        setProperty(new FieldProperty(name), value);
        options.add(Option.FIELD_INJECTION);
    }

    public void setMethodProperty(String name, Object value){
        setProperty(new SetterProperty(name), value);
    }

    private void setProperty(Property key, Object value) {
        if (value instanceof UnsetPropertiesRecipe) {
            allow(Option.IGNORE_MISSING_PROPERTIES);
        }
        properties.put(key, value);
    }


    public void setAllProperties(Map<?,?> map) {
        if (map == null) throw new NullPointerException("map is null");
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String name = (String) entry.getKey();
            Object value = entry.getValue();
            setProperty(name, value);
        }
    }

    public Map<String,Object> getUnsetProperties() {
        return unsetProperties;
    }

    public List<Recipe> getNestedRecipes() {
        List<Recipe> nestedRecipes = new ArrayList<Recipe>(properties.size());
        for (Object o : properties.values()) {
            if (o instanceof Recipe) {
                Recipe recipe = (Recipe) o;
                nestedRecipes.add(recipe);
            }
        }
        return nestedRecipes;
    }

    public List<Recipe> getConstructorRecipes() {
        Construction construction = constructionStrategy.getConstruction(this, Object.class);
        if (!construction.hasInstanceFactory()) {
            // only include recipes used in the construcor args
            List<String> parameterNames = construction.getParameterNames();
            List<Recipe> nestedRecipes = new ArrayList<Recipe>(parameterNames.size());
            for (Map.Entry<Property, Object> entry : properties.entrySet()) {
                if (parameterNames.contains(entry.getKey().name) && entry.getValue() instanceof Recipe) {
                    Recipe recipe = (Recipe) entry.getValue();
                    nestedRecipes.add(recipe);
                }
            }
            return nestedRecipes;
        } else {
            // when there is an instance factory all nested recipes are used in the constructor
            return getNestedRecipes();
        }
    }

    public boolean canCreate(Class type) {
        Class myType = getType();
        return type.isAssignableFrom(myType) || type.isAssignableFrom(myType);
    }

    protected Object internalCreate(Class expectedType, boolean lazyRefAllowed) throws ConstructionException {
        unsetProperties.clear();
        // load the type class
        Class typeClass = getType();

        // verify that it is a class we can construct
        if (!Modifier.isPublic(typeClass.getModifiers())) {
            throw new ConstructionException("Class is not public: " + typeClass.getName());
        }
        if (Modifier.isInterface(typeClass.getModifiers())) {
            throw new ConstructionException("Class is an interface: " + typeClass.getName());
        }
        if (Modifier.isAbstract(typeClass.getModifiers())) {
            throw new ConstructionException("Class is abstract: " + typeClass.getName());
        }

        // clone the properties so they can be used again
        Map<Property,Object> propertyValues = new LinkedHashMap<Property,Object>(properties);

        // create the instance
        Construction construction = constructionStrategy.getConstruction(this, expectedType);
        Object[] parameters = extractConstructorArgs(propertyValues,
                construction.getParameterNames(),
                construction.getParameterTypes());
        Object instance = construction.create(parameters);

        // add to execution context if name is specified
        if (getName() != null) {
            ExecutionContext.getContext().addObject(getName(), instance);
        }
       
        // set the properties
        setProperties(propertyValues, instance, instance.getClass());

        // call instance factory method
        instance = construction.callInstanceFactory(instance);

        return instance;
    }

    public void setProperties(Object instance) throws ConstructionException {
        unsetProperties.clear();

        // clone the properties so they can be used again
        Map<Property,Object> propertyValues = new LinkedHashMap<Property,Object>(properties);

        setProperties(propertyValues, instance, instance.getClass());
    }

    public Class setStaticProperties() throws ConstructionException {
        unsetProperties.clear();

        // load the type class
        Class typeClass = getType();

        // verify that it is a class we can construct
        if (!Modifier.isPublic(typeClass.getModifiers())) {
            throw new ConstructionException("Class is not public: " + typeClass.getName());
        }
        if (Modifier.isInterface(typeClass.getModifiers())) {
            throw new ConstructionException("Class is an interface: " + typeClass.getName());
        }
        if (Modifier.isAbstract(typeClass.getModifiers())) {
            throw new ConstructionException("Class is abstract: " + typeClass.getName());
        }

        // clone the properties so they can be used again
        Map<Property,Object> propertyValues = new LinkedHashMap<Property,Object>(properties);

        setProperties(propertyValues, null, typeClass);

        return typeClass;
    }

    public Class getType() {
        if (typeClass != null || typeName != null) {
            Class type = typeClass;
            if (type == null) {
                try {
                    type = RecipeHelper.loadClass(typeName);
                } catch (ClassNotFoundException e) {
                    throw new ConstructionException("Type class could not be found: " + typeName);
                }
            }

            return type;
        }

        return null;
    }

    private void setProperties(Map<Property, Object> propertyValues, Object instance, Class clazz) {
        // set remaining properties
        for (Map.Entry<Property, Object> entry : RecipeHelper.prioritizeProperties(propertyValues)) {
            Property propertyName = entry.getKey();
            Object propertyValue = entry.getValue();

            setProperty(instance, clazz, propertyName, propertyValue);
        }

    }

    private void setProperty(Object instance, Class clazz, Property propertyName, Object propertyValue) {

        Member member;
        try {
            if (propertyName instanceof SetterProperty){
                member = new MethodMember(ReflectionUtil.findSetter(clazz, propertyName.name, propertyValue, options));
            } else if (propertyName instanceof FieldProperty){
                member = new FieldMember(ReflectionUtil.findField(clazz, propertyName.name, propertyValue, options));
            } else {
                try {
                    member = new MethodMember(ReflectionUtil.findSetter(clazz, propertyName.name, propertyValue, options));
                } catch (MissingAccessorException noSetter) {
                    if (!options.contains(Option.FIELD_INJECTION)) {
                        throw noSetter;
                    }

                    try {
                        member = new FieldMember(ReflectionUtil.findField(clazz, propertyName.name, propertyValue, options));
                    } catch (MissingAccessorException noField) {
                        throw (noField.getMatchLevel() > noSetter.getMatchLevel())? noField: noSetter;
                    }
                }
            }
        } catch (MissingAccessorException e) {
            if (options.contains(Option.IGNORE_MISSING_PROPERTIES)) {
                unsetProperties.put(propertyName.name, propertyValue);
                return;
            }
            throw e;
        }

        try {
            propertyValue = RecipeHelper.convert(member.getType(), propertyValue, false);
            member.setValue(instance, propertyValue);
        } catch (Exception e) {
            Throwable t = e;
            if (e instanceof InvocationTargetException) {
                InvocationTargetException invocationTargetException = (InvocationTargetException) e;
                if (invocationTargetException.getCause() != null) {
                    t = invocationTargetException.getCause();
                }
            }
            throw new ConstructionException("Error setting property: " + member, t);
        }
    }

    private Object[] extractConstructorArgs(Map propertyValues, List<String> argNames, List<Class> argTypes) {
        Object[] parameters = new Object[argNames.size()];
        for (int i = 0; i < argNames.size(); i++) {
            Property name = new Property(argNames.get(i));
            Class type = argTypes.get(i);

            Object value;
            if (propertyValues.containsKey(name)) {
                value = propertyValues.remove(name);
                if (!RecipeHelper.isInstance(type, value) && !RecipeHelper.isConvertable(type, value)) {
                    throw new ConstructionException("Invalid and non-convertable constructor parameter type: " +
                            "name=" + name + ", " +
                            "index=" + i + ", " +
                            "expected=" + type.getName() + ", " +
                            "actual=" + (value == null ? "null" : value.getClass().getName()));
                }
                value = RecipeHelper.convert(type, value, false);
            } else {
                value = getDefaultValue(type);
            }


            parameters[i] = value;
        }
        return parameters;
    }

    private static Object getDefaultValue(Class type) {
        if (type.equals(Boolean.TYPE)) {
            return Boolean.FALSE;
        } else if (type.equals(Character.TYPE)) {
            return (char) 0;
        } else if (type.equals(Byte.TYPE)) {
            return (byte) 0;
        } else if (type.equals(Short.TYPE)) {
            return (short) 0;
        } else if (type.equals(Integer.TYPE)) {
            return 0;
        } else if (type.equals(Long.TYPE)) {
            return (long) 0;
        } else if (type.equals(Float.TYPE)) {
            return (float) 0;
        } else if (type.equals(Double.TYPE)) {
            return (double) 0;
        }
        return null;
    }

    public static interface Member {
        Class getType();
        void setValue(Object instance, Object value) throws Exception;
    }

    public static class MethodMember implements Member {
        private final Method setter;

        public MethodMember(Method method) {
            this.setter = method;
        }

        public Class getType() {
            return setter.getParameterTypes()[0];
        }

        public void setValue(Object instance, Object value) throws Exception {
            setter.invoke(instance, value);
        }

        public String toString() {
            return setter.toString();
        }
    }

    public static class FieldMember implements Member {
        private final Field field;

        public FieldMember(Field field) {
            this.field = field;
        }

        public Class getType() {
            return field.getType();
        }

        public void setValue(Object instance, Object value) throws Exception {
            field.set(instance, value);
        }

        public String toString() {
            return field.toString();
        }
    }

    public static class Property {
        private final String name;

        public Property(String name) {
            if (name == null) throw new NullPointerException("name is null");
            this.name = name;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null) return false;
            if (o instanceof String){
                return this.name.equals(o);
            }
            if (o instanceof Property) {
                Property property = (Property) o;
                return this.name.equals(property.name);
            }
            return false;
        }

        public int hashCode() {
            return name.hashCode();
        }

        public String toString() {
            return name;
        }
    }

    public static class SetterProperty extends Property {
        public SetterProperty(String name) {
            super(name);
        }
        public int hashCode() {
            return super.hashCode()+2;
        }
        public String toString() {
            return "[setter] "+super.toString();
        }

    }

    public static class FieldProperty extends Property {
        public FieldProperty(String name) {
            super(name);
        }

        public int hashCode() {
            return super.hashCode()+1;
        }
        public String toString() {
            return "[field] "+ super.toString();
        }
    }
}
