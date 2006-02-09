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
import java.util.Arrays;

import junit.framework.TestCase;

/**
 * @version $Rev$ $Date$
 */
public class ParameterNamesTest extends TestCase {
    public void testConstructor() throws Exception {
        Constructor constructor = TestClass.class.getConstructor(new Class[] {int.class, Object.class, Long.class});
        assertParameterNames(new String[] {"one", "two", "three"}, constructor);
    }

    public void testMethod() throws Exception {
        Method method = TestClass.class.getMethod("factoryMethod", new Class[] {int.class, Object.class, Long.class});
        assertParameterNames(new String[] {"a", "b", "c"}, method);
    }

    private static class TestClass {
        public TestClass(int one, Object two, Long three) {}
        public static void factoryMethod(int a, Object b, Long c) {}
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
        assertEquals(Arrays.asList(expectedNames), Arrays.asList(actualNames));
    }
}
