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

import org.apache.xbean.propertyeditor.PropertyEditors;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * @version $Rev: 6688 $ $Date: 2005-12-29T02:08:29.200064Z $
 */
public class ObjectRecipe extends AbstractRecipe {
    private final String type;
    private final String factoryMethod;
    private final String[] constructorArgNames;
    private final Class[] constructorArgTypes;
    private final LinkedHashMap<Property,Object> properties;
    private final List<Option> options = new ArrayList<Option>();
    private final Map<String,Object> unsetProperties = new LinkedHashMap<String,Object>();

    public ObjectRecipe(Class type) {
        this(type.getName());
    }

    public ObjectRecipe(Class type, String factoryMethod) {
        this(type.getName(), factoryMethod);
    }

    public ObjectRecipe(Class type, Map<String,Object> properties) {
        this(type.getName(), properties);
    }

    public ObjectRecipe(Class type, String[] constructorArgNames, Class[] constructorArgTypes) {
        this(type.getName(), constructorArgNames, constructorArgTypes);
    }

    public ObjectRecipe(Class type, String factoryMethod, String[] constructorArgNames, Class[] constructorArgTypes) {
        this(type.getName(), factoryMethod, constructorArgNames, constructorArgTypes);
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

    public ObjectRecipe(String typeName, String[] constructorArgNames, Class[] constructorArgTypes) {
        this(typeName, null, constructorArgNames, constructorArgTypes, null);
    }

    public ObjectRecipe(String typeName, String factoryMethod, String[] constructorArgNames, Class[] constructorArgTypes) {
        this(typeName, factoryMethod, constructorArgNames, constructorArgTypes, null);
    }

    public ObjectRecipe(String type, String factoryMethod, String[] constructorArgNames, Class[] constructorArgTypes, Map<String,Object> properties) {
        options.add(Option.FIELD_INJECTION);
        
        this.type = type;
        this.factoryMethod = factoryMethod;
        if (constructorArgNames != null) {
            this.constructorArgNames = constructorArgNames;
        } else {
            this.constructorArgNames = new String[0];
        }
        if (constructorArgTypes != null) {
            this.constructorArgTypes = constructorArgTypes;
        } else {
            this.constructorArgTypes = new Class[0];
        }
        if (properties != null) {
            this.properties = new LinkedHashMap<Property,Object>();
            setAllProperties(properties);
        } else {
            this.properties = new LinkedHashMap<Property,Object>();
        }
    }

    public void allow(Option option){
        options.add(option);
    }

    public void disallow(Option option){
        options.remove(option);
    }

    public Object getProperty(String name) {
        Object value = properties.get(new Property(name));
        return value;
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


    public void setAllProperties(Map map) {
        if (map == null) throw new NullPointerException("map is null");
        for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();
            setProperty(name, value);
        }
    }

    public Map<String,Object> getUnsetProperties() {
        return unsetProperties;
    }

    public boolean canCreate(Class type, ClassLoader classLoader) {
        Class myType = getType(classLoader);
        return type.isAssignableFrom(myType);
    }

    public Object create(ClassLoader classLoader) throws ConstructionException {
        unsetProperties.clear();
        // load the type class
        Class typeClass = getType(classLoader);

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

        // find the factory method is one is declared
        Method factoryMethod = null;
        if (this.factoryMethod != null) {
            factoryMethod = findFactoryMethod(typeClass, this.factoryMethod);
        }

        // create the instance
        Object result;
        if (factoryMethod != null && Modifier.isStatic(factoryMethod.getModifiers())) {
            result = createInstance(factoryMethod, propertyValues, classLoader);
        } else {
            Constructor constructor = selectConstructor(typeClass);
            result = createInstance(constructor, propertyValues, classLoader);
        }
        Object instance = result;

        boolean allowPrivate = options.contains(Option.PRIVATE_PROPERTIES);
        boolean ignoreMissingProperties = options.contains(Option.IGNORE_MISSING_PROPERTIES);

        // set remaining properties
        for (Map.Entry<Property, Object> entry : RecipeHelper.prioritizeProperties(propertyValues)) {
            Property propertyName = entry.getKey();
            Object propertyValue = entry.getValue();

            setProperty(instance, propertyName, propertyValue, allowPrivate, ignoreMissingProperties, classLoader);
        }

        // call instance factory method
        if (factoryMethod != null && !Modifier.isStatic(factoryMethod.getModifiers())) {
            try {
                instance = factoryMethod.invoke(instance);
            } catch (Exception e) {
                Throwable t = e;
                if (e instanceof InvocationTargetException) {
                    InvocationTargetException invocationTargetException = (InvocationTargetException) e;
                    if (invocationTargetException.getCause() != null) {
                        t = invocationTargetException.getCause();
                    }
                }
                throw new ConstructionException("Error calling factory method: " + factoryMethod, t);
            }
        }

        return instance;
    }

    private Class getType(ClassLoader classLoader) {
        Class typeClass = null;
        try {
            typeClass = Class.forName(type, true, classLoader);
        } catch (ClassNotFoundException e) {
            throw new ConstructionException("Type class could not be found: " + type);
        }
        return typeClass;
    }

    private void setProperty(Object instance, Property propertyName, Object propertyValue, boolean allowPrivate, boolean ignoreMissingProperties, ClassLoader classLoader) {
        Member member;
        try {
            if (propertyName instanceof SetterProperty){
                member = new MethodMember(findSetter(instance.getClass(), propertyName.name, propertyValue, allowPrivate, classLoader));
            } else if (propertyName instanceof FieldProperty){
                member = new FieldMember(findField(instance.getClass(), propertyName.name, propertyValue, allowPrivate, classLoader));
            } else {
                try {
                    member = new MethodMember(findSetter(instance.getClass(), propertyName.name, propertyValue, allowPrivate, classLoader));
                } catch (MissingAccessorException noSetter) {
                    if (!options.contains(Option.FIELD_INJECTION)) {
                        throw noSetter;
                    }

                    try {
                        member = new FieldMember(findField(instance.getClass(), propertyName.name, propertyValue, allowPrivate, classLoader));
                    } catch (MissingAccessorException noField) {
                        throw (noField.getMatchLevel() > noSetter.getMatchLevel())? noField: noSetter;
                    }
                }
            }
        } catch (MissingAccessorException e) {
            if (ignoreMissingProperties){
                unsetProperties.put(propertyName.name, propertyValue);
                return;
            } else {
                throw e;
            }
        }

        try {
            propertyValue = convert(member.getType(), propertyValue, classLoader);
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

    private Object[] extractConstructorArgs(Map propertyValues, Class[] constructorArgTypes, ClassLoader classLoader) {
        Object[] parameters = new Object[constructorArgNames.length];
        for (int i = 0; i < constructorArgNames.length; i++) {
            Property name = new Property(constructorArgNames[i]);
            Class type = constructorArgTypes[i];

            Object value;
            if (propertyValues.containsKey(name)) {
                value = propertyValues.remove(name);
                if (!RecipeHelper.isInstance(type, value) && !RecipeHelper.isConvertable(type, value, classLoader)) {
                    throw new ConstructionException("Invalid and non-convertable constructor parameter type: " +
                            "name=" + name + ", " +
                            "index=" + i + ", " +
                            "expected=" + type.getName() + ", " +
                            "actual=" + getClassName(value));
                }
                value = convert(type, value, classLoader);
            } else {
                value = getDefaultValue(type);
            }


            parameters[i] = value;
        }
        return parameters;
    }

    private static String getClassName(Object value) {
        if (value == null) return "null";

        return value.getClass().getName();
    }

    private Object convert(Class type, Object value, ClassLoader classLoader) {
        if (value instanceof Recipe) {
            if (value instanceof SecretRecipe) {
                SecretRecipe recipe = (SecretRecipe) value;
                value = recipe.create(this, classLoader);
            } else {
                Recipe recipe = (Recipe) value;
                value = recipe.create(classLoader);
            }
        }

        if (value instanceof String && (type != Object.class)) {
            String stringValue = (String) value;
            value = PropertyEditors.getValue(type, stringValue);
        }
        return value;
    }

    private static Object getDefaultValue(Class type) {
        if (type.equals(Boolean.TYPE)) {
            return Boolean.FALSE;
        } else if (type.equals(Character.TYPE)) {
            return new Character((char) 0);
        } else if (type.equals(Byte.TYPE)) {
            return new Byte((byte) 0);
        } else if (type.equals(Short.TYPE)) {
            return new Short((short) 0);
        } else if (type.equals(Integer.TYPE)) {
            return new Integer(0);
        } else if (type.equals(Long.TYPE)) {
            return new Long(0);
        } else if (type.equals(Float.TYPE)) {
            return new Float(0);
        } else if (type.equals(Double.TYPE)) {
            return new Double(0);
        }
        return null;
    }

    private Object createInstance(Constructor constructor, Map propertyValues, ClassLoader classLoader) {
        // get the constructor parameters
        Object[] parameters = extractConstructorArgs(propertyValues, constructor.getParameterTypes(), classLoader);

        try {
            Object object = constructor.newInstance(parameters);
            return object;
        } catch (Exception e) {
            Throwable t = e;
            if (e instanceof InvocationTargetException) {
                InvocationTargetException invocationTargetException = (InvocationTargetException) e;
                if (invocationTargetException.getCause() != null) {
                    t = invocationTargetException.getCause();
                }
            }
            throw new ConstructionException("Error invoking constructor: " + constructor, t);
        }
    }

    private Object createInstance(Method method, Map propertyValues, ClassLoader classLoader) {
        // get the constructor parameters
        Object[] parameters = extractConstructorArgs(propertyValues, method.getParameterTypes(), classLoader);

        try {
            Object object = method.invoke(null, parameters);
            return object;
        } catch (Exception e) {
            Throwable t = e;
            if (e instanceof InvocationTargetException) {
                InvocationTargetException invocationTargetException = (InvocationTargetException) e;
                if (invocationTargetException.getCause() != null) {
                    t = invocationTargetException.getCause();
                }
            }
            throw new ConstructionException("Error invoking factory method: " + method, t);
        }
    }

    private Constructor selectConstructor(Class typeClass) {
        if (constructorArgNames.length > 0 && constructorArgTypes.length == 0) {
            ArrayList<Constructor> matches = new ArrayList<Constructor>();

            Constructor[] constructors = typeClass.getConstructors();
            for (Constructor constructor : constructors) {
                if (constructor.getParameterTypes().length == constructorArgNames.length) {
                    matches.add(constructor);
                }
            }

            if (matches.size() < 1) {
                StringBuffer buffer = new StringBuffer("No parameter types supplied; unable to find a potentially valid constructor: ");
                buffer.append("constructor= public ").append(typeClass.getName());
                buffer.append(toArgumentList(constructorArgNames));
                throw new ConstructionException(buffer.toString());
            } else if (matches.size() > 1) {
                StringBuffer buffer = new StringBuffer("No parameter types supplied; found too many potentially valid constructors: ");
                buffer.append("constructor= public ").append(typeClass.getName());
                buffer.append(toArgumentList(constructorArgNames));
                throw new ConstructionException(buffer.toString());
            }

            return matches.get(0);
        }

        try {
            Constructor constructor = typeClass.getConstructor(constructorArgTypes);

            if (!Modifier.isPublic(constructor.getModifiers())) {
                // this will never occur since private constructors are not returned from
                // getConstructor, but leave this here anyway, just to be safe
                throw new ConstructionException("Constructor is not public: " + constructor);
            }

            return constructor;
        } catch (NoSuchMethodException e) {
            // try to find a matching private method
            Constructor[] constructors = typeClass.getDeclaredConstructors();
            for (Constructor constructor : constructors) {
                if (isAssignableFrom(constructorArgTypes, constructor.getParameterTypes())) {
                    if (!Modifier.isPublic(constructor.getModifiers())) {
                        throw new ConstructionException("Constructor is not public: " + constructor);
                    }
                }
            }

            StringBuffer buffer = new StringBuffer("Unable to find a valid constructor: ");
            buffer.append("constructor= public ").append(typeClass.getName());
            buffer.append(toParameterList(constructorArgTypes));
            throw new ConstructionException(buffer.toString());
        }
    }

    private String toParameterList(Class[] parameterTypes) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("(");
        for (int i = 0; i < parameterTypes.length; i++) {
            Class type = parameterTypes[i];
            if (i > 0) buffer.append(", ");
            buffer.append(type.getName());
        }
        buffer.append(")");
        return buffer.toString();
    }

    private String toArgumentList(String[] parameterNames) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("(");
        for (int i = 0; i < parameterNames.length; i++) {
            String parameterName = parameterNames[i];
            if (i > 0) buffer.append(", ");
            buffer.append('<').append(parameterName).append('>');
        }
        buffer.append(")");
        return buffer.toString();
    }

    public Method findFactoryMethod(Class typeClass, String factoryMethod) {
        if (factoryMethod == null) throw new NullPointerException("name is null");
        if (factoryMethod.length() == 0) throw new IllegalArgumentException("name is an empty string");

        int matchLevel = 0;
        MissingFactoryMethodException missException = null;

        List<Method> methods = new ArrayList<Method>(Arrays.asList(typeClass.getMethods()));
        methods.addAll(Arrays.asList(typeClass.getDeclaredMethods()));
        for (Method method : methods) {
            if (method.getName().equals(factoryMethod)) {
                if (Modifier.isStatic(method.getModifiers())) {
                    if (method.getParameterTypes().length != constructorArgNames.length) {
                        if (matchLevel < 1) {
                            matchLevel = 1;
                            missException = new MissingFactoryMethodException("Static factory method has " + method.getParameterTypes().length + " arugments " +
                                    "but expected " + constructorArgNames.length + " arguments: " + method);
                        }
                        continue;
                    }

                    if (constructorArgTypes.length > 0 && !isAssignableFrom(constructorArgTypes, method.getParameterTypes())) {
                        if (matchLevel < 2) {
                            matchLevel = 2;
                            missException = new MissingFactoryMethodException("Static factory method has signature " +
                                    "public static " + typeClass.getName() + "." + factoryMethod + toParameterList(method.getParameterTypes()) +
                                    " but expected signature " +
                                    "public static " + typeClass.getName() + "." + factoryMethod + toParameterList(constructorArgTypes));
                        }
                        continue;
                    }
                } else {
                    if (method.getParameterTypes().length != 0) {
                        if (matchLevel < 1) {
                            matchLevel = 1;
                            missException = new MissingFactoryMethodException("Instance factory method has parameters: " + method);
                        }
                        continue;
                    }
                }

                if (method.getReturnType() == Void.TYPE) {
                    if (matchLevel < 3) {
                        matchLevel = 3;
                        missException = new MissingFactoryMethodException("Factory method does not return a value: " + method);
                    }
                    continue;
                }

                if (Modifier.isAbstract(method.getModifiers())) {
                    if (matchLevel < 4) {
                        matchLevel = 4;
                        missException = new MissingFactoryMethodException("Factory method is abstract: " + method);
                    }
                    continue;
                }

                if (!Modifier.isPublic(method.getModifiers())) {
                    if (matchLevel < 5) {
                        matchLevel = 5;
                        missException = new MissingFactoryMethodException("Factory method is not public: " + method);
                    }
                    continue;
                }

                return method;
            }
        }

        if (missException != null) {
            throw missException;
        } else {
            StringBuffer buffer = new StringBuffer("Unable to find a valid factory method: ");
            buffer.append("public void ").append(typeClass.getName()).append(".");
            buffer.append(factoryMethod).append("()");
            throw new MissingFactoryMethodException(buffer.toString());
        }
    }

    public static Method findSetter(Class typeClass, String propertyName, Object propertyValue, boolean allowPrivate, ClassLoader classLoader) {
        if (propertyName == null) throw new NullPointerException("name is null");
        if (propertyName.length() == 0) throw new IllegalArgumentException("name is an empty string");

        String setterName = "set" + Character.toUpperCase(propertyName.charAt(0));
        if (propertyName.length() > 0) {
            setterName += propertyName.substring(1);
        }


        int matchLevel = 0;
        MissingAccessorException missException = null;

        List<Method> methods = new ArrayList<Method>(Arrays.asList(typeClass.getMethods()));
        methods.addAll(Arrays.asList(typeClass.getDeclaredMethods()));
        for (Method method : methods) {
            if (method.getName().equals(setterName)) {
                if (method.getParameterTypes().length == 0) {
                    if (matchLevel < 1) {
                        matchLevel = 1;
                        missException = new MissingAccessorException("Setter takes no parameters: " + method, matchLevel);
                    }
                    continue;
                }

                if (method.getParameterTypes().length > 1) {
                    if (matchLevel < 1) {
                        matchLevel = 1;
                        missException = new MissingAccessorException("Setter takes more then one parameter: " + method, matchLevel);
                    }
                    continue;
                }

                if (method.getReturnType() != Void.TYPE) {
                    if (matchLevel < 2) {
                        matchLevel = 2;
                        missException = new MissingAccessorException("Setter returns a value: " + method, matchLevel);
                    }
                    continue;
                }

                if (Modifier.isAbstract(method.getModifiers())) {
                    if (matchLevel < 3) {
                        matchLevel = 3;
                        missException = new MissingAccessorException("Setter is abstract: " + method, matchLevel);
                    }
                    continue;
                }

                if (!allowPrivate && !Modifier.isPublic(method.getModifiers())) {
                    if (matchLevel < 4) {
                        matchLevel = 4;
                        missException = new MissingAccessorException("Setter is not public: " + method, matchLevel);
                    }
                    continue;
                }

                if (Modifier.isStatic(method.getModifiers())) {
                    if (matchLevel < 4) {
                        matchLevel = 4;
                        missException = new MissingAccessorException("Setter is static: " + method, matchLevel);
                    }
                    continue;
                }

                Class methodParameterType = method.getParameterTypes()[0];
                if (methodParameterType.isPrimitive() && propertyValue == null) {
                    if (matchLevel < 6) {
                        matchLevel = 6;
                        missException = new MissingAccessorException("Null can not be assigned to " +
                                methodParameterType.getName() + ": " + method, matchLevel);
                    }
                    continue;
                }


                if (!RecipeHelper.isInstance(methodParameterType, propertyValue) && !RecipeHelper.isConvertable(methodParameterType, propertyValue, classLoader)) {
                    if (matchLevel < 5) {
                        matchLevel = 5;
                        missException = new MissingAccessorException(getClassName(propertyValue) + " can not be assigned or converted to " +
                                methodParameterType.getName() + ": " + method, matchLevel);
                    }
                    continue;
                }

                if (allowPrivate && !Modifier.isPublic(method.getModifiers())) {
                    setAccessible(method);
                }

                return method;
            }

        }

        if (missException != null) {
            throw missException;
        } else {
            StringBuffer buffer = new StringBuffer("Unable to find a valid setter method: ");
            buffer.append("public void ").append(typeClass.getName()).append(".");
            buffer.append(setterName).append("(").append(getClassName(propertyValue)).append(")");
            throw new MissingAccessorException(buffer.toString(), -1);
        }
    }

    public static Field findField(Class typeClass, String propertyName, Object propertyValue, boolean allowPrivate, ClassLoader classLoader) {
        if (propertyName == null) throw new NullPointerException("name is null");
        if (propertyName.length() == 0) throw new IllegalArgumentException("name is an empty string");

        int matchLevel = 0;
        MissingAccessorException missException = null;

        List<Field> fields = new ArrayList<Field>(Arrays.asList(typeClass.getDeclaredFields()));
        Class parent = typeClass.getSuperclass();
        while (parent != null){
            fields.addAll(Arrays.asList(parent.getDeclaredFields()));
            parent = parent.getSuperclass();
        }

        for (Field field : fields) {
            if (field.getName().equals(propertyName)) {

                if (!allowPrivate && !Modifier.isPublic(field.getModifiers())) {
                    if (matchLevel < 4) {
                        matchLevel = 4;
                        missException = new MissingAccessorException("Field is not public: " + field, matchLevel);
                    }
                    continue;
                }

                if (Modifier.isStatic(field.getModifiers())) {
                    if (matchLevel < 4) {
                        matchLevel = 4;
                        missException = new MissingAccessorException("Field is static: " + field, matchLevel);
                    }
                    continue;
                }

                Class fieldType = field.getType();
                if (fieldType.isPrimitive() && propertyValue == null) {
                    if (matchLevel < 6) {
                        matchLevel = 6;
                        missException = new MissingAccessorException("Null can not be assigned to " +
                                fieldType.getName() + ": " + field, matchLevel);
                    }
                    continue;
                }


                if (!RecipeHelper.isInstance(fieldType, propertyValue) && !RecipeHelper.isConvertable(fieldType, propertyValue, classLoader)) {
                    if (matchLevel < 5) {
                        matchLevel = 5;
                        missException = new MissingAccessorException(getClassName(propertyValue) + " can not be assigned or converted to " +
                                fieldType.getName() + ": " + field, matchLevel);
                    }
                    continue;
                }

                if (allowPrivate && !Modifier.isPublic(field.getModifiers())) {
                    setAccessible(field);
                }

                return field;
            }

        }

        if (missException != null) {
            throw missException;
        } else {
            StringBuffer buffer = new StringBuffer("Unable to find a valid field: ");
            buffer.append("public ").append(" ").append(getClassName(propertyValue));
            buffer.append(" ").append(propertyName).append(";");
            throw new MissingAccessorException(buffer.toString(), -1);
        }
    }

    public static boolean isAssignableFrom(Class[] expectedTypes, Class[] actualTypes) {
        if (expectedTypes.length != actualTypes.length) {
            return false;
        }
        for (int i = 0; i < expectedTypes.length; i++) {
            Class expectedType = expectedTypes[i];
            Class actualType = actualTypes[i];
            if (expectedType != actualType && !RecipeHelper.isAssignableFrom(expectedType, actualType)) {
                return false;
            }
        }
        return true;
    }

    private static void setAccessible(final Method method) {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                method.setAccessible(true);
                return null;
            }
        });
    }

    private static void setAccessible(final Field field) {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                field.setAccessible(true);
                return null;
            }
        });
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

    private static class Property {
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

    private static class SetterProperty extends Property {
        public SetterProperty(String name) {
            super(name);
        }
        public int hashCode() {
            return super.hashCode()+2;
        }
        public String toString() {
            return "[setter] "+toString();
        }

    }
    private static class FieldProperty extends Property {
        public FieldProperty(String name) {
            super(name);
        }

        public int hashCode() {
            return super.hashCode()+1;
        }
        public String toString() {
            return "[field] "+toString();
        }
    }
}
