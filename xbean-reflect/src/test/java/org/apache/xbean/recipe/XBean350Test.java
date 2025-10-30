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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class XBean350Test {

    @Test
    public void testImplicitConstructorSelectionNeverTriggeredXBean350() {
        final Set<String> availableProperties = new HashSet<>();
        Collections.addAll(availableProperties,
                "locale",
                "resourceName",
                "useLowerCase"
        );

        final List<String> parameterNames = new ArrayList<>();
        Collections.addAll(parameterNames,
                "locale",
                "resourceName",
                "useLowerCase"
        );

        final ReflectionUtil.ConstructorFactory constructor = ReflectionUtil.findConstructor(
                MultiConstructor.class,
                parameterNames,
                null,
                availableProperties,
                EnumSet.of(Option.NAMED_PARAMETERS));

        assertNotNull(constructor);
        assertEquals(parameterNames, constructor.getParameterNames());
        assertEquals(Locale.class, constructor.getParameterTypes().get(0));
        assertEquals(String.class, constructor.getParameterTypes().get(1));
        assertEquals(boolean.class, constructor.getParameterTypes().get(2));
    }

    @Test
    public void testFindConstructorWithParameterNames() {

        final Set<String> availableProperties = new HashSet<>();
        Collections.addAll(availableProperties,
                "locale",
                "resourceName",
                "useLowerCase"
        );

        final List<String> parameterNames = new ArrayList<>();
        Collections.addAll(parameterNames,
                "locale",
                "resourceName",
                "useLowerCase"
        );

        final ReflectionUtil.ConstructorFactory constructor = ReflectionUtil.findConstructor(
                MultiConstructor.class,
                parameterNames,
                null,
                availableProperties,
                EnumSet.of(Option.FIELD_INJECTION, Option.PRIVATE_PROPERTIES, Option.CASE_INSENSITIVE_PROPERTIES, Option.IGNORE_MISSING_PROPERTIES));

        assertNotNull(constructor);
        assertEquals(parameterNames, constructor.getParameterNames());
        assertEquals(Locale.class, constructor.getParameterTypes().get(0));
        assertEquals(String.class, constructor.getParameterTypes().get(1));
        assertEquals(boolean.class, constructor.getParameterTypes().get(2));

    }

    @Test
    public void testFindNoArgConstructor() {
        final ReflectionUtil.ConstructorFactory constructor = ReflectionUtil.findConstructor(
                MultiConstructor.class,
                null,
                null,
                new HashSet<>(),
                EnumSet.of(Option.FIELD_INJECTION, Option.PRIVATE_PROPERTIES, Option.CASE_INSENSITIVE_PROPERTIES, Option.IGNORE_MISSING_PROPERTIES));

        assertNotNull(constructor);
        assertEquals(0, constructor.getParameterTypes().size());
        assertEquals(0, constructor.getParameterNames().size());
    }

    @Test(expected = ConstructionException.class)
    public void testNonPublicClassThrows() {
        ReflectionUtil.findConstructor(
                NonPublicClass.class,
                null,
                null,
                Collections.emptySet(),
                EnumSet.noneOf(Option.class)
        );
    }

    @Test(expected = ConstructionException.class)
    public void testInterfaceThrows() {
        ReflectionUtil.findConstructor(
                InterfaceType.class,
                null,
                null,
                Collections.emptySet(),
                EnumSet.noneOf(Option.class)
        );
    }

    @Test(expected = ConstructionException.class)
    public void testAbstractClassThrows() {
        ReflectionUtil.findConstructor(
                AbstractType.class,
                null,
                null,
                Collections.emptySet(),
                EnumSet.noneOf(Option.class)
        );
    }

    @Test(expected = MissingFactoryMethodException.class)
    public void testPrivateConstructorNotAllowed() {
        ReflectionUtil.findConstructor(
                PrivateConstructorClass.class,
                null,
                null,
                Collections.emptySet(),
                EnumSet.noneOf(Option.class)
        );
    }

    @Test
    public void testPrivateConstructorAllowed() {
        ReflectionUtil.ConstructorFactory factory = ReflectionUtil.findConstructor(
                PrivateConstructorClass.class,
                null,
                null,
                Collections.emptySet(),
                EnumSet.of(Option.PRIVATE_CONSTRUCTOR)
        );

        assertNotNull(factory);
        assertEquals(0, factory.getParameterTypes().size());
    }

    @Test(expected = ConstructionException.class)
    public void testParameterCountMismatch() {
        List<String> names = Arrays.asList("a");
        List<Class<?>> types = Arrays.asList(String.class, Integer.class);

        ReflectionUtil.findConstructor(
                MultiConstructor.class,
                names,
                types,
                Collections.emptySet(),
                EnumSet.noneOf(Option.class)
        );
    }

    @Test(expected = MissingFactoryMethodException.class)
    public void testTypeMismatch() {
        List<String> names = Arrays.asList("locale", "resourceName", "useLowerCase");
        List<Class<?>> types = Arrays.asList(String.class, Integer.class, Boolean.class); // wrong type for 2nd param

        ReflectionUtil.findConstructor(
                MultiConstructor.class,
                names,
                types,
                new HashSet<>(names),
                EnumSet.noneOf(Option.class)
        );
    }

    @Test(expected = ConstructionException.class)
    public void testNoMatchingConstructor() {
        List<String> names = Arrays.asList("doesNotExist");
        List<Class<?>> types = Arrays.asList(Integer.class);

        ReflectionUtil.findConstructor(
                MultiConstructor.class,
                names,
                types,
                new HashSet<>(names),
                EnumSet.noneOf(Option.class)
        );
    }

    @Test
    public void testProtectedConstructorAllowedViaPrivateOption() {
        ReflectionUtil.ConstructorFactory factory = ReflectionUtil.findConstructor(
                ProtectedConstructorClass.class,
                Arrays.asList("s"),
                Arrays.asList(String.class),
                new HashSet<>(Arrays.asList("s")),
                EnumSet.of(Option.PRIVATE_CONSTRUCTOR)
        );

        assertNotNull(factory);
        assertEquals(1, factory.getParameterNames().size());
    }

    public interface InterfaceType {
    }

    public abstract static class AbstractType {
        public AbstractType() {
        }
    }

    private static class NonPublicClass {
        public NonPublicClass() {
        }
    }

    public static class PrivateConstructorClass {
        private PrivateConstructorClass() {
        }
    }

    public static class ProtectedConstructorClass {
        protected ProtectedConstructorClass(String s) {
        }
    }

    public static class MultiConstructor {

        public MultiConstructor() {
        }

        public MultiConstructor(Locale locale, String resourceName, boolean useLowerCase) {

        }

        public MultiConstructor(String locale, Properties properties, boolean useLowerCase) {

        }

        public MultiConstructor(String locale, Map<String, String> props, boolean useLowerCase) {

        }
    }
}
