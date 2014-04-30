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

import javax.naming.Referenceable;
import javax.naming.Reference;
import javax.naming.NamingException;
import javax.naming.StringRefAddr;

/**
 * @version $Rev$ $Date$
 */
public class Foo implements Referenceable {

    private final String value;

    public Foo(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public Reference getReference() throws NamingException {
        return new Reference(Foo.class.getName(),
                new StringRefAddr("value", value),
                FooFactory.class.getName(),
                null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Foo foo = (Foo) o;

        if (value != null ? !value.equals(foo.value) : foo.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }
}
