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
package org.apache.xbean.spring.jndi;

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import java.util.Hashtable;

import junit.framework.TestCase;

/**
 * @version $Revision: 657 $
 */
public class DefaultContextTest extends TestCase {
    Context context = new DefaultContext(new Hashtable());

    public void testSimpleName() throws Exception {
        assertContextNamed("name");
    }

    public void testNestedName() throws Exception {
        assertContextNamed("jdbc/name");
    }

    public void testDoubleNestedName() throws Exception {
        assertContextNamed("jdbc/foo/name");
    }

    protected void assertContextNamed(String name) throws NamingException {
        context.bind(name, "James");
        assertJNDILookup(name, "James");

        context.rebind(name, "Rob");
        assertJNDILookup(name, "Rob");

        context.unbind(name);

        try {
            context.lookup(name);
            fail("Should ave thrown NameNotFoundException!");
        }
        catch (NameNotFoundException e) {
            System.out.println("Caught expected exception: " + e);
        }
    }

    protected void assertJNDILookup(String name, String expected) throws NamingException {
        Object value = context.lookup(name);
        assertEquals("Lookup failed for: " + name, expected, value);
    }

}
