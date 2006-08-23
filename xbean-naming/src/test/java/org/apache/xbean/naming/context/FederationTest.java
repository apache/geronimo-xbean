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
    private static final String STRING_VAL = "some string";

    public void testBasic() throws Exception {
        Map map = new HashMap();
        map.put("string", FederationTest.STRING_VAL);
        map.put("nested/context/string", FederationTest.STRING_VAL);
        map.put("java:comp/env/string", FederationTest.STRING_VAL);
        map.put("java:comp/env/one", new Integer(1));
        map.put("java:comp/env/two", new Integer(2));
        map.put("java:comp/env/three", new Integer(3));

        Context context = new WritableContext();
        FederationTest.bind(context, map);

        assertEq(map, context);

        Map env2Map = new HashMap();
        env2Map.put("string", FederationTest.STRING_VAL);
        env2Map.put("one", new Integer(1));
        env2Map.put("two", new Integer(2));
        env2Map.put("three", new Integer(3));

        ImmutableContext actualEnv2Context = new ImmutableContext(env2Map);
        assertEq(env2Map, actualEnv2Context);

        context.bind("java:comp/env2", actualEnv2Context);

        Context env2Context = (Context) context.lookup("java:comp/env2");
        assertEq(env2Map, env2Context);

        putAllBindings(map, "java:comp/env2", env2Map);
        assertEq(map, context);
    }

    public void testAddBinding() throws Exception {
        Map map = new HashMap();
        map.put("string", FederationTest.STRING_VAL);
        map.put("nested/context/string", FederationTest.STRING_VAL);
        map.put("a/b/c/d/e/string", FederationTest.STRING_VAL);
        map.put("a/b/c/d/e/one", new Integer(1));
        map.put("a/b/c/d/e/two", new Integer(2));
        map.put("a/b/c/d/e/three", new Integer(3));

        Context context = new WritableContext();
        FederationTest.bind(context, map);

        assertEq(map, context);

        // add a new deep tree
        map.put("a/b/c/d/e/forty-two", new Integer(42));
        context.bind("a/b/c/d/e/forty-two", new Integer(42));

        assertEq(map, context);

    }


    public void testRemoveBinding() throws Exception {
        Map map = new HashMap();
        map.put("string", FederationTest.STRING_VAL);
        map.put("nested/context/string", FederationTest.STRING_VAL);
        map.put("a/b/c/d/e/string", FederationTest.STRING_VAL);
        map.put("a/b/c/d/e/one", new Integer(1));
        map.put("a/b/c/d/e/two", new Integer(2));
        map.put("a/b/c/d/e/three", new Integer(3));

        Context context = new WritableContext();
        FederationTest.bind(context, map);

        assertEq(map, context);

        // remove from an exisitng node
        map.remove("a/b/c/d/e/three");
        context.unbind("a/b/c/d/e/three");

        assertEq(map, context);
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
