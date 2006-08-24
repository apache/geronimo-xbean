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
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NotContextException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @version $Rev: 355877 $ $Date: 2005-12-10 18:48:27 -0800 (Sat, 10 Dec 2005) $
 */
public class WritableContextTest extends AbstractContextTest {
    private static final String STRING_VAL = "some string";
    private Map bindings;
    private Context context;

    public void setUp() throws Exception {
        super.setUp();

        // initialize the bindings map
        bindings = new HashMap();
        bindings.put("string", WritableContextTest.STRING_VAL);
        bindings.put("nested/context/string", WritableContextTest.STRING_VAL);
        bindings.put("a/b/c/d/e/string", WritableContextTest.STRING_VAL);
        bindings.put("a/b/c/d/e/one", new Integer(1));
        bindings.put("a/b/c/d/e/two", new Integer(2));
        bindings.put("a/b/c/d/e/three", new Integer(3));

        // create the contxt
        context = new WritableContext();

        // bind the bindings
        for (Iterator iterator = bindings.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();
            Name parsedName = context.getNameParser("").parse(name);
            for (int i =1; i < parsedName.size(); i++) {
                Name contextName = parsedName.getPrefix(i);
                if (!bindingExists(context, contextName)) {
                    context.createSubcontext(contextName);
                }
            }
            context.bind(name, value);
        }
    }


    public void testLookupAndList() throws Exception {
        assertEq(bindings, context);
    }

    public void testLookupExceptions() throws Exception{
        //
        // lookup a non-existing value of an exisitng context
        //
        try {
            context.lookup("a/b/c/d/e/NoSuchValue");
            fail("Expected NameNotFoundException");
        } catch (NameNotFoundException expected) {
        }

        try {
            context.lookup(parse("a/b/c/d/e/NoSuchValue"));
            fail("Expected NameNotFoundException");
        } catch (NameNotFoundException expected) {
        }

        //
        // lookup a non-existing value of a non-exisitng context
        //
        try {
            context.list("a/b/NoSuchContext/NoSuchContext");
            fail("Expected NameNotFoundException");
        } catch (NotContextException expected) {
        }

        try {
            context.list(parse("a/b/NoSuchContext/NoSuchContext"));
            fail("Expected NameNotFoundException");
        } catch (NotContextException expected) {
        }

        //
        // lookup null
        //
        try {
            context.lookup((String)null);
            fail("Expected NotContextException");
        } catch (NullPointerException expected) {
        }

        try {
            context.lookup((Name)null);
        } catch (NullPointerException expected) {
        }

    }

    public void testLookupLinkExceptions() throws Exception{
        //
        // lookupLink a non-existing value of an exisitng context
        //
        try {
            context.lookupLink("a/b/c/d/e/NoSuchValue");
            fail("Expected NameNotFoundException");
        } catch (NameNotFoundException expected) {
        }

        try {
            context.lookupLink(parse("a/b/c/d/e/NoSuchValue"));
            fail("Expected NameNotFoundException");
        } catch (NameNotFoundException expected) {
        }

        //
        // lookupLink a non-existing value of a non-exisitng context
        //
        try {
            context.list("a/b/NoSuchContext/NoSuchContext");
            fail("Expected NameNotFoundException");
        } catch (NotContextException expected) {
        }

        try {
            context.list(parse("a/b/NoSuchContext/NoSuchContext"));
            fail("Expected NameNotFoundException");
        } catch (NotContextException expected) {
        }

        //
        // lookupLink null
        //
        try {
            context.lookupLink((String)null);
            fail("Expected NotContextException");
        } catch (NullPointerException expected) {
        }

        try {
            context.lookupLink((Name)null);
        } catch (NullPointerException expected) {
        }

    }

    public void testListExceptions() throws Exception{
        //
        // list a non-existing subcontext of an exisitng context
        //
        try {
            context.list("a/b/c/d/e/NoSuchContext");
            fail("Expected NameNotFoundException");
        } catch (NotContextException expected) {
        }

        try {
            context.list(parse("a/b/c/d/e/NoSuchContext"));
            fail("Expected NameNotFoundException");
        } catch (NotContextException expected) {
        }

        //
        // list a non-existing subcontext of a non-exisitng context
        //
        try {
            context.list("a/b/NoSuchContext/NoSuchContext");
            fail("Expected NameNotFoundException");
        } catch (NotContextException expected) {
        }

        try {
            context.list(parse("a/b/NoSuchContext/NoSuchContext"));
            fail("Expected NameNotFoundException");
        } catch (NotContextException expected) {
        }

        //
        // list a binding that is a value instead of a context
        //
        try {
            context.list("a/b/c/d/e/three");
            fail("Expected NameNotFoundException");
        } catch (NotContextException expected) {
        }

        try {
            context.list(parse("a/b/c/d/e/three"));
            fail("Expected NameNotFoundException");
        } catch (NotContextException expected) {
        }

        //
        // list null
        //
        try {
            context.list((String)null);
            fail("Expected NotContextException");
        } catch (NullPointerException expected) {
        }

        try {
            context.list((Name)null);
        } catch (NullPointerException expected) {
        }

    }

    public void testListBindingsExceptions() throws Exception{
        //
        // listBindings a non-existing subcontext of an exisitng context
        //
        try {
            context.listBindings("a/b/c/d/e/NoSuchContext");
            fail("Expected NameNotFoundException");
        } catch (NotContextException expected) {
        }

        try {
            context.listBindings(parse("a/b/c/d/e/NoSuchContext"));
            fail("Expected NameNotFoundException");
        } catch (NotContextException expected) {
        }

        //
        // listBindings a non-existing subcontext of a non-exisitng context
        //
        try {
            context.listBindings("a/b/NoSuchContext/NoSuchContext");
            fail("Expected NameNotFoundException");
        } catch (NotContextException expected) {
        }

        try {
            context.listBindings(parse("a/b/NoSuchContext/NoSuchContext"));
            fail("Expected NameNotFoundException");
        } catch (NotContextException expected) {
        }

        //
        // listBindings a binding that is a value instead of a context
        //
        try {
            context.listBindings("a/b/c/d/e/three");
            fail("Expected NameNotFoundException");
        } catch (NotContextException expected) {
        }

        try {
            context.listBindings(parse("a/b/c/d/e/three"));
            fail("Expected NameNotFoundException");
        } catch (NotContextException expected) {
        }

        //
        // listBindings null
        //
        try {
            context.listBindings((String)null);
            fail("Expected NotContextException");
        } catch (NullPointerException expected) {
        }

        try {
            context.listBindings((Name)null);
        } catch (NullPointerException expected) {
        }

    }

    public void testBind() throws Exception {
        // bind(String)
        bindings.put("a/b/c/d/e/forty-two", new Integer(42));
        context.bind("a/b/c/d/e/forty-two", new Integer(42));

        assertEq(bindings, context);

        // bind(Name)
        bindings.put("a/b/c/d/e/forty-four", new Integer(44));
        context.bind(parse("a/b/c/d/e/forty-four"), new Integer(44));

        assertEq(bindings, context);
    }

    public void testUnbind() throws Exception {
        // unbind(String)
        bindings.remove("a/b/c/d/e/three");
        context.unbind("a/b/c/d/e/three");

        assertEq(bindings, context);

        // unbind(Name)
        bindings.remove("a/b/c/d/e/two");
        context.unbind(parse("a/b/c/d/e/two"));

        assertEq(bindings, context);
    }

    public void testRebind() throws Exception {
        // rebind(String)
        bindings.put("a/b/c/d/e/three", new Integer(33));
        context.rebind("a/b/c/d/e/three", new Integer(33));

        assertEq(bindings, context);

        // rebind(Name)
        bindings.put("a/b/c/d/e/three", new Integer(33333));
        context.rebind(parse("a/b/c/d/e/three"), new Integer(33333));

        assertEq(bindings, context);
    }

    public void testRename() throws Exception {
        // rename(String, String)
        Object value = bindings.remove("a/b/c/d/e/three");
        bindings.put("a/b/c/d/e/boo", value);
        context.rename("a/b/c/d/e/three", "a/b/c/d/e/boo");

        assertEq(bindings, context);

        // rename(Name, Name)
        value = bindings.remove("a/b/c/d/e/boo");
        bindings.put("a/b/c/d/e/moo", value);
        context.rename(parse("a/b/c/d/e/boo"), parse("a/b/c/d/e/moo"));

        assertEq(bindings, context);
    }

    public void testCreateSubcontext() throws Exception {
        // createSubcontext(String)
        bindings.put("a/b/c/d/e/f/foo", "bar");
        context.createSubcontext("a/b/c/d/e/f");
        context.bind("a/b/c/d/e/f/foo", "bar");

        assertEq(bindings, context);

        // createSubcontext(Name)
        bindings.put("a/b/c/d/e/f2/foo", "bar");
        context.createSubcontext(parse("a/b/c/d/e/f2"));
        context.bind(parse("a/b/c/d/e/f2/foo"), "bar");

        assertEq(bindings, context);
    }

    public void testDestroySubcontext() throws Exception {
        // destroySubcontext(String)
        bindings.put("a/b/c/d/e/f/foo", "bar");
        context.createSubcontext("a/b/c/d/e/f");
        context.bind("a/b/c/d/e/f/foo", "bar");
        assertEq(bindings, context);

        bindings.remove("a/b/c/d/e/f/foo");
        context.unbind("a/b/c/d/e/f/foo");
        context.destroySubcontext("a/b/c/d/e/f");

        assertEq(bindings, context);

        // destroySubcontext(Name)
        bindings.put("a/b/c/d/e/f2/foo", "bar");
        context.createSubcontext(parse("a/b/c/d/e/f2"));
        context.bind(parse("a/b/c/d/e/f2/foo"), "bar");
        assertEq(bindings, context);

        bindings.remove("a/b/c/d/e/f2/foo");
        context.unbind(parse("a/b/c/d/e/f2/foo"));
        context.destroySubcontext(parse("a/b/c/d/e/f2"));

        assertEq(bindings, context);
    }
}
