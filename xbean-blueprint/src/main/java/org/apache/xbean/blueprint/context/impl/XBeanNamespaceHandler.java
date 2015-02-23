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
import java.beans.PropertyEditor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutableCollectionMetadata;
import org.apache.aries.blueprint.mutable.MutableMapMetadata;
import org.apache.aries.blueprint.mutable.MutableRefMetadata;
import org.apache.aries.blueprint.mutable.MutableValueMetadata;
import org.osgi.framework.Bundle;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.MapEntry;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.NonNullMetadata;
import org.osgi.service.blueprint.reflect.NullMetadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.osgi.service.blueprint.reflect.ServiceReferenceMetadata;
import org.osgi.service.blueprint.reflect.ValueMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @version $Rev$ $Date$
 */
public class XBeanNamespaceHandler implements NamespaceHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(XBeanNamespaceHandler.class);

    public static final String BLUEPRINT_NAMESPACE = "http://www.osgi.org/xmlns/blueprint/v1.0.0";
    private static final String BEAN_REFERENCE_PREFIX = "#";
    private static final String NULL_REFERENCE = "#null";

    private final String namespace;
    private final URL schemaLocation;
    private final Set<Class> managedClasses;
    private final MappingMetaData mappingMetaData;
    private final Map<String, Class> managedClassesByName;
    private final Map<String, Class<? extends PropertyEditor>> propertyEditors;
    private final NamedConstructorArgs namedConstructorArgs = new NamedConstructorArgs();

    public XBeanNamespaceHandler(String namespace, URL schemaLocation, Set<Class> managedClasses, Map<String, Class<? extends PropertyEditor>> propertyEditors, Properties properties) {
        this.namespace = namespace;
        this.schemaLocation = schemaLocation;
        this.managedClasses = managedClasses;
        managedClassesByName = mapClasses(managedClasses);
        this.propertyEditors = propertyEditors;
        this.mappingMetaData = new MappingMetaData(properties);
    }

    public XBeanNamespaceHandler(String namespace, String schemaLocation, Bundle bundle, String propertiesLocation) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        URL propertiesUrl = bundle.getResource(propertiesLocation);
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
        propertyEditors = propertyEditorsFromProperties(bundle, properties);
        this.mappingMetaData = new MappingMetaData(properties);
    }

    public XBeanNamespaceHandler(String namespace, String schemaLocation, String propertiesLocation) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        ClassLoader cl = getClass().getClassLoader();
        URL propertiesUrl = cl.getResource(propertiesLocation);
        if (propertiesUrl == null) {
            throw new IOException("Could not locate properties at " + propertiesLocation);
        }
        InputStream in = propertiesUrl.openStream();
        Properties properties = new Properties();
        try {
            properties.load(in);
        } finally {
            in.close();
        }
        this.namespace = namespace;
        this.schemaLocation = cl.getResource(schemaLocation);
        this.managedClasses = managedClassesFromProperties(cl, properties);
        managedClassesByName = mapClasses(managedClasses);
        propertyEditors = propertyEditorsFromProperties(cl, properties);
        this.mappingMetaData = new MappingMetaData(properties);
    }

    private static Set<Class> managedClassesFromProperties(ClassLoader cl, Properties properties) {
        Set<Class> managedClasses = new HashSet<Class>();
        Properties methods = new Properties();
        for (Map.Entry entry : properties.entrySet()) {
            String key = (String) entry.getKey();
            if (key.indexOf(".") < 0) {
                String className = (String) entry.getValue();
                try {
                    Class<?> beanClass = cl.loadClass(className);
                    managedClasses.add(beanClass);
                    findAnnotations(key, beanClass, methods);
                } catch (NoClassDefFoundError e) {
                    LOGGER.warn("Could not load class: {} due to {}", className, e.getMessage());
                } catch (ClassNotFoundException e) {
                    LOGGER.warn("Could not load class: {}", className);
                }
            }
        }
        properties.putAll(methods);
        return managedClasses;
    }

    private static Set<Class> managedClassesFromProperties(Bundle bundle, Properties properties) {
        Set<Class> managedClasses = new HashSet<Class>();
        Properties methods = new Properties();
        for (Map.Entry entry : properties.entrySet()) {
            String key = (String) entry.getKey();
            if (key.indexOf(".") < 0) {
                String className = (String) entry.getValue();
                try {
                    Class<?> beanClass = bundle.loadClass(className);
                    managedClasses.add(beanClass);
                    findAnnotations(key, beanClass, methods);
                } catch (NoClassDefFoundError e) {
                    LOGGER.warn("Could not load class: {} due to {}", className, e.getMessage());
                } catch (ClassNotFoundException e) {
                    LOGGER.warn("Could not load class: {}", className);
                }
            }
        }
        properties.putAll(methods);
        return managedClasses;
    }

    private static void findAnnotations(String key, Class<?> beanClass, Properties methods) {
        for (Method m : beanClass.getMethods()) {
            if (m.isAnnotationPresent(PostConstruct.class)) {
                methods.put(key + ".initMethod", m.getName());
            }
            if (m.isAnnotationPresent(PreDestroy.class)) {
                methods.put(key + ".destroyMethod", m.getName());
            }
        }
    }

    private Map<String, Class<? extends PropertyEditor>> propertyEditorsFromProperties(Bundle bundle, Properties properties) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        Map<String, Class<? extends PropertyEditor>> propertyEditors = new HashMap<String, Class<? extends PropertyEditor>>();
        for (Map.Entry entry : properties.entrySet()) {
            String key = (String) entry.getKey();
            if (key.endsWith(".propertyEditor")) {
                String className = (String) entry.getValue();
                Class<? extends PropertyEditor> clazz = bundle.loadClass(className).asSubclass(PropertyEditor.class);
                propertyEditors.put(className, clazz);
            }
        }
        return propertyEditors;
    }

    private Map<String, Class<? extends PropertyEditor>> propertyEditorsFromProperties(ClassLoader classLoader, Properties properties) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        Map<String, Class<? extends PropertyEditor>> propertyEditors = new HashMap<String, Class<? extends PropertyEditor>>();
        for (Map.Entry entry : properties.entrySet()) {
            String key = (String) entry.getKey();
            if (key.endsWith(".propertyEditor")) {
                String className = (String) entry.getValue();
                Class<? extends PropertyEditor> clazz = classLoader.loadClass(className).asSubclass(PropertyEditor.class);
                propertyEditors.put(className, clazz);
            }
        }
        return propertyEditors;
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
        beanMetaData.setScope(BeanMetadata.SCOPE_SINGLETON);
        beanMetaData.setActivation(BeanMetadata.ACTIVATION_EAGER);
        beanMetaData.setRuntimeClass(managedClassesByName.get(className));
        if (beanMetaData.getRuntimeClass() == null) {
            throw new ComponentDefinitionException("Unknown bean class: " + className);
        }

        if (element.hasAttributeNS(BLUEPRINT_NAMESPACE, "id")) {
            String id = element.getAttributeNS(BLUEPRINT_NAMESPACE, "id");
            beanMetaData.setId(id);
        } else {
            beanMetaData.setId(parserContext.generateId());
        }

        lifecycleMethods(beanTypeName, beanMetaData);

        attributeProperties(element, parserContext, beanTypeName, beanMetaData);
        contentProperty(beanMetaData, element, parserContext);
        nestedProperties(beanMetaData, element, beanTypeName, className, parserContext);
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
            if (namespace.equals(attr.getNamespaceURI()) || attr.getNamespaceURI() == null) {
                String attrName = attr.getLocalName();
                String value = attr.getValue().trim();
                String propertyName = mappingMetaData.getPropertyName(beanTypeName, attrName);
                String propertyEditor = mappingMetaData.getPropertyEditor(beanTypeName, attrName);
                addProperty(propertyName, value, propertyEditor, beanMetaData, parserContext);
            }
        }
    }

    private void contentProperty(MutableBeanMetadata definition, Element element, ParserContext parserContext) {
        String name = mappingMetaData.getContentProperty(element.getLocalName());
        String propertyEditor = mappingMetaData.getPropertyEditor(element.getLocalName(), name);
        if (name != null) {
            String value = getElementText(element).trim();
            addProperty(name, value, propertyEditor, definition, parserContext);
        } else {
            ByteArrayInputStream in = new ByteArrayInputStream(getElementText(element).getBytes());
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
                addProperty(key, value, propertyEditor, definition, parserContext);
            }
        }
    }

    private void addProperty(String name, String value, String propertyEditor, MutableBeanMetadata definition, ParserContext parserContext) {
        Metadata m = getValue(value, propertyEditor, parserContext);
        definition.addProperty(name, m);
    }

    private void nestedProperties(MutableBeanMetadata beanMetadata, Element element, String beanTypeName, String className, ParserContext parserContext) {
        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element) {
                Element child = (Element) node;
                String childName = child.getLocalName();
                String namespace = child.getNamespaceURI();
                if (!this.namespace.equals(namespace)) {
                    BeanProperty prop = parserContext.parseElement(BeanProperty.class, beanMetadata, child);
                    beanMetadata.addProperty(prop);
                    continue;
                }
                Metadata childMetadata = null;
                PropertyDescriptor pd = getPropertyDescriptor(mappingMetaData.getClassName(beanTypeName), childName);
                Class propertyType = pd == null ? null : pd.getPropertyType();
                String propertyName = mappingMetaData.getNestedListProperty(beanTypeName, childName);
                boolean isList = false;
                //explicit list
                if (propertyName != null || isCollectionType(propertyType)) {
                    propertyName = propertyName == null ? childName : propertyName;
                    childMetadata = parserContext.parseElement(CollectionMetadata.class, beanMetadata, child);
                } else if ((propertyName = mappingMetaData.getFlatCollectionProperty(beanTypeName, childName)) != null) {
                    //flat collection
                    Metadata elementMetadata = parse(child, parserContext);
                    BeanProperty list = propertyByName(propertyName, beanMetadata);
                    MutableCollectionMetadata listMeta;
                    if (list == null) {
                        listMeta = parserContext.createMetadata(MutableCollectionMetadata.class);
                        childMetadata = listMeta;
                    } else {
                        listMeta = (MutableCollectionMetadata) list.getValue();
                    }
                    isList = true;
                    listMeta.addValue(elementMetadata);
                } else if ((propertyName = mappingMetaData.getNestedProperty(beanTypeName, childName)) != null) {
                    // lets find the first child bean that parses fine
                    childMetadata = parseChildExtensionBean(child, beanMetadata, parserContext);

                } else if (mappingMetaData.isFlatProperty(beanTypeName, childName)) {
                    propertyName = childName;
                    String flatClassName = getPropertyDescriptor(mappingMetaData.getClassName(beanTypeName), childName).getPropertyType().getName();
                    childMetadata = parseInternal(child, parserContext, childName, flatClassName);
                } else {
                    childMetadata = tryParseNestedPropertyViaIntrospection(beanMetadata, className, child, parserContext);
                    propertyName = childName;
                }
                if (childMetadata == null && !isList) {
                    String text = getElementText(child);
                    if (text != null) {
                        MutableValueMetadata m = parserContext.createMetadata(MutableValueMetadata.class);
                        m.setStringValue(text.trim());
                        childMetadata = m;
                    }


//                    propertyName = mappingMetaData.getPropertyName(beanTypeName, childName);
//                    NodeList childNodes = child.getChildNodes();
//                    StringBuilder buf = new StringBuilder();
//                    for (int j = 0; j < childNodes.getLength(); j++) {
//                        Node childNode = childNodes.item(j);
//                        if (childNode instanceof Element) {
//                            Element childElement = (Element) childNode;
//                            if (namespace.equals(childElement.getNamespaceURI())) {
//                                childMetadata = parse(childElement, parserContext);
//                            } else {
//                                try {
//                                    childMetadata = parserContext.parseElement(BeanMetadata.class, beanMetaData, childElement);
//                                } catch (Exception e) {
//                                    childMetadata = parserContext.parseElement(ValueMetadata.class, beanMetaData, childElement);
//                                }
//                            }
//
//                            break;
//                        } else if (childNode instanceof Text) {
//                            String value = childNode.getNodeValue();
//                            buf.append(value);
//                        }
//                    }
//                    if (childMetadata == null) {
//                        MutableValueMetadata m = parserContext.createMetadata(MutableValueMetadata.class);
//                        m.setStringValue(buf.toString().trim());
//                        childMetadata = m;
//                    }
                }
                if (childMetadata != null) {
                    beanMetadata.addProperty(propertyName, childMetadata);
                }
            }
        }
    }

    private Metadata parseChildExtensionBean(Element child, MutableBeanMetadata beanMetadata, ParserContext parserContext) {
        NodeList nl = child.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element childElement = (Element) node;
                String uri = childElement.getNamespaceURI();
                String localName = childElement.getLocalName();
                Metadata value = parserContext.parseElement(Metadata.class, beanMetadata, childElement);
                if (value != null) {
                    return value;
                }
                //TODO ARIES-111
//                if (uri == null ||
//                        uri.equals(BLUEPRINT_NAMESPACE)) {
//                    if ("bean".equals(localName)) {
//                        return parserContext.parseElement(BeanMetadata.class, beanMetadata, childElement);
//                    } else {
//                        return parserContext.parseElement(ValueMetadata.class, beanMetadata, childElement);
//                    }
//                } else {
//                    Metadata value = parse(childElement, parserContext);
//                    if (value != null) {
//                        return value;
//                    }
//                }
            }
        }
        return null;
    }

    private Metadata tryParseNestedPropertyViaIntrospection(MutableBeanMetadata beanMetadata, String className, Element element, ParserContext parserContext) {
        String localName = element.getLocalName();
        PropertyDescriptor descriptor = getPropertyDescriptor(className, localName);
        if (descriptor != null) {
            return parseNestedPropertyViaIntrospection(beanMetadata, element, descriptor.getName(), descriptor.getPropertyType(), parserContext);
        } else {
            return parseNestedPropertyViaIntrospection(beanMetadata, element, localName, Object.class, parserContext);
        }
    }

    private Metadata parseNestedPropertyViaIntrospection(MutableBeanMetadata beanMetadata, Element element, String propertyName, Class propertyType, ParserContext parserContext) {
        if (isMap(propertyType)) {
            return parseCustomMapElement(beanMetadata, element, propertyName, parserContext);
        } else if (isCollection(propertyType)) {
            return parserContext.parseElement(CollectionMetadata.class, beanMetadata, element);
        } else {
            return parseChildExtensionBean(element, beanMetadata, parserContext);
        }
    }

    private boolean isMap(Class type) {
        return Map.class.isAssignableFrom(type);
    }

    /**
     * Returns true if the given type is a collection type or an array
     */
    private boolean isCollection(Class type) {
        return type.isArray() || Collection.class.isAssignableFrom(type);
    }

    protected String getLocalName(Element element) {
        String localName = element.getLocalName();
        if (localName == null) {
            localName = element.getNodeName();
        }
        return localName;
    }

    protected Metadata parseCustomMapElement(MutableBeanMetadata beanMetadata, Element element, String name, ParserContext parserContext) {
        MutableMapMetadata map = parserContext.createMetadata(MutableMapMetadata.class);

        Element parent = (Element) element.getParentNode();
        String entryName = mappingMetaData.getMapEntryName(getLocalName(parent), name);
        String keyName = mappingMetaData.getMapKeyName(getLocalName(parent), name);
        String dups = mappingMetaData.getMapDupsMode(getLocalName(parent), name);
        boolean flat = mappingMetaData.isFlatMap(getLocalName(parent), name);
        String defaultKey = mappingMetaData.getMapDefaultKey(getLocalName(parent), name);

        if (entryName == null) entryName = "property";
        if (keyName == null) keyName = "key";
        if (dups == null) dups = "replace";

        // TODO : support further customizations
        //String valueName = "value";
        //boolean keyIsAttr = true;
        //boolean valueIsAttr = false;
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element childElement = (Element) node;

                String localName = childElement.getLocalName();
                String uri = childElement.getNamespaceURI();
                if (localName == null || localName.equals("xmlns") || localName.startsWith("xmlns:")) {
                    continue;
                }

                // we could use namespaced attributes to differentiate real spring
                // attributes from namespace-specific attributes
                if (!flat && !isEmpty(uri) && localName.equals(entryName)) {
                    String key = childElement.getAttributeNS(uri, keyName);
                    if (key == null || key.length() == 0) {
                        key = defaultKey;
                    }
                    if (key == null) {
                        throw new RuntimeException("No key defined for map " + entryName);
                    }

                    NonNullMetadata keyValue = (NonNullMetadata) getValue(key, mappingMetaData.getPropertyEditor(localName, key), parserContext);

                    Element valueElement = getFirstChildElement(childElement);
                    Metadata value;
                    if (valueElement != null) {
                        value = parserContext.parseElement(Metadata.class, beanMetadata, valueElement);
//                        String valueElUri = valueElement.getNamespaceURI();
//                        String valueElLocalName = valueElement.getLocalName();
//                        if (valueElUri == null ||
//                                valueElUri.equals(BLUEPRINT_NAMESPACE)) {
//                            if ("bean".equals(valueElLocalName)) {
//                                value = parserContext.parseElement(BeanMetadata.class, beanMetadata, valueElement);
//                            } else {
//                                value = parserContext.parseElement(BeanProperty.class, beanMetadata, valueElement).getValue();
//                            }
//                        } else {
//                            value = parserContext.parseElement(ValueMetadata.class, beanMetadata, valueElement);
//                        }
                    } else {
                        value = getValue(getElementText(childElement), mappingMetaData.getPropertyEditor(localName, key), parserContext);
                    }

                    addValueToMap(map, keyValue, value, dups, parserContext);
                } else if (flat && !isEmpty(uri)) {
                    String key = childElement.getAttributeNS(uri, keyName);
                    if (key == null || key.length() == 0) {
                        key = defaultKey;
                    }
                    if (key == null) {
                        throw new RuntimeException("No key defined for map entry " + entryName);
                    }
                    NonNullMetadata keyValue = (NonNullMetadata) getValue(key, mappingMetaData.getPropertyEditor(localName, key), parserContext);
                    childElement = cloneElement(childElement);
                    childElement.removeAttributeNS(uri, keyName);
                    Metadata bdh = parse(childElement, parserContext);
                    addValueToMap(map, keyValue, bdh, dups, parserContext);
                }
            }
        }
        return map;
    }

    /**
     * Creates a clone of the element and its attribute (though not its content)
     */
    protected Element cloneElement(Element element) {
        Element answer = element.getOwnerDocument().createElementNS(element.getNamespaceURI(), element.getNodeName());
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0, size = attributes.getLength(); i < size; i++) {
            Attr attribute = (Attr) attributes.item(i);
            String uri = attribute.getNamespaceURI();
            answer.setAttributeNS(uri, attribute.getName(), attribute.getNodeValue());
        }
        return answer;
    }


    protected void addValueToMap(MutableMapMetadata map, NonNullMetadata keyValue, Metadata value, String dups, ParserContext parserContext) {
        if (hasKey(map, keyValue)) {
            if ("discard".equalsIgnoreCase(dups)) {
                // Do nothing
            } else if ("replace".equalsIgnoreCase(dups)) {
                map.addEntry(keyValue, value);
            } else if ("allow".equalsIgnoreCase(dups)) {
                MutableCollectionMetadata l = parserContext.createMetadata(MutableCollectionMetadata.class);
                l.addValue(get(map, keyValue));
                l.addValue(value);
                map.addEntry(keyValue, l);
            } else if ("always".equalsIgnoreCase(dups)) {
                MutableCollectionMetadata l = (MutableCollectionMetadata) get(map, keyValue);
                l.addValue(value);
            }
        } else {
            if ("always".equalsIgnoreCase(dups)) {
                MutableCollectionMetadata l = (MutableCollectionMetadata) get(map, keyValue);
                if (l == null) {
                    l = parserContext.createMetadata(MutableCollectionMetadata.class);
                    map.addEntry(keyValue, l);
                }
                l.addValue(value);
            } else {
                map.addEntry(keyValue, value);
            }
        }
    }

    private Metadata get(MutableMapMetadata map, NonNullMetadata keyValue) {
        for (MapEntry entry : map.getEntries()) {
            if (equals(entry.getKey(), keyValue)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean equals(NonNullMetadata key1, NonNullMetadata key2) {
        if (key1 == key2) return true;
        if (key1.getClass() != key2.getClass()) return false;
        if (key1 instanceof RefMetadata) return ((RefMetadata) key1).getComponentId().equals(((RefMetadata) key2).getComponentId());
        if (key1 instanceof ReferenceMetadata) {
            if (((ReferenceMetadata) key1).getTimeout() != ((ReferenceMetadata) key2).getTimeout()) return false;
        }
        if (key1 instanceof ServiceReferenceMetadata) {
            ServiceReferenceMetadata sr1 = (ServiceReferenceMetadata) key1;
            ServiceReferenceMetadata sr2 = (ServiceReferenceMetadata) key2;
            return sr1.getAvailability() == sr2.getAvailability()
                    && sr1.getInterface().equals(sr2.getInterface())
                    && sr1.getComponentName().equals(sr2.getComponentName())
                    && sr1.getFilter().equals(sr2.getFilter())
                    && sr1.getReferenceListeners().equals(sr2.getReferenceListeners())

                    && sr1.getId().equals(sr2.getId())
                    && sr1.getActivation() == sr2.getActivation()
                    && sr1.getDependsOn().equals(sr2.getDependsOn());
        }
        if (key1 instanceof ValueMetadata) {
            ValueMetadata v1 = (ValueMetadata) key1;
            ValueMetadata v2 = (ValueMetadata) key2;
            if (v1.getStringValue() != null ? v1.getStringValue().equals(v2.getStringValue()) : v2.getStringValue() == null
                    && v1.getType() != null ? v1.getType().equals(v2.getType()) : v2.getType() == null) {
                return true;
            }
        }
        return false;
    }

    private boolean hasKey(MutableMapMetadata map, NonNullMetadata keyValue) {
        return get(map, keyValue) != null;
    }

    protected boolean isEmpty(String uri) {
        return uri == null || uri.length() == 0;
    }

    protected Metadata getValue(String value, String propertyEditorName, ParserContext parserContext) {
        if (value == null) return null;

        //
        // If value is #null then we are explicitly setting the value null instead of an empty string
        //
        if (NULL_REFERENCE.equals(value)) {
            return parserContext.createMetadata(NullMetadata.class);
        }

        //
        // If value starts with # then we have a ref
        //
        if (value.startsWith(BEAN_REFERENCE_PREFIX)) {
            // strip off the #
            value = value.substring(BEAN_REFERENCE_PREFIX.length());

            // if the new value starts with a #, then we had an excaped value (e.g. ##value)
            if (!value.startsWith(BEAN_REFERENCE_PREFIX)) {
                MutableRefMetadata ref = parserContext.createMetadata(MutableRefMetadata.class);
                ref.setComponentId(value);
                return ref;
            }
        }

//        if( propertyEditor!=null ) {
//        	PropertyEditor p = createPropertyEditor(propertyEditor);
//
//        	RootBeanDefinition def = new RootBeanDefinition();
//        	def.setBeanClass(PropertyEditorFactory.class);
//        	def.getPropertyValues().addPropertyValue("propertyEditor", p);
//        	def.getPropertyValues().addPropertyValue("value", value);
//
//        	return def;
//        }

        //
        // Neither null nor a reference
        //
        if (propertyEditorName != null) {
            MutableBeanMetadata factory = parserContext.createMetadata(MutableBeanMetadata.class);
            factory.setRuntimeClass(propertyEditors.get(propertyEditorName));

            MutableValueMetadata metadata = parserContext.createMetadata(MutableValueMetadata.class);
            metadata.setStringValue(value);
            factory.addProperty("asText", metadata);
            
            MutableBeanMetadata bean = parserContext.createMetadata(MutableBeanMetadata.class);
            bean.setFactoryComponent(factory);
            bean.setFactoryMethod("getValue");
            return bean;
        }
        MutableValueMetadata metadata = parserContext.createMetadata(MutableValueMetadata.class);
        metadata.setStringValue(value);
        return metadata;
    }

    protected Element getFirstChildElement(Element element) {
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                return (Element) node;
            }
        }
        return null;
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
        StringBuilder buffer = new StringBuilder();
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
