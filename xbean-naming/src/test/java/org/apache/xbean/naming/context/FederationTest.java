/**
 *
 * Copyright 2006 The Apache Software Foundation
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
package org.apache.xbean.naming.context;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.Name;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @version $Rev: 355877 $ $Date: 2005-12-10 18:48:27 -0800 (Sat, 10 Dec 2005) $
 */
public class FederationTest extends AbstractContextTest {
    private Context rootContext;
    private Map env2Bindings;
    private MutableContext actualEnv2Context;
    private Map rootBindings;

    private final class MutableContext extends WritableContext {
        public MutableContext(Map bindings) throws NamingException {
            super("", bindings, ContextAccess.UNMODIFIABLE);
        }

        public void addDeepBinding(Name name, Object value, boolean rebind, boolean createIntermediateContexts) throws NamingException {
            super.addDeepBinding(name, value, rebind, createIntermediateContexts);
        }

        protected void removeDeepBinding(Name name, boolean pruneEmptyContexts) throws NamingException {
            super.removeDeepBinding(name, pruneEmptyContexts);
        }
    }

    public void setUp() throws Exception {
        super.setUp();

        rootBindings = new HashMap();
        rootBindings.put("string", "blah");
        rootBindings.put("nested/context/string", "blah");
        rootBindings.put("java:comp/env/string", "blah");
        rootBindings.put("java:comp/env/one", new Integer(1));
        rootBindings.put("java:comp/env/two", new Integer(2));
        rootBindings.put("java:comp/env/three", new Integer(3));

        rootContext = new WritableContext();
        FederationTest.bind(rootContext, rootBindings);

        assertEq(rootBindings, rootContext);

        env2Bindings = new HashMap();
        env2Bindings.put("string", "blah");
        env2Bindings.put("one", new Integer(1));
        env2Bindings.put("two", new Integer(2));
        env2Bindings.put("three", new Integer(3));

        actualEnv2Context = new MutableContext(env2Bindings);
        assertEq(env2Bindings, actualEnv2Context);

        rootContext.bind("java:comp/env2", actualEnv2Context);
        putAllBindings(rootBindings, "java:comp/env2", env2Bindings);
    }

    public void testBasic() throws Exception {
        assertEq(rootBindings, rootContext);
    }

    public void testMutability() throws Exception {
        assertModifiable(rootContext);
        assertUnmodifiable(actualEnv2Context);
        assertModifiable(lookupSubcontext(rootContext, "java:comp/env2"));
    }

    public void testBindOverFederated() throws Exception {
        // update the verification map
        rootBindings.put("java:comp/env2/TEST", "TEST_VALUE");

        // bind into root context OVER the env2 context
        rootContext.bind("java:comp/env2/TEST", "TEST_VALUE");

        // visible from root context
        assertEq(rootBindings, rootContext);

        // not-visible from actualEnv2Context
        assertEq(env2Bindings, actualEnv2Context);
    }

    public void testBindDirectIntoFederated() throws Exception {
        // update the verification maps
        rootBindings.put("java:comp/env2/DIRECT", "DIRECT_VALUE");
        env2Bindings.put("DIRECT", "DIRECT_VALUE");

        // bind directly into the actual env2 context
        actualEnv2Context.addDeepBinding(parse("DIRECT"), "DIRECT_VALUE", false, true);

        // visible from root context
        assertEq(rootBindings, rootContext);

        // visible from actualEnv2Context
        assertEq(env2Bindings, actualEnv2Context);
    }

    public void testUnbindOverFederated() throws Exception {
        // unbind value under env2... no exception occurs since unbind is idempotent
        rootContext.unbind("java:comp/env2/three");

        // no change in root context
        assertEq(rootBindings, rootContext);

        // no change in actualEnv2Context
        assertEq(env2Bindings, actualEnv2Context);

        // unbind value deep env2... no exception occurs since unbind is idempotent
        rootContext.unbind("java:comp/env2/three");
    }

    public void testUnbindDirectIntoFederated() throws Exception {
        // update the verification maps
        rootBindings.remove("java:comp/env2/three");
        env2Bindings.remove("three");

        // bind directly into the actual env2 context
        actualEnv2Context.removeDeepBinding(parse("three"), true);

        // visible from root context
        assertEq(rootBindings, rootContext);

        // visible from actualEnv2Context
        assertEq(env2Bindings, actualEnv2Context);
    }


    public static void putAllBindings(Map rootBindings, String nestedPath, Map nestedBindings) {
        for (Iterator iterator = nestedBindings.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();
            String fullName = nestedPath + "/" + name;
            rootBindings.put(fullName, value);
        }
    }

    public static void bind(Context context, Map bindings) throws NamingException {
        for (Iterator iterator = bindings.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();
            Name parsedName = context.getNameParser("").parse(name);
            for (int i =1; i < parsedName.size(); i++) {
                Name contextName = parsedName.getPrefix(i);
                if (!FederationTest.bindingExists(context, contextName)) {
                    context.createSubcontext(contextName);
                }
            }
            context.bind(name, value);
        }
    }

    public static boolean bindingExists(Context context, Name contextName) {
        try {
            return context.lookup(contextName) != null;
        } catch (NamingException e) {
        }
        return false;
    }
}
