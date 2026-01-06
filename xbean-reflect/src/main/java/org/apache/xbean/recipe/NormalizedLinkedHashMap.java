/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.xbean.recipe;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * LinkedHashMap that normalizes keys using a function to for example make a case-insensitive LinkedHashMap
 */
public class NormalizedLinkedHashMap<K,V> extends LinkedHashMap<K, V> {
    private final Function<K, K> normalizer;

    public NormalizedLinkedHashMap(Function<K, K> normalizer, Map<K, V> initialMap) {
        this.normalizer = normalizer;

        putAll(initialMap);
    }

    @Override
    public V get(Object key) {
        return super.get(normalizer.apply((K) key));
    }

    @Override
    public V put(K key, V value) {
        return super.put(normalizer.apply(key), value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        // can't just easily normalize keys here due to LinkedHashMap internals, so just call put for each entry
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public boolean remove(Object key, Object value) {
        return super.remove(normalizer.apply((K) key), value);
    }

    @Override
    public V remove(Object key) {
        return super.remove(normalizer.apply((K) key));
    }

    @Override
    public boolean containsKey(Object key) {
        return super.containsKey(normalizer.apply((K) key));
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        return super.getOrDefault(normalizer.apply((K) key), defaultValue);
    }

    @Override
    public V replace(K key, V value) {
        return super.replace(normalizer.apply(key), value);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return super.replace(normalizer.apply(key), oldValue, newValue);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return super.putIfAbsent(normalizer.apply(key), value);
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return super.merge(normalizer.apply(key), value, remappingFunction);
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return super.compute(normalizer.apply(key), remappingFunction);
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return super.computeIfAbsent(normalizer.apply(key), mappingFunction);
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return super.computeIfPresent(normalizer.apply(key), remappingFunction);
    }
}
