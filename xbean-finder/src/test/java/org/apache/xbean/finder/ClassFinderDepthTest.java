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

import org.apache.xbean.finder.archive.Archive;
import org.apache.xbean.finder.archive.ClassesArchive;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;

/**
 * @version $Rev$ $Date$
 */
public class ClassFinderDepthTest extends TestCase {


    public static interface Hue<T> {
    }

    public static interface Saturation {
    }

    public static interface Brightness {
    }

    public static interface HSB<T> extends Hue<T>, Saturation, Brightness {
    }

    public static class Color<T> implements HSB<T> {
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

    @Deprecated
    public static class CustomEvent extends EventObject {
        private static final long serialVersionUID = 1L;

        public CustomEvent() {
            super(new Object());
        }
    }

    public void testFindSubclassesIncomplete() throws Exception {
        for (int i = 0; i < 10; i++) { // try to avoid AsynchronousInheritanceAnnotationFinder "luck" issues
            for (final AnnotationFinder finder : new AnnotationFinder[] {
                new AnnotationFinder(new ClassesArchive(Crimson.class, Square.class)).link(),
                new AsynchronousInheritanceAnnotationFinder(new ClassesArchive(Crimson.class, Square.class)).link()
            }) {

                assertSubclasses(finder, Color.class, Red.class, Crimson.class);
                assertSubclasses(finder, Red.class, Crimson.class);
                assertSubclasses(finder, Crimson.class);

                assertSubclasses(finder, Shape.class, Square.class);
                assertSubclasses(finder, Square.class);
            }
        }
    }

    public void testFindSubclassesOfExternalAfterGet() {
        final Archive archive = new ClassesArchive(CustomEvent.class);
        for (int i = 0; i < 10; i++) { // try to avoid AsynchronousInheritanceAnnotationFinder "luck" issues
            for (final AnnotationFinder finder : new AnnotationFinder[] {
                new AnnotationFinder(archive),
                new AsynchronousInheritanceAnnotationFinder(archive)
            }) {
                assertEquals(Collections.singletonList(CustomEvent.class), finder.findAnnotatedClasses(Deprecated.class));
                finder.link();
                assertSubclasses(finder, EventObject.class, CustomEvent.class);
            }
        }
    }

    public void testFindImplementations() throws Exception {
        for (int i = 0; i < 10; i++) { // try to avoid AsynchronousInheritanceAnnotationFinder "luck" issues
            for (final AnnotationFinder finder : new AnnotationFinder[] {
                new AnnotationFinder(new ClassesArchive(Crimson.class, Square.class)).link(),
                new AsynchronousInheritanceAnnotationFinder(new ClassesArchive(Crimson.class, Square.class)).link()
            }) {

                assertImplementations(finder, HSB.class, Color.class, Red.class, Crimson.class);
                assertImplementations(finder, Hue.class, HSB.class, Color.class, Red.class, Crimson.class);
                assertImplementations(finder, Saturation.class, HSB.class, Color.class, Red.class, Crimson.class);
            }
        }
    }

    public void testFindImplementationsOfExternalAfterGet() {
        final Archive archive = new ClassesArchive(CustomEvent.class);
        for (int i = 0; i < 10; i++) { // try to avoid AsynchronousInheritanceAnnotationFinder "luck" issues
            for (final AnnotationFinder finder : new AnnotationFinder[] {
                new AnnotationFinder(archive),
                new AsynchronousInheritanceAnnotationFinder(archive)
            }) {
                assertEquals(Collections.singletonList(CustomEvent.class), finder.findAnnotatedClasses(Deprecated.class));
                finder.link();
                assertImplementations(finder, Serializable.class, EventObject.class, CustomEvent.class);
            }
        }
    }
    
    private void assertSubclasses(AnnotationFinder finder, Class<?> clazz, Class... subclasses) {
        final List<Class<?>> classes = new ArrayList(finder.findSubclasses(clazz));

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
