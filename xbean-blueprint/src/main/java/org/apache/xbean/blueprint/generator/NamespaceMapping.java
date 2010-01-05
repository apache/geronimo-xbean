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
package org.apache.xbean.blueprint.generator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class NamespaceMapping implements Comparable {
    private final String namespace;
    private final Set elements;
    private final Map elementsByName;
    private final ElementMapping rootElement;

    public NamespaceMapping(String namespace, Set elements, ElementMapping rootElement) {
        this.namespace = namespace;
        this.elements = Collections.unmodifiableSet(new TreeSet(elements));
        this.rootElement = rootElement;

        Map elementsByName = new HashMap();
        for (Iterator iterator = elements.iterator(); iterator.hasNext();) {
            ElementMapping elementMapping = (ElementMapping) iterator.next();
            elementsByName.put(elementMapping.getElementName(), elementMapping);
        }
        this.elementsByName = Collections.unmodifiableMap(elementsByName);
    }

    public String getNamespace() {
        return namespace;
    }

    public Set getElements() {
        return elements;
    }

    public ElementMapping getElement(String elementName) {
        return (ElementMapping) elementsByName.get(elementName);
    }

    public ElementMapping getRootElement() {
        return rootElement;
    }

    public int hashCode() {
        return namespace.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj instanceof NamespaceMapping) {
            return namespace.equals(((NamespaceMapping) obj).namespace);
        }
        return false;
    }

    public int compareTo(Object obj) {
        return namespace.compareTo(((NamespaceMapping) obj).namespace);
    }

    private final HashMap checkedTypes = new HashMap();

    public boolean isSimpleType(Type type) {
        Boolean b = (Boolean) checkedTypes.get(type);
        if (b == null){
            b = Utils.isSimpleType(type)? Boolean.TRUE: Boolean.FALSE;
            checkedTypes.put(type, b);
        }
        return b.booleanValue();
    }
}
