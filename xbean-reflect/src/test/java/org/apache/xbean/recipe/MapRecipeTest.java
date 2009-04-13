/**
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import junit.framework.TestCase;

/**
 * @version $Rev$ $Date$
 */
public class MapRecipeTest extends TestCase {

    public void testMap() throws Exception {
        assertEquals(LinkedHashMap.class, new MapRecipe().create(Map.class, false).getClass());
        assertEquals(TreeMap.class, new MapRecipe().create(SortedMap.class, false).getClass());
        assertEquals(ConcurrentHashMap.class, new MapRecipe().create(ConcurrentMap.class, false).getClass());
        assertEquals(HashMap.class, new MapRecipe(HashMap.class).create(Map.class, false).getClass());
    }
}
