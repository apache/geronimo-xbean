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
package org.apache.xbean.propertyeditor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Primitives {
    private static final Map<Class, Class> PRIMITIVE_TO_WRAPPER;
    private static final Map<Class, Class> WRAPPER_TO_PRIMITIVE;

    static {
        {
            final Map<Class, Class> map = new HashMap<Class, Class>();
            map.put(boolean.class, Boolean.class);
            map.put(char.class, Character.class);
            map.put(byte.class, Byte.class);
            map.put(short.class, Short.class);
            map.put(int.class, Integer.class);
            map.put(long.class, Long.class);
            map.put(float.class, Float.class);
            map.put(double.class, Double.class);
            PRIMITIVE_TO_WRAPPER = Collections.unmodifiableMap(map);
        }

        {
            final Map<Class, Class> map = new HashMap<Class, Class>();
            map.put(Boolean.class, boolean.class);
            map.put(Character.class, char.class);
            map.put(Byte.class, byte.class);
            map.put(Short.class, short.class);
            map.put(Integer.class, int.class);
            map.put(Long.class, long.class);
            map.put(Float.class, float.class);
            map.put(Double.class, double.class);
            WRAPPER_TO_PRIMITIVE = Collections.unmodifiableMap(map);
        }
    }

    public static Class<?> findSibling(final Class<?> wrapperOrPrimitive) {
        final Class aClass = PRIMITIVE_TO_WRAPPER.get(wrapperOrPrimitive);
        return aClass != null ? aClass : WRAPPER_TO_PRIMITIVE.get(wrapperOrPrimitive);
    }

    public static Class<?> toWrapper(final Class<?> primitive) {
        return PRIMITIVE_TO_WRAPPER.get(primitive);
    }

    public static Class<?> toPrimitive(final Class<?> wrapper) {
        return WRAPPER_TO_PRIMITIVE.get(wrapper);
    }

    private Primitives() {
        // no-op
    }
}
