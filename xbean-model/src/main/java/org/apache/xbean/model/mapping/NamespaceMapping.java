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
package org.apache.xbean.model.mapping;

import org.apache.xbean.model.Type;

import java.util.*;

/**
 * Mapping of configuration or its subset to elements and attributes.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class NamespaceMapping implements Comparable<NamespaceMapping> {
    private final String namespace;
    private final Set<ElementMapping> elements;
    private final Map<String, ElementMapping> elementsByName;
    private final ElementMapping rootElement;

    public NamespaceMapping(String namespace, Set<ElementMapping> elements, ElementMapping rootElement) {
        this.namespace = namespace;
        this.elements = Collections.unmodifiableSet(new TreeSet<>(elements));
        this.rootElement = rootElement;

        Map<String, ElementMapping> elementsByName = new HashMap<>();
        for (ElementMapping elementMapping : elements) {
            elementsByName.put(elementMapping.getElementName(), elementMapping);
        }
        this.elementsByName = Collections.unmodifiableMap(elementsByName);
    }

    public String getNamespace() {
        return namespace;
    }

    public Set<ElementMapping> getElements() {
        return elements;
    }

    public ElementMapping getElement(String elementName) {
        return elementsByName.get(elementName);
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

    public int compareTo(NamespaceMapping obj) {
        return namespace.compareTo((obj).namespace);
    }

    public List<ElementMapping> findImplementationsOf(Type type) {
        List<ElementMapping> elements = new ArrayList();
        String nestedTypeName = type.getName();

        for (ElementMapping element : getElements()) {
            if (element.getClassName().equals(nestedTypeName) ||
                element.getInterfaces().contains(nestedTypeName) ||
                element.getSuperClasses().contains(nestedTypeName))
            {
                elements.add(element);
            }
        }
        return elements;
    }

}
