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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutableCollectionMetadata;
import org.apache.aries.blueprint.mutable.MutableValueMetadata;
import org.apache.aries.blueprint.reflect.BeanPropertyImpl;
import org.osgi.framework.Bundle;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.ValueMetadata;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

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
        URL propertiesUrl = bundle.getEntry(propertiesLocation);
        InputStream in = propertiesUrl.openStream();
        Properties properties = new Properties();
        try {
            properties.load(in);
        } finally {
            in.close();
        }
        this.namespace = namespace;
        this.schemaLocation = bundle.getEntry(schemaLocation);
        this.managedClasses = managedClassesFromProperties(bundle, properties);
        managedClassesByName = mapClasses(managedClasses);
        this.mappingMetaData = new MappingMetaData(properties);
    }

    private static Set<Class> managedClassesFromProperties(Bundle bundle, Properties properties) throws ClassNotFoundException {
        Set<Class> managedClasses = new HashSet<Class>();
        for (Map.Entry entry : properties.entrySet()) {
            String key = (String) entry.getKey();
            if (key.indexOf(".") < 0) {
                String className = (String) entry.getValue();
                Class clazz = bundle.loadClass(className);
                managedClasses.add(clazz);
            }
        }
        return managedClasses;
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
        return parseInternal(element, parserContext, beanTypeName, className);
    }

    private Metadata parseInternal(Element element, ParserContext parserContext, String beanTypeName, String className) {
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

        lifecycleMethods(beanTypeName, beanMetaData);

        attributeProperties(element, parserContext, beanTypeName, beanMetaData);
        contentProperty(beanMetaData, element, parserContext);
        nestedProperties(element, parserContext, beanTypeName, beanMetaData);
        //QName resolution
        coerceNamespaceAwarePropertyValues(beanMetaData, element, parserContext);
        namedConstructorArgs.processParameters(beanMetaData, mappingMetaData, parserContext);
        return beanMetaData;
    }

    private void lifecycleMethods(String beanTypeName, MutableBeanMetadata beanMetaData) {
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
    }

    private void attributeProperties(Element element, ParserContext parserContext, String beanTypeName, MutableBeanMetadata beanMetaData) {
        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Attr attr = (Attr) attrs.item(i);
            if (namespace.equals(attr.getNamespaceURI())) {
                String attrName = attr.getLocalName();
                String value = attr.getValue().trim();
                String propertyName = mappingMetaData.getPropertyName(beanTypeName, attrName);
                addProperty(propertyName, value, beanMetaData, parserContext);
            }
        }
    }

    private void contentProperty(MutableBeanMetadata definition, Element element, ParserContext parserContext) {
        String name = mappingMetaData.getContentProperty(element.getLocalName());
        if (name != null) {
            String value = getElementText(element).trim();
            addProperty(name, value, definition, parserContext);
        } else {
            StringBuilder buffer = new StringBuilder();
            NodeList childNodes = element.getChildNodes();
            for (int i = 0, size = childNodes.getLength(); i < size; i++) {
                Node node = childNodes.item(i);
                if (node instanceof Text) {
                    buffer.append(((Text) node).getData());
                }
            }

            ByteArrayInputStream in = new ByteArrayInputStream(buffer.toString().getBytes());
            Properties properties = new Properties();
            try {
                properties.load(in);
            }
            catch (IOException e) {
                return;
            }
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();
                addProperty(key, value, definition, parserContext);
            }
        }
    }

    private void addProperty(String name, String value, MutableBeanMetadata definition, ParserContext parserContext) {
        MutableValueMetadata m = parserContext.createMetadata(MutableValueMetadata.class);
        m.setStringValue(value);
        BeanProperty beanProperty = new BeanPropertyImpl(name, m);
        definition.addProperty(beanProperty);
    }

    private void nestedProperties(Element element, ParserContext parserContext, String beanTypeName, MutableBeanMetadata beanMetaData) {
        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element) {
                Element child = (Element) node;
                String childName = child.getLocalName();
                String namespace = child.getNamespaceURI();
                if (!this.namespace.equals(namespace)) {
                    BeanProperty prop = parserContext.parseElement(BeanProperty.class, beanMetaData, child);
                    beanMetaData.addProperty(prop);
                    continue;
                }
                Metadata childMetadata = null;
                PropertyDescriptor pd = getPropertyDescriptor(mappingMetaData.getClassName(beanTypeName), childName);
                Class propertyType = pd == null ? null : pd.getPropertyType();
                String propertyName = mappingMetaData.getNestedListProperty(beanTypeName, childName);
                //explicit list
                if (propertyName != null || isCollectionType(propertyType)) {
                    propertyName = propertyName == null ? childName : propertyName;
                    childMetadata = parserContext.parseElement(CollectionMetadata.class, beanMetaData, child);
                } else if ((propertyName = mappingMetaData.getFlatCollectionProperty(beanTypeName, childName)) != null) {
                    //flat collection
                    Metadata elementMetadata = parse(child, parserContext);
                    BeanProperty list = propertyByName(propertyName, beanMetaData);
                    MutableCollectionMetadata listMeta;
                    if (list == null) {
                        listMeta = parserContext.createMetadata(MutableCollectionMetadata.class);
                        childMetadata = listMeta;
                    } else {
                        listMeta = (MutableCollectionMetadata) list.getValue();
                    }
                    listMeta.addValue(elementMetadata);
                } else if ((propertyName = mappingMetaData.getNestedProperty(beanTypeName, childName)) != null) {

                } else if (mappingMetaData.isFlatProperty(beanTypeName, childName)) {
                    propertyName = childName;
                    String flatClassName = getPropertyDescriptor(mappingMetaData.getClassName(beanTypeName), childName).getPropertyType().getName();
                    childMetadata = parseInternal(child, parserContext, childName, flatClassName);
                } else {
                    propertyName = mappingMetaData.getPropertyName(beanTypeName, childName);
                    NodeList childNodes = child.getChildNodes();
                    StringBuilder buf = new StringBuilder();
                    for (int j = 0; j < childNodes.getLength(); j++) {
                        Node childNode = childNodes.item(j);
                        if (childNode instanceof Element) {
                            Element childElement = (Element) childNode;
                            if (namespace.equals(childElement.getNamespaceURI())) {
                                childMetadata = parse(childElement, parserContext);
                            } else {
                                try {
                                    childMetadata = parserContext.parseElement(BeanMetadata.class, beanMetaData, childElement);
                                } catch (Exception e) {
                                    childMetadata = parserContext.parseElement(ValueMetadata.class, beanMetaData, childElement);
                                }
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
                if (childMetadata != null) {
                    BeanProperty beanProperty = new BeanPropertyImpl(propertyName, childMetadata);
                    beanMetaData.addProperty(beanProperty);
                }
            }
        }
    }

    private boolean isCollectionType(Class propertyType) {
        if (propertyType == null) {
            return false;
        }
        if (Collection.class.isAssignableFrom(propertyType)) {
            return true;
        }
        if (propertyType.isArray()) {
            return true;
        }
        return false;
    }

    public static BeanProperty propertyByName(String name, BeanMetadata meta) {
        for (BeanProperty prop : meta.getProperties()) {
            if (name.equals(prop.getName())) {
                return prop;
            }
        }
        return null;
    }

    public ComponentMetadata decorate(Node node, ComponentMetadata componentMetadata, ParserContext parserContext) {
        return componentMetadata;
    }

    private void coerceNamespaceAwarePropertyValues(MutableBeanMetadata bd, Element element, ParserContext parserContext) {
        // lets check for any QName types
        BeanInfo beanInfo = getBeanInfo(getClass(bd.getClassName()));
        if (beanInfo != null) {
            PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor descriptor : descriptors) {
                QNameHelper.coerceNamespaceAwarePropertyValues(bd, element, descriptor, parserContext);
            }
        }
    }

    private PropertyDescriptor getPropertyDescriptor(String className, String localName) {
        Class clazz = getClass(className);
        return getPropertyDescriptor(clazz, localName);
    }

    private PropertyDescriptor getPropertyDescriptor(Class clazz, String localName) {
        BeanInfo beanInfo = getBeanInfo(clazz);
        if (beanInfo != null) {
            PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();
            for (int i = 0; i < descriptors.length; i++) {
                PropertyDescriptor descriptor = descriptors[i];
                String name = descriptor.getName();
                if (name.equals(localName)) {
                    return descriptor;
                }
            }
        }
        return null;
    }

    private Class getClass(String className) throws ComponentDefinitionException {
        if (className == null) {
            return null;
        }

        Class type = managedClassesByName.get(className);
        if (type == null) {
            throw new ComponentDefinitionException("Unknown type: " + className);
        }
        return type;
    }

    private BeanInfo getBeanInfo(Class type) {
        if (type == null) {
            return null;
        }
        try {
            return Introspector.getBeanInfo(type);
        }
        catch (IntrospectionException e) {
            throw new ComponentDefinitionException("Failed to introspect type: " + type.getName() + ". Reason: " + e, e);
        }
    }

    private String getElementText(Element element) {
        StringBuffer buffer = new StringBuffer();
        NodeList nodeList = element.getChildNodes();
        for (int i = 0, size = nodeList.getLength(); i < size; i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
                buffer.append(node.getNodeValue());
            }
        }
        return buffer.toString();
    }

}
