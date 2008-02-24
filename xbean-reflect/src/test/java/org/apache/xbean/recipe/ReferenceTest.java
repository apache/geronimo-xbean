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

import java.util.Arrays;
import java.util.Collections;

import junit.framework.TestCase;

public class ReferenceTest extends TestCase {
    public void testReference() {
        Reference reference = new Reference("fruit");
        TestAction action = new TestAction();
        reference.setAction(action);

        assertFalse(reference.isResolved());
        assertNull(reference.get());
        assertFalse(action.isSet);
        assertNull(action.value);
        assertNull(action.ref);

        reference.set("apple");

        assertTrue(reference.isResolved());
        assertEquals("apple", reference.get());
        assertTrue(action.isSet);
        assertEquals("apple", action.value);
        assertSame(reference, action.ref);

        try {
            reference.set("orange");
            fail("Expected ConstructionException because reference has already been set");
        } catch (ConstructionException expected) {
        }
    }

    public void testNullReference() {
        Reference reference = new Reference("fruit");
        TestAction action = new TestAction();
        reference.setAction(action);

        assertFalse(reference.isResolved());
        assertNull(reference.get());
        assertFalse(action.isSet);
        assertNull(action.value);
        assertNull(action.ref);

        reference.set(null);

        assertTrue(reference.isResolved());
        assertNull(reference.get());
        assertTrue(action.isSet);
        assertNull(action.value);
        assertSame(reference, action.ref);

        try {
            reference.set(null);
            fail("Expected ConstructionException because reference has already been set");
        } catch (ConstructionException expected) {
        }
    }

    public void testContext() {
        DefaultExecutionContext context = new DefaultExecutionContext();
        Reference reference = new Reference("fruit");
        TestAction action = new TestAction();
        reference.setAction(action);

        context.addReference(reference);

        assertFalse(reference.isResolved());
        assertNull(reference.get());
        assertFalse(action.isSet);
        assertNull(action.value);
        assertNull(action.ref);

        context.addObject("fruit", "apple");

        assertTrue(reference.isResolved());
        assertEquals("apple", reference.get());
        assertTrue(action.isSet);
        assertEquals("apple", action.value);
        assertSame(reference, action.ref);
    }

    public void testNullContext() {
        DefaultExecutionContext context = new DefaultExecutionContext();
        Reference reference = new Reference("fruit");
        TestAction action = new TestAction();
        reference.setAction(action);

        context.addReference(reference);

        assertFalse(reference.isResolved());
        assertNull(reference.get());
        assertFalse(action.isSet);
        assertNull(action.value);
        assertNull(action.ref);

        context.addObject("fruit", null);

        assertTrue(reference.isResolved());
        assertNull(reference.get());
        assertTrue(action.isSet);
        assertNull(action.value);
        assertSame(reference, action.ref);
    }

    public void testRecipe() {
        ReferenceRecipe recipe = new ReferenceRecipe("fruit");
        assertEquals("fruit", recipe.getReferenceName());

        //
        // Create methods will fail bcause no object has been registered with the
        // name "fruit" and lazy references are not allowed by default
        try {
            recipe.create();
            fail("Expected ConstructionException because referenced object does not exist and lazy references are not allowed");
        } catch (ConstructionException expected) {
        }

        try {
            recipe.create(getClass().getClassLoader());
            fail("Expected ConstructionException because referenced object does not exist and lazy references are not allowed");
        } catch (ConstructionException expected) {
        }

        try {
            recipe.create(Object.class, true);
            fail("Expected UnresolvedReferencesException");
        } catch (UnresolvedReferencesException expected) {
            assertEquals(Collections.singleton("fruit"), expected.getUnresolvedRefs().keySet());
        }

        //
        // Test lazy references
        DefaultExecutionContext context = new DefaultExecutionContext();
        ExecutionContext.setContext(context);

        try {
            Object value = recipe.create(Object.class, true);
            assertTrue("value should be an instance of Reference", value instanceof Reference);
            Reference reference = (Reference) value;
            TestAction action = new TestAction();
            reference.setAction(action);

            assertFalse(reference.isResolved());
            assertNull(reference.get());
            assertFalse(action.isSet);
            assertNull(action.value);
            assertNull(action.ref);

            context.addObject("fruit", "apple");

            assertTrue(reference.isResolved());
            assertEquals("apple", reference.get());
            assertTrue(action.isSet);
            assertEquals("apple", action.value);
            assertSame(reference, action.ref);

            //
            // Test normal create calls with preexisting object
            assertEquals("apple", recipe.create());
            assertEquals("apple", recipe.create(getClass().getClassLoader()));
            assertEquals("apple", recipe.create(Object.class, false));
            assertEquals("apple", recipe.create(Object.class, true));
        } finally {
            ExecutionContext.setContext(null);
        }
    }

    public void testCircularReference() {
        ReferenceRecipe apple = new ReferenceRecipe("tree");
        apple.setName("apple");
        ReferenceRecipe tree = new ReferenceRecipe("seed");
        tree.setName("tree");
        ReferenceRecipe seed = new ReferenceRecipe("apple");
        seed.setName("seed");

        DefaultExecutionContext context = new DefaultExecutionContext();
        ExecutionContext.setContext(context);
        try {
            context.addObject("apple", apple);
            context.addObject("tree", tree);
            context.addObject("seed", seed);

            try {
                apple.create(Object.class, false);
                fail("Expected CircularDependencyException");
            } catch (CircularDependencyException expected) {
                assertEquals(Arrays.<Recipe>asList(apple, tree, seed, apple),
                        expected.getCircularDependency());
            }
            try {
                tree.create(Object.class, false);
                fail("Expected CircularDependencyException");
            } catch (CircularDependencyException expected) {
                assertEquals(Arrays.<Recipe>asList(tree, seed, apple, tree),
                        expected.getCircularDependency());
            }
        } finally {
            ExecutionContext.setContext(null);
        }
    }

    public class TestAction implements Reference.Action {
        public boolean isSet;
        public Object value;
        public Reference ref;

        public void onSet(Reference ref) {
            isSet = true;
            value = ref.get();
            this.ref = ref;
        }
    }
}
