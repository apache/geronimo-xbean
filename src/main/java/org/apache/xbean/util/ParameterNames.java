/*
 * Copyright 2002-2006 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.xbean.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.EmptyVisitor;

/**
 * Determines the parameter names of Constructors or Methods.
 */
public interface ParameterNames {
    /**
     * Gets the parameter names of the specified method or null if the class was compiled without debug symbols on.
     * @param method the method for which the parameter names should be retrieved
     * @return the parameter names or null if the class was compilesd without debug symbols on
     */
    String[] get(Method method);

    /**
     * Gets the parameter names of the specified constructor or null if the class was compiled without debug symbols on.
     * @param constructor the constructor for which the parameters should be retrieved
     * @return the parameter names or null if the class was compiled without debug symbols on
     */
    String[] get(Constructor constructor);

    /**
     * Gets the parameter names of all constructoror null if the class was compiled without debug symbols on.
     * @param clazz the class for which the constructor parameter names should be retrieved
     * @return a map from Constructor object to the parameter names or null if the class was compiled without debug symbols on
     */
    Map getAllConstructorParameters(Class clazz);

    /**
     * Gets the parameter names of all methods with the specified name or null if the class was compiled without debug symbols on.
     * @param clazz the class for which the method parameter names should be retrieved
     * @param methodName the of the method for which the parameters should be retrieved
     * @return a map from Method object to the parameter names or null if the class was compiled without debug symbols on
     */
    Map getAllMethodParameters(Class clazz, String methodName);
}
