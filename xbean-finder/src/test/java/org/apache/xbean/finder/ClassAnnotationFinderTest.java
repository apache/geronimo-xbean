package org.apache.xbean.finder;

import org.acme.ClassAnnotatedClass;
import org.acme.NotAnnotated;
import org.acme.bar.ClassAnnotation;
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
}
