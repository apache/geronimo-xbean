/**
 *
 * Copyright 2005 The Apache Software Foundation
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
package org.apache.xbean.util;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.commons.EmptyVisitor;

/**
 * Implementation of ParameterNames that uses ASM to read the parameter names from the local variable table in the
 * class byte code.
 *
 * This wonderful piece of code was taken from org.springframework.core.LocalVariableTableParameterNameDiscover
 */
public class AsmParameterNames implements ParameterNames {
    /**
     * Weak map from Constructor or Method to a String[].
     */
    private final WeakHashMap cache = new WeakHashMap();

    /**
     * Gets the parameter names of the specified method or null if the class was compiled without debug symbols on.
     * @param method the method for which the parameter names should be retrieved
     * @return the parameter names or null if the class was compilesd without debug symbols on
     */
    public String[] get(Method method) {
        // check the cache
        if (cache.containsKey(method)) {
            return (String[]) cache.get(method);
        }

        Map allMethodParameters = getAllMethodParameters(method.getDeclaringClass(), method.getName());
        return (String[]) allMethodParameters.get(method);
    }

    /**
     * Gets the parameter names of the specified constructor or null if the class was compiled without debug symbols on.
     * @param constructor the constructor for which the parameters should be retrieved
     * @return the parameter names or null if the class was compiled without debug symbols on
     */
    public String[] get(Constructor constructor) {
        // check the cache
        if (cache.containsKey(constructor)) {
            return (String[]) cache.get(constructor);
        }

        Map allConstructorParameters = getAllConstructorParameters(constructor.getDeclaringClass());
        return (String[]) allConstructorParameters.get(constructor);
    }

    /**
     * Gets the parameter names of all constructoror null if the class was compiled without debug symbols on.
     * @param clazz the class for which the constructor parameter names should be retrieved
     * @return a map from Constructor object to the parameter names or null if the class was compiled without debug symbols on
     */
    public Map getAllConstructorParameters(Class clazz) {
        // Determine the constructors?
        Constructor[] constructors = clazz.getConstructors();
        if (constructors.length == 0) {
            return Collections.EMPTY_MAP;
        }

        // Check the cache
        if (cache.containsKey(constructors[0])) {
            Map allParameterNames = new HashMap();
            for (int i = 0; i < constructors.length; i++) {
                Constructor constructor = constructors[i];
                allParameterNames.put(constructor, cache.get(constructor));
            }
            return allParameterNames;
        }

        // Load the parameter names using ASM
        Map allParameterNames = new HashMap();
        try {
            ClassReader reader = AsmParameterNames.createClassReader(clazz);

            AsmParameterNames.AllParameterNamesDiscoveringVisitor visitor = new AsmParameterNames.AllParameterNamesDiscoveringVisitor(clazz);
            reader.accept(visitor, false);

            Map exceptions = visitor.getExceptions();
            if (exceptions.size() == 1) {
                throw new RuntimeException((Exception)exceptions.values().iterator().next());
            }
            if (!exceptions.isEmpty()) {
                throw new RuntimeException(exceptions.toString());
            }

            allParameterNames = visitor.getAllParameterNames();
        } catch (IOException ex) {
        }

        // Cache the names
        for (int i = 0; i < constructors.length; i++) {
            Constructor constructor = constructors[i];
            cache.put(constructor, allParameterNames.get(constructor));
        }
        return allParameterNames;
    }

    /**
     * Gets the parameter names of all methods with the specified name or null if the class was compiled without debug symbols on.
     * @param clazz the class for which the method parameter names should be retrieved
     * @param methodName the of the method for which the parameters should be retrieved
     * @return a map from Method object to the parameter names or null if the class was compiled without debug symbols on
     */
    public Map getAllMethodParameters(Class clazz, String methodName) {
        // Determine the constructors?
        Method[] methods = getMethods(clazz, methodName);
        if (methods.length == 0) {
            return Collections.EMPTY_MAP;
        }

        // Check the cache
        if (cache.containsKey(methods[0])) {
            Map allParameterNames = new HashMap();
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];
                allParameterNames.put(method, cache.get(method));
            }
            return allParameterNames;
        }

        // Load the parameter names using ASM
        Map allParameterNames = new HashMap();
        try {
            ClassReader reader = AsmParameterNames.createClassReader(clazz);

            AsmParameterNames.AllParameterNamesDiscoveringVisitor visitor = new AsmParameterNames.AllParameterNamesDiscoveringVisitor(clazz, methodName);
            reader.accept(visitor, false);

            Map exceptions = visitor.getExceptions();
            if (exceptions.size() == 1) {
                throw new RuntimeException((Exception)exceptions.values().iterator().next());
            }
            if (!exceptions.isEmpty()) {
                throw new RuntimeException(exceptions.toString());
            }

            allParameterNames = visitor.getAllParameterNames();
        } catch (IOException ex) {
        }

        // Cache the names
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            cache.put(method, allParameterNames.get(method));
        }
        return allParameterNames;
    }

    private Method[] getMethods(Class clazz, String methodName) {
        Method[] methods = clazz.getMethods();
        List matchingMethod = new ArrayList(methods.length);
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (method.getName().equals(methodName)) {
                matchingMethod.add(method);
            }
        }
        return (Method[]) matchingMethod.toArray(new Method[matchingMethod.size()]);
    }

    private static ClassReader createClassReader(Class declaringClass) throws IOException {
        InputStream in = null;
        try {
            ClassLoader classLoader = declaringClass.getClassLoader();
            in = classLoader.getResourceAsStream(declaringClass.getName().replace('.', '/') + ".class");
            ClassReader reader = new ClassReader(in);
            return reader;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static class AllParameterNamesDiscoveringVisitor extends EmptyVisitor {
        private final Map allParameterNames = new HashMap();
        private final Map exceptions = new HashMap();
        private final String methodName;
        private final Map methodMap = new HashMap();

        public AllParameterNamesDiscoveringVisitor(Class type, String methodName) {
            this.methodName = methodName;

            Method[] methods = type.getMethods();
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];
                if (method.getName().equals(methodName)) {
                    methodMap.put(Type.getMethodDescriptor(method), method);
                }
            }
        }

        public AllParameterNamesDiscoveringVisitor(Class type) {
            this.methodName = "<init>";

            Constructor[] constructors = type.getConstructors();
            for (int i = 0; i < constructors.length; i++) {
                Constructor constructor = constructors[i];
                Type[] types = new Type[constructor.getParameterTypes().length];
                for (int j = 0; j < types.length; j++) {
                    types[j] = Type.getType(constructor.getParameterTypes()[j]);
                }
                methodMap.put(Type.getMethodDescriptor(Type.VOID_TYPE, types), constructor);
            }
        }

        public Map getAllParameterNames() {
            return allParameterNames;
        }

        public Map getExceptions() {
            return exceptions;
        }

        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if (!name.equals(this.methodName)) {
                return null;
            }

            try {
                final String[] parameterNames;
                final boolean isStaticMethod;

                if (methodName.equals("<init>")) {
                    Constructor constructor = (Constructor) methodMap.get(desc);
                    if (constructor == null) {
                        return null;
                    }
                    parameterNames = new String[constructor.getParameterTypes().length];
                    allParameterNames.put(constructor, parameterNames);
                    isStaticMethod = false;
                } else {
                    Method method = (Method) methodMap.get(desc);
                    if (method == null) {
                        return null;
                    }
                    parameterNames = new String[method.getParameterTypes().length];
                    allParameterNames.put(method, parameterNames);
                    isStaticMethod = Modifier.isStatic(method.getModifiers());
                }

                return new EmptyVisitor() {
                    // assume static method until we get a first parameter name
                    public void visitLocalVariable(String name, String description, String signature, Label start, Label end, int index) {
                        if (isStaticMethod) {
                            parameterNames[index] = name;
                        } else if (index > 0) {
                            parameterNames[(index -1)] = name;
                        }
                    }
                };
            } catch (Exception e) {
                this.exceptions.put(signature, e);
            }
            return null;
        }
    }
}
