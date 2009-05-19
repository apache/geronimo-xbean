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


package org.apache.xbean.naming.referenceable;

import java.util.Hashtable;

import javax.naming.spi.ObjectFactory;
import javax.naming.Referenceable;
import javax.naming.Reference;
import javax.naming.NamingException;
import javax.naming.Name;
import javax.naming.Context;
import javax.naming.RefAddr;

/**
 * @version $Rev$ $Date$
 */
public class FooFactory implements Referenceable, ObjectFactory {

    private Foo foo;

    public FooFactory() {
    }

    public FooFactory(Foo foo) {
        this.foo = foo;
    }

    public Foo getFoo() {
        return foo;
    }

    public void setFoo(Foo foo) {
        this.foo = foo;
    }

    public Reference getReference() throws NamingException {
        return foo.getReference();
    }

    public Object getObjectInstance(Object o, Name name, Context context, Hashtable<?, ?> hashtable) throws Exception {
        if (o instanceof Reference) {
            Reference ref = (Reference) o;
            if (Foo.class.getName().equals(ref.getClassName())) {
                RefAddr addr = ref.get("value");
                return new Foo((String) addr.getContent());
            }
        }
        return null;
    }
    
}
