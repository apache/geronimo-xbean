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
package org.apache.xbean.spring.context.impl;

/**
 * DefaultProperty contains the default value assigned to a property with a specific name and type.
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class DefaultProperty {
    private String name;
    private Class type;
    private Object value;

    /**
     * Creates a new empty default property.  This instance is unusable until the name, type and values are assigned.
     */
    public DefaultProperty() {
    }

    /**
     * Creates new default property value for a property with the specified name and type.
     * @param name the name of the property
     * @param type the type of the property
     * @param value the default value
     */
    public DefaultProperty(String name, Class type, Object value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    /**
     * Gets the property name.
     * @return the property name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the property name.
     * @param name the property name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the property type.
     * @return the property type
     */
    public Class getType() {
        return type;
    }

    /**
     * Sets the property type.
     * @param type the property type
     */
    public void setType(Class type) {
        this.type = type;
    }

    /**
     * Gets the default value.
     * @return the default value
     */
    public Object getValue() {
        return value;
    }

    /**
     * Sets the default value.
     * @param value the default value
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "[" + name + ", " + type + ", " + value + "]";
    }
}
