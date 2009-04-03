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
import java.util.HashMap;
import java.util.Map;

/**
 * @version $Rev: 355877 $ $Date: 2005-12-10 18:48:27 -0800 (Sat, 10 Dec 2005) $
 */
public class ImmutableContextTest extends AbstractContextTest {
    private static final String STRING_VAL = "some string";

    public void testBasic() throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("string", STRING_VAL);
        map.put("nested/context/string", STRING_VAL);
        map.put("a/b/c/d/e/string", STRING_VAL);
        map.put("a/b/c/d/e/one", 1);
        map.put("a/b/c/d/e/two", 2);
        map.put("a/b/c/d/e/three", 3);
        map.put("a/a/b/c/d/e/three", 3);
        map.put("a/b/b/c/d/e/three", 3);

        Context context = new ImmutableContext(map);

        assertEq(map, context);

        assertEquals("a", ((Context)context.lookup("a")).getNameInNamespace());
        assertEquals("a/a", ((Context)context.lookup("a/a")).getNameInNamespace());
        assertEquals("a/b/b", ((Context)context.lookup("a/b/b")).getNameInNamespace());
        assertEquals("a/b", ((Context)context.lookup("a/b")).getNameInNamespace());
        assertEquals("a/b/c", ((Context)context.lookup("a/b/c")).getNameInNamespace());
        assertEquals("a/b/c/d", ((Context)context.lookup("a/b/c/d")).getNameInNamespace());
        assertEquals("a/b/c/d/e", ((Context)context.lookup("a/b/c/d/e")).getNameInNamespace());

    }

    public void testNameInNamespace() throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("string", STRING_VAL);
        map.put("nested/context/string", STRING_VAL);
        map.put("a/b/c/d/e/string", STRING_VAL);
        map.put("a/b/c/d/e/one", 1);
        map.put("a/b/c/d/e/two", 2);
        map.put("a/b/c/d/e/three", 3);
        map.put("a/a/b/c/d/e/three", 3);
        map.put("a/b/b/c/d/e/three", 3);

        Context context = new ImmutableContext("a", map, false);
        assertEquals("a/a", ((Context)context.lookup("a")).getNameInNamespace());
        assertEquals("a/a/a", ((Context)context.lookup("a/a")).getNameInNamespace());
        assertEquals("a/a/b/b", ((Context)context.lookup("a/b/b")).getNameInNamespace());
        assertEquals("a/a/b", ((Context)context.lookup("a/b")).getNameInNamespace());
        assertEquals("a/a/b/c", ((Context)context.lookup("a/b/c")).getNameInNamespace());
        assertEquals("a/a/b/c/d", ((Context)context.lookup("a/b/c/d")).getNameInNamespace());
        assertEquals("a/a/b/c/d/e", ((Context)context.lookup("a/b/c/d/e")).getNameInNamespace());

    }
}
