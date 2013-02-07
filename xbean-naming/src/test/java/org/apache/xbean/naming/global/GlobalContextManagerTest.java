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
package org.apache.xbean.naming.global;

import org.apache.xbean.naming.context.AbstractContextTest;
import org.apache.xbean.naming.context.ImmutableContext;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NoInitialContextException;
import java.util.Hashtable;
import java.util.Map;
import java.util.HashMap;

/**
 * @version $Rev$ $Date$
 */
public class GlobalContextManagerTest extends AbstractContextTest {
    public void testNoGlobalContextSet() throws Exception {
        GlobalContextManager.setGlobalContext(null); // force reset since in java 7 order is not guaranteed
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, GlobalContextManager.class.getName());

        try {
            InitialContext initialContext = new InitialContext(env);
            initialContext.lookup("");
            fail("expected a NoInitialContextException");
        } catch (NoInitialContextException expected) {
        }
    }

    public void testDefaultInitialContext() throws Exception {
        Map bindings = new HashMap();

        bindings.put("string", "foo");
        bindings.put("nested/context/string", "bar");
        bindings.put("a/b/c/d/e/string", "beer");
        bindings.put("a/b/c/d/e/one", new Integer(1));
        bindings.put("a/b/c/d/e/two", new Integer(2));
        bindings.put("a/b/c/d/e/three", new Integer(3));
        bindings.put("java:comp/env/foo", new Integer(42));
        bindings.put("foo:bar/baz", "caz");
        ImmutableContext immutableContext = new ImmutableContext(bindings);

        assertEq(bindings, immutableContext);

        GlobalContextManager.setGlobalContext(immutableContext);

        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, GlobalContextManager.class.getName());

        InitialContext initialContext = new InitialContext(env);

        assertEq(bindings, initialContext);
    }

    public void testPackageDir() throws Exception {
        Map bindings = new HashMap();

        bindings.put("java:comp/string", "foo");
        bindings.put("java:comp/nested/context/string", "bar");
        bindings.put("java:comp/a/b/c/d/e/string", "beer");
        bindings.put("java:comp/a/b/c/d/e/one", new Integer(1));
        bindings.put("java:comp/a/b/c/d/e/two", new Integer(2));
        bindings.put("java:comp/a/b/c/d/e/three", new Integer(3));
        bindings.put("java:comp/env/foo", new Integer(42));
        bindings.put("java:comp/baz", "caz");
        ImmutableContext immutableContext = new ImmutableContext(bindings);

        assertEq(bindings, immutableContext);

        GlobalContextManager.setGlobalContext(immutableContext);

        Hashtable env = new Hashtable();
        env.put(Context.URL_PKG_PREFIXES, "org.apache.xbean.naming");

        Context ctx = (Context) new InitialContext(env).lookup("java:comp");

        assertEq(bindings, "java:comp", ctx);
    }
}
