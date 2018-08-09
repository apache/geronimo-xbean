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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.xbean.propertyeditor.Primitives;
import org.apache.xbean.propertyeditor.PropertyEditorRegistry;
import org.apache.xbean.propertyeditor.PropertyEditors;

/**
 * @version $Rev: 6687 $ $Date: 2005-12-28T21:08:56.733437Z $
 */
public final class RecipeHelper {
    private RecipeHelper() {
    }

    public static Recipe getCaller() {
        LinkedList<Recipe> stack = ExecutionContext.getContext().getStack();
        if (stack.size() < 2) {
            return null;
        }
        return stack.get(stack.size() - 2);
    }

    public static Class loadClass(String name) throws ClassNotFoundException {
        ClassLoader classLoader = ExecutionContext.getContext().getClassLoader();
        Class<?> type = Class.forName(name, true, classLoader);
        return type;
    }

    public static boolean hasDefaultConstructor(Class type) {
        if (!Modifier.isPublic(type.getModifiers())) {
            return false;
        }
        if (Modifier.isAbstract(type.getModifiers())) {
            return false;
        }
        Constructor[] constructors = type.getConstructors();
        for (Constructor constructor : constructors) {
            if (Modifier.isPublic(constructor.getModifiers()) &&
                    constructor.getParameterTypes().length == 0) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSimpleType(Object o) {
        return  o == null ||
                o instanceof Boolean ||
                o instanceof Character ||
                o instanceof Byte ||
                o instanceof Short ||
                o instanceof Integer ||
                o instanceof Long ||
                o instanceof Float ||
                o instanceof Double ||
                o instanceof String ||
                o instanceof Recipe;

    }

    public static <K,V> List<Map.Entry<K,V>> prioritizeProperties(Map<K,V> properties) {
        ArrayList<Map.Entry<K,V>> entries = new ArrayList<Map.Entry<K,V>>(properties.entrySet());
        Collections.sort(entries, new RecipeComparator());
        return entries;
    }

    public static boolean isInstance(Type t, Object instance) {
        Class type = toClass(t);
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

    public static boolean isConvertable(Type type, Object propertyValue, PropertyEditorRegistry registry) {
        if (propertyValue instanceof Recipe) {
            Recipe recipe = (Recipe) propertyValue;
            return recipe.canCreate(type);
        }
        return (propertyValue instanceof String && (registry == null ? PropertyEditors.registry() : registry).findConverter(toClass(type)) != null)
            || (type == String.class && char[].class.isInstance(propertyValue));
    }

    public static boolean isAssignableFrom(Class expected, Class actual) {
        if (expected == null) return true;

        if (expected.isPrimitive()) {
            // verify actual is the correct wrapper type
            return actual.equals(Primitives.toWrapper(expected));
        }

        return expected.isAssignableFrom(actual);
    }

    public static Object convert(Type expectedType, Object value, boolean lazyRefAllowed, PropertyEditorRegistry registry) {
        if (value instanceof Recipe) {
            Recipe recipe = (Recipe) value;
            value = recipe.create(expectedType, lazyRefAllowed);
        }

        // some shortcuts for common string operations
        if (char[].class == expectedType && String.class.isInstance(value)) {
            return String.class.cast(value).toCharArray();
        }
        if (String.class == expectedType && char[].class.isInstance(value)) {
            return new String(char[].class.cast(value));
        }

        if (value instanceof String && (expectedType != Object.class)) {
            String stringValue = (String) value;
            value = (registry == null ? PropertyEditors.registry() : registry).getValue(expectedType, stringValue);
        }
        return value;
    }

    public static boolean isAssignableFrom(List<? extends Class<?>> expectedTypes, List<? extends Class<?>> actualTypes) {
        if (expectedTypes.size() != actualTypes.size()) {
            return false;
        }
        for (int i = 0; i < expectedTypes.size(); i++) {
            Class expectedType = expectedTypes.get(i);
            Class actualType = actualTypes.get(i);
            if (expectedType != actualType && !isAssignableFrom(expectedType, actualType)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAssignable(Type expectedType, Type actualType) {
        Class expectedClass = toClass(expectedType);
        Class actualClass = toClass(actualType);
        return expectedClass.isAssignableFrom(actualClass);
    }

    public static Class toClass(Type type) {
        // GenericArrayType, ParameterizedType, TypeVariable<D>, WildcardType
        if (type instanceof Class) {
            Class clazz = (Class) type;
            return clazz;
        } else if (type instanceof GenericArrayType) {
            GenericArrayType arrayType = (GenericArrayType) type;
            Class componentType = toClass(arrayType.getGenericComponentType());
            return Array.newInstance(componentType, 0).getClass();
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            return toClass(parameterizedType.getRawType());
        }
        return Object.class;
    }

    public static class RecipeComparator implements Comparator<Object> {
        public int compare(Object left, Object right) {
            if (!(left instanceof Recipe) && !(right instanceof Recipe)) return 0;
            if (left instanceof Recipe && !(right instanceof Recipe)) return 1;
            if (!(left instanceof Recipe) && right instanceof Recipe) return -1;

            float leftPriority = ((Recipe) left).getPriority();
            float rightPriority = ((Recipe) right).getPriority();

            if (leftPriority > rightPriority) return 1;
            if (leftPriority < rightPriority) return -1;
            return 0;
        }
    }

    public static Type[] getTypeParameters(Class desiredType, Type type) {
        if (type instanceof Class) {
            Class rawClass = (Class) type;

            // if this is the collection class we're done
            if (desiredType.equals(type)) {
                return null;
            }

            for (Type intf : rawClass.getGenericInterfaces()) {
                Type[] collectionType = getTypeParameters(desiredType, intf);
                if (collectionType != null) {
                    return collectionType;
                }
            }

            Type[] collectionType = getTypeParameters(desiredType, rawClass.getGenericSuperclass());
            return collectionType;
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;

            Type rawType = parameterizedType.getRawType();
            if (desiredType.equals(rawType)) {
                Type[] argument = parameterizedType.getActualTypeArguments();
                return argument;
            }
            Type[] collectionTypes = getTypeParameters(desiredType,rawType);
            if (collectionTypes != null) {
                for (int i = 0; i < collectionTypes.length; i++) {
                    if (collectionTypes[i] instanceof TypeVariable) {
                        TypeVariable typeVariable = (TypeVariable) collectionTypes[i];
                        TypeVariable[] rawTypeParams = ((Class) rawType).getTypeParameters();
                        for (int j = 0; j < rawTypeParams.length; j++) {
                            if (typeVariable.getName().equals(rawTypeParams[j].getName())) {
                                collectionTypes[i] = parameterizedType.getActualTypeArguments()[j];
                            }
                        }
                    }
                }
            }
            return collectionTypes;
        }
        return null;
    }
}
