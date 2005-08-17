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
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class StringServiceNameTest extends TestCase {
    public void testConstructor() {
        new StringServiceName("foo");

        try {
            new StringServiceName(null);
            fail("new StringServiceName(null) should have thrown NullPointerException");
        } catch (NullPointerException expected) {
        }
    }

    public void testToString() {
        assertEquals("foo", new StringServiceName("foo").toString());
    }

    public void testEquals() {
        assertEquals(new StringServiceName("foo"), new StringServiceName("foo"));
        assertFalse(new StringServiceName("bar").equals(new StringServiceName("foo")));
    }

    public void testHashCode() {
        assertEquals(new StringServiceName("foo").hashCode(), new StringServiceName("foo").hashCode());
        assertFalse(new StringServiceName("bar").hashCode() == new StringServiceName("foo").hashCode());
    }
}
