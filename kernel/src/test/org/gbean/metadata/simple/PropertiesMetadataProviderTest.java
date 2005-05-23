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

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import org.gbean.kernel.OperationSignature;
import org.gbean.kernel.ConstructorSignature;
import org.gbean.metadata.ClassMetadata;
import org.gbean.metadata.MethodMetadata;
import org.gbean.metadata.ParameterMetadata;
import org.gbean.metadata.ConstructorMetadata;

/**
 * @version $Revision$ $Date$
 */
public class PropertiesMetadataProviderTest extends TestCase {
    private static final Map NAMES = new HashMap();
    static {
        NAMES.put(int.class, "i");
        NAMES.put(byte.class, "b");
        NAMES.put(short.class, "s");
        NAMES.put(int.class, "i");
        NAMES.put(long.class, "l");
        NAMES.put(float.class, "f");
        NAMES.put(double.class, "d");
        NAMES.put(char.class, "c");
        NAMES.put(boolean.class, "bool");
        NAMES.put(String.class, "string");
        NAMES.put(byte[].class, "bArray");
        NAMES.put(short[].class, "sArray");
        NAMES.put(int[].class, "iArray");
        NAMES.put(long[].class, "lArray");
        NAMES.put(float[].class, "fArray");
        NAMES.put(double[].class, "dArray");
        NAMES.put(char[].class, "cArray");
        NAMES.put(boolean[].class, "boolArray");
        NAMES.put(String[].class, "stringArray");
    }

    public void testLoad() {
        PropertiesMetadataProvider propertiesMetadataProvider = new PropertiesMetadataProvider();
        Class type = LotsOfTypes.class;
        ClassMetadata classMetadata = propertiesMetadataProvider.getClassMetadata(type);
        assertNotNull(classMetadata);

        Constructor[] constructors = type.getDeclaredConstructors();
        for (int i = 0; i < constructors.length; i++) {
            Constructor constructor = constructors[i];
            ConstructorMetadata constructorMetadata = classMetadata.getConstructor(constructor);
            assertNotNull(constructorMetadata);
            assertEquals(new ConstructorSignature(constructor), constructorMetadata.getSignature());
            assertEquals(constructor.getParameterTypes().length, constructorMetadata.getParameters().size());
            verifyParameterNames(constructor.getParameterTypes(), constructorMetadata.getParameters());
        }

        Method[] methods = type.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            MethodMetadata methodMetadata = classMetadata.getMethod(method);
            assertNotNull(methodMetadata);
            assertEquals(new OperationSignature(method), methodMetadata.getSignature());
            assertEquals(method.getParameterTypes().length, methodMetadata.getParameters().size());
            verifyParameterNames(method.getParameterTypes(), methodMetadata.getParameters());
        }

        assertEquals(classMetadata.get("description"), "class description");
        assertEquals(classMetadata.getConstructor(new ConstructorSignature(new String[]{})).get("description"), "constructor description");
        assertEquals(classMetadata.getMethod(new OperationSignature("getB", new String[]{})).get("description"), "method description");

    }

    private void verifyParameterNames(Class[] parameterTypes, List parameterMetadata) {
        for (int i = 0; i < parameterTypes.length; i++) {
            Class parameterType = parameterTypes[i];
            ParameterMetadata metadata = (ParameterMetadata) parameterMetadata.get(i);
            assertEquals(i, metadata.getIndex());
            assertEquals(parameterType, metadata.getType());
            String name = (String) metadata.get("name");
            assertNotNull(name);
            String typeName = (String) NAMES.get(parameterType);
            assertNotNull(typeName);
            assertEquals(typeName, name);
        }
    }
}
