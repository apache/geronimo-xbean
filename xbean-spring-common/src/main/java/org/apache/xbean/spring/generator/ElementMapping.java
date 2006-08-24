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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class ElementMapping implements Comparable {
    private final String namespace;
    private final String elementName;
    private final String className;
    private final String description;
    private final boolean rootElement;
    private final String initMethod;
    private final String destroyMethod;
    private final String factoryMethod;
    private final String contentProperty;
    private final Set attributes;
    private final Map attributesByName;
    private final List constructors;
    private final List flatProperties;
    private final Map maps;
    private final Map flatCollections;
	private final List superClasses;
	private final HashSet interfaces;
    
    public ElementMapping(String namespace, String elementName, String className, String description, 
            boolean rootElement, String initMethod, String destroyMethod, String factoryMethod, 
            String contentProperty, Set attributes, List constructors, List flatProperties, Map maps, 
            Map flatCollections, List superClasses, HashSet interfaces) {
        this.superClasses = superClasses;
		this.interfaces = interfaces;
		if (namespace == null) throw new NullPointerException("namespace");
        if (elementName == null) throw new NullPointerException("elementName");
        if (className == null) throw new NullPointerException("className");
        if (attributes == null) throw new NullPointerException("attributes");
        if (constructors == null) throw new NullPointerException("constructors");

        this.namespace = namespace;
        this.elementName = elementName;
        this.className = className;
        this.description = description;
        this.rootElement = rootElement;
        this.initMethod = initMethod;
        this.destroyMethod = destroyMethod;
        this.factoryMethod = factoryMethod;
        this.contentProperty = contentProperty;
        this.constructors = constructors;
        this.attributes = Collections.unmodifiableSet(new TreeSet(attributes));
        this.maps = Collections.unmodifiableMap(maps);
        this.flatProperties = Collections.unmodifiableList(flatProperties);
        this.flatCollections = Collections.unmodifiableMap(flatCollections);
        
        Map attributesByName = new HashMap();
        for (Iterator iterator = attributes.iterator(); iterator.hasNext();) {
            AttributeMapping attribute = (AttributeMapping) iterator.next();
            attributesByName.put(attribute.getAttributeName(), attribute);
        }
        this.attributesByName = Collections.unmodifiableMap(attributesByName);
    }

    public String getNamespace() {
        return namespace;
    }

    public String getElementName() {
        return elementName;
    }

    public String getClassName() {
        return className;
    }

    public String getDescription() {
        return description;
    }

    public boolean isRootElement() {
        return rootElement;
    }

    public String getInitMethod() {
        return initMethod;
    }

    public String getDestroyMethod() {
        return destroyMethod;
    }

    public String getFactoryMethod() {
        return factoryMethod;
    }

    public String getContentProperty() {
        return contentProperty;
    }

    public Set getAttributes() {
        return attributes;
    }

    public AttributeMapping getAttribute(String attributeName) {
        return (AttributeMapping) attributesByName.get(attributeName);
    }

    public Map getMapMappings() {
        return maps;
    }

    public MapMapping getMapMapping(String name) {
        return (MapMapping) maps.get(name);
    }
    
    public Map getFlatCollections() {
        return flatCollections;
    }

    public List getFlatProperties() {
        return flatProperties;
    }

    public List getConstructors() {
        return constructors;
    }

    public int hashCode() {
        return elementName.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj instanceof ElementMapping) {
            return elementName.equals(((ElementMapping) obj).elementName);
        }
        return false;
    }

    public int compareTo(Object obj) {
        return elementName.compareTo(((ElementMapping) obj).elementName);
    }

	public HashSet getInterfaces() {
		return interfaces;
	}

	public List getSuperClasses() {
		return superClasses;
	}
}
