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

import org.acme.ClassAnnotatedClass;
import org.acme.ClassMultipleAnnotatedClass;
import org.acme.NotAnnotated;
import org.acme.bar.ClassAnnotation;
import org.acme.bar.Get;
import org.acme.foo.Property;
import org.apache.xbean.finder.archive.ClassesArchive;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ClassAnnotationFinderTest {

    @Test
    public void checkClassAnnotationIsNotFound() {
        final AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(ClassAnnotatedClass.class, NotAnnotated.class));
        final List<Class<?>> annotations = finder.findAnnotatedClasses(ClassAnnotation.class);
        assertEquals(0, annotations.size());
    }

    @Test
    public void checkClassAnnotationIsFound() {
        final AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(ClassAnnotatedClass.class, NotAnnotated.class), false);
        final List<Class<?>> annotations = finder.findAnnotatedClasses(ClassAnnotation.class);
        assertEquals(1, annotations.size());
        assertEquals(ClassAnnotatedClass.class, annotations.iterator().next());
    }

    @Test
    public void checkClassAnnotationOnMethod() throws Exception{
        final AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(ClassAnnotatedClass.class, NotAnnotated.class), false);
        final List<Method> annotations = finder.findAnnotatedMethods(ClassAnnotation.class);
        assertEquals(1, annotations.size());
        assertEquals(ClassAnnotatedClass.class.getDeclaredMethod("green"), annotations.get(0));
    }

    @Test
    public void checkClassAnnotationOnConstructor() throws Exception {
        final AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(ClassAnnotatedClass.class, NotAnnotated.class), false);
        final List<Constructor> annotations = finder.findAnnotatedConstructors(ClassAnnotation.class);
        assertEquals(1, annotations.size());
        assertEquals(ClassAnnotatedClass.class.getDeclaredConstructor(), annotations.get(0));
    }

    @Test
    public void checkClassAnnotationOnField() throws Exception {
        final AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(ClassAnnotatedClass.class, NotAnnotated.class), false);
        final List<Field> annotations = finder.findAnnotatedFields(ClassAnnotation.class);
        assertEquals(1, annotations.size());
        assertEquals(ClassAnnotatedClass.class.getDeclaredField("green"), annotations.get(0));
    }

    @Test
    public void checkClassAnnotationOnMethodDefaults() {
        final AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(ClassAnnotatedClass.class, NotAnnotated.class));
        final List<Method> annotations = finder.findAnnotatedMethods(ClassAnnotation.class);
        assertEquals(0, annotations.size());
    }


    @Test
    public void checkClassAnnotationOnConstructorDefaults() {
        final AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(ClassAnnotatedClass.class, NotAnnotated.class));
        final List<Constructor> annotations = finder.findAnnotatedConstructors(ClassAnnotation.class);
        assertEquals(0, annotations.size());
    }

    @Test
    public void checkClassAnnotationOnFieldDefaults() {
        final AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(ClassAnnotatedClass.class, NotAnnotated.class));
        final List<Field> annotations = finder.findAnnotatedFields(ClassAnnotation.class);
        assertEquals(0, annotations.size());
    }

    @Test
    public void checkOnlyWhitelistedAnnotationsAreScanned() {
        {   // default is all annotations
            final AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(ClassMultipleAnnotatedClass.class));
            final int properties = finder.findAnnotatedMethods(Property.class).size();
            final int annotations = finder.findAnnotatedMethods(Get.class).size();
            assertEquals(2,properties + annotations);
        }
        {
            final AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(ClassMultipleAnnotatedClass.class)) {
                @Override
                protected boolean isTracked(final String annotationType) {
                    return "Lorg/acme/bar/Get;".equals(annotationType);
                }
            };
            assertEquals(1,
                    finder.findAnnotatedMethods(Property.class).size() +
                            finder.findAnnotatedMethods(Get.class).size());
        }
    }

    @Test
    public void checkClassInfosAreRemovedWhenNaked() {
        {   // default is all annotations
            final AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(ClassMultipleAnnotatedClass.class, NotAnnotated.class));
            assertEquals(2,finder.getAnnotatedClassNames().size());
        }
        {
            final AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(ClassMultipleAnnotatedClass.class, NotAnnotated.class)) {
                @Override
                protected boolean cleanOnNaked() {
                    return true;
                }
            };
            assertEquals(1, finder.getAnnotatedClassNames().size());
        }
    }
}
