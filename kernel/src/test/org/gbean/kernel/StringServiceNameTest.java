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
package org.gbean.kernel;

import junit.framework.TestCase;

/**
 * Tests StringServiceName.
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class StringServiceNameTest extends TestCase {
    /**
     * Tests that the constuctor works when passed a string and fails when passed null.
     */
    public void testConstructor() {
        new StringServiceName("foo");

        try {
            new StringServiceName(null);
            fail("new StringServiceName(null) should have thrown NullPointerException");
        } catch (NullPointerException expected) {
        }
    }

    /**
     * Tests that toString returns equivalent String as the one passed to the construcor.
     */
    public void testToString() {
        assertEquals("foo", new StringServiceName("foo").toString());
    }

    /**
     * Tests that equals works when comparing two names created with equivalent strings, and fails when not.
     */
    public void testEquals() {
        assertEquals(new StringServiceName("foo"), new StringServiceName("foo"));
        assertFalse(new StringServiceName("bar").equals(new StringServiceName("foo")));
    }

    /**
     * Tests that hashCode creates the same value when used on two names created with equivalent strings, and fails when not.
     */
    public void testHashCode() {
        assertEquals(new StringServiceName("foo").hashCode(), new StringServiceName("foo").hashCode());
        assertFalse(new StringServiceName("bar").hashCode() == new StringServiceName("foo").hashCode());
    }
}
