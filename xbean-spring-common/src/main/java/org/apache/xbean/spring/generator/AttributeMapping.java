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

/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class AttributeMapping implements Comparable {
    private final String attributeName;
    private final String propertyName;
    private final String description;
    private final Type type;
    private final String value;
    private final boolean fixed;
    private final boolean required;
	private final String propertyEditor;

    public AttributeMapping(String attributeName, String propertyName, String description, Type type, String value, boolean fixed, boolean required, String propertyEditor) {
        this.propertyEditor = propertyEditor;
		if (attributeName == null) throw new NullPointerException("attributeName");
        if (propertyName == null) throw new NullPointerException("propertyName");
        if (type == null) throw new NullPointerException("type");
        this.attributeName = attributeName;
        this.propertyName = propertyName;
        if (description == null) description = "";
        this.description = description;
        this.type = type;
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

    public Type getType() {
        return type;
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

	public String getPropertyEditor() {
		return propertyEditor;
	}
}
