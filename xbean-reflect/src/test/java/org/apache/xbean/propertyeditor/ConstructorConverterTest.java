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
package org.apache.xbean.propertyeditor;

import org.junit.Assert;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.*;

public class ConstructorConverterTest extends Assert {

    @Test
    public void test() throws Exception {
        final String string = "some://where?lives=code";
        assertConversion(URI.class, string, new URI(string));
    }

    @Test
    public void convertShort() throws Exception {
        final String string = "" + Short.MAX_VALUE;
        assertConversion(java.lang.Short.class, string, new java.lang.Short(string));
    }

    @Test
    public void convertLong() throws Exception {
        final String string = "" + Long.MAX_VALUE;
        assertConversion(java.lang.Long.class, string, new java.lang.Long(string));
    }

    @Test
    public void convertInteger() throws Exception {
        final String string = "" + Integer.MAX_VALUE;
        assertConversion(java.lang.Integer.class, string, new java.lang.Integer(string));
    }

    @Test
    public void convertFloat() throws Exception {
        final String string = "" + Float.MAX_VALUE;
        assertConversion(java.lang.Float.class, string, new Float(string));
    }

    @Test
    public void convertDouble() throws Exception {
        final String string = "" + Double.MAX_VALUE;
        assertConversion(java.lang.Double.class, string, new Double(string));
    }

    private void assertConversion(final Class<?> type, final String text, final Object expected) {
        final Object actual = convert(type, text, expected);
        assertEquals(expected, actual);
    }

    private static Object convert(final Class<?> type, final String text, final Object expected) {
        final ConstructorConverter editor = ConstructorConverter.editor(type);
        assertNotNull(editor);

        final Object actual = editor.toObjectImpl(text);
        assertNotNull(actual);
        assertEquals(expected.getClass(), actual.getClass());
        return actual;
    }

}