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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

/**
 * @version $Rev$ $Date$
 */
public class ParameterNameLoaderTest extends TestCase {

    protected ParameterNameLoader parameterNameLoader = new ParameterNameLoader() {
        public List<String> get(Method method) {
            return ReflectionUtil.getParameterNames(method);
        }

        public List<String> get(Constructor constructor) {
            return ReflectionUtil.getParameterNames(constructor);
        }
    };

    public void testConstructor() throws Exception {
        Constructor constructor = TestClass.class.getConstructor(int.class, Object.class, Long.class);
        assertParameterNames(Arrays.asList("one", "two", "three"), constructor);
    }

    public void tesConstructorAnnotated() throws Exception {
        Constructor constructor = AnnotatedClass.class.getConstructor(int.class, Object.class, Long.class);
        assertParameterNames(Arrays.asList("one", "two", "three"), constructor);
    }

    public void testMethod() throws Exception {
        Method method = TestClass.class.getMethod("instanceMethod", int.class, Object.class, Long.class);
        assertParameterNames(Arrays.asList("x", "y", "z"), method);
    }

    public void testMethodAnnotated() throws Exception {
        Method method = AnnotatedClass.class.getMethod("instanceMethod", int.class, Object.class, Long.class);
        assertParameterNames(Arrays.asList("x", "y", "z"), method);
    }

    public void testStaticMethod() throws Exception {
        Method method = TestClass.class.getMethod("factoryMethod", int.class, Object.class, Long.class);
        assertParameterNames(Arrays.asList("a", "b", "c"), method);
    }

    public void testStaticMethodAnnotated() throws Exception {
        Method method = AnnotatedClass.class.getMethod("factoryMethod", int.class, Object.class, Long.class);
        assertParameterNames(Arrays.asList("a", "b", "c"), method);
    }

    public void testInheritedMethod() throws Exception {
        Method method = TestClass.class.getMethod("inheritedMethod", Map.class);
        assertParameterNames(Arrays.asList("nothing"), method);
    }

    public void testInheritedMethodAnnotated() throws Exception {
        Method method = AnnotatedClass.class.getMethod("inheritedMethod", Map.class);
        assertParameterNames(Arrays.asList("nothing"), method);
    }

    public void testPrivateConstructor() throws Exception {
        Constructor constructor = findPrivateConstructor(TestClass.class, Double.class);
        assertParameterNames(Arrays.asList("scotch"), constructor);
    }

    public void testPrivateConstructorAnnotated() throws Exception {
        Constructor constructor = findPrivateConstructor(AnnotatedClass.class, Double.class);
        assertParameterNames(Arrays.asList("scotch"), constructor);
    }

    public void testPrivateMethod() throws Exception {
        Method method = findPrivateMethod(TestClass.class, "factoryMethod", Arrays.asList(Double.class));
        assertParameterNames(Arrays.asList("shot"), method);
    }

    public void testPrivateMethodAnnotated() throws Exception {
        Method method = findPrivateMethod(AnnotatedClass.class, "factoryMethod", Arrays.asList(Double.class));
        assertParameterNames(Arrays.asList("shot"), method);
    }

    public void testEmptyMethod() throws Exception {
        Method method = TestClass.class.getMethod("emptyMethod");
        assertParameterNames(Collections.<String>emptyList(), method);
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

        public void emptyMethod() {}
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private static class ParentAnnotatedClass {
        @ParameterNames({"nothing"})
        public void inheritedMethod(Map arg1) {}
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private static abstract class AnnotatedClass extends ParentAnnotatedClass {
        @ParameterNames({"one", "two", "three"})
        public AnnotatedClass(int arg1, Object arg2, Long arg3) {}
        @ParameterNames({"foo"})
        public AnnotatedClass(int arg1) {}
        @ParameterNames({"bar"})
        public AnnotatedClass(Object arg1) {}
        @ParameterNames({"objectArray"})
        public AnnotatedClass(Object[] arg1) {}
        @ParameterNames({"scotch"})
        private AnnotatedClass(Double arg1) {}

        @ParameterNames({"a", "b", "c"})
        public static void factoryMethod(int arg1, Object arg2, Long arg3) {}
        @ParameterNames({"beer"})
        public static void factoryMethod(int arg1) {}
        @ParameterNames({"pizza"})
        public static void factoryMethod(Object arg1) {}
        @ParameterNames({"shot"})
        private static void factoryMethod(Double arg1) {}

        @ParameterNames({"x", "y", "z"})
        public void instanceMethod(int arg1, Object arg2, Long arg3) {}
        @ParameterNames({"apple"})
        public void instanceMethod(int arg1) {}
        @ParameterNames({"ipod"})
        public void instanceMethod(Object arg1) {}
        @ParameterNames({"psp"})
        private void instanceMethod(Double arg1) {}

        @ParameterNames({"gin"})
        public static void mixedMethods(Double arg1) {}
        @ParameterNames({"tonic"})
        public void mixedMethods(Short arg1) {}

        @ParameterNames({"ear"})
        public abstract void abstractMethod(Byte arg1);
    }

    private void assertParameterNames(List<String> expectedNames, Constructor constructor) {
        List<String> actualNames = parameterNameLoader.get(constructor);
        assertEquals(expectedNames, actualNames);
    }

    private void assertParameterNames(List<String> expectedNames, Method method) {
        List<String> actualNames = parameterNameLoader.get(method);
        assertEquals(expectedNames, actualNames);
    }

    private static void assertEquals(List<String> expectedNames, List<String> actualNames) {
        assertNotNull(expectedNames);
        assertNotNull(actualNames);
        assertEquals(Arrays.asList(expectedNames), Arrays.asList(actualNames));
    }

    private Constructor findPrivateConstructor(Class clazz, Class<?>... parameterTypes) {
        Constructor[] constructors = clazz.getDeclaredConstructors();
        for (Constructor constructor : constructors) {
            if (RecipeHelper.isAssignableFrom(Arrays.asList(parameterTypes), Arrays.<Class<?>>asList(constructor.getParameterTypes()))) {
                if (!Modifier.isPublic(constructor.getModifiers())) {
                    return constructor;
                }
            }
        }
        fail("Private constructor not found");
        return null;
    }

    private Method findPrivateMethod(Class clazz, String methodName, List<? extends Class<?>> parameterTypes) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName) && RecipeHelper.isAssignableFrom(parameterTypes, Arrays.asList(method.getParameterTypes()))) {
                if (!Modifier.isPublic(method.getModifiers())) {
                    return method;
                }
            }
        }
        fail("Private method not found");
        return null;
    }
}
