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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @version $Rev: 355877 $ $Date: 2005-12-10 18:48:27 -0800 (Sat, 10 Dec 2005) $
 */
public class ContextAccessControlListTest extends AbstractContextTest {
    private ContextAccessControlList allowDenyACL;
    private ContextAccessControlList denyAllowACL;
    private Context allowDenyContext;
    private WritableContext denyAllowContext;

    public void testAllowDeny() throws Exception {
        // outside of listed
        assertTrue(allowDenyACL.isModifiable(parse("foo")));

        // explicitly allowed
        assertTrue(allowDenyACL.isModifiable(parse("allow")));

        // child of explicitly allowed
        assertTrue(allowDenyACL.isModifiable(parse("allow/foo")));

        // explicitly denied
        assertFalse(allowDenyACL.isModifiable(parse("deny")));

        // child of explicitly denied
        assertFalse(allowDenyACL.isModifiable(parse("deny/foo")));

        // parent of denied
        assertTrue(allowDenyACL.isModifiable(parse("a/b")));
        assertTrue(allowDenyACL.isModifiable(parse("one/two")));

        // explicitly denied
        assertFalse(allowDenyACL.isModifiable(parse("a/b/c")));
        assertFalse(allowDenyACL.isModifiable(parse("one/two/three")));

        // child of denied
        assertFalse(allowDenyACL.isModifiable(parse("a/b/c/foo")));
        assertFalse(allowDenyACL.isModifiable(parse("one/two/three/foo")));
        assertFalse(allowDenyACL.isModifiable(parse("a/b/c/d")));
        assertFalse(allowDenyACL.isModifiable(parse("one/two/three/four")));

        // deny override
        assertTrue(allowDenyACL.isModifiable(parse("a/b/c/d/e")));
        assertTrue(allowDenyACL.isModifiable(parse("one/two/three/four/five")));

        // child of deny override
        assertTrue(allowDenyACL.isModifiable(parse("a/b/c/d/e/foo")));
        assertTrue(allowDenyACL.isModifiable(parse("one/two/three/four/five/foo")));
    }

    public void testDenyAllow() throws Exception {
        // outside of listed
        assertFalse(denyAllowACL.isModifiable(parse("foo")));

        // explicitly allowed
        assertTrue(denyAllowACL.isModifiable(parse("allow")));

        // child of explicitly allowed
        assertTrue(denyAllowACL.isModifiable(parse("allow/foo")));

        // explicitly denied
        assertFalse(denyAllowACL.isModifiable(parse("deny")));

        // child of explicitly denied
        assertFalse(denyAllowACL.isModifiable(parse("deny/foo")));

        // parent of allowed
        assertFalse(denyAllowACL.isModifiable(parse("a/b")));
        assertFalse(denyAllowACL.isModifiable(parse("one/two")));

        // explicitly allowed
        assertTrue(denyAllowACL.isModifiable(parse("a/b/c")));
        assertTrue(denyAllowACL.isModifiable(parse("one/two/three")));

        // child of allowed
        assertTrue(denyAllowACL.isModifiable(parse("a/b/c/foo")));
        assertTrue(denyAllowACL.isModifiable(parse("one/two/three/foo")));
        assertTrue(denyAllowACL.isModifiable(parse("a/b/c/d")));
        assertTrue(denyAllowACL.isModifiable(parse("one/two/three/four")));

        // allow override
        assertFalse(denyAllowACL.isModifiable(parse("a/b/c/d/e")));
        assertFalse(denyAllowACL.isModifiable(parse("one/two/three/four/five")));

        // children of allow override
        assertFalse(denyAllowACL.isModifiable(parse("a/b/c/d/e/foo")));
        assertFalse(denyAllowACL.isModifiable(parse("one/two/three/four/five/foo")));
    }

    public void testAllowDenyContext() throws Exception {
        // outside of listed
        assertModifiable(lookupSubcontext(allowDenyContext, "foo"));

        // explicitly allowed
        assertModifiable(lookupSubcontext(allowDenyContext, "allow"));

        // child of explicitly allowed
        assertModifiable(lookupSubcontext(allowDenyContext, "allow/foo"));

        // explicitly denied
        assertUnmodifiable(lookupSubcontext(allowDenyContext, "deny"));

        // child of explicitly denied
        assertUnmodifiable(lookupSubcontext(allowDenyContext, "deny/foo"));

        // parent of denied
        assertModifiable(lookupSubcontext(allowDenyContext, "a/b"));
        assertModifiable(lookupSubcontext(allowDenyContext, "one/two"));

        // explicitly denied
        assertUnmodifiable(lookupSubcontext(allowDenyContext, "a/b/c"));
        assertUnmodifiable(lookupSubcontext(allowDenyContext, "one/two/three"));

        // child of denied
        assertUnmodifiable(lookupSubcontext(allowDenyContext, "a/b/c/foo"));
        assertUnmodifiable(lookupSubcontext(allowDenyContext, "one/two/three/foo"));
        assertUnmodifiable(lookupSubcontext(allowDenyContext, "a/b/c/d"));
        assertUnmodifiable(lookupSubcontext(allowDenyContext, "one/two/three/four"));

        // deny override
        assertModifiable(lookupSubcontext(allowDenyContext, "a/b/c/d/e"));
        assertModifiable(lookupSubcontext(allowDenyContext, "one/two/three/four/five"));

        // child of deny override
        assertModifiable(lookupSubcontext(allowDenyContext, "a/b/c/d/e/foo"));
        assertModifiable(lookupSubcontext(allowDenyContext, "one/two/three/four/five/foo"));
    }

    public void testDenyAllowContext() throws Exception {
        // outside of listed
        assertUnmodifiable(lookupSubcontext(denyAllowContext, "foo"));

        // explicitly allowed
        assertModifiable(lookupSubcontext(denyAllowContext, "allow"));

        // child of explicitly allowed
        assertModifiable(lookupSubcontext(denyAllowContext, "allow/foo"));

        // explicitly denied
        assertUnmodifiable(lookupSubcontext(denyAllowContext, "deny"));

        // child of explicitly denied
        assertUnmodifiable(lookupSubcontext(denyAllowContext, "deny/foo"));

        // parent of allowed
        assertUnmodifiable(lookupSubcontext(denyAllowContext, "a/b"));
        assertUnmodifiable(lookupSubcontext(denyAllowContext, "one/two"));

        // explicitly allowed
        assertModifiable(lookupSubcontext(denyAllowContext, "a/b/c"));
        assertModifiable(lookupSubcontext(denyAllowContext, "one/two/three"));

        // child of allowed
        assertModifiable(lookupSubcontext(denyAllowContext, "a/b/c/foo"));
        assertModifiable(lookupSubcontext(denyAllowContext, "one/two/three/foo"));
        assertModifiable(lookupSubcontext(denyAllowContext, "a/b/c/d"));
        assertModifiable(lookupSubcontext(denyAllowContext, "one/two/three/four"));

        // allow override
        assertUnmodifiable(lookupSubcontext(denyAllowContext, "a/b/c/d/e"));
        assertUnmodifiable(lookupSubcontext(denyAllowContext, "one/two/three/four/five"));

        // children of allow override
        assertUnmodifiable(lookupSubcontext(denyAllowContext, "a/b/c/d/e/foo"));
        assertUnmodifiable(lookupSubcontext(denyAllowContext, "one/two/three/four/five/foo"));
    }

    protected void setUp() throws Exception {
        super.setUp();

        allowDenyACL = new ContextAccessControlList(true,
                Arrays.asList(new String[]{"allow", "a/b/c/d/e", "one/two/three/four/five"}),
                Arrays.asList(new String[]{"deny", "a/b/c", "one/two/three"}));

        denyAllowACL = new ContextAccessControlList(false,
                Arrays.asList(new String[]{"allow", "a/b/c", "one/two/three"}),
                Arrays.asList(new String[]{"deny", "a/b/c/d/e", "one/two/three/four/five"}));


        Map map = new HashMap();

        // outside of listed
        map.put("foo/value", "bar");

        // explicitly allowed
        map.put("allow/foo/cheese", "cheddar");

        // explicitly denied
        map.put("deny/foo/cheese", "american");

        // child of denied
        map.put("a/b/c/foo/value", "bar");
        map.put("one/two/three/foo/value", "bar");

        // child of deny override
        map.put("a/b/c/d/e/foo/value", "bar");
        map.put("one/two/three/four/five/foo/value", "bar");

        allowDenyContext = new WritableContext("", map, allowDenyACL);
        assertEq(map, allowDenyContext);

        denyAllowContext = new WritableContext("", map, denyAllowACL);
        assertEq(map, denyAllowContext);
    }

}
