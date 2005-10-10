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
package org.gbean.server.annotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * AnnotationProvider is a simple abstraction over the various annotatiom systems used in Java 1.4 and default provider
 * in Java 5.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public interface AnnotationProvider {
    /**
     * Gets the type of the specified annotation or null if not an recognized annotation.
     * @param annotation the annotation instance
     * @return the type of the annotation or null if not an recognized annotation
     */
    Class getAnnotationType(Object annotation);

    /**
     * Determines if the specific class had been annotated with the specified annotation.
     * @param annotation the annotation type to test
     * @param target the class to test
     * @return true if the class has been annotated with the specified annotation
     */
    boolean isAnnotationPresent(Class annotation, Class target);

    /**
     * Gets all of the annotations on the specified class.
     * @param target the class from which the annotations should be retrieved
     * @return the annotations on the specified class
     */
    Object[] getAnnotations(Class target);

    /**
     * Gets a specific annotation on the specified class.
     * @param annotation the annotation to retrieve from the class
     * @param target the class from which the annotation should be retrieved
     * @return an instance of the specified annotation or null if the class has not have the annotated
     */
    Object getAnnotation(Class annotation, Class target);

    /**
     * Determines if the specific method had been annotated with the specified annotation.
     * @param annotation the annotation type to test
     * @param target the method to test
     * @return true if the method has been annotated with the specified annotation
     */
    boolean isAnnotationPresent(Class annotation, Method target);

    /**
     * Gets all of the annotations on the specified method.
     * @param target the method from which the annotations should be retrieved
     * @return the annotations on the specified method
     */
    Object[] getAnnotations(Method target);

    /**
     * Gets a specific annotation on the specified method.
     * @param annotation the annotation to retrieve from the method
     * @param target the method from which the annotation should be retrieved
     * @return an instance of the specified annotation or null if the method has not have the annotated
     */
    Object getAnnotation(Class annotation, Method target);

    /**
     * Gets all of the annotations for each parameter of the specified method.
     * @param target the method from which the parameter annotations should be retrieved
     * @return the annotations on the parameters specified method
     */
    Object[][] getParameterAnnotations(Method target);

    /**
     * Determines if the specific constructor had been annotated with the specified annotation.
     * @param annotation the annotation type to test
     * @param target the constructor to test
     * @return true if the constructor has been annotated with the specified annotation
     */
    boolean isAnnotationPresent(Class annotation, Constructor target);

    /**
     * Gets all of the annotations on the specified constructor.
     * @param target the constructor from which the annotations should be retrieved
     * @return the annotations on the specified constructor
     */
    Object[] getAnnotations(Constructor target);

    /**
     * Gets a specific annotation on the specified constructor.
     * @param annotation the annotation to retrieve from the constructor
     * @param target the constructor from which the annotation should be retrieved
     * @return an instance of the specified annotation or null if the constructor has not have the annotated
     */
    Object getAnnotation(Class annotation, Constructor target);

    /**
     * Gets all of the annotations for each parameter of the specified constructor.
     * @param target the constructor from which the parameter annotations should be retrieved
     * @return the annotations on the parameters specified constructor
     */
    Object[][] getParameterAnnotations(Constructor target);

    /**
     * Determines if the specific field had been annotated with the specified annotation.
     * @param annotation the annotation type to test
     * @param target the field to test
     * @return true if the field has been annotated with the specified annotation
     */
    boolean isAnnotationPresent(Class annotation, Field target);

    /**
     * Gets all of the annotations on the specified field.
     * @param target the field from which the annotations should be retrieved
     * @return the annotations on the specified field
     */
    Object[] getAnnotations(Field target);

    /**
     * Gets a specific annotation on the specified field.
     * @param annotation the annotation to retrieve from the field
     * @param target the field from which the annotation should be retrieved
     * @return an instance of the specified annotation or null if the field has not have the annotated
     */
    Object getAnnotation(Class annotation, Field target);
}
