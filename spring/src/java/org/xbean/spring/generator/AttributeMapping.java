/**
 *
 * Copyright 2005 the original author or authors.
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
package org.xbean.spring.generator;

/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class AttributeMapping implements Comparable {
    private final String attributeName;
    private final String propertyName;
    private final String description;
    private final String type;
    private final boolean primitive;
    private final boolean array;
    private final String arrayType;
    private final boolean list;
    private final String value;
    private final boolean fixed;
    private final boolean required;

    public AttributeMapping(String attributeName, String propertyName, String description, String type, boolean primitive, boolean array, String arrayType, boolean list, String value, boolean fixed, boolean required) {
        if (attributeName == null) throw new NullPointerException("attributeName");
        if (propertyName == null) throw new NullPointerException("propertyName");
        if (type == null) throw new NullPointerException("type");
        this.attributeName = attributeName;
        this.propertyName = propertyName;
        this.description = description;
        this.type = type;
        this.primitive = primitive;
        this.array = array;
        this.arrayType = arrayType;
        this.list = list;
        this.value = value;
        this.fixed = fixed;
        this.required = required;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public boolean isPrimitive() {
        return primitive;
    }

    public boolean isArray() {
        return array;
    }

    public String getArrayType() {
        return arrayType;
    }

    public boolean isList() {
        return list;
    }

    public String getValue() {
        return value;
    }

    public boolean isFixed() {
        return fixed;
    }

    public boolean isRequired() {
        return required;
    }

    public int hashCode() {
        return attributeName.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj instanceof AttributeMapping) {
            return attributeName.equals(((AttributeMapping) obj).attributeName);
        }
        return false;
    }

    public int compareTo(Object obj) {
        return attributeName.compareTo(((AttributeMapping) obj).attributeName);
    }
}
