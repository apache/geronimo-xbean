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

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

public class CollectionTest extends TestCase {
    public Map notGeneric;
    public Map<Key, Value> simple;
    public Map<?,?> wildcard;
    public Map<? extends Key, ? extends Value> extendsWildcard;
    public Map<? super Key, ? extends Value> superWildcard;


    public MyMap notGenericMyMap;
    public MyMap<Fake, Value, Key> simpleMyMap;
    public MyMap<?,?,?> wildcardMyMap;
    public MyMap<? extends Fake, ? extends Value, ? extends Key> extendsWildcardMyMap;
    public MyMap<? super Fake, ? super Value, ? extends Key> superWildcardMyMap;

    public FixedMap fixedMap;

    public void testReflection() {
        for (Field field : getClass().getFields()) {
            Type[] types = RecipeHelper.getTypeParameters(Map.class, field.getGenericType());
            if (types == null) {
                System.out.println("Map");
            } else {
                System.out.println("Map" + Arrays.toString(types));                
            }
        }
    }


    public static class MyMap<Unused, MapValue, MapKey> extends AbstractMap<MapKey, MapValue> {
        public Iterator<MapKey> iterator() {
            return null;
        }

        public int size() {
            return 0;
        }

        public Set<Entry<MapKey, MapValue>> entrySet() {
            return null;
        }
    }

    public static class FixedMap extends MyMap<Fake,Value,Key> {
    }

    public static class Fake {}
    public static class Key {}
    public static class Value {}
}
