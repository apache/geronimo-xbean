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
package org.gbean.metadata.simple;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.gbean.kernel.ConstructorSignature;
import org.gbean.kernel.OperationSignature;
import org.gbean.metadata.ClassMetadata;
import org.gbean.metadata.ConstructorMetadata;
import org.gbean.metadata.MethodMetadata;

/**
 * @version $Revision$ $Date$
 */
public class SimpleClassMetadata implements ClassMetadata {
    private final Map properties = new LinkedHashMap();
    private final Class type;
    private final Map methodMetadata = new HashMap();
    private final Map constructorMetadata = new HashMap();

    public SimpleClassMetadata(Class type) {
        this.type = type;

        Constructor[] constructors = type.getConstructors();
        for (int i = 0; i < constructors.length; i++) {
            Constructor constructor = constructors[i];
            ConstructorSignature signature = new ConstructorSignature(constructor);
            SimpleConstructorMetadata data = new SimpleConstructorMetadata(constructor);
            constructorMetadata.put(signature, data);
        }

        Method[] methods = type.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            OperationSignature signature = new OperationSignature(method);
            MethodMetadata data = new SimpleMethodMetadata(method);
            methodMetadata.put(signature, data);
        }
    }

    public Class getType() {
        return type;
    }

    public Set getConstructors() {
        return new HashSet(constructorMetadata.values());
    }

    public ConstructorMetadata getConstructor(ConstructorSignature signature) {
        return (ConstructorMetadata) constructorMetadata.get(signature);
    }

    public ConstructorMetadata getConstructor(Constructor constructor) {
        return (ConstructorMetadata) constructorMetadata.get(new ConstructorSignature(constructor));
    }

    public Set getMethods() {
        return new HashSet(methodMetadata.values());
    }

    public MethodMetadata getMethod(OperationSignature signature) {
        return (MethodMetadata) methodMetadata.get(signature);
    }

    public MethodMetadata getMethod(Method method) {
        OperationSignature signature = new OperationSignature(method);
        return (MethodMetadata) methodMetadata.get(signature);
    }

    public Map getProperties() {
        return properties;
    }

    public Object get(Object key) {
        return properties.get(key);
    }

    public Object put(Object key, Object value) {
        return properties.put(key, value);
    }

    public Object remove(Object key) {
        return properties.remove(key);
    }
}
