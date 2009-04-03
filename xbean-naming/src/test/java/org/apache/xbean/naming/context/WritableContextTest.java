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
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NotContextException;
import javax.naming.NameAlreadyBoundException;
import javax.naming.InvalidNameException;
import javax.naming.ContextNotEmptyException;
import javax.naming.NamingException;

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
        assertEq(bindings, context);

        try {
            context.lookup(parse("a/b/c/d/e/NoSuchValue"));
            fail("Expected NameNotFoundException");
        } catch (NameNotFoundException expected) {
        }
        assertEq(bindings, context);

        //
        // lookup a non-existing value of a non-exisitng context
        //
        try {
            context.list("a/b/NoSuchContext/NoSuchContext");
            fail("Expected NotContextException");
        } catch (NotContextException expected) {
        }
        assertEq(bindings, context);

        try {
            context.list(parse("a/b/NoSuchContext/NoSuchContext"));
            fail("Expected NotContextException");
        } catch (NotContextException expected) {
        }
        assertEq(bindings, context);

        //
        // lookup null
        //
        try {
            context.lookup((String)null);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
        }
        assertEq(bindings, context);

        try {
            context.lookup((Name)null);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
        }
        assertEq(bindings, context);
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
        assertEq(bindings, context);

        try {
            context.lookupLink(parse("a/b/c/d/e/NoSuchValue"));
            fail("Expected NameNotFoundException");
        } catch (NameNotFoundException expected) {
        }
        assertEq(bindings, context);

        //
        // lookupLink a non-existing value of a non-exisitng context
        //
        try {
            context.list("a/b/NoSuchContext/NoSuchContext");
            fail("Expected NotContextException");
        } catch (NotContextException expected) {
        }
        assertEq(bindings, context);

        try {
            context.list(parse("a/b/NoSuchContext/NoSuchContext"));
            fail("Expected NotContextException");
        } catch (NotContextException expected) {
        }
        assertEq(bindings, context);

        //
        // lookupLink null
        //
        try {
            context.lookupLink((String)null);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
        }
        assertEq(bindings, context);

        try {
            context.lookupLink((Name)null);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
        }
        assertEq(bindings, context);
    }

    public void testListExceptions() throws Exception{
        //
        // list a non-existing subcontext of an exisitng context
        //
        try {
            context.list("a/b/c/d/e/NoSuchContext");
            fail("Expected NotContextException");
        } catch (NotContextException expected) {
        }
        assertEq(bindings, context);

        try {
            context.list(parse("a/b/c/d/e/NoSuchContext"));
            fail("Expected NotContextException");
        } catch (NotContextException expected) {
        }
        assertEq(bindings, context);

        //
        // list a non-existing subcontext of a non-exisitng context
        //
        try {
            context.list("a/b/NoSuchContext/NoSuchContext");
            fail("Expected NotContextException");
        } catch (NotContextException expected) {
        }
        assertEq(bindings, context);

        try {
            context.list(parse("a/b/NoSuchContext/NoSuchContext"));
            fail("Expected NotContextException");
        } catch (NotContextException expected) {
        }
        assertEq(bindings, context);

        //
        // list a binding that is a value instead of a context
        //
        try {
            context.list("a/b/c/d/e/three");
            fail("Expected NotContextException");
        } catch (NotContextException expected) {
        }
        assertEq(bindings, context);

        try {
            context.list(parse("a/b/c/d/e/three"));
            fail("Expected NotContextException");
        } catch (NotContextException expected) {
        }
        assertEq(bindings, context);

        //
        // list null
        //
        try {
            context.list((String)null);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
        }
        assertEq(bindings, context);

        try {
            context.list((Name)null);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
        }
        assertEq(bindings, context);
    }

    public void testListBindingsExceptions() throws Exception{
        //
        // listBindings a non-existing subcontext of an exisitng context
        //
        try {
            context.listBindings("a/b/c/d/e/NoSuchContext");
            fail("Expected NotContextException");
        } catch (NotContextException expected) {
        }
        assertEq(bindings, context);

        try {
            context.listBindings(parse("a/b/c/d/e/NoSuchContext"));
            fail("Expected NotContextException");
        } catch (NotContextException expected) {
        }
        assertEq(bindings, context);

        //
        // listBindings a non-existing subcontext of a non-exisitng context
        //
        try {
            context.listBindings("a/b/NoSuchContext/NoSuchContext");
            fail("Expected NotContextException");
        } catch (NotContextException expected) {
        }
        assertEq(bindings, context);

        try {
            context.listBindings(parse("a/b/NoSuchContext/NoSuchContext"));
            fail("Expected NotContextException");
        } catch (NotContextException expected) {
        }
        assertEq(bindings, context);

        //
        // listBindings a binding that is a value instead of a context
        //
        try {
            context.listBindings("a/b/c/d/e/three");
            fail("Expected NotContextException");
        } catch (NotContextException expected) {
        }
        assertEq(bindings, context);

        try {
            context.listBindings(parse("a/b/c/d/e/three"));
            fail("Expected NotContextException");
        } catch (NotContextException expected) {
        }
        assertEq(bindings, context);

        //
        // listBindings null
        //
        try {
            context.listBindings((String)null);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
        }
        assertEq(bindings, context);

        try {
            context.listBindings((Name)null);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
        }
        assertEq(bindings, context);
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

    public void testBindExceptions() throws Exception{
        //
        // bind over existing value of an exisitng context
        //
        try {
            context.bind("a/b/c/d/e/three", "value");
            fail("Expected NameAlreadyBoundException");
        } catch (NameAlreadyBoundException expected) {
        }
        assertEq(bindings, context);

        try {
            context.bind(parse("a/b/c/d/e/three"), "value");
            fail("Expected NameAlreadyBoundException");
        } catch (NameAlreadyBoundException expected) {
        }
        assertEq(bindings, context);

        //
        // bind over root context
        //
        try {
            context.bind("", "value");
            fail("Expected NameAlreadyBoundException");
        } catch (NameAlreadyBoundException expected) {
        }
        assertEq(bindings, context);

        try {
            context.bind(parse(""), "value");
            fail("Expected NameAlreadyBoundException");
        } catch (NameAlreadyBoundException expected) {
        }
        assertEq(bindings, context);

        //
        // bind to non-existing context
        //
        try {
            context.bind("a/b/NoSuchContext/name", "value");
            fail("Expected NotContextException");
        } catch (NotContextException expected) {
        }
        assertEq(bindings, context);

        try {
            context.bind(parse("a/b/NoSuchContext/name"), "value");
            fail("Expected NotContextException");
        } catch (NotContextException expected) {
        }
        assertEq(bindings, context);

        //
        // bind null
        //
        try {
            context.bind((String)null, "value");
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
        }
        assertEq(bindings, context);

        try {
            context.bind((Name)null, "value");
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
        }
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

    public void testUnbindExceptions() throws Exception{
        //
        // unbind non empty context
        //
        try {
            context.unbind("a/b/c");
            fail("Expected ContextNotEmptyException");
        } catch (ContextNotEmptyException expected) {
        }
        assertEq(bindings, context);

        try {
            context.unbind(parse("a/b/c"));
            fail("Expected ContextNotEmptyException");
        } catch (ContextNotEmptyException expected) {
        }
        assertEq(bindings, context);

        //
        // unbind root context
        //
        try {
            context.unbind("");
            fail("Expected InvalidNameException");
        } catch (InvalidNameException expected) {
        }
        assertEq(bindings, context);

        try {
            context.unbind(parse(""));
            fail("Expected InvalidNameException");
        } catch (InvalidNameException expected) {
        }
        assertEq(bindings, context);

        //
        // unbind non-existing context
        //
        try {
            context.unbind("a/b/NoSuchContext/name");
            fail("Expected NotContextException");
        } catch (NotContextException expected) {
        }
        assertEq(bindings, context);

        try {
            context.unbind(parse("a/b/NoSuchContext/name"));
            fail("Expected NotContextException");
        } catch (NotContextException expected) {
        }
        assertEq(bindings, context);

        //
        // unbind null
        //
        try {
            context.unbind((String)null);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
        }
        assertEq(bindings, context);

        try {
            context.unbind((Name)null);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
        }
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

        // rebind(String) - New Value
        bindings.put("a/b/c/d/e/forty-two", new Integer(42));
        context.rebind("a/b/c/d/e/forty-two", new Integer(42));

        assertEq(bindings, context);

        // rebind(Name) - New Value
        bindings.put("a/b/c/d/e/forty-four", new Integer(44));
        context.rebind(parse("a/b/c/d/e/forty-four"), new Integer(44));

        assertEq(bindings, context);
    }

    public void testRebindExceptions() throws Exception{
        //
        // rebind over root context
        //
        try {
            context.rebind("", "value");
            fail("Expected NameAlreadyBoundException");
        } catch (NameAlreadyBoundException expected) {
        }
        assertEq(bindings, context);

        try {
            context.rebind(parse(""), "value");
            fail("Expected NameAlreadyBoundException");
        } catch (NameAlreadyBoundException expected) {
        }
        assertEq(bindings, context);

        //
        // rebind to non-existing context
        //
        try {
            context.rebind("a/b/NoSuchContext/name", "value");
            fail("Expected NotContextException");
        } catch (NotContextException expected) {
        }
        assertEq(bindings, context);

        try {
            context.rebind(parse("a/b/NoSuchContext/name"), "value");
            fail("Expected NotContextException");
        } catch (NotContextException expected) {
        }
        assertEq(bindings, context);

        //
        // rebind null
        //
        try {
            context.rebind((String)null, "value");
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
        }
        assertEq(bindings, context);

        try {
            context.rebind((Name)null, "value");
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
        }
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

    public void testRenameExceptions() throws Exception{
        //
        // rename over existing value of an exisitng context
        //
        try {
            context.rename("a/b/c/d/e/one", "a/b/c/d/e/three");
            fail("Expected NameAlreadyBoundException");
        } catch (NameAlreadyBoundException expected) {
        }
        assertEq(bindings, context);

        try {
            context.rename(parse("a/b/c/d/e/one"), parse("a/b/c/d/e/three"));
            fail("Expected NameAlreadyBoundException");
        } catch (NameAlreadyBoundException expected) {
        }
        assertEq(bindings, context);

        //
        // rename over root context
        //
        try {
            context.rename("a/b/c/d/e/one", "");
            fail("Expected NameAlreadyBoundException");
        } catch (NameAlreadyBoundException expected) {
        }
        assertEq(bindings, context);

        try {
            context.rename(parse("a/b/c/d/e/one"), parse(""));
            fail("Expected NameAlreadyBoundException");
        } catch (NameAlreadyBoundException expected) {
        }
        assertEq(bindings, context);

        //
        // rename to non-existing context
        //
        try {
            context.rename("a/b/c/d/e/one", "a/b/NoSuchContext/name");
            fail("Expected NotContextException");
        } catch (NotContextException expected) {
        }
        assertEq(bindings, context);

        try {
            context.rename(parse("a/b/c/d/e/one"), parse("a/b/NoSuchContext/name"));
            fail("Expected NotContextException");
        } catch (NotContextException expected) {
        }
        assertEq(bindings, context);

        //
        // rename null
        //
        try {
            context.rename(null, "SOME_NAME");
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
        }
        assertEq(bindings, context);

        try {
            context.rename(null, parse("SOME_NAME"));
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
        }
        assertEq(bindings, context);

        try {
            context.rename("SOME_NAME", null);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
        }
        assertEq(bindings, context);

        try {
            context.rename(null, parse("SOME_NAME"));
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
        }
        assertEq(bindings, context);

        try {
            context.rename((String)null, (String)null);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
        }
        assertEq(bindings, context);

        try {
            context.rename((Name)null, (Name)null);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
        }

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

    public void testCreateSubcontextExceptions() throws Exception{
        //
        // createSubcontext over existing value of an exisitng context
        //
        try {
            context.createSubcontext("a/b/c/d/e/three");
            fail("Expected NameAlreadyBoundException");
        } catch (NameAlreadyBoundException expected) {
        }
        assertEq(bindings, context);

        try {
            context.createSubcontext(parse("a/b/c/d/e/three"));
            fail("Expected NameAlreadyBoundException");
        } catch (NameAlreadyBoundException expected) {
        }
        assertEq(bindings, context);

        //
        // createSubcontext over existing context
        //
        try {
            context.createSubcontext("a/b/c");
            fail("Expected NameAlreadyBoundException");
        } catch (NameAlreadyBoundException expected) {
        }
        assertEq(bindings, context);

        try {
            context.createSubcontext(parse("a/b/c"));
            fail("Expected NameAlreadyBoundException");
        } catch (NameAlreadyBoundException expected) {
        }
        assertEq(bindings, context);

        //
        // createSubcontext over root context
        //
        try {
            context.createSubcontext("");
            fail("Expected NameAlreadyBoundException");
        } catch (NameAlreadyBoundException expected) {
        }
        assertEq(bindings, context);

        try {
            context.createSubcontext(parse(""));
            fail("Expected NameAlreadyBoundException");
        } catch (NameAlreadyBoundException expected) {
        }
        assertEq(bindings, context);

        //
        // createSubcontext in a non-existing context
        //
        try {
            context.createSubcontext("a/b/NoSuchContext/name");
            fail("Expected NotContextException");
        } catch (NotContextException expected) {
        }
        assertEq(bindings, context);

        try {
            context.createSubcontext(parse("a/b/NoSuchContext/name"));
            fail("Expected NotContextException");
        } catch (NotContextException expected) {
        }
        assertEq(bindings, context);

        //
        // createSubcontext null
        //
        try {
            context.createSubcontext((String)null);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
        }
        assertEq(bindings, context);

        try {
            context.createSubcontext((Name)null);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
        }
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

    public void testDestroySubcontextExceptions() throws Exception{
        //
        // destroySubcontext a value (not a context)
        //
        try {
            context.createSubcontext("a/b/c/d/e/three");
            fail("Expected NameAlreadyBoundException");
        } catch (NameAlreadyBoundException expected) {
        }
        assertEq(bindings, context);

        try {
            context.createSubcontext(parse("a/b/c/d/e/three"));
            fail("Expected NameAlreadyBoundException");
        } catch (NameAlreadyBoundException expected) {
        }
        assertEq(bindings, context);

        //
        // destroySubcontext non empty context
        //
        try {
            context.destroySubcontext("a/b/c");
            fail("Expected ContextNotEmptyException");
        } catch (ContextNotEmptyException expected) {
        }
        assertEq(bindings, context);

        try {
            context.destroySubcontext(parse("a/b/c"));
            fail("Expected ContextNotEmptyException");
        } catch (ContextNotEmptyException expected) {
        }
        assertEq(bindings, context);

        //
        // destroySubcontext root context
        //
        try {
            context.destroySubcontext("");
            fail("Expected InvalidNameException");
        } catch (InvalidNameException expected) {
        }
        assertEq(bindings, context);

        try {
            context.destroySubcontext(parse(""));
            fail("Expected InvalidNameException");
        } catch (InvalidNameException expected) {
        }
        assertEq(bindings, context);

        //
        // destroySubcontext non-existing context
        //
        try {
            context.destroySubcontext("a/b/NoSuchContext/name");
            fail("Expected NotContextException");
        } catch (NotContextException expected) {
        }
        assertEq(bindings, context);

        try {
            context.destroySubcontext(parse("a/b/NoSuchContext/name"));
            fail("Expected NotContextException");
        } catch (NotContextException expected) {
        }
        assertEq(bindings, context);

        //
        // destroySubcontext null
        //
        try {
            context.destroySubcontext((String)null);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
        }
        assertEq(bindings, context);

        try {
            context.destroySubcontext((Name)null);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
        }
        assertEq(bindings, context);
    }

    public void testLookupSubContext() throws Exception {
        Context ctx = (Context) context.lookup("a/b/c");
        String s = (String) ctx.lookup("d/e/string");
        assertEquals(s, WritableContextTest.STRING_VAL);
    }

    public void testDeepBinding() throws Exception {
        WritableContext w = new WritableContext("jca:");
        w.addDeepBinding(w.getNameParser("").parse("test/test/GBean/resourceSource"), WritableContextTest.STRING_VAL, false, true);
        assertEquals(WritableContextTest.STRING_VAL, w.lookup("test/test/GBean/resourceSource"));
        w.rebind("test/test/GBean/resourceSource", 1);
        assertEquals(new Integer(1), w.lookup("test/test/GBean/resourceSource"));
    }
    
    public void testRemoveDeepBinding_Leaf() throws Exception {
        WritableContext w = new WritableContext("jca:");
        // Test when only one object
        w.addDeepBinding(w.getNameParser("").parse("test/test/GBean/resourceSource"), WritableContextTest.STRING_VAL, true, true);
        assertEquals(WritableContextTest.STRING_VAL, w.lookup("test/test/GBean/resourceSource"));
        w.removeDeepBinding(w.getNameParser("").parse("test/test/GBean/resourceSource"), true, true);
        try {
        	w.lookup("test");
            fail("Expected NameNotFoundException");
        } catch (NameNotFoundException expected) {
        }
        w.addDeepBinding(w.getNameParser("").parse("test/test/GBean/resourceSource"), WritableContextTest.STRING_VAL, true, true);
        assertEquals(WritableContextTest.STRING_VAL, w.lookup("test/test/GBean/resourceSource"));
        w.addDeepBinding(w.getNameParser("").parse("test/test/GBean/rresourceSource2"), new Integer(2), true, true);
        assertEquals(new Integer(2), w.lookup("test/test/GBean/rresourceSource2"));

        w.removeDeepBinding(w.getNameParser("").parse("test/test/GBean/resourceSource"), true, true);
        assertEquals(new Integer(2), w.lookup("test/test/GBean/rresourceSource2"));
        try {
            w.lookup("test/test/GBean/resourceSource");
            fail("Expected NameNotFoundException");
        } catch (NameNotFoundException expected) {
        }
        w.removeDeepBinding(w.getNameParser("").parse("test/test/GBean/rresourceSource2"), true, true);
        try {
        	w.lookup("test");
            fail("Expected NameNotFoundException");
        } catch (NameNotFoundException expected) {
        }
    }
    
    public void testRemoveDeepBinding_Intermediate() throws Exception {
        WritableContext w = new WritableContext("jca:");
        // Test when only one object
        w.addDeepBinding(w.getNameParser("").parse("test/test1/GBean/resourceSource"), WritableContextTest.STRING_VAL, true, true);
        assertEquals(WritableContextTest.STRING_VAL, w.lookup("test/test1/GBean/resourceSource"));
        w.removeDeepBinding(w.getNameParser("").parse("test/test1/GBean/resourceSource"), true, true);
        try {
        	w.lookup("test");
            fail("Expected NameNotFoundException");
        } catch (NameNotFoundException expected) {
        }
        w.addDeepBinding(w.getNameParser("").parse("test/test1/GBean/resourceSource"), WritableContextTest.STRING_VAL, true, true);
        assertEquals(WritableContextTest.STRING_VAL, w.lookup("test/test1/GBean/resourceSource"));
        w.addDeepBinding(w.getNameParser("").parse("test/test2/GBean/rresourceSource2"), new Integer(2), true, true);
        assertEquals(new Integer(2), w.lookup("test/test2/GBean/rresourceSource2"));

        w.removeDeepBinding(w.getNameParser("").parse("test/test1/GBean/resourceSource"), true, true);
        assertEquals(new Integer(2), w.lookup("test/test2/GBean/rresourceSource2"));
        try {
            w.lookup("test/test1");
            fail("Expected NameNotFoundException");
        } catch (NameNotFoundException expected) {
        }
        w.removeDeepBinding(w.getNameParser("").parse("test/test2/GBean/rresourceSource2"), true, true);
        try {
        	w.lookup("test");
            fail("Expected NameNotFoundException");
        } catch (NameNotFoundException expected) {
        }
    }

    public void test2PathsCreateSubcontext() throws Exception {
        WritableContext w = new WritableContext();
        doBind(w, "a/b/c", "c");
        doBind(w, "a/b/b/c", "c");
        assertEquals("a", ((Context)w.lookup("a")).getNameInNamespace());
        assertEquals("a/b", ((Context)w.lookup("a/b")).getNameInNamespace());
        assertEquals("a/b/b", ((Context)w.lookup("a/b/b")).getNameInNamespace());

        w = new WritableContext("a");
        doBind(w, "a/b/c", "c");
        doBind(w, "a/b/b/c", "c");
        assertEquals("a/a", ((Context)w.lookup("a")).getNameInNamespace());
        assertEquals("a/a/b", ((Context)w.lookup("a/b")).getNameInNamespace());
        assertEquals("a/a/b/b", ((Context)w.lookup("a/a/b/b")).getNameInNamespace());
    }

    private void doBind(Context context, String nameString, Object value) throws NamingException {
        Name name = context.getNameParser("").parse(nameString);
        Context current = context;
        for (int i = 0; i< name.size() - 1; i++) {
            String part = name.get(i);
            try {
                Object o = current.lookup(part);
                if (!(o instanceof Context)) {
                    throw new NamingException("not a context at " + part +" found: " + o);
                }
                current = (Context) o;
            } catch (NamingException e) {
                current = current.createSubcontext(part);
            }
        }
        current.bind(name.get(name.size() - 1), value);
    }

}
