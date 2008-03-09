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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.xbean.recipe.ReflectionUtil.*;

/**
 * @version $Rev: 6688 $ $Date: 2005-12-29T02:08:29.200064Z $
 */
public class ObjectRecipe extends AbstractRecipe {
    private String typeName;
    private Class typeClass;
    private String factoryMethod;
    private List<String> constructorArgNames;
    private List<Class<?>> constructorArgTypes;
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
        this.constructorArgNames = constructorArgNames != null ? Arrays.asList(constructorArgNames) : null;
        this.constructorArgTypes = constructorArgTypes != null ? Arrays.<Class<?>>asList(constructorArgTypes) : null;
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
        this.constructorArgNames = constructorArgNames != null ? Arrays.asList(constructorArgNames) : null;
        this.constructorArgTypes = constructorArgTypes != null ? Arrays.<Class<?>>asList(constructorArgTypes) : null;
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

    public Set<Option> getOptions() {
        return Collections.unmodifiableSet(options);
    }

    public List<String> getConstructorArgNames() {
        return constructorArgNames;
    }

    public void setConstructorArgNames(String[] constructorArgNames) {
        this.constructorArgNames = constructorArgNames != null ? Arrays.asList(constructorArgNames) : null;
    }

    public void setConstructorArgNames(List<String> constructorArgNames) {
        this.constructorArgNames = constructorArgNames;
    }

    public List<Class<?>> getConstructorArgTypes() {
        return constructorArgTypes;
    }

    public void setConstructorArgTypes(Class[] constructorArgTypes) {
        this.constructorArgTypes = constructorArgTypes != null ? Arrays.<Class<?>>asList(constructorArgTypes) : null;
    }

    public void setConstructorArgTypes(List<? extends Class<?>> constructorArgTypes) {
        this.constructorArgTypes = new ArrayList<Class<?>>(constructorArgTypes);
    }

    public String getFactoryMethod() {
        return factoryMethod;
    }

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
        // find the factory that will be used to create the class instance
        Factory factory = findFactory(Object.class);

        // if we are NOT using an instance factory to create the object
        // (we have a factory method and it is not a static factory method)
        if (factoryMethod != null && !(factory instanceof StaticFactory)) {
            // only include recipes used in the construcor args
            List<String> parameterNames = factory.getParameterNames();
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

    public boolean canCreate(Type type) {
        Class myType = getType();
        return RecipeHelper.isAssignable(type, myType) || RecipeHelper.isAssignable(type, myType);
    }

    protected Object internalCreate(Type expectedType, boolean lazyRefAllowed) throws ConstructionException {
        unsetProperties.clear();

        //
        // load the type class
        Class typeClass = getType();

        //
        // clone the properties so they can be used again
        Map<Property,Object> propertyValues = new LinkedHashMap<Property,Object>(properties);

        //
        // create the instance
        Factory factory = findFactory(expectedType);
        Object[] parameters = extractConstructorArgs(propertyValues, factory);
        Object instance = factory.create(parameters);

        //
        // add to execution context if name is specified
        if (getName() != null) {
            ExecutionContext.getContext().addObject(getName(), instance);
        }

        //
        // set the properties
        setProperties(propertyValues, instance, instance.getClass());

        //
        // call instance factory method

        // if we have a factory method name and did not find a static factory,
        // then we have an instance factory
        if (factoryMethod != null && !(factory instanceof StaticFactory)) {
            // find the instance factory method
            Method instanceFactory = ReflectionUtil.findInstanceFactory(instance.getClass(), factoryMethod, null);

            try {
                instance = instanceFactory.invoke(instance);
            } catch (Exception e) {
                Throwable t = e;
                if (e instanceof InvocationTargetException) {
                    InvocationTargetException invocationTargetException = (InvocationTargetException) e;
                    if (invocationTargetException.getCause() != null) {
                        t = invocationTargetException.getCause();
                    }
                }
                throw new ConstructionException("Error calling instance factory method: " + instanceFactory, t);
            }
        }

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

    private Factory findFactory(Type expectedType) {
        Class type = getType();

        //
        // attempt to find a static factory
        if (factoryMethod != null) {
            try {
                StaticFactory staticFactory = ReflectionUtil.findStaticFactory(
                        type,
                        factoryMethod,
                        constructorArgNames,
                        constructorArgTypes,
                        getProperties().keySet(),
                        options);
                return staticFactory;
            } catch (MissingFactoryMethodException ignored) {
            }

        }

        //
        // factory was not found, look for a constuctor

        // if expectedType is a subclass of the assigned type, we create
        // the sub class instead
        Class consturctorClass;
        if (RecipeHelper.isAssignable(type, expectedType)) {
            consturctorClass = RecipeHelper.toClass(expectedType);
        } else {
            consturctorClass = type;
        }

        ConstructorFactory constructor = ReflectionUtil.findConstructor(
                consturctorClass,
                constructorArgNames,
                constructorArgTypes,
                getProperties().keySet(),
                options);

        return constructor;
    }

    private Object[] extractConstructorArgs(Map propertyValues, Factory factory) {
        List<String> parameterNames = factory.getParameterNames();
        List<Type> parameterTypes = factory.getParameterTypes();

        Object[] parameters = new Object[parameterNames.size()];
        for (int i = 0; i < parameterNames.size(); i++) {
            Property name = new Property(parameterNames.get(i));
            Type type = parameterTypes.get(i);

            Object value;
            if (propertyValues.containsKey(name)) {
                value = propertyValues.remove(name);
                if (!RecipeHelper.isInstance(type, value) && !RecipeHelper.isConvertable(type, value)) {
                    throw new ConstructionException("Invalid and non-convertable constructor parameter type: " +
                            "name=" + name + ", " +
                            "index=" + i + ", " +
                            "expected=" + RecipeHelper.toClass(type).getName() + ", " +
                            "actual=" + (value == null ? "null" : value.getClass().getName()));
                }
                value = RecipeHelper.convert(type, value, false);
            } else {
                value = getDefaultValue(RecipeHelper.toClass(type));
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
        Type getType();
        void setValue(Object instance, Object value) throws Exception;
    }

    public static class MethodMember implements Member {
        private final Method setter;

        public MethodMember(Method method) {
            this.setter = method;
        }

        public Type getType() {
            return setter.getGenericParameterTypes()[0];
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

        public Type getType() {
            return field.getGenericType();
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
