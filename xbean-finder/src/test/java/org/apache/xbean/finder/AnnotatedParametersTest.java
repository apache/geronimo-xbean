/*
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
package org.apache.xbean.finder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import org.acme.NotAnnotated;
import org.acme.bar.FullyAnnotated;
import org.acme.bar.ParamA;
import org.apache.xbean.finder.archive.ClassesArchive;
import org.junit.Test;

public class AnnotatedParametersTest {

    @Test
    public void testFindAnnotatedMethodParameters() {
        final AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(FullyAnnotated.class, NotAnnotated.class));
        List<Parameter<Method>> methodParameters = finder.findAnnotatedMethodParameters(ParamA.class);
        assertEquals(1, methodParameters.size());
        Parameter<Method> parameter = methodParameters.get(0);
        assertEquals(0, parameter.getIndex());
        assertEquals(FullyAnnotated.class, parameter.getDeclaringExecutable().getDeclaringClass());
        assertEquals("setMoreStrings", parameter.getDeclaringExecutable().getName());
        assertEquals(1, parameter.getDeclaringExecutable().getParameterTypes().length);
        assertEquals(String[][].class, parameter.getDeclaringExecutable().getParameterTypes()[0]);
        assertTrue(parameter.isAnnotationPresent(ParamA.class));
    }

    @Test
    public void testFindAnnotatedConstructorParameters() {
        final AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(FullyAnnotated.class, NotAnnotated.class));
        List<Parameter<Constructor<?>>> constructorParameters = finder.findAnnotatedConstructorParameters(ParamA.class);
        assertEquals(1, constructorParameters.size());
        Parameter<Constructor<?>> parameter = constructorParameters.get(0);
        assertEquals(0, parameter.getIndex());
        assertEquals(FullyAnnotated.class, parameter.getDeclaringExecutable().getDeclaringClass());
        assertEquals(2, parameter.getDeclaringExecutable().getParameterTypes().length);
        assertEquals(String.class, parameter.getDeclaringExecutable().getParameterTypes()[0]);
        assertEquals(int.class, parameter.getDeclaringExecutable().getParameterTypes()[1]);
        assertTrue(parameter.isAnnotationPresent(ParamA.class));
    }
}
