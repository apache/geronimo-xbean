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

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class StaticFactoryConverterTest extends Assert {

    /**
     * Real-world usage of get*
     */
    @Test
    public void convertMac() throws Exception {
        final String string = "HmacSHA1";
        convert(javax.crypto.Mac.class, string, javax.crypto.Mac.getInstance(string));
    }

    /**
     * Real-world usage of get*
     */
    @Test
    public void convertKeyAgreement() throws Exception {
        final String string = "DiffieHellman";
        convert(javax.crypto.KeyAgreement.class, string, javax.crypto.KeyAgreement.getInstance(string));
    }

    /**
     * Real-world usage of get*
     */
    @Test
    public void convertSecretKeyFactory() throws Exception {
        final String string = "DES";
        convert(javax.crypto.SecretKeyFactory.class, string, javax.crypto.SecretKeyFactory.getInstance(string));
    }

    /**
     * Real-world usage of get*
     */
    @Test
    public void convertCipher() throws Exception {
        final String string = "AES/CBC/PKCS5Padding";
        convert(javax.crypto.Cipher.class, string, javax.crypto.Cipher.getInstance(string));
    }

    /**
     * Real-world usage of get*
     */
    @Test
    public void convertKeyGenerator() throws Exception {
        final String string = "HmacSHA256";
        convert(javax.crypto.KeyGenerator.class, string, javax.crypto.KeyGenerator.getInstance(string));
    }

    /**
     * Enum valueOf
     */
    @Test
    public void convertElementType() throws Exception {
        final String string = "METHOD";
        assertConversion(java.lang.annotation.ElementType.class, string, java.lang.annotation.ElementType.valueOf(string));
    }

    /**
     * Enum valueOf
     */
    @Test
    public void convertMemoryType() throws Exception {
        final String string = "HEAP";
        assertConversion(java.lang.management.MemoryType.class, string, java.lang.management.MemoryType.valueOf(string));
    }

    /**
     * Non-enum valueOf
     */
    @Test
    public void convertDate() throws Exception {
        final String string = "1976-03-30";
        assertConversion(java.sql.Date.class, string, java.sql.Date.valueOf(string));
    }

// Restore when we support Java 8
//    /**
//     * Real-world usage of "of"
//     */
//    @Test
//    public void convertZoneId() throws Exception {
//        final String string = "UTC+3";
//        assertConversion(java.time.ZoneId.class, string, java.time.ZoneId.of(string));
//    }
//
//    /**
//     * Real-world usage of "of*"
//     */
//    @Test
//    public void convertDateTimeFormatter() throws Exception {
//        final String string = "yyyy MM dd";
//        final Object expected = java.time.format.DateTimeFormatter.ofPattern(string);
//        final Object actual = convert(java.time.format.DateTimeFormatter.class, string, expected);
//        assertEquals(expected.toString(), actual.toString());
//    }

    /**
     * Real-world usage of from*
     */
    @Test
    public void convertUUID() throws Exception {
        final String string = "6bb98919-ef60-4191-9f76-b77be2cb9812";
        assertConversion(java.util.UUID.class, string, java.util.UUID.fromString(string));
    }

    /**
     * Real-world usage of compile
     */
    @Test
    public void convertPattern() throws Exception {
        final String string = ".*";
        final Object expected = java.util.regex.Pattern.compile(string);
        final Object actual = convert(java.util.regex.Pattern.class, string, expected);
        assertEquals(expected.toString(), actual.toString());
    }

    /**
     * Real-word usage of get*
     */
    @Test
    public void convertObjectName() throws Exception {
        final String string = "*:type=Foo,name=Bar";
        assertConversion(javax.management.ObjectName.class, string, javax.management.ObjectName.getInstance(string));
    }

    /**
     * Real-word usage of get*
     */
    @Test
    public void convertDTD() throws Exception {
        final String string = "html";
        final Object expected = javax.swing.text.html.parser.DTD.getDTD(string);
        final Object actual = convert(javax.swing.text.html.parser.DTD.class, string, expected);
        assertEquals(expected.toString(), actual.toString());
    }

    /**
     * Real-word usage of get*
     */
    @Test
    public void convertKeyStroke() throws Exception {
        final String string = "D";
        assertConversion(javax.swing.KeyStroke.class, string, javax.swing.KeyStroke.getKeyStroke(string));
    }

    /**
     * Real-word usage of get*
     */
    @Test
    public void convertLogger() throws Exception {
        final String string = "xbean";
        assertConversion(java.util.logging.Logger.class, string, java.util.logging.Logger.getLogger(string));
    }

    /**
     * Real-world usage of parse
     */
    @Test
    public void convertLevel() throws Exception {
        final String string = "CONFIG";
        assertConversion(java.util.logging.Level.class, string, java.util.logging.Level.parse(string));
    }

    /**
     * Real-world usage of for*
     */
    @Test
    public void convertLocale() throws Exception {
        final String string = "en-US-x-lvariant-POSIX";
        assertConversion(java.util.Locale.class, string, java.util.Locale.forLanguageTag(string));
    }

    /**
     * Real-world usage of for*
     */
    @Test
    public void convertCharset() throws Exception {
        final String string = "UTF-8";
        assertConversion(java.nio.charset.Charset.class, string, java.nio.charset.Charset.forName(string));
    }

    /**
     * Real-world usage of create
     */
    @Test
    public void convertURI() throws Exception {
        final String string = "some://where?lives=code";
        assertConversion(java.net.URI.class, string, java.net.URI.create(string));
    }

    /**
     * Real-word usage of get*
     */
    @Test
    public void convertInetAddress() throws Exception {
        final String string = "localhost";
        assertConversion(java.net.InetAddress.class, string, java.net.InetAddress.getByName(string));
    }

    /**
     * Multiple factory methods
     */
    @Test
    public void convertShort() throws Exception {
        final String string = "" + Short.MAX_VALUE;
        assertConversion(java.lang.Short.class, string, java.lang.Short.valueOf(string));
    }

    /**
     * Multiple factory methods
     */
    @Test
    public void convertLong() throws Exception {
        final String string = "" + Long.MAX_VALUE;
        assertConversion(java.lang.Long.class, string, java.lang.Long.valueOf(string));
    }

    /**
     * Multiple factory methods
     */
    @Test
    public void convertInteger() throws Exception {
        final String string = "" + Integer.MAX_VALUE;
        assertConversion(java.lang.Integer.class, string, java.lang.Integer.valueOf(string));
    }

    /**
     * Multiple factory methods
     */
    @Test
    public void convertFloat() throws Exception {
        final String string = "" + Float.MAX_VALUE;
        assertConversion(java.lang.Float.class, string, java.lang.Float.valueOf(string));
    }

    /**
     * Multiple factory methods
     */
    @Test
    public void convertDouble() throws Exception {
        final String string = "" + Double.MAX_VALUE;
        assertConversion(java.lang.Double.class, string, java.lang.Double.valueOf(string));
    }

    /**
     * Real-world usage of for*
     */
    @Test
    public void convertClass() throws Exception {
        final String string = this.getClass().getName();
        assertConversion(java.lang.Class.class, string, java.lang.Class.forName(string));
    }

    /**
     * Multiple factory methods
     */
    @Test
    public void convertByte() throws Exception {
        final String string = "" + Byte.MAX_VALUE;
        assertConversion(java.lang.Byte.class, string, java.lang.Byte.valueOf(string));
    }

    /**
     * Multiple factory methods
     */
    @Test
    public void convertBoolean() throws Exception {
        final String string = "false";
        assertConversion(java.lang.Boolean.class, string, java.lang.Boolean.valueOf(string));
    }

    /**
     * Real-world usage of decode
     */
    @Test
    public void convertFont() throws Exception {
        final String string = "Helvetica";
        assertConversion(java.awt.Font.class, string, java.awt.Font.decode(string));
    }

    /**
     * Real-world usage of decode
     */
    @Test
    public void convertColor() throws Exception {
        final String string = "#C0C0C0";
        assertConversion(java.awt.Color.class, string, java.awt.Color.decode(string));
    }

    /**
     * Non-enum valueOf
     */
    @Test
    public void convertQName() throws Exception {
        final String string = "http://www.w3.org/XML/1998/namespace";
        assertConversion(javax.xml.namespace.QName.class, string, javax.xml.namespace.QName.valueOf(string));
    }

    private void assertConversion(final Class<?> type, final String text, final Object expected) {
        final Object actual = convert(type, text, expected);
        assertEquals(expected, actual);
    }

    private static Object convert(final Class<?> type, final String text, final Object expected) {
        final StaticFactoryConverter editor = StaticFactoryConverter.editor(type);
        assertNotNull(editor);

        final Object actual = editor.toObjectImpl(text);
        assertNotNull(actual);
        assertEquals(expected.getClass(), actual.getClass());
        return actual;
    }

    @Test
    public void getCandidates() throws Exception {
        final List<Method> candidates = StaticFactoryConverter.getCandidates(Red.class);

        assertTrue(candidates.contains(red("valueOf")));
        assertTrue(candidates.contains(red("parse")));
        assertTrue(candidates.contains(red("from")));
        assertTrue(candidates.contains(red("parseRed")));
        assertEquals(4, candidates.size());
    }

    @Test
    public void testSort() throws Exception {
        final List<Method> candidates = StaticFactoryConverter.getCandidates(Green.class);
        StaticFactoryConverter.sort(candidates);

        final List<Method> expected = from(Green.class,
                "valueOf",
                "valueOfObject",
                "valueOfString",
                "newGreen",
                "newInstance",
                "newObject",
                "decode",
                "decodeGreen",
                "decodeObject",
                "decodeString",
                "forGreen",
                "forObject",
                "forString",
                "of",
                "ofGreen",
                "ofObject",
                "ofString",
                "parse",
                "parseGreen",
                "parseObject",
                "parseString",
                "from",
                "fromString",
                "fromGreen",
                "fromObject",
                "create",
                "createGreen",
                "createObject",
                "createString",
                "compile",
                "compileGreen",
                "compileObject",
                "compileString",
                "a",
                "b",
                "c",
                "d",
                "e",
                "f",
                "g",
                "h",
                "i",
                "j",
                "k",
                "l",
                "m",
                "n",
                "o",
                "p",
                "q",
                "r",
                "s",
                "t",
                "w",
                "x",
                "y",
                "z",
                "get",
                "getGreen",
                "getInstance",
                "getObject",
                "getString"
        );

        assertEquals(names(expected), names(candidates));
    }

    private String names(final List<Method> list) {
        final StringBuilder results = new StringBuilder();
        for (final Method method : list) {
            results.append(method.getName());
            results.append("\n");
        }
        return results.toString();
    }

    static Method red(final String methodName) throws NoSuchMethodException {
        return Red.class.getMethod(methodName, String.class);
    }

    static Method green(final String methodName) throws NoSuchMethodException {
        return Green.class.getMethod(methodName, String.class);
    }


    public static class Red {
        /**
         * if (!Modifier.isStatic(method.getModifiers())) continue;
         * if (!Modifier.isPublic(method.getModifiers())) continue;
         * if (!method.getReturnType().equals(type)) continue;
         * if (method.getParameterTypes().length != 1) continue;
         * if (!method.getParameterTypes()[0].equals(String.class)) continue;  FAIL
         */
        public static Red notString(URI notString) {
            return null;
        }

        /**
         * if (!Modifier.isStatic(method.getModifiers())) continue;  FAIL
         * if (!Modifier.isPublic(method.getModifiers())) continue;
         * if (!method.getReturnType().equals(type)) continue;
         * if (method.getParameterTypes().length != 1) continue;
         * if (!method.getParameterTypes()[0].equals(String.class)) continue;
         */
        public Red nonStatic(String string) {
            return null;
        }

        /**
         * if (!Modifier.isStatic(method.getModifiers())) continue;
         * if (!Modifier.isPublic(method.getModifiers())) continue;  FAIL
         * if (!method.getReturnType().equals(type)) continue;
         * if (method.getParameterTypes().length != 1) continue;
         * if (!method.getParameterTypes()[0].equals(String.class)) continue;
         */
        static Red nonPublic(String string) {
            return null;
        }

        /**
         * if (!Modifier.isStatic(method.getModifiers())) continue;
         * if (!Modifier.isPublic(method.getModifiers())) continue;
         * if (!method.getReturnType().equals(type)) continue;   FAIL
         * if (method.getParameterTypes().length != 1) continue;
         * if (!method.getParameterTypes()[0].equals(String.class)) continue;
         */
        public static Object nonRed(String string) {
            return null;
        }

        /**
         * if (!Modifier.isStatic(method.getModifiers())) continue;
         * if (!Modifier.isPublic(method.getModifiers())) continue;
         * if (!method.getReturnType().equals(type)) continue;
         * if (method.getParameterTypes().length != 1) continue; FAIL
         * if (!method.getParameterTypes()[0].equals(String.class)) continue;
         */
        public static Red tooMany(String string, boolean more) {
            return null;
        }

        /**
         * if (!Modifier.isStatic(method.getModifiers())) continue;
         * if (!Modifier.isPublic(method.getModifiers())) continue;
         * if (!method.getReturnType().equals(type)) continue;   FAIL
         * if (method.getParameterTypes().length != 1) continue;
         * if (!method.getParameterTypes()[0].equals(String.class)) continue;
         */
        public static Crimson closeButNotQuite(String string) {
            return null;
        }

        public static Red parse(String string) {
            return null;
        }

        public static Red parseRed(String string) {
            return null;
        }

        public static Red valueOf(String string) {
            return null;
        }

        public static Red from(String string) {
            return null;
        }

    }

    public static class Crimson {

    }

    public static List<Method> from(final Class clazz, final String... names) throws NoSuchMethodException {
        final List<Method> methods = new ArrayList<Method>();
        for (final String name : names) {
            methods.add(clazz.getMethod(name, String.class));
        }
        return methods;
    }

    /**
     * We want to ensure that the method selected is stable and does
     * not use reflection ordering which is vm specific and even
     * vm instance specific as it can change between startups.
     *
     * To test this we intentionally have methods a-z in the
     * class in non-alphabetical order.  Method names of a known
     * converting connotation sort higher.
     */
    public static class Green {
        public static Green forObject(final String string) {
            return null;
        }

        public static Green fromString(final String string) {
            return null;
        }

        public static Green compileGreen(final String string) {
            return null;
        }

        public static Green l(final String string) {
            return null;
        }

        public static Green r(final String string) {
            return null;
        }

        public static Green ofString(final String string) {
            return null;
        }

        public static Green parseString(final String string) {
            return null;
        }

        public static Green getInstance(final String string) {
            return null;
        }

        public static Green parse(final String string) {
            return null;
        }

        public static Green a(final String string) {
            return null;
        }

        public static Green create(final String string) {
            return null;
        }

        public static Green w(final String string) {
            return null;
        }

        public static Green newGreen(final String string) {
            return null;
        }

        public static Green newInstance(final String string) {
            return null;
        }

        public static Green ofGreen(final String string) {
            return null;
        }

        public static Green decodeObject(final String string) {
            return null;
        }

        public static Green z(final String string) {
            return null;
        }

        public static Green s(final String string) {
            return null;
        }

        public static Green c(final String string) {
            return null;
        }

        public static Green forString(final String string) {
            return null;
        }

        public static Green q(final String string) {
            return null;
        }

        public static Green i(final String string) {
            return null;
        }

        public static Green valueOf(final String string) {
            return null;
        }

        public static Green valueOfString(final String string) {
            return null;
        }

        public static Green ofObject(final String string) {
            return null;
        }

        public static Green j(final String string) {
            return null;
        }

        public static Green t(final String string) {
            return null;
        }

        public static Green compile(final String string) {
            return null;
        }

        public static Green valueOfObject(final String string) {
            return null;
        }

        public static Green h(final String string) {
            return null;
        }

        public static Green decodeString(final String string) {
            return null;
        }

        public static Green createString(final String string) {
            return null;
        }

        public static Green e(final String string) {
            return null;
        }

        public static Green d(final String string) {
            return null;
        }

        public static Green createGreen(final String string) {
            return null;
        }

        public static Green fromObject(final String string) {
            return null;
        }

        public static Green parseGreen(final String string) {
            return null;
        }

        public static Green y(final String string) {
            return null;
        }

        public static Green get(final String string) {
            return null;
        }

        public static Green from(final String string) {
            return null;
        }

        public static Green m(final String string) {
            return null;
        }

        public static Green createObject(final String string) {
            return null;
        }

        public static Green compileObject(final String string) {
            return null;
        }

        public static Green b(final String string) {
            return null;
        }

        public static Green newObject(final String string) {
            return null;
        }

        public static Green getGreen(final String string) {
            return null;
        }

        public static Green of(final String string) {
            return null;
        }

        public static Green o(final String string) {
            return null;
        }

        public static Green decode(final String string) {
            return null;
        }

        public static Green getObject(final String string) {
            return null;
        }

        public static Green forGreen(final String string) {
            return null;
        }

        public static Green getString(final String string) {
            return null;
        }

        public static Green x(final String string) {
            return null;
        }

        public static Green compileString(final String string) {
            return null;
        }

        public static Green parseObject(final String string) {
            return null;
        }

        public static Green fromGreen(final String string) {
            return null;
        }

        public static Green p(final String string) {
            return null;
        }

        public static Green f(final String string) {
            return null;
        }

        public static Green g(final String string) {
            return null;
        }

        public static Green n(final String string) {
            return null;
        }

        public static Green decodeGreen(final String string) {
            return null;
        }

        public static Green k(final String string) {
            return null;
        }
    }
}