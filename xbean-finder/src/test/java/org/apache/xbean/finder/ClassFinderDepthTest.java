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
package org.apache.xbean.finder;

import junit.framework.TestCase;
import org.apache.xbean.finder.archive.ClassesArchive;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @version $Rev$ $Date$
 */
public class ClassFinderDepthTest extends TestCase {
    @Deprecated
    public static abstract class TargetImpl implements java.lang.annotation.Target {
    }

    public static interface Hue<T> {
    }

    public static interface Saturation {
    }

    public static interface Brightness {
    }

    public static interface HSB<T> extends Hue<T>, Saturation, Brightness {
    }

    public static class Color<T> implements HSB<T> {
        @Deprecated
        private String foo;
    }

    public static class Red extends Color {
    }

    public static class Crimson extends Red {
    }

    // added to ensure there are classes that shouldn't match

    public static class Shape {
    }

    public static class Square extends Shape {
    }

    public void testFindParentFieldOutsideArchive() {
        final AnnotationFinder finder  = new AnnotationFinder(new ClassesArchive(Red.class) {
            @Override
            public InputStream getBytecode(final String className) throws IOException, ClassNotFoundException {
                if (!className.contains("Red")) {
                    throw new ClassNotFoundException();
                }
                return super.getBytecode(className);
            }
        }).link();
        assertEquals(1, finder.findAnnotatedFields(Deprecated.class).size());
    }

    public void testFindSubclassesIncomplete() throws Exception {
        for (int i = 0; i < 10; i++) {
            for (final AnnotationFinder finder : new AnnotationFinder[] {
                new AnnotationFinder(new ClassesArchive(Crimson.class, Square.class)).link()
            }) {

                assertSubclasses(finder, Color.class, Red.class, Crimson.class);
                assertSubclasses(finder, Red.class, Crimson.class);
                assertSubclasses(finder, Crimson.class);

                assertSubclasses(finder, Shape.class, Square.class);
                assertSubclasses(finder, Square.class);
            }
        }
    }

    public void testFindAnnotatedInterfaceImplementationsAfterGet() {
        for (int i = 0; i < 10; i++) {
                final ClassesArchive archive = new ClassesArchive(TargetImpl.class);
                for (final AnnotationFinder finder : new AnnotationFinder[] {
                            new AnnotationFinder(archive)
                                }) {
                        assertEquals(Collections.singletonList(TargetImpl.class), finder.findAnnotatedClasses(Deprecated.class));
                        finder.link();
                        assertImplementations(finder, java.lang.annotation.Target.class, TargetImpl.class);
                    }
            }
    }

    public void testFindImplementations() throws Exception {
        for (int i = 0; i < 10; i++) {
            final AnnotationFinder finder =
                    new AnnotationFinder(new ClassesArchive(Crimson.class, Square.class)).link();

            assertImplementations(finder, HSB.class, Color.class, Red.class, Crimson.class);
            assertImplementations(finder, Hue.class, HSB.class, Color.class, Red.class, Crimson.class);
            assertImplementations(finder, Saturation.class, HSB.class, Color.class, Red.class, Crimson.class);
        }
    }

    private void assertSubclasses(AnnotationFinder finder, Class<?> clazz, Class... subclasses) {
        final List<Class<?>> classes = new ArrayList<Class<?>>(finder.findSubclasses(clazz));

        for (Class subclass : subclasses) {
            assertContains(classes, subclass);
        }
        assertSize(classes, subclasses.length);
    }

    private void assertImplementations(AnnotationFinder finder, Class<?> clazz, Class... implementations) {
        final List<Class<?>> classes = new ArrayList(finder.findImplementations(clazz));

        for (Class subclass : implementations) {
            assertContains(classes, subclass);
        }
        assertSize(classes, implementations.length);
    }

    private void assertSize(List<?> list, int size) {
        assertEquals(size, list.size());
    }

    private void assertContains(List<Class<?>> classes, Class<?> clazz) {
        assertTrue("missing " + clazz.getSimpleName(), classes.contains(clazz));
    }

}
