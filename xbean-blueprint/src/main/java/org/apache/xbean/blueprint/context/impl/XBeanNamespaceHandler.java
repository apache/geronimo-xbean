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


package org.apache.xbean.blueprint.context.impl;

import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutableValueMetadata;
import org.apache.aries.blueprint.reflect.BeanPropertyImpl;
import org.osgi.framework.Bundle;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.ValueMetadata;
import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.HashMap;

/**
 * @version $Rev$ $Date$
 */
public class XBeanNamespaceHandler implements NamespaceHandler {

    private final String namespace;
    private final URL schemaLocation;
    private final Set<Class> managedClasses;
    private final MappingMetaData mappingMetaData;
    private final Map<String, Class> managedClassesByName;
    private final NamedConstructorArgs namedConstructorArgs = new NamedConstructorArgs();

    public XBeanNamespaceHandler(String namespace, URL schemaLocation, Set<Class> managedClasses, Properties properties) {
        this.namespace = namespace;
        this.schemaLocation = schemaLocation;
        this.managedClasses = managedClasses;
        managedClassesByName = mapClasses(managedClasses);
        this.mappingMetaData = new MappingMetaData(properties);
    }

    public XBeanNamespaceHandler(String namespace, String schemaLocation, Bundle bundle, String propertiesLocation) throws IOException, ClassNotFoundException {
        this.namespace = namespace;
        this.schemaLocation = bundle.getEntry(schemaLocation);
        URL propertiesUrl = bundle.getEntry(propertiesLocation);
        InputStream in = propertiesUrl.openStream();
        Properties properties = new Properties();
        try {
            properties.load(in);
        } finally {
            in.close();
        }
        managedClasses = new HashSet<Class>();
        for (Map.Entry entry : properties.entrySet()) {
            String key = (String) entry.getKey();
            if (key.indexOf(".") < 0) {
                String className = (String) entry.getValue();
                Class clazz = bundle.loadClass(className);
                managedClasses.add(clazz);
            }
        }
        managedClassesByName = mapClasses(managedClasses);
        this.mappingMetaData = new MappingMetaData(properties);
    }

    private Map<String, Class> mapClasses(Set<Class> managedClasses) {
        Map<String, Class> map = new HashMap<String, Class>();
        for (Class clazz : managedClasses) {
            map.put(clazz.getName(), clazz);
        }
        return map;
    }

    public URL getSchemaLocation(String s) {
        if (namespace.equals(s)) {
            return schemaLocation;
        }
        return null;
    }

    public Set<Class> getManagedClasses() {
        return managedClasses;
    }

    public Metadata parse(Element element, ParserContext parserContext) {
        String beanTypeName = element.getLocalName();
        String className = mappingMetaData.getClassName(beanTypeName);
        if (className == null) {
            throw new ComponentDefinitionException(beanTypeName + " not known to xbean namespace handler for " + namespace);
        }
        MutableBeanMetadata beanMetaData = parserContext.createMetadata(MutableBeanMetadata.class);
        beanMetaData.setClassName(className);
        beanMetaData.setRuntimeClass(managedClassesByName.get(className));
        if (beanMetaData.getRuntimeClass() == null) {
            throw new ComponentDefinitionException("Unknown bean class: " + className);
        }

        if (element.hasAttributeNS("http://www.osgi.org/xmlns/blueprint/v1.0.0", "id")) {
            String id = element.getAttributeNS("http://www.osgi.org/xmlns/blueprint/v1.0.0", "id");
            beanMetaData.setId(id);
        } else {
            beanMetaData.setId(parserContext.generateId());
        }

        String initMethod = mappingMetaData.getInitMethodName(beanTypeName);
        if (initMethod != null) {
            beanMetaData.setInitMethod(initMethod);
        }
        String destroyMethod = mappingMetaData.getDestroyMethodName(beanTypeName);
        if (destroyMethod != null) {
            beanMetaData.setDestroyMethod(destroyMethod);
        }
        String factoryMethod = mappingMetaData.getFactoryMethodName(beanTypeName);
        if (factoryMethod != null) {
            beanMetaData.setFactoryMethod(factoryMethod);
        }

        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Attr attr = (Attr) attrs.item(i);
            if (namespace.equals(attr.getNamespaceURI())) {
                String attrName = attr.getLocalName();
                String value = attr.getValue().trim();
                String propertyName = mappingMetaData.getPropertyName(beanTypeName, attrName);
                MutableValueMetadata m = parserContext.createMetadata(MutableValueMetadata.class);
                m.setStringValue(value);
//            m.setType(null);
                BeanProperty beanProperty = new BeanPropertyImpl(propertyName, m);
                beanMetaData.addProperty(beanProperty);
            }
        }

        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element) {
                Element child = (Element) node;
                String childName = child.getLocalName();
                Metadata childMetadata = null;
                String propertyName = mappingMetaData.getNestedListProperty(beanTypeName, childName);
                //explicit list
                if (propertyName != null) {
                    childMetadata = parserContext.parseElement(CollectionMetadata.class, beanMetaData, child);
                } else if ((propertyName = mappingMetaData.getFlatCollectionProperty(beanTypeName, childName)) != null) {
                    //flat collection

//                } else if ((propertyName = mappingMetaData.getNestedProperty(beanTypeName, childName)) != null) {
//
//                } else if ((propertyName = mappingMetaData.get))
                } else {
                    propertyName = mappingMetaData.getPropertyName(beanTypeName, childName);
                    NodeList childNodes = child.getChildNodes();
                    StringBuilder buf = new StringBuilder();
                    for (int j = 0; j < childNodes.getLength(); j++) {
                        Node childNode = childNodes.item(j);
                        if (childNode instanceof Element) {
                            Element childElement = (Element) childNode;
                            try {
                                childMetadata = parserContext.parseElement(BeanMetadata.class, beanMetaData, (Element) childNode);
                            } catch (Exception e) {
                                childMetadata = parserContext.parseElement(ValueMetadata.class, beanMetaData, childElement);
                            }
                            break;
                        } else if (childNode instanceof Text) {
                            String value = childNode.getNodeValue();
                            buf.append(value);
                        }
                    }
                    if (childMetadata == null) {
                        MutableValueMetadata m = parserContext.createMetadata(MutableValueMetadata.class);
                        m.setStringValue(buf.toString().trim());
                        childMetadata = m;
                    }
                }
                BeanProperty beanProperty = new BeanPropertyImpl(propertyName, childMetadata);
                beanMetaData.addProperty(beanProperty);
            }
        }
        namedConstructorArgs.processParameters(beanMetaData, mappingMetaData, parserContext);
        return beanMetaData;
    }

    public ComponentMetadata decorate(Node node, ComponentMetadata componentMetadata, ParserContext parserContext) {
        return componentMetadata;
    }
}
