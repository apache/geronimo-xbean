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

import java.lang.reflect.Modifier;
import java.lang.reflect.Constructor;
import java.util.Comparator;
import java.util.Map;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

/**
 * @version $Rev: 6687 $ $Date: 2005-12-28T21:08:56.733437Z $
 */
public final class RecipeHelper {
    private RecipeHelper() {
    }

    public static boolean hasDefaultConstructor(Class type) {
        if (!Modifier.isPublic(type.getModifiers())) {
            return false;
        }
        Constructor[] constructors = type.getConstructors();
        for (int i = 0; i < constructors.length; i++) {
            Constructor constructor = constructors[i];
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

    public static boolean isInstance(Class type, Object instance) {
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

    public static boolean isConvertable(Class type, Object propertyValue, ClassLoader classLoader) {
        if (propertyValue instanceof Recipe) {
            Recipe recipe = (Recipe) propertyValue;
            return recipe.canCreate(type, classLoader);
        }
        return (propertyValue instanceof String && PropertyEditors.canConvert(type));
    }

    public static boolean isAssignableFrom(Class expected, Class actual) {
        if (expected.isPrimitive()) {
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

        return expected.isAssignableFrom(actual);
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
}
