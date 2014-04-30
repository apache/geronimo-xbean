/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.xbean.naming.context;

import junit.framework.TestCase;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * @version $Rev$ $Date$
 */
public class ImmutableFederatedContextTest  extends TestCase {

    public void testJavaContextFederation() throws Exception {
        Context comp = new ImmutableContext(Collections.<String, Object>singletonMap("comp/env/foo", "foo"));
        Context module = new ImmutableContext(Collections.<String, Object>singletonMap("module/env/bar", "bar"));
        Context application = new ImmutableContext(Collections.<String, Object>singletonMap("application/env/baz", "baz"));
        Context global1 = new ImmutableContext(Collections.<String, Object>singletonMap("global/env/foo1", "foo1"));
        Context global2 = new ImmutableContext(Collections.<String, Object>singletonMap("global/env/foo2", "foo2"));

        Set<Context> globals = new LinkedHashSet<Context>();
        ImmutableFederatedContext global = new ImmutableFederatedContext("", globals);

        Set<Context> locals = new LinkedHashSet<Context>();
        locals.add(comp);
        locals.add(module);
        locals.add(application);
        locals.add(global);
        ImmutableFederatedContext w  = new ImmutableFederatedContext("", locals);

        assertEquals("foo", w.lookup("comp/env/foo"));
        assertEquals("bar", w.lookup("module/env/bar"));
        assertEquals("baz", w.lookup("application/env/baz"));
        try {
            w.lookup("global/env/foo1");
            fail("foo1 not yet bound");
        } catch (NamingException e) {

        }
        global.federateContext(global1);
        assertEquals("foo1", w.lookup("global/env/foo1"));
        try {
            w.lookup("global/env/foo2");
            fail("foo2 not yet bound");
        } catch (NamingException e) {

        }

        global.federateContext(global2);
        assertEquals("foo2", w.lookup("global/env/foo2"));

        Context c = (Context) w.lookup("global");
        assertEquals("foo1", c.lookup("env/foo1"));
        assertEquals("foo2", c.lookup("env/foo2"));

        global.unfederateContext(global2);
        try {
            w.lookup("global/env/foo2");
            fail("foo2 not yet bound");
        } catch (NamingException e) {

        }
    }
}
