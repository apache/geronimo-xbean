/**
 *
 * Copyright 2005-2006 The Apache Software Foundation or its licensors, as applicable.
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
package org.apache.xbean.recipe;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.xbean.ClassLoading;
import org.apache.xbean.propertyeditor.PropertyEditors;
import org.apache.xbean.util.ParameterNames;
import org.apache.xbean.util.AsmParameterNames;

/**
 * @version $Rev: 6688 $ $Date: 2005-12-29T02:08:29.200064Z $
 */
public class ObjectRecipe implements Recipe {
    private static final ParameterNames parameterNames = new AsmParameterNames();
    // todo add instanceFactoryMethod
    private final String type;
    private final String factoryMethod;
    private final LinkedHashMap properties;
    private String postConstruct;
    private String preDestroy;

    public ObjectRecipe(Class type) {
        this(type.getName());
    }

    public ObjectRecipe(Class type, String factoryMethod) {
        this(type.getName(), factoryMethod);
    }

    public ObjectRecipe(Class type, Map properties) {
        this(type.getName(), properties);
    }

    public ObjectRecipe(String typeName, Map properties) {
        this(typeName, null, properties);
    }

    public ObjectRecipe(String typeName) {
        this(typeName, null, null);
    }

    public ObjectRecipe(String typeName, String factoryMethod) {
        this(typeName, factoryMethod, null);
    }

    private ObjectRecipe(String type, String factoryMethod, Map properties) {
        this.type = type;
        this.factoryMethod = factoryMethod;
        if (properties != null) {
            this.properties = new LinkedHashMap(properties);
            setAllProperties(properties);
        } else {
            this.properties = new LinkedHashMap();
        }
    }

    public Object getProperty(String name) {
        if (name == null) throw new NullPointerException("name is null");
        Object value = properties.get(name);
        return value;
    }

    public void setProperty(String name, Object value) {
        if (name == null) throw new NullPointerException("name is null");
        if (!RecipeHelper.isSimpleType(value)) {
            value = new ValueRecipe(value);
        }
        properties.put(name, value);
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

    public String getPostConstruct() {
        return postConstruct;
    }

    public void setPostConstruct(String postConstruct) {
        this.postConstruct = postConstruct;
    }

    public String getPreDestroy() {
        return preDestroy;
    }

    public void setPreDestroy(String preDestroy) {
        this.preDestroy = preDestroy;
    }

    public Object create() throws ConstructionException {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        return create(contextClassLoader);
    }

    public Object create(ClassLoader classLoader) throws ConstructionException {
        // load the type class
        Class typeClass = null;
        try {
            typeClass = ClassLoading.loadClass(type, classLoader);
        } catch (ClassNotFoundException e) {
            throw new ConstructionException("Type class could not be found: " + type);
        }

        // verify that is is a class we can construct
        if (!Modifier.isPublic(typeClass.getModifiers())) {
            throw new ConstructionException("Class is not public: " + ClassLoading.getClassName(typeClass, true));
        }
        if (Modifier.isInterface(typeClass.getModifiers())) {
            throw new ConstructionException("Class is an interface: " + ClassLoading.getClassName(typeClass, true));
        }
        if (Modifier.isAbstract(typeClass.getModifiers())) {
            throw new ConstructionException("Class is abstract: " + ClassLoading.getClassName(typeClass, true));
        }

        // get the PostConstruct method
        Method postConstructMethod = null;
        if (postConstruct != null) {
            try {
                postConstructMethod = typeClass.getMethod(postConstruct, null);
            } catch (NoSuchMethodException e) {
                throw new ConstructionException("Unable to find post construct method: " + postConstruct);
            }
        }

        // get object values for all recipe properties
        Map propertyValues = new LinkedHashMap(properties);
        for (Iterator iterator = propertyValues.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            Object value = entry.getValue();
            if (value instanceof Recipe) {
                Recipe recipe = ((Recipe) value);
                value = recipe.create(classLoader);
                entry.setValue(value);
            }
        }

        // determine which properties can be set via setters
        Map setters = new HashMap();
        Map closeMatch = new HashMap();
        for (Iterator iterator = propertyValues.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();
            try {
                Method setter = findSetter(typeClass, name, value);
                setters.put(name, setter);
            } catch (ConstructionException e) {
                closeMatch.put(name, e);
            }
        }

        // determine the properties that must be used
        Map requiredProperties = new HashMap(propertyValues);
        requiredProperties.keySet().removeAll(setters.keySet());

        // get the factory
        Factory factory = selectFactory(typeClass, requiredProperties);
        if (factory == null) {
            // todo use close match
            throw new ConstructionException("Unable to find a constructor with properties: " + requiredProperties);
        }

        // remove the parameters from the propertyValues map
        List parameters = removePropertyValues(factory, propertyValues);

        // create the instance
        Object instance = factory.create(parameters);

        // set remaining properties
        for (Iterator iterator = propertyValues.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String propertyName = (String) entry.getKey();

            Method setter = (Method) setters.get(propertyName);

            Object propertyValue = entry.getValue();
            propertyValue = convert(setter.getParameterTypes()[0], propertyValue);

            try {
                setter.invoke(instance, new Object[]{propertyValue});
            } catch (Exception e) {
                throw new ConstructionException("Error setting property: " + setter, e);
            }
        }

        try {
            postConstructMethod.invoke(instance, null);
        } catch(Exception e) {
            throw new ConstructionException("Error calling post construct method: " + postConstruct, e);
        }
        return instance;
    }

    private List removePropertyValues(Factory factory, Map propertyValues) {
        String[] parameterNames = factory.getParameterNames();
        Class[] parameterTypes = factory.getParameterTypes();
        List parameters = new ArrayList(parameterNames.length);
        for (int i = 0; i < parameterNames.length; i++) {
            String parameterName = parameterNames[i];
            Class parameterType = parameterTypes[i];
            Object value = removePropertyValue(propertyValues, parameterName, parameterType);
            parameters.add(value);
        }
        return parameters;
    }

    private Object removePropertyValue(Map propertyValues, String propertyName, Class propertyType) {
        Object value;
        if (propertyValues.containsKey(propertyName)) {
            value = propertyValues.remove(propertyName);
        } else {
            value = getDefaultValue(propertyType);
        }
        value = convert(propertyType, value);
        return value;
    }

    private static Object convert(Class type, Object value) {
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

    private Factory selectFactory(Class typeClass, Map requiredProperties) {
        if (factoryMethod != null) {
            SortedMap allMethodParameters = new TreeMap(ARGUMENT_LENGTH_COMPARATOR);
            allMethodParameters.putAll(parameterNames.getAllMethodParameters(typeClass, factoryMethod));

            CloseMatchException closeMatch = null;
            for (Iterator iterator = allMethodParameters.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry entry = (Map.Entry) iterator.next();
                Method method = (Method) entry.getKey();
                String[] parameterNames = (String[]) entry.getValue();

                if (parameterNames != null && Arrays.asList(parameterNames).containsAll(requiredProperties.keySet())) {
                    Class[] propertyTypes = getPropertyTypes(parameterNames, requiredProperties);
                    if (isAssignableFrom(method.getParameterTypes(), propertyTypes)) {
                        CloseMatchException problem = checkFactory(method);
                        if (problem == null) {
                            return new FactoryMethodFactory(parameterNames, method);
                        } else {
                            closeMatch = CloseMatchException.greater(closeMatch, problem);
                        }
                    } else {
                        closeMatch = CloseMatchException.greater(closeMatch, CloseMatchException.typeMismatch(method, parameterNames, propertyTypes));
                    }
                } else {
                    // todo remember most consuming method
                }
            }
            throw closeMatch;
        } else {
            SortedMap allConstructorParameters = new TreeMap(ARGUMENT_LENGTH_COMPARATOR);
            allConstructorParameters.putAll(parameterNames.getAllConstructorParameters(typeClass));

            CloseMatchException closeMatch = null;
            for (Iterator iterator = allConstructorParameters.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry entry = (Map.Entry) iterator.next();
                Constructor constructor = (Constructor) entry.getKey();
                String[] parameterNames = (String[]) entry.getValue();

                if (Arrays.asList(parameterNames).containsAll(requiredProperties.keySet())) {
                    Class[] propertyTypes = getPropertyTypes(parameterNames, requiredProperties);
                    if (isAssignableFrom(constructor.getParameterTypes(), propertyTypes)) {
                        return new ConstructorFactory(parameterNames, constructor);
                    } else {
                        closeMatch = CloseMatchException.greater(closeMatch, CloseMatchException.typeMismatch(constructor, parameterNames, propertyTypes));
                    }
                } else {
                    // todo remember most consuming method
                }
            }
            throw closeMatch;
        }
    }

    private Class[] getPropertyTypes(String[] propertyNames, Map properties) {
        Class[] propertyTypes = new Class[propertyNames.length];
        for (int i = 0; i < propertyNames.length; i++) {
            String parameterName = propertyNames[i];
            Object value = properties.get(parameterName);
            if (value != null) {
                propertyTypes[i] = value.getClass();
            }
        }
        return propertyTypes;
    }

    private static final ArgumentLengthComparator ARGUMENT_LENGTH_COMPARATOR = new ArgumentLengthComparator();

    private static class ArgumentLengthComparator implements Comparator {
        public int compare(Object left, Object right) {
            return getArgumentLength(left) - getArgumentLength(right);
        }

        private int getArgumentLength(Object object) {
            if (object instanceof Method) {
                return ((Method) object).getParameterTypes().length;
            } else {
                return ((Constructor) object).getParameterTypes().length;
            }
        }
    }

    private static Method findSetter(Class typeClass, String propertyName, Object propertyValue) {
        if (propertyName == null) throw new NullPointerException("name is null");
        if (propertyName.length() == 0) throw new IllegalArgumentException("name is an empty string");

        String setterName = "set" + Character.toUpperCase(propertyName.charAt(0));
        if (propertyName.length() > 0) {
            setterName += propertyName.substring(1);
        }

        int matchLevel = 0;
        ConstructionException missException = null;

        List methods = new ArrayList(Arrays.asList(typeClass.getMethods()));
        methods.addAll(Arrays.asList(typeClass.getDeclaredMethods()));
        for (Iterator iterator = methods.iterator(); iterator.hasNext();) {
            Method method = (Method) iterator.next();
            if (method.getName().equals(setterName)) {
                if (method.getParameterTypes().length == 0) {
                    if (matchLevel < 1) {
                        matchLevel = 1;
                        missException = new ConstructionException("Setter takes no parameters: " + method);
                    }
                    continue;
                }

                if (method.getParameterTypes().length > 1) {
                    if (matchLevel < 1) {
                        matchLevel = 1;
                        missException = new ConstructionException("Setter takes more then one parameter: " + method);
                    }
                    continue;
                }

                if (method.getReturnType() != Void.TYPE) {
                    if (matchLevel < 2) {
                        matchLevel = 2;
                        missException = new ConstructionException("Setter returns a value: " + method);
                    }
                    continue;
                }

                if (Modifier.isAbstract(method.getModifiers())) {
                    if (matchLevel < 3) {
                        matchLevel = 3;
                        missException = new ConstructionException("Setter is abstract: " + method);
                    }
                    continue;
                }

                if (!Modifier.isPublic(method.getModifiers())) {
                    if (matchLevel < 4) {
                        matchLevel = 4;
                        missException = new ConstructionException("Setter is not public: " + method);
                    }
                    continue;
                }

                if (Modifier.isStatic(method.getModifiers())) {
                    if (matchLevel < 4) {
                        matchLevel = 4;
                        missException = new ConstructionException("Setter is static: " + method);
                    }
                    continue;
                }

                Class methodParameterType = method.getParameterTypes()[0];
                if (methodParameterType.isPrimitive() && propertyValue == null) {
                    if (matchLevel < 6) {
                        matchLevel = 6;
                        missException = new ConstructionException("Null can not be assigned to " +
                                ClassLoading.getClassName(methodParameterType, true) + ": " + method);
                    }
                    continue;
                }


                if (!isInstance(methodParameterType, propertyValue) && !isConvertable(methodParameterType, propertyValue))
                {
                    if (matchLevel < 5) {
                        matchLevel = 5;
                        missException = new ConstructionException(ClassLoading.getClassName(propertyValue, true) + " can not be assigned or converted to " +
                                ClassLoading.getClassName(methodParameterType, true) + ": " + method);
                    }
                    continue;
                }
                return method;
            }

        }

        if (missException != null) {
            throw missException;
        } else {
            StringBuffer buffer = new StringBuffer("Unable to find a valid setter method: ");
            buffer.append("public void ").append(ClassLoading.getClassName(typeClass, true)).append(".");
            buffer.append(setterName).append("(").append(ClassLoading.getClassName(propertyValue, true)).append(")");
            throw new ConstructionException(buffer.toString());
        }
    }

    private CloseMatchException checkFactory(Method method) {
        if (!Modifier.isPublic(method.getModifiers())) {
            // this will never occur since private methods are not returned from
            // getMethod, but leave this here anyway, just to be safe
            return CloseMatchException.factoryMethodIsNotPublic(method);
        }

        if (!Modifier.isStatic(method.getModifiers())) {
            return CloseMatchException.factoryMethodIsNotStatic(method);
        }

        if (method.getReturnType().equals(Void.TYPE)) {
            return CloseMatchException.factoryMethodWithNoReturn(method);
        }

        if (method.getReturnType().isPrimitive()) {
            return CloseMatchException.factoryMethodReturnsPrimitive(method);
        }

        return null;
    }

    private static boolean isConvertable(Class methodParameterType, Object propertyValue) {
        return (propertyValue instanceof String && PropertyEditors.canConvert(methodParameterType));
    }

    private static boolean isInstance(Class type, Object instance) {
        if (type.isPrimitive()) {
            // for primitives the insance can't be null
            if (instance == null) {
                return false;
            }

            // verify instance is the correct wrapper type
            if (type.equals(boolean.class)) {
                return instance instanceof Boolean;
            } else if (type.equals(char.class)) {
                return instance instanceof Character;
            } else if (type.equals(byte.class)) {
                return instance instanceof Byte;
            } else if (type.equals(short.class)) {
                return instance instanceof Short;
            } else if (type.equals(int.class)) {
                return instance instanceof Integer;
            } else if (type.equals(long.class)) {
                return instance instanceof Long;
            } else if (type.equals(float.class)) {
                return instance instanceof Float;
            } else if (type.equals(double.class)) {
                return instance instanceof Double;
            } else {
                throw new AssertionError("Invalid primitve type: " + type);
            }
        }

        return instance == null || type.isInstance(instance);
    }

    public static boolean isAssignableFrom(Class expected, Class actual) {
        // if actual is null we are ok, since we can always use a default value
        if (actual == null) return true;

        if (expected.isPrimitive()) {
            // if actual is a String we are ok, since we can always have a converter to primitives
            if (actual.equals(String.class)) return true;

            // verify actual is the correct wrapper type
            if (expected.equals(boolean.class)) {
                return actual.equals(Boolean.class);
            } else if (expected.equals(char.class)) {
                return actual.equals(Character.class);
            } else if (expected.equals(byte.class)) {
                return actual.equals(Byte.class);
            } else if (expected.equals(short.class)) {
                return actual.equals(Short.class);
            } else if (expected.equals(int.class)) {
                return actual.equals(Integer.class);
            } else if (expected.equals(long.class)) {
                return actual.equals(Long.class);
            } else if (expected.equals(float.class)) {
                return actual.equals(Float.class);
            } else if (expected.equals(double.class)) {
                return actual.equals(Double.class);
            } else {
                throw new AssertionError("Invalid primitve type: " + expected);
            }
        }

        // actual can be cast to expected
        if (expected.isAssignableFrom(actual)) return true;

        // if actual is a string and we have a property editory we are ok
        return actual.equals(String.class) && PropertyEditors.canConvert(expected);
    }

    public static boolean isAssignableFrom(Class[] expectedTypes, Class[] actualTypes) {
        if (expectedTypes.length != actualTypes.length) {
            return false;
        }
        for (int i = 0; i < expectedTypes.length; i++) {
            Class expectedType = expectedTypes[i];
            Class actualType = actualTypes[i];
            if (!isAssignableFrom(expectedType, actualType)) {
                return false;
            }
        }
        return true;
    }

    private static interface Factory {
        Object create(List parameters);

        String[] getParameterNames();

        Class[] getParameterTypes();
    }

    private static class ConstructorFactory implements Factory {
        private final String[] parameterNames;
        private final Constructor constructor;

        public ConstructorFactory(String[] parameterNames, Constructor constructor) {
            this.parameterNames = parameterNames;
            this.constructor = constructor;
        }

        public String[] getParameterNames() {
            return parameterNames;
        }

        public Class[] getParameterTypes() {
            return constructor.getParameterTypes();
        }

        public Object create(List parameters) {
            Object[] parameterArray = parameters.toArray(new Object[parameters.size()]);
            try {
                Object object = constructor.newInstance(parameterArray);
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
    }

    private static class FactoryMethodFactory implements Factory {
        private final String[] parameterNames;
        private final Method factoryMethod;

        public FactoryMethodFactory(String[] parameterNames, Method factoryMethod) {
            this.parameterNames = parameterNames;
            this.factoryMethod = factoryMethod;
        }

        public String[] getParameterNames() {
            return parameterNames;
        }

        public Class[] getParameterTypes() {
            return factoryMethod.getParameterTypes();
        }

        public Object create(List parameters) {
            Object[] parameterArray = parameters.toArray(new Object[parameters.size()]);
            try {
                Object object = factoryMethod.invoke(null, parameterArray);
                return object;
            } catch (Exception e) {
                Throwable t = e;
                if (e instanceof InvocationTargetException) {
                    InvocationTargetException invocationTargetException = (InvocationTargetException) e;
                    if (invocationTargetException.getCause() != null) {
                        t = invocationTargetException.getCause();
                    }
                }
                throw new ConstructionException("Error invoking factory method: " + factoryMethod, t);
            }
        }
    }

}
