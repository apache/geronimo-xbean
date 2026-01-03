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

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class XBean353Test {

    @Test
    public void testCreateObjectCaseInsensitive() {
        final Map<String, Object> availableProperties = new HashMap<>();
        availableProperties.put("PoolSize", 10);

        final String[] paramNames = new String[]{"poolSize"};
        ObjectRecipe recipe = new ObjectRecipe(Constructor.class, paramNames);
        recipe.setAllProperties(availableProperties);
        recipe.allow(Option.CASE_INSENSITIVE_PROPERTIES);

        final Object o = recipe.create();
        assertNotNull(o);
        assertTrue(o instanceof Constructor);
        assertEquals(10, ((Constructor) o).poolSize);
    }

    @Test
    public void testCreateObjectCaseInsensitiveViaStaticFactory() {
        final Map<String, Object> availableProperties = new HashMap<>();
        availableProperties.put("PoolSize", 10);

        final String[] paramNames = new String[]{"poolSize"};
        ObjectRecipe recipe = new ObjectRecipe(ConstructorFactory.class, paramNames);
        recipe.setFactoryMethod("create");
        recipe.setAllProperties(availableProperties);
        recipe.allow(Option.CASE_INSENSITIVE_PROPERTIES);

        final Object o = recipe.create();
        assertNotNull(o);
        assertTrue(o instanceof Constructor);
        assertEquals(10, ((Constructor) o).poolSize);
    }

    public static class ConstructorFactory{
        public static Constructor create(int poolSize) {
            return new Constructor(poolSize);
        }
    }

    @Test
    public void testPropertyOrderNotAlteredCaseInsensitive() {
        ObjectRecipe recipe = new ObjectRecipe(CatchAll.class);
        recipe.allow(Option.CASE_INSENSITIVE_PROPERTIES);

        recipe.setProperty("123", "a");
        recipe.setProperty("zzz", 1);
        recipe.setProperty("aaa", 2);
        recipe.setProperty("properties", new UnsetPropertiesRecipe());

        CatchAll catchAll = (CatchAll) recipe.create();

        assertEquals(3, catchAll.properties.size());
    }

    public static class Constructor {

        int poolSize;

        public Constructor(int poolSize) {
            this.poolSize = poolSize;
        }
    }

    public static class CatchAll {
        Map<String, Object> properties;

        public void setProperties(Map<String, Object> properties) {
            this.properties = properties;
        }
    }
}