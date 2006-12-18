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
package org.apache.xbean.naming.context;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.Name;
import javax.naming.NameParser;
import java.util.HashMap;
import java.util.Map;

/**
 * @version $Rev: 355877 $ $Date: 2005-12-10 18:48:27 -0800 (Sat, 10 Dec 2005) $
 */
public class UnmodifiableContextTest extends AbstractContextTest {
    private static final String STRING_VAL = "some string";

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

    public void testBasic() throws Exception {
        Map map = new HashMap();
        map.put("string", STRING_VAL);
        map.put("nested/context/string", STRING_VAL);
        map.put("a/b/c/d/e/string", STRING_VAL);
        map.put("a/b/c/d/e/one", new Integer(1));
        map.put("a/b/c/d/e/two", new Integer(2));
        map.put("a/b/c/d/e/three", new Integer(3));

        Context context = new WritableContext("", map, ContextAccess.UNMODIFIABLE);

        assertEq(map, context);
        assertUnmodifiable(context);
    }

    public void testAddBinding() throws Exception {
        Map map = new HashMap();
        map.put("string", STRING_VAL);
        map.put("nested/context/string", STRING_VAL);
        map.put("a/b/c/d/e/string", STRING_VAL);
        map.put("a/b/c/d/e/one", new Integer(1));
        map.put("a/b/c/d/e/two", new Integer(2));
        map.put("a/b/c/d/e/three", new Integer(3));

        MutableContext context = new MutableContext(map);

        assertEq(map, context);
        assertUnmodifiable(context);

        // add a new deep tree
        map.put("uno/dos/tres", new Integer(123));
        NameParser parser = context.getNameParser();
        context.addDeepBinding(parser.parse("uno/dos/tres"), new Integer(123), false, true);

        assertEq(map, context);
        assertUnmodifiable(context);

        // modify an existing context
        map.put("a/b/c/d/e/four", new Integer(4));
        context.addDeepBinding(parser.parse("a/b/c/d/e/four"), new Integer(4), false, true);

        assertEq(map, context);
        assertUnmodifiable(context);
    }


    public void testRemoveBinding() throws Exception {
        Map map = new HashMap();
        map.put("string", STRING_VAL);
        map.put("nested/context/string", STRING_VAL);
        map.put("a/b/c/d/e/string", STRING_VAL);
        map.put("a/b/c/d/e/one", new Integer(1));
        map.put("a/b/c/d/e/two", new Integer(2));
        map.put("a/b/c/d/e/three", new Integer(3));

        MutableContext context = new MutableContext(map);

        assertEq(map, context);
        assertUnmodifiable(context);

        // remove from an exisitng node
        map.remove("a/b/c/d/e/three");
        NameParser parser = context.getNameParser();
        context.removeDeepBinding(parser.parse("a/b/c/d/e/three"), true);

        assertEq(map, context);
        assertUnmodifiable(context);

        // remove a deep single element element... empty nodes should be removed
        map.remove("nested/context/string");
        context.removeDeepBinding(parser.parse("nested/context/string"), true);

        assertEq(map, context);
        assertUnmodifiable(context);
    }
}
