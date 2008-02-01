/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static org.apache.xbean.recipe.RecipeHelper.isAssignableFrom;

public final class ReflectionUtil {
    private ReflectionUtil() {
    }

    public static Field findField(Class typeClass, String propertyName, Object propertyValue, EnumSet<Option> options) {
        if (typeClass == null) throw new NullPointerException("typeClass is null");
        if (propertyName == null) throw new NullPointerException("name is null");
        if (propertyName.length() == 0) throw new IllegalArgumentException("name is an empty string");
        if (options == null) options = EnumSet.noneOf(Option.class);

        int matchLevel = 0;
        MissingAccessorException missException = null;

        if (propertyName.contains("/")){
            String[] strings = propertyName.split("/");
            if (strings == null || strings.length != 2) throw new IllegalArgumentException("badly formed <class>/<attribute> property name: " + propertyName);

            String className = strings[0];
            propertyName = strings[1];

            boolean found = false;
            while(!typeClass.equals(Object.class) && !found){
                if (typeClass.getName().equals(className)){
                    found = true;
                    break;
                } else {
                    typeClass = typeClass.getSuperclass();
                }
            }

            if (!found) throw new MissingAccessorException("Type not assignable to class: " + className, -1);
        }

        List<Field> fields = new ArrayList<Field>(Arrays.asList(typeClass.getDeclaredFields()));
        Class parent = typeClass.getSuperclass();
        while (parent != null){
            fields.addAll(Arrays.asList(parent.getDeclaredFields()));
            parent = parent.getSuperclass();
        }

        boolean allowPrivate = options.contains(Option.PRIVATE_PROPERTIES);
        boolean allowStatic = options.contains(Option.STATIC_PROPERTIES);
        boolean caseInsesnitive = options.contains(Option.CASE_INSENSITIVE_PROPERTIES);

        for (Field field : fields) {
            if (field.getName().equals(propertyName) || (caseInsesnitive && field.getName().equalsIgnoreCase(propertyName))) {

                if (!allowPrivate && !Modifier.isPublic(field.getModifiers())) {
                    if (matchLevel < 4) {
                        matchLevel = 4;
                        missException = new MissingAccessorException("Field is not public: " + field, matchLevel);
                    }
                    continue;
                }

                if (!allowStatic && Modifier.isStatic(field.getModifiers())) {
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


                if (!RecipeHelper.isInstance(fieldType, propertyValue) && !RecipeHelper.isConvertable(fieldType, propertyValue)) {
                    if (matchLevel < 5) {
                        matchLevel = 5;
                        missException = new MissingAccessorException((propertyValue == null ? "null" : propertyValue.getClass().getName()) + " can not be assigned or converted to " +
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
            buffer.append("public ").append(" ").append(propertyValue == null ? "null" : propertyValue.getClass().getName());
            buffer.append(" ").append(propertyName).append(";");
            throw new MissingAccessorException(buffer.toString(), -1);
        }
    }

    public static Method findSetter(Class typeClass, String propertyName, Object propertyValue, EnumSet<Option> options) {
        if (typeClass == null) throw new NullPointerException("typeClass is null");
        if (propertyName == null) throw new NullPointerException("name is null");
        if (propertyName.length() == 0) throw new IllegalArgumentException("name is an empty string");
        if (options == null) options = EnumSet.noneOf(Option.class);

        if (propertyName.contains("/")){
            String[] strings = propertyName.split("/");
            if (strings == null || strings.length != 2) throw new IllegalArgumentException("badly formed <class>/<attribute> property name: " + propertyName);

            String className = strings[0];
            propertyName = strings[1];

            boolean found = false;
            while(!typeClass.equals(Object.class) && !found){
                if (typeClass.getName().equals(className)){
                    found = true;
                    break;
                } else {
                    typeClass = typeClass.getSuperclass();
                }
            }

            if (!found) throw new MissingAccessorException("Type not assignable to class: " + className, -1);
        }

        String setterName = "set" + Character.toUpperCase(propertyName.charAt(0));
        if (propertyName.length() > 0) {
            setterName += propertyName.substring(1);
        }


        int matchLevel = 0;
        MissingAccessorException missException = null;

        boolean allowPrivate = options.contains(Option.PRIVATE_PROPERTIES);
        boolean allowStatic = options.contains(Option.STATIC_PROPERTIES);
        boolean caseInsesnitive = options.contains(Option.CASE_INSENSITIVE_PROPERTIES);

        List<Method> methods = new ArrayList<Method>(Arrays.asList(typeClass.getMethods()));
        methods.addAll(Arrays.asList(typeClass.getDeclaredMethods()));
        for (Method method : methods) {
            if (method.getName().equals(setterName) || (caseInsesnitive && method.getName().equalsIgnoreCase(setterName))) {
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

                if (!allowStatic && Modifier.isStatic(method.getModifiers())) {
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


                if (!RecipeHelper.isInstance(methodParameterType, propertyValue) && !RecipeHelper.isConvertable(methodParameterType, propertyValue)) {
                    if (matchLevel < 5) {
                        matchLevel = 5;
                        missException = new MissingAccessorException((propertyValue == null ? "null" : propertyValue.getClass().getName()) + " can not be assigned or converted to " +
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
            buffer.append(setterName).append("(").append(propertyValue == null ? "null" : propertyValue.getClass().getName()).append(")");
            throw new MissingAccessorException(buffer.toString(), -1);
        }
    }

    public static Constructor findConstructor(Class typeClass, Class[] argTypes, EnumSet<Option> options) {
        if (typeClass == null) throw new NullPointerException("typeClass is null");
        if (argTypes == null) throw new NullPointerException("argTypes is null");
        if (options == null) options = EnumSet.noneOf(Option.class);

        int matchLevel = 0;
        MissingFactoryMethodException missException = null;

        boolean allowPrivate = options.contains(Option.PRIVATE_CONSTRUCTOR);

        List<Constructor> constructors = new ArrayList<Constructor>(Arrays.asList(typeClass.getConstructors()));
        constructors.addAll(Arrays.asList(typeClass.getDeclaredConstructors()));
        for (Constructor constructor : constructors) {
            if (constructor.getParameterTypes().length != argTypes.length) {
                if (matchLevel < 1) {
                    matchLevel = 1;
                    missException = new MissingFactoryMethodException("Constructor has " + constructor.getParameterTypes().length + " arugments " +
                            "but expected " + argTypes.length + " arguments: " + constructor);
                }
                continue;
            }

            if (!isAssignableFrom(argTypes, constructor.getParameterTypes())) {
                if (matchLevel < 2) {
                    matchLevel = 2;
                    missException = new MissingFactoryMethodException("Constructor has signature " +
                            "public static " + typeClass.getName() + toParameterList(constructor.getParameterTypes()) +
                            " but expected signature " +
                            "public static " + typeClass.getName() + toParameterList(argTypes));
                }
                continue;
            }

            if (Modifier.isAbstract(constructor.getModifiers())) {
                if (matchLevel < 4) {
                    matchLevel = 4;
                    missException = new MissingFactoryMethodException("Constructor is abstract: " + constructor);
                }
                continue;
            }

            if (!allowPrivate && !Modifier.isPublic(constructor.getModifiers())) {
                if (matchLevel < 5) {
                    matchLevel = 5;
                    missException = new MissingFactoryMethodException("Constructor is not public: " + constructor);
                }
                continue;
            }

            if (allowPrivate && !Modifier.isPublic(constructor.getModifiers())) {
                setAccessible(constructor);
            }

            return constructor;
        }

        if (missException != null) {
            throw missException;
        } else {
            StringBuffer buffer = new StringBuffer("Unable to find a valid constructor: ");
            buffer.append("public void ").append(typeClass.getName()).append(toParameterList(argTypes));
            throw new ConstructionException(buffer.toString());
        }
    }

    public static Method findStaticFactory(Class typeClass, String factoryMethod, Class[] argTypes, EnumSet<Option> options) {
        if (typeClass == null) throw new NullPointerException("typeClass is null");
        if (factoryMethod == null) throw new NullPointerException("name is null");
        if (factoryMethod.length() == 0) throw new IllegalArgumentException("name is an empty string");
        if (argTypes == null) throw new NullPointerException("argTypes is null");
        if (options == null) options = EnumSet.noneOf(Option.class);

        int matchLevel = 0;
        MissingFactoryMethodException missException = null;

        boolean allowPrivate = options.contains(Option.PRIVATE_FACTORY);
        boolean caseInsesnitive = options.contains(Option.CASE_INSENSITIVE_FACTORY);

        List<Method> methods = new ArrayList<Method>(Arrays.asList(typeClass.getMethods()));
        methods.addAll(Arrays.asList(typeClass.getDeclaredMethods()));
        for (Method method : methods) {
            if (method.getName().equals(factoryMethod) || (caseInsesnitive && method.getName().equalsIgnoreCase(method.getName()))) {
                if (method.getParameterTypes().length != argTypes.length) {
                    if (matchLevel < 1) {
                        matchLevel = 1;
                        missException = new MissingFactoryMethodException("Static factory method has " + method.getParameterTypes().length + " arugments " +
                                "but expected " + argTypes.length + " arguments: " + method);
                    }
                    continue;
                }

                if (!isAssignableFrom(argTypes, method.getParameterTypes())) {
                    if (matchLevel < 2) {
                        matchLevel = 2;
                        missException = new MissingFactoryMethodException("Static factory method has signature " +
                                "public static " + typeClass.getName() + "." + factoryMethod + toParameterList(method.getParameterTypes()) +
                                " but expected signature " +
                                "public static " + typeClass.getName() + "." + factoryMethod + toParameterList(argTypes));
                    }
                    continue;
                }

                if (method.getReturnType() == Void.TYPE) {
                    if (matchLevel < 3) {
                        matchLevel = 3;
                        missException = new MissingFactoryMethodException("Static factory method does not return a value: " + method);
                    }
                    continue;
                }

                if (Modifier.isAbstract(method.getModifiers())) {
                    if (matchLevel < 4) {
                        matchLevel = 4;
                        missException = new MissingFactoryMethodException("Static factory method is abstract: " + method);
                    }
                    continue;
                }

                if (!allowPrivate && !Modifier.isPublic(method.getModifiers())) {
                    if (matchLevel < 5) {
                        matchLevel = 5;
                        missException = new MissingFactoryMethodException("Static factory method is not public: " + method);
                    }
                    continue;
                }

                if (!Modifier.isStatic(method.getModifiers())) {
                    if (matchLevel < 6) {
                        matchLevel = 6;
                        missException = new MissingFactoryMethodException("Static factory method is not static: " + method);
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
            StringBuffer buffer = new StringBuffer("Unable to find a valid factory method: ");
            buffer.append("public void ").append(typeClass.getName()).append(".");
            buffer.append(factoryMethod).append(toParameterList(argTypes));
            throw new MissingFactoryMethodException(buffer.toString());
        }
    }

    public static Method findInstanceFactory(Class typeClass, String factoryMethod, EnumSet<Option> options) {
        if (typeClass == null) throw new NullPointerException("typeClass is null");
        if (factoryMethod == null) throw new NullPointerException("name is null");
        if (factoryMethod.length() == 0) throw new IllegalArgumentException("name is an empty string");
        if (options == null) options = EnumSet.noneOf(Option.class);
        
        int matchLevel = 0;
        MissingFactoryMethodException missException = null;

        boolean allowPrivate = options.contains(Option.PRIVATE_FACTORY);
        boolean caseInsesnitive = options.contains(Option.CASE_INSENSITIVE_FACTORY);

        List<Method> methods = new ArrayList<Method>(Arrays.asList(typeClass.getMethods()));
        methods.addAll(Arrays.asList(typeClass.getDeclaredMethods()));
        for (Method method : methods) {
            if (method.getName().equals(factoryMethod) || (caseInsesnitive && method.getName().equalsIgnoreCase(method.getName()))) {
                if (Modifier.isStatic(method.getModifiers())) {
                    if (matchLevel < 1) {
                        matchLevel = 1;
                        missException = new MissingFactoryMethodException("Instance factory method is static: " + method);
                    }
                    continue;
                }

                if (method.getParameterTypes().length != 0) {
                    if (matchLevel < 2) {
                        matchLevel = 2;
                        missException = new MissingFactoryMethodException("Instance factory method has signature " +
                                "public " + typeClass.getName() + "." + factoryMethod + toParameterList(method.getParameterTypes()) +
                                " but expected signature " +
                                "public " + typeClass.getName() + "." + factoryMethod + "()");
                    }
                    continue;
                }

                if (method.getReturnType() == Void.TYPE) {
                    if (matchLevel < 3) {
                        matchLevel = 3;
                        missException = new MissingFactoryMethodException("Instance factory method does not return a value: " + method);
                    }
                    continue;
                }

                if (Modifier.isAbstract(method.getModifiers())) {
                    if (matchLevel < 4) {
                        matchLevel = 4;
                        missException = new MissingFactoryMethodException("Instance factory method is abstract: " + method);
                    }
                    continue;
                }

                if (!allowPrivate && !Modifier.isPublic(method.getModifiers())) {
                    if (matchLevel < 5) {
                        matchLevel = 5;
                        missException = new MissingFactoryMethodException("Instance factory method is not public: " + method);
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
            StringBuffer buffer = new StringBuffer("Unable to find a valid factory method: ");
            buffer.append("public void ").append(typeClass.getName()).append(".");
            buffer.append(factoryMethod).append("()");
            throw new MissingFactoryMethodException(buffer.toString());
        }
    }

    private static void setAccessible(final AccessibleObject accessibleObject) {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                accessibleObject.setAccessible(true);
                return null;
            }
        });
    }

    private static String toParameterList(Class[] parameterTypes) {
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
}
