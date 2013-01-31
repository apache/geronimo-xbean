/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.xbean.finder;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public abstract class Parameter<E extends java.lang.reflect.Member> implements AnnotatedElement {
    private final E declaringExecutable;
    private final int index;

    private Parameter(E declaringExecutable, int index) {
        super();
        if (declaringExecutable == null) {
            throw new NullPointerException("declaringExecutable");
        }
        this.declaringExecutable = declaringExecutable;
        if (index < 0) {
            throw new IndexOutOfBoundsException(Integer.toString(index));
        }
        this.index = index;
    }

    public E getDeclaringExecutable() {
        return declaringExecutable;
    }

    public int getIndex() {
        return index;
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        for (Annotation annotation : getAnnotations()) {
            if (annotationClass.equals(annotation.annotationType())) {
                @SuppressWarnings("unchecked")
                final T result = (T) annotation;
                return result;
            }
        }
        return null;
    }

    public Annotation[] getAnnotations() {
        return getDeclaredAnnotations();
    }

    public Annotation[] getDeclaredAnnotations() {
        return getParameterAnnotations()[index];
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return getAnnotation(annotationClass) != null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof Parameter == false) {
            return false;
        }
        Parameter<?> p = (Parameter<?>) o;
        return declaringExecutable.equals(p.declaringExecutable) && index == p.index;
    }

    @Override
    public int hashCode() {
        int result = declaringExecutable.hashCode() << 4;
        result |= index;
        return result;
    }

    @Override
    public String toString() {
        return String.format("Parameter[index %s of %s]", index, declaringExecutable);
    }

    protected abstract Annotation[][] getParameterAnnotations();

    public static <T> Parameter<Constructor<T>> declaredBy(Constructor<T> ctor, int index) {
        return new Parameter<Constructor<T>>(ctor, index) {

            @Override
            protected Annotation[][] getParameterAnnotations() {
                return getDeclaringExecutable().getParameterAnnotations();
            }
        };
    }

    public static Parameter<Method> declaredBy(Method method, int index) {
        return new Parameter<Method>(method, index) {

            @Override
            protected Annotation[][] getParameterAnnotations() {
                return getDeclaringExecutable().getParameterAnnotations();
            }
        };
    }
}
