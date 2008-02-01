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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExplicitConstruction implements Construction {
    private Constructor constructor;
    private Method staticFactory;
    private String[] parameterNames;
    protected String factoryMethod;

    public ExplicitConstruction(Constructor constructor, Method staticFactory, String[] parameterNames, String factoryMethod) {
        this.constructor = constructor;
        this.factoryMethod = factoryMethod;
        this.parameterNames = parameterNames;
        this.staticFactory = staticFactory;
    }

    public boolean hasInstanceFactory() {
        return factoryMethod != null;
    }

    public List<String> getParameterNames() {
        if (parameterNames == null) {
            throw new ConstructionException("InstanceFactory has not been initialized");
        }

        return new ArrayList<String>(Arrays.asList(parameterNames));
    }

    public List<Class> getParameterTypes() {
        if (constructor == null && staticFactory == null) {
            throw new ConstructionException("InstanceFactory has not been initialized");
        }

        if (staticFactory != null) {
            return new ArrayList<Class>(Arrays.asList(staticFactory.getParameterTypes()));
        }
        return new ArrayList<Class>(Arrays.asList(constructor.getParameterTypes()));
    }

    public Object create(Object... parameters) throws ConstructionException {
        if (constructor == null && staticFactory == null) {
            throw new ConstructionException("InstanceFactory has not been initialized");
        }

        // create the instance
        Object instance;
        try {
            if (staticFactory != null) {
                instance = staticFactory.invoke(null, parameters);
            } else {
                instance = constructor.newInstance(parameters);
            }
        } catch (Exception e) {
            Throwable t = e;
            if (e instanceof InvocationTargetException) {
                InvocationTargetException invocationTargetException = (InvocationTargetException) e;
                if (invocationTargetException.getCause() != null) {
                    t = invocationTargetException.getCause();
                }
            }
            if (staticFactory != null) {
                throw new ConstructionException("Error invoking factory method: " + staticFactory, t);
            } else {
                throw new ConstructionException("Error invoking constructor: " + constructor, t);
            }
        }
        return instance;
    }

    public Object callInstanceFactory(Object instance) throws ConstructionException {
        // if we have a factory method name and did not find a static factory,
        // look for a instance factory method
        if (factoryMethod != null && staticFactory == null) {
            // find the instance factory method
            Method instanceFactory = ReflectionUtil.findInstanceFactory(instance.getClass(), factoryMethod, null);

            try {
                instance = instanceFactory.invoke(instance);
            } catch (Exception e) {
                Throwable t = e;
                if (e instanceof InvocationTargetException) {
                    InvocationTargetException invocationTargetException = (InvocationTargetException) e;
                    if (invocationTargetException.getCause() != null) {
                        t = invocationTargetException.getCause();
                    }
                }
                throw new ConstructionException("Error calling instance factory method: " + instanceFactory, t);
            }
        }
        return instance;
    }
}