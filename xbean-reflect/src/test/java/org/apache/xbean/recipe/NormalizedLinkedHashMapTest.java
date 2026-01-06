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

import org.junit.Test;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import static org.junit.Assert.*;

public class NormalizedLinkedHashMapTest {

    private final Function<String, String> lowerCaseNormalizer = s -> s == null ? null : s.toLowerCase();

    @Test
    public void testBasicPutAndGet() {
        NormalizedLinkedHashMap<String, String> map = new NormalizedLinkedHashMap<>(lowerCaseNormalizer, Collections.emptyMap());
        map.put("Key", "value");
        assertEquals("value", map.get("Key"));
        assertEquals("value", map.get("key"));
        assertEquals("value", map.get("KEY"));
    }

    @Test
    public void testInitialization() {
        Map<String, String> initial = new HashMap<>();
        initial.put("Key1", "value1");
        initial.put("KEY2", "value2");

        NormalizedLinkedHashMap<String, String> map = new NormalizedLinkedHashMap<>(lowerCaseNormalizer, initial);

        assertEquals(2, map.size());
        assertEquals("value1", map.get("key1"));
        assertEquals("value2", map.get("key2"));
    }

    @Test
    public void testPutAll() {
        NormalizedLinkedHashMap<String, String> map = new NormalizedLinkedHashMap<>(lowerCaseNormalizer, Collections.emptyMap());
        Map<String, String> toAdd = new HashMap<>();
        toAdd.put("Key1", "value1");
        toAdd.put("KEY2", "value2");

        map.putAll(toAdd);

        assertEquals(2, map.size());
        assertEquals("value1", map.get("key1"));
        assertEquals("value2", map.get("key2"));
    }

    @Test
    public void testRemove() {
        NormalizedLinkedHashMap<String, String> map = new NormalizedLinkedHashMap<>(lowerCaseNormalizer, Collections.singletonMap("Key", "value"));
        assertTrue(map.remove("KEY", "value"));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testCompute() {
        NormalizedLinkedHashMap<String, String> map = new NormalizedLinkedHashMap<>(lowerCaseNormalizer, Collections.emptyMap());
        map.put("Key", "value");

        map.compute("KEY", (k, v) -> v + "2");
        assertEquals("value2", map.get("key"));
    }

    @Test
    public void testComputeIfAbsent() {
        NormalizedLinkedHashMap<String, String> map = new NormalizedLinkedHashMap<>(lowerCaseNormalizer, Collections.emptyMap());
        map.computeIfAbsent("Key", k -> "value");
        assertEquals("value", map.get("key"));
        
        map.computeIfAbsent("KEY", k -> "new_value");
        assertEquals("value", map.get("key"));
    }

    @Test
    public void testComputeIfPresent() {
        NormalizedLinkedHashMap<String, String> map = new NormalizedLinkedHashMap<>(lowerCaseNormalizer, Collections.emptyMap());
        map.put("Key", "value");

        map.computeIfPresent("KEY", (k, v) -> v + "2");
        assertEquals("value2", map.get("key"));
        
        map.computeIfPresent("Missing", (k, v) -> "should_not_be_here");
        assertNull(map.get("missing"));
    }

    @Test
    public void testContainsKey() {
        NormalizedLinkedHashMap<String, String> map = new NormalizedLinkedHashMap<>(lowerCaseNormalizer, Collections.singletonMap("Key", "value"));

        assertTrue("Should contain 'Key'", map.containsKey("Key"));
        assertTrue("Should contain 'key'", map.containsKey("key"));
        assertTrue("Should contain 'KEY'", map.containsKey("KEY"));
    }

    @Test
    public void testRemoveByKey() {
        NormalizedLinkedHashMap<String, String> map = new NormalizedLinkedHashMap<>(lowerCaseNormalizer, Collections.singletonMap("Key", "value"));

        assertEquals("value", map.remove("KEY"));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testGetOrDefault() {
        NormalizedLinkedHashMap<String, String> map = new NormalizedLinkedHashMap<>(lowerCaseNormalizer, Collections.singletonMap("Key", "value"));

        assertEquals("value", map.getOrDefault("KEY", "default"));
        assertEquals("default", map.getOrDefault("Missing", "default"));
    }

    @Test
    public void testReplace() {
        NormalizedLinkedHashMap<String, String> map = new NormalizedLinkedHashMap<>(lowerCaseNormalizer, Collections.singletonMap("Key", "value"));
        assertEquals("value", map.replace("KEY", "newValue"));
        assertEquals("newValue", map.get("key"));
        
        assertTrue(map.replace("KEY", "newValue", "newestValue"));
        assertEquals("newestValue", map.get("key"));
    }

    @Test
    public void testPutIfAbsent() {
        NormalizedLinkedHashMap<String, String> map = new NormalizedLinkedHashMap<>(lowerCaseNormalizer, Collections.emptyMap());
        assertNull(map.putIfAbsent("Key", "value"));
        assertEquals("value", map.get("key"));
        
        assertEquals("value", map.putIfAbsent("KEY", "new_value"));
        assertEquals("value", map.get("key"));
    }

    @Test
    public void testMerge() {
        NormalizedLinkedHashMap<String, String> map = new NormalizedLinkedHashMap<>(lowerCaseNormalizer, Collections.emptyMap());
        map.put("Key", "value");
        
        map.merge("KEY", "Suffix", String::concat);
        assertEquals("valueSuffix", map.get("key"));
    }
}