/**
 *
 * Copyright 2005 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.xbean.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import junit.framework.TestCase;
import org.apache.xbean.recipe.ObjectRecipe;

/**
 * @version $Rev$ $Date$
 */
public class ParameterNamesTest extends TestCase {
    public void testConstructor() throws Exception {
        Constructor constructor = TestClass.class.getConstructor(new Class[] {int.class, Object.class, Long.class});
        assertParameterNames(new String[] {"one", "two", "three"}, constructor);
    }

    public void testMethod() throws Exception {
        Method method = TestClass.class.getMethod("instanceMethod", new Class[] {int.class, Object.class, Long.class});
        assertParameterNames(new String[] {"x", "y", "z"}, method);
    }

    public void testStaticMethod() throws Exception {
        Method method = TestClass.class.getMethod("factoryMethod", new Class[] {int.class, Object.class, Long.class});
        assertParameterNames(new String[] {"a", "b", "c"}, method);
    }

    public void testInheritedMethod() throws Exception {
        Method method = TestClass.class.getMethod("inheritedMethod", new Class[] {Map.class});
        assertParameterNames(new String[] {"nothing"}, method);
    }

    public void testPrivateConstructor() throws Exception {
        Constructor constructor = findPrivateConstructor(TestClass.class, new Class[]{Double.class});
        assertParameterNames(new String[] {"scotch"}, constructor);
    }

    public void testPrivateMethod() throws Exception {
        Method method = findPrivateMethod(TestClass.class, "factoryMethod", new Class[] {Double.class});
        assertParameterNames(new String[] {"shot"}, method);
    }

    public void testAllConstructors() throws Exception {
        Map expectedMap = new HashMap();
        expectedMap.put(TestClass.class.getConstructor(new Class[] {int.class, Object.class, Long.class}),new String[] {"one", "two", "three"});
        expectedMap.put(TestClass.class.getConstructor(new Class[] {int.class}),new String[] {"foo"});
        expectedMap.put(TestClass.class.getConstructor(new Class[] {Object.class}),new String[] {"bar"});

        Map actualMap = ParameterNames.getAllConstructorParameters(TestClass.class);
        assertEquals(expectedMap, actualMap);
    }

    public void testAllInstanceMethods() throws Exception {
        Map expectedMap = new HashMap();
        expectedMap.put(TestClass.class.getMethod("instanceMethod", new Class[] {int.class, Object.class, Long.class}), new String[] {"x", "y", "z"});
        expectedMap.put(TestClass.class.getMethod("instanceMethod", new Class[] {int.class}), new String[] {"apple"});
        expectedMap.put(TestClass.class.getMethod("instanceMethod", new Class[] {Object.class}), new String[] {"ipod"});

        Map actualMap = ParameterNames.getAllMethodParameters(TestClass.class, "instanceMethod");
        assertEquals(expectedMap, actualMap);
    }

    public void testAllStataicMethods() throws Exception {
        Map expectedMap = new HashMap();
        expectedMap.put(TestClass.class.getMethod("factoryMethod", new Class[] {int.class, Object.class, Long.class}), new String[] {"a", "b", "c"});
        expectedMap.put(TestClass.class.getMethod("factoryMethod", new Class[] {int.class}), new String[] {"beer"});
        expectedMap.put(TestClass.class.getMethod("factoryMethod", new Class[] {Object.class}), new String[] {"pizza"});

        Map actualMap = ParameterNames.getAllMethodParameters(TestClass.class, "factoryMethod");
        assertEquals(expectedMap, actualMap);
    }

    public void testAllMixedMethods() throws Exception {
        Map expectedMap = new HashMap();
        expectedMap.put(TestClass.class.getMethod("mixedMethods", new Class[] {Double.class}), new String[] {"gin"});
        expectedMap.put(TestClass.class.getMethod("mixedMethods", new Class[] {Short.class}), new String[] {"tonic"});

        Map actualMap = ParameterNames.getAllMethodParameters(TestClass.class, "mixedMethods");
        assertEquals(expectedMap, actualMap);
    }

    private static class ParentTestClass {
        public void inheritedMethod(Map nothing) {}
    }

    private static abstract class TestClass extends ParentTestClass {
        public TestClass(int one, Object two, Long three) {}
        public TestClass(int foo) {}
        public TestClass(Object bar) {}
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

    private static void assertParameterNames(String[] expectedNames, Constructor constructor) {
        String[] actualNames = ParameterNames.get(constructor);
        assertEquals(expectedNames, actualNames);
    }

    private static void assertParameterNames(String[] expectedNames, Method method) {
        String[] actualNames = ParameterNames.get(method);
        assertEquals(expectedNames, actualNames);
    }

    private static void assertEquals(String[] expectedNames, String[] actualNames) {
        assertNotNull(expectedNames);
        assertNotNull(actualNames);
        assertEquals(Arrays.asList(expectedNames), Arrays.asList(actualNames));
    }

    private void assertEquals(Map expectedMap, Map actualMap) {
        for (Iterator iterator = actualMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            Object key = entry.getKey();
            String[] expectedNames = (String[]) expectedMap.get(key);
            String[] actualNames = (String[]) entry.getValue();
            assertEquals(expectedNames, actualNames);
        }
    }

    private Constructor findPrivateConstructor(Class clazz, Class[] argTypes) {
        Constructor[] constructors = clazz.getDeclaredConstructors();
        for (int i = 0; i < constructors.length; i++) {
            Constructor constructor = constructors[i];
            if (ObjectRecipe.isAssignableFrom(argTypes, constructor.getParameterTypes())) {
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
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (method.getName().equals(methodName) && ObjectRecipe.isAssignableFrom(argTypes, method.getParameterTypes())) {
                if (!Modifier.isPublic(method.getModifiers())) {
                    return method;
                }
            }
        }
        fail("Private method not found");
        return null;
    }
}
