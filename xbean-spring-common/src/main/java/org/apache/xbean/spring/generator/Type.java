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
package org.apache.xbean.spring.generator;

import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class Type {
    private final String name;
    private final Type nestedType;
    private final boolean primitive;

    public static Type newSimpleType(String name) {
        if (name == null) throw new NullPointerException("type");
        if (name.indexOf("[") >= 0 || name.indexOf("]") >= 0) {
            throw new IllegalArgumentException("Name can not contain '[' or ']' " + name);
        }
        return new Type(name, null);
    }

    public static Type newArrayType(String type, int dimensions) {
        if (type == null) throw new NullPointerException("type");
        if (dimensions < 1) throw new IllegalArgumentException("dimensions must be atleast one");
        StringBuffer buf = new StringBuffer(type.length() + (dimensions * 2));
        buf.append(type);
        for (int i = 0; i < dimensions; i ++) {
            buf.append("[]");
        }
        return new Type(buf.toString(), newSimpleType(type));
    }

    public static Type newCollectionType(String collectionType, Type elementType) {
        if (collectionType == null) throw new NullPointerException("collectionType");
        if (elementType == null) throw new NullPointerException("elementType");
        return new Type(collectionType, elementType);
    }

    private Type(String name, Type nestedType) {
        this.name = name;
        this.nestedType = nestedType;
        primitive = (nestedType == null) && primitives.contains(name);
    }

    public String getName() {
        return name;
    }

    public Type getNestedType() {
        return nestedType;
    }

    public boolean isCollection() {
        return nestedType != null;
    }

    public boolean isPrimitive() {
        return primitive;
    }

    public int hashCode() {
        return super.hashCode();
    }

    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    private static final Set primitives;

    static {
        Set set = new HashSet();
        set.add("boolean");
        set.add("byte");
        set.add("char");
        set.add("short");
        set.add("int");
        set.add("long");
        set.add("float");
        set.add("double");
        primitives = Collections.unmodifiableSet(set);
    }
}
