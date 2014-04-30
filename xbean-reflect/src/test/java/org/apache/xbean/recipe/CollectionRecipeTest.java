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

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import junit.framework.TestCase;

/**
 * @version $Rev$ $Date$
 */
public class CollectionRecipeTest extends TestCase {

    public void testList() throws Exception {
        assertEquals(ArrayList.class, new CollectionRecipe().create(List.class, false).getClass());
        assertEquals(ArrayList.class, new CollectionRecipe().create(ArrayList.class, false).getClass());
        assertEquals(LinkedList.class, new CollectionRecipe().create(LinkedList.class, false).getClass());
    }
    
    public void testSet() throws Exception {
        assertEquals(LinkedHashSet.class, new CollectionRecipe().create(Set.class, false).getClass());
        assertEquals(TreeSet.class, new CollectionRecipe().create(SortedSet.class, false).getClass());
        assertEquals(HashSet.class, new CollectionRecipe().create(HashSet.class, false).getClass());
    }
        
    public void testListRecipe() throws Exception {
        CollectionRecipe recipe = new CollectionRecipe(List.class);
        assertEquals(ArrayList.class, recipe.create(List.class, false).getClass());
        assertEquals(ArrayList.class, recipe.create(ArrayList.class, false).getClass());
        assertEquals(LinkedList.class, recipe.create(LinkedList.class, false).getClass());
        assertEquals(Vector.class, recipe.create(Vector.class, false).getClass());
        
        assertTrue(recipe.canCreate(List.class));
        assertTrue(recipe.canCreate(ArrayList.class));
        assertTrue(recipe.canCreate(LinkedList.class));
        assertTrue(recipe.canCreate(Vector.class));
        assertTrue(recipe.canCreate(Object.class));
        
        assertFalse(recipe.canCreate(Date.class));   
        assertFalse(recipe.canCreate(URI.class));
        
        assertFalse(recipe.canCreate(Set.class));
    }
    
    public void testSetRecipe() throws Exception {
        CollectionRecipe recipe = new CollectionRecipe(Set.class);
        assertEquals(LinkedHashSet.class, recipe.create(Set.class, false).getClass());
        assertEquals(HashSet.class, recipe.create(HashSet.class, false).getClass());
        assertEquals(TreeSet.class, recipe.create(SortedSet.class, false).getClass());
        assertEquals(TreeSet.class, recipe.create(TreeSet.class, false).getClass());
        
        assertTrue(recipe.canCreate(Set.class));
        assertTrue(recipe.canCreate(HashSet.class));
        assertTrue(recipe.canCreate(SortedSet.class));
        assertTrue(recipe.canCreate(TreeSet.class));
        assertTrue(recipe.canCreate(Object.class));
        
        assertFalse(recipe.canCreate(Date.class));   
        assertFalse(recipe.canCreate(URI.class));
        
        assertFalse(recipe.canCreate(List.class));
    }
}
