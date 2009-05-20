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

import javax.naming.Context;

import org.apache.xbean.naming.referenceable.Foo;
import org.apache.xbean.naming.referenceable.FooFactory;

/**
 * @version $Rev$ $Date$
 */
public class ReferenceableTest extends AbstractContextTest {
    
    public void testReferenceable() throws Exception {
        Context context = new WritableContext();
        context.createSubcontext("bar");

        Foo foo1 = new Foo("foo1");
        context.bind("bar/foo1", foo1);
        Object o1 = context.lookup("bar/foo1");
        assertEquals(foo1, o1);
//        assertNotSame(foo1, o1);
    }
    public void testReferenceable2() throws Exception {
        Context context = new WritableContext();
        context.createSubcontext("bar");

        Foo foo1 = new Foo("foo1");
        FooFactory fooFactory1 = new FooFactory(foo1);
        context.bind("bar/foo1", fooFactory1);
        Object o1 = context.lookup("bar/foo1");
        assertEquals(foo1, o1);
        assertNotSame(foo1, o1);
    }
}
