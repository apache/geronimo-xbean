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
package org.apache.xbean.parameter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import org.apache.xbean.recipe.RecipeHelper;

/**
 * @version $Rev$ $Date$
 */
public class ParameterNamesTest extends TestCase {
    private final ParameterNames parameterNames = new AsmParameterNames();
    public void testConstructor() throws Exception {
        Constructor constructor = TestClass.class.getConstructor(int.class, Object.class, Long.class);
        assertParameterNames(Arrays.asList("one", "two", "three"), constructor);
    }

    public void testMethod() throws Exception {
        Method method = TestClass.class.getMethod("instanceMethod", int.class, Object.class, Long.class);
        assertParameterNames(Arrays.asList("x", "y", "z"), method);
    }

    public void testStaticMethod() throws Exception {
        Method method = TestClass.class.getMethod("factoryMethod", int.class, Object.class, Long.class);
        assertParameterNames(Arrays.asList("a", "b", "c"), method);
    }

    public void testInheritedMethod() throws Exception {
        Method method = TestClass.class.getMethod("inheritedMethod", Map.class);
        assertParameterNames(Arrays.asList("nothing"), method);
    }

    public void testPrivateConstructor() throws Exception {
        Constructor constructor = findPrivateConstructor(TestClass.class, new Class[]{Double.class});
        assertNull(parameterNames.get(constructor));
    }

    public void testPrivateMethod() throws Exception {
        Method method = findPrivateMethod(TestClass.class, "factoryMethod", new Class[] {Double.class});
        assertNull(parameterNames.get(method));
    }

    public void testAllConstructors() throws Exception {
        Map<Constructor,List<String>> expectedMap = new HashMap<Constructor,List<String>>();
        expectedMap.put(TestClass.class.getConstructor(int.class, Object.class, Long.class),Arrays.asList("one", "two", "three"));
        expectedMap.put(TestClass.class.getConstructor(int.class),Arrays.asList("foo"));
        expectedMap.put(TestClass.class.getConstructor(Object.class),Arrays.asList("bar"));
        expectedMap.put(TestClass.class.getConstructor(Object[].class),Arrays.asList("objectArray"));

        Map<Constructor, List<String>> actualMap = parameterNames.getAllConstructorParameters(TestClass.class);
        assertEquals(expectedMap, actualMap);
    }

    public void testAllInstanceMethods() throws Exception {
        Map<Method,List<String>> expectedMap = new HashMap<Method,List<String>>();
        expectedMap.put(TestClass.class.getMethod("instanceMethod", int.class, Object.class, Long.class), Arrays.asList("x", "y", "z"));
        expectedMap.put(TestClass.class.getMethod("instanceMethod", int.class), Arrays.asList("apple"));
        expectedMap.put(TestClass.class.getMethod("instanceMethod", Object.class), Arrays.asList("ipod"));

        Map<Method, List<String>> actualMap = parameterNames.getAllMethodParameters(TestClass.class, "instanceMethod");
        assertEquals(expectedMap, actualMap);
    }

    public void testAllStataicMethods() throws Exception {
        Map<Method,List<String>> expectedMap = new HashMap<Method,List<String>>();
        expectedMap.put(TestClass.class.getMethod("factoryMethod", int.class, Object.class, Long.class), Arrays.asList("a", "b", "c"));
        expectedMap.put(TestClass.class.getMethod("factoryMethod", int.class), Arrays.asList("beer"));
        expectedMap.put(TestClass.class.getMethod("factoryMethod", Object.class), Arrays.asList("pizza"));

        Map<Method, List<String>> actualMap = parameterNames.getAllMethodParameters(TestClass.class, "factoryMethod");
        assertEquals(expectedMap, actualMap);
    }

    public void testAllMixedMethods() throws Exception {
        Map<Method,List<String>> expectedMap = new HashMap<Method,List<String>>();
        expectedMap.put(TestClass.class.getMethod("mixedMethods", Double.class), Arrays.asList("gin"));
        expectedMap.put(TestClass.class.getMethod("mixedMethods", Short.class), Arrays.asList("tonic"));

        Map<Method, List<String>> actualMap = parameterNames.getAllMethodParameters(TestClass.class, "mixedMethods");
        assertEquals(expectedMap, actualMap);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private static class ParentTestClass {
        public void inheritedMethod(Map nothing) {}
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private static abstract class TestClass extends ParentTestClass {
        public TestClass(int one, Object two, Long three) {}
        public TestClass(int foo) {}
        public TestClass(Object bar) {}
        public TestClass(Object[] objectArray) {}
        private TestClass(Double scotch) {}

        public static void factoryMethod(int a, Object b, Long c) {}
        public static void factoryMethod(int beer) {}
        public static void factoryMethod(Object pizza) {}
        private static void factoryMethod(Double shot) {}

        public void instanceMethod(int x, Object y, Long z) {}
        public void instanceMethod(int apple) {}
        public void instanceMethod(Object ipod) {}
        private void instanceMethod(Double psp) {}

        public static void mixedMethods(Double gin) {}
        public void mixedMethods(Short tonic) {}

        public abstract void abstractMethod(Byte ear);
    }

    private void assertParameterNames(List<String> expectedNames, Constructor constructor) {
        List<String> actualNames = parameterNames.get(constructor);
        assertEquals(expectedNames, actualNames);
    }

    private void assertParameterNames(List<String> expectedNames, Method method) {
        List<String> actualNames = parameterNames.get(method);
        assertEquals(expectedNames, actualNames);
    }

    private static void assertEquals(List<String> expectedNames, List<String> actualNames) {
        assertNotNull(expectedNames);
        assertNotNull(actualNames);
        assertEquals(Arrays.asList(expectedNames), Arrays.asList(actualNames));
    }

    @SuppressWarnings({"SuspiciousMethodCalls"})
    private void assertEquals(Map<?,List<String>> expectedMap, Map<?,List<String>> actualMap) {
        for (Map.Entry<?, List<String>> entry : actualMap.entrySet()) {
            Object key = entry.getKey();
            List<String> expectedNames = expectedMap.get(key);
            List<String> actualNames = entry.getValue();
            assertEquals(expectedNames, actualNames);
        }
    }

    private Constructor findPrivateConstructor(Class clazz, Class[] argTypes) {
        Constructor[] constructors = clazz.getDeclaredConstructors();
        for (Constructor constructor : constructors) {
            if (RecipeHelper.isAssignableFrom(argTypes, constructor.getParameterTypes())) {
                if (!Modifier.isPublic(constructor.getModifiers())) {
                    return constructor;
                }
            }
        }
        fail("Private constructor not found");
        return null;
    }

    private Method findPrivateMethod(Class clazz, String methodName, Class[] argTypes) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName) && RecipeHelper.isAssignableFrom(argTypes, method.getParameterTypes())) {
                if (!Modifier.isPublic(method.getModifiers())) {
                    return method;
                }
            }
        }
        fail("Private method not found");
        return null;
    }
}
