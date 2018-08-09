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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class PropertyEditorsTest extends Assert {

    @Test
    public void testCanConvert() throws Exception {
        assertTrue(PropertyEditors.canConvert(Blue.class));
    }

    /**
     * Although an object can be converted via static factory
     * or constructor, if there is a propertyeditor present,
     * we must use the editor.
     */
    @Test
    public void propertyEditorIsUsed() throws Exception {
        final Blue expected = new Blue("blue");
        calls.clear();

        final Object actual = PropertyEditors.getValue(Blue.class, "blue");

        assertNotNull(actual);
        assertEquals(expected, actual);

        final List<String> expectedCalls = Arrays.asList(
                "BlueEditor.setAsText",
                "Blue.constructor");

        assertEquals(join(expectedCalls), join(calls));
    }

    /**
     * Constructors beat static factory methods
     */
    @Test
    public void constructorIsUsed() throws Exception {
        final Orange expected = new Orange("orange");
        calls.clear();

        final Object actual = PropertyEditors.getValue(Orange.class, "orange");

        assertNotNull(actual);
        assertEquals(expected, actual);

        assertEquals("Orange.constructor", join(calls));
    }

    /**
     * With no editor and no public constructor, we default
     * to the static factory method.
     */
    @Test
    public void staticFactoryIsUsed() throws Exception {
        final Red expected = new Red("red");
        calls.clear();

        final Object actual = PropertyEditors.getValue(Red.class, "red");

        assertNotNull(actual);
        assertEquals(expected, actual);

        final List<String> expectedCalls = Arrays.asList(
                "Red.valueOf",
                "Red.constructor");

        assertEquals(join(expectedCalls), join(calls));
    }

    public static String join(Collection<?> items) {
        final StringBuilder sb = new StringBuilder();
        for (final Object item : items) {
            sb.append(item + "\n");
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString();
    }

    private static final List<String> calls = new ArrayList<String>();

    /**
     * The BluePropertyEditor should be found and preferred over
     * the constructor or static factory method
     */
    public static class Blue {

        private final String string;

        public Blue(final String string) {
            this.string = string;
            calls.add("Blue.constructor");
        }

        public static Blue valueOf(final String string) {
            calls.add("Blue.valueOf");
            return new Blue(string);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final Blue blue = (Blue) o;

            if (!string.equals(blue.string)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return string.hashCode();
        }
    }

    public static class BlueEditor extends java.beans.PropertyEditorSupport {

        public void setAsText(String text) throws IllegalArgumentException {
            calls.add("BlueEditor.setAsText");
            setValue(new Blue(text));
        }
    }

    /**
     * The constructor is public so should be preferred over the
     * static factory method
     */
    public static class Orange {

        private final String string;

        public Orange(final String string) {
            this.string = string;
            calls.add("Orange.constructor");
        }

        public static Orange valueOf(final String string) {
            calls.add("Orange.valueOf");
            return new Orange(string);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final Orange Orange = (Orange) o;

            if (!string.equals(Orange.string)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return string.hashCode();
        }
    }

    /**
     * The constructor is private so should not be used
     */
    public static class Red {

        private final String string;

        private Red(final String string) {
            this.string = string;
            calls.add("Red.constructor");
        }

        public static Red valueOf(final String string) {
            calls.add("Red.valueOf");
            return new Red(string);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final Red Red = (Red) o;

            if (!string.equals(Red.string)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return string.hashCode();
        }
    }
}