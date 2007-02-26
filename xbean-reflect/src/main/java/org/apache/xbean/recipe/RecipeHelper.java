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
