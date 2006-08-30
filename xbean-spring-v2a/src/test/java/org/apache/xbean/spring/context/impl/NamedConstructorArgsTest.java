/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.xbean.spring.context.impl;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Properties;

import junit.framework.TestCase;

/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class NamedConstructorArgsTest extends TestCase {
    private Properties properties = new Properties();

    public void testPropertyParsing() {
        assertEquals("bar", properties.getProperty("foo"));
        assertEquals("blah", properties.getProperty("foo,chese"));
        assertEquals("StringBuffer", properties.getProperty("java.lang.String(java.lang.StringBuffer)"));
        assertEquals("char[]", properties.getProperty("java.lang.String([C)"));
        assertEquals("byte[],int,int", properties.getProperty("java.lang.String([B,int,int)"));
        assertEquals("URL[],ClassLoader", properties.getProperty("java.net.URLClassLoader([Ljava.net.URL;,java.lang.ClassLoader)"));
    }

    public void testMappingMetaData() throws Exception {
        MappingMetaData mappingMetaData = new MappingMetaData(properties);
        Constructor constructor = URLClassLoader.class.getConstructor(new Class[] { URL[].class, ClassLoader.class});
        assertTrue(mappingMetaData.isDefaultConstructor(constructor));
        assertEquals(Arrays.asList(new String[] { "urls", "parent" }),
                Arrays.asList(mappingMetaData.getParameterNames(constructor)));

        constructor = String.class.getConstructor(new Class[] { byte[].class, int.class, int.class});
        assertFalse(mappingMetaData.isDefaultConstructor(constructor));
        assertEquals(Arrays.asList(new String[] { "bytes", "offset", "length" }),
                Arrays.asList(mappingMetaData.getParameterNames(constructor)));
    }

    protected void setUp() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("# test properties\n");
        buf.append("foo=bar\n");
        buf.append("foo,chese=blah\n");
        Constructor constructor = String.class.getConstructor(new Class[] { StringBuffer.class});
        buf.append(MappingMetaData.constructorToPropertyName(constructor) + "=StringBuffer\n");
        constructor = String.class.getConstructor(new Class[] { char[].class});
        buf.append(MappingMetaData.constructorToPropertyName(constructor) + "=char[]\n");
        constructor = String.class.getConstructor(new Class[] { byte[].class, int.class, int.class});
        buf.append(MappingMetaData.constructorToPropertyName(constructor) + "=byte[],int,int\n");
        constructor = URLClassLoader.class.getConstructor(new Class[] { URL[].class, ClassLoader.class});
        buf.append(MappingMetaData.constructorToPropertyName(constructor) + "=URL[],ClassLoader\n");

        properties.load(new ByteArrayInputStream(buf.toString().getBytes()));

        constructor = URLClassLoader.class.getConstructor(new Class[] { URL[].class, ClassLoader.class});
        properties.put(MappingMetaData.constructorToPropertyName(constructor) + ".default", "true");
        properties.put(MappingMetaData.constructorToPropertyName(constructor) + ".parameterNames", "urls,parent");
        constructor = String.class.getConstructor(new Class[] { byte[].class, int.class, int.class});
        properties.put(MappingMetaData.constructorToPropertyName(constructor) + ".default", "false");
        properties.put(MappingMetaData.constructorToPropertyName(constructor) + ".parameterNames", "bytes,offset,length");
    }
}
