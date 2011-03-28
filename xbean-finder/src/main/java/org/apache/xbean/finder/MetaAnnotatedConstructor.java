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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;

/**
 * @version $Rev$ $Date$
 */
public class MetaAnnotatedConstructor<T> extends MetaAnnotatedObject<Constructor<T>> implements AnnotatedMethod<Constructor<T>> {

    public MetaAnnotatedConstructor(Constructor<T> target) {
        super(target, unroll(target.getDeclaringClass(), target));
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return get().getDeclaredAnnotations();
    }

    @Override
    public Annotation[][] getParameterAnnotations() {
        return get().getParameterAnnotations();
    }

    @Override
    public Class<?> getDeclaringClass() {
        return get().getDeclaringClass();
    }

    @Override
    public String getName() {
        return get().getName();
    }

    @Override
    public int getModifiers() {
        return get().getModifiers();
    }

    @Override
    public Class<?>[] getParameterTypes() {
        return get().getParameterTypes();
    }

    @Override
    public java.lang.reflect.Type[] getGenericParameterTypes() {
        return get().getGenericParameterTypes();
    }

    @Override
    public Class<?>[] getExceptionTypes() {
        return get().getExceptionTypes();
    }

    @Override
    public java.lang.reflect.Type[] getGenericExceptionTypes() {
        return get().getGenericExceptionTypes();
    }

    @Override
    public String toGenericString() {
        return get().toGenericString();
    }

    @Override
    public boolean isVarArgs() {
        return get().isVarArgs();
    }

    @Override
    public boolean isSynthetic() {
        return get().isSynthetic();
    }

}
