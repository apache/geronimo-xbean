/**
 *
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
package org.apache.xbean.recipe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ExplicitConstructionStrategy implements ConstructionStrategy {
    public Construction getConstruction(ObjectRecipe recipe, Class expectedType) throws ConstructionException {
        Class type = recipe.getType();

        //
        // verify that it is a class we can construct
        if (!Modifier.isPublic(type.getModifiers())) {
            throw new ConstructionException("Class is not public: " + type.getName());
        }
        if (Modifier.isInterface(type.getModifiers())) {
            throw new ConstructionException("Class is an interface: " + type.getName());
        }

        //
        // attempt to find a static factory
        String[] parameterNames = recipe.getConstructorArgNames();
        if (parameterNames == null) parameterNames = new String[0];
        Class[] parameterTypes = recipe.getConstructorArgTypes();
        if (parameterTypes == null) parameterTypes = new Class[parameterNames.length];
        if (parameterNames.length != parameterTypes.length) {
            throw new ConstructionException("Invalid ObjectRecipe: recipe has " + parameterNames.length +
                    " parameter names and " + parameterTypes.length + " parameter types");
        }
        if (recipe.getFactoryMethod() != null) {
            try {
                Method staticFactory = ReflectionUtil.findStaticFactory(type, recipe.getFactoryMethod(), parameterTypes, null);
                return new ExplicitConstruction(null, staticFactory, parameterNames, null);
            } catch (MissingFactoryMethodException ignored) {
            }

        }

        //
        // factory was not found, look for a constuctor

        // if expectedType is a subclass of the assigned type, we create
        // the sub class instead
        Class consturctorClass;
        if (type.isAssignableFrom(expectedType)) {
            consturctorClass = expectedType;
        } else {
            consturctorClass = type;
        }

        if (Modifier.isAbstract(consturctorClass.getModifiers())) {
            throw new ConstructionException("Class is abstract: " + consturctorClass.getName());
        }

        Constructor constructor = ReflectionUtil.findConstructor(consturctorClass, parameterTypes, null);
        return new ExplicitConstruction(constructor, null, parameterNames, recipe.getFactoryMethod());
    }
}
