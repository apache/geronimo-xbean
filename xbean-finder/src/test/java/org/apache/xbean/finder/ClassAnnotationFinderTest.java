package org.apache.xbean.finder;

import org.acme.ClassAnnotatedClass;
import org.acme.bar.ClassAnnotation;
import org.apache.xbean.finder.archive.ClassesArchive;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ClassAnnotationFinderTest {
    @Test
    public void checkClassAnnotationIsNotFound() {
        final AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(ClassAnnotatedClass.class));
        final List<Class<?>> annotations = finder.findAnnotatedClasses(ClassAnnotation.class);
        assertEquals(0, annotations.size());
    }

    @Test
    public void checkClassAnnotationIsFound() {
        final AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(ClassAnnotatedClass.class), false);
        final List<Class<?>> annotations = finder.findAnnotatedClasses(ClassAnnotation.class);
        assertEquals(1, annotations.size());
        assertEquals(ClassAnnotatedClass.class, annotations.iterator().next());
    }
}
