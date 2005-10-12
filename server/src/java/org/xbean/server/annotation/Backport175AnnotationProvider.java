/**
 *
 * Copyright 2005 the original author or authors.
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
package org.xbean.server.annotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.codehaus.backport175.reader.Annotation;
import org.codehaus.backport175.reader.Annotations;

/**
 * Annotation provider for backport 175.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class Backport175AnnotationProvider implements AnnotationProvider {
    private static final Annotation[][] NO_PARAMETER_ANNOTATIONS = new Annotation[0][0];

    /**
     * {@inheritDoc}
     */
    public Class getAnnotationType(Object annotation) {
        if (annotation instanceof Annotation) {
            return ((Annotation) annotation).annotationType();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAnnotationPresent(Class annotation, Class target) {
        return Annotations.isAnnotationPresent(annotation, target);
    }

    /**
     * {@inheritDoc}
     */
    public Object[] getAnnotations(Class target) {
        return Annotations.getAnnotations(target);
    }

    /**
     * {@inheritDoc}
     */
    public Object getAnnotation(Class annotation, Class target) {
        return Annotations.getAnnotation(annotation, target);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAnnotationPresent(Class annotation, Method target) {
        return Annotations.isAnnotationPresent(annotation, target);
    }

    /**
     * {@inheritDoc}
     */
    public Object[] getAnnotations(Method method) {
        return Annotations.getAnnotations(method);
    }

    /**
     * {@inheritDoc}
     */
    public Object getAnnotation(Class annotation, Method target) {
        return Annotations.getAnnotation(annotation, target);
    }

    /**
     * {@inheritDoc}
     */
    public Object[][] getParameterAnnotations(Method target) {
        // backport 175 does not support parameter annotations
        return NO_PARAMETER_ANNOTATIONS;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAnnotationPresent(Class annotation, Constructor target) {
        return Annotations.isAnnotationPresent(annotation, target);
    }

    /**
     * {@inheritDoc}
     */
    public Object[] getAnnotations(Constructor target) {
        return Annotations.getAnnotations(target);
    }

    /**
     * {@inheritDoc}
     */
    public Object getAnnotation(Class annotation, Constructor target) {
        return Annotations.getAnnotation(annotation, target);
    }

    /**
     * {@inheritDoc}
     */
    public Object[][] getParameterAnnotations(Constructor target) {
        // backport 175 does not support parameter annotations
        return NO_PARAMETER_ANNOTATIONS;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAnnotationPresent(Class annotation, Field target) {
        return Annotations.isAnnotationPresent(annotation, target);
    }

    /**
     * {@inheritDoc}
     */
    public Object[] getAnnotations(Field field) {
        return Annotations.getAnnotations(field);
    }

    /**
     * {@inheritDoc}
     */
    public Object getAnnotation(Class annotation, Field target) {
        return Annotations.getAnnotation(annotation, target);
    }
}
