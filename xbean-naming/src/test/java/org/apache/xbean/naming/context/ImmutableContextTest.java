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
import java.util.HashMap;
import java.util.Map;

/**
 * @version $Rev: 355877 $ $Date: 2005-12-10 18:48:27 -0800 (Sat, 10 Dec 2005) $
 */
public class ImmutableContextTest extends AbstractContextTest {
    private static final String STRING_VAL = "some string";

    public void testBasic() throws Exception {
        Map map = new HashMap();
        map.put("string", STRING_VAL);
        map.put("nested/context/string", STRING_VAL);
        map.put("a/b/c/d/e/string", STRING_VAL);
        map.put("a/b/c/d/e/one", new Integer(1));
        map.put("a/b/c/d/e/two", new Integer(2));
        map.put("a/b/c/d/e/three", new Integer(3));

        Context context = new ImmutableContext(map);

        assertEq(map, context);
    }
}
