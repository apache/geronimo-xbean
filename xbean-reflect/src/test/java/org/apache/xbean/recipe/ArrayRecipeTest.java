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

import junit.framework.TestCase;

/**
 * @version $Rev$ $Date$
 */
public class ArrayRecipeTest extends TestCase {

    public void testArray() throws Exception {
        ArrayRecipe recipe;

        recipe = new ArrayRecipe(int.class);
        assertTrue(recipe.canCreate(int[].class));
        assertEquals(int[].class, recipe.create(int[].class, false).getClass());
        assertEquals(int[].class, recipe.create(Object.class, false).getClass());
        
        recipe = new ArrayRecipe(Number.class);
        assertTrue(recipe.canCreate(Integer[].class));
        assertEquals(Integer[].class, recipe.create(Integer[].class, false).getClass());
        assertEquals(Number[].class, recipe.create(Object.class, false).getClass());

        recipe = new ArrayRecipe();
        
        assertTrue(recipe.canCreate(int[].class));
        assertEquals(int[].class, recipe.create(int[].class, false).getClass());
        
        assertTrue(recipe.canCreate(Number[].class));
        assertEquals(Number[].class, recipe.create(Number[].class, false).getClass());
    }
}
