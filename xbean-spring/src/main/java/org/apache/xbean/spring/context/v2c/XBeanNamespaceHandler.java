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
package org.apache.xbean.spring.context.v2c;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.XMLConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xbean.spring.context.impl.MappingMetaData;
import org.apache.xbean.spring.context.impl.NamedConstructorArgs;
import org.apache.xbean.spring.context.impl.NamespaceHelper;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.util.StringUtils;
import org.springframework.core.io.ResourceLoader;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * An enhanced XML parser capable of handling custom XML schemas.
 *
 * @author James Strachan
 * @version $Id$
 * @since 2.0
 */
public class XBeanNamespaceHandler implements NamespaceHandler {

    public static final String SPRING_SCHEMA = "http://xbean.apache.org/schemas/spring/1.0";
    public static final String SPRING_SCHEMA_COMPAT = "http://xbean.org/schemas/spring/1.0";

    private static final Log log = LogFactory.getLog(XBeanNamespaceHandler.class);

    private static final String QNAME_ELEMENT = "qname";
    
    private static final String DESCRIPTION_ELEMENT = "description";

    /**
     * All the reserved Spring XML element names which cannot be overloaded by
     * an XML extension
     */
    protected static final String[] RESERVED_ELEMENT_NAMES = { 
            "beans", 
            DESCRIPTION_ELEMENT, 
            DefaultBeanDefinitionDocumentReader.IMPORT_ELEMENT,
            DefaultBeanDefinitionDocumentReader.ALIAS_ELEMENT, 
            DefaultBeanDefinitionDocumentReader.BEAN_ELEMENT, 
            BeanDefinitionParserDelegate.CONSTRUCTOR_ARG_ELEMENT, 
            BeanDefinitionParserDelegate.PROPERTY_ELEMENT, 
            BeanDefinitionParserDelegate.LOOKUP_METHOD_ELEMENT,
            BeanDefinitionParserDelegate.REPLACED_METHOD_ELEMENT, 
            BeanDefinitionParserDelegate.ARG_TYPE_ELEMENT, 
            BeanDefinitionParserDelegate.REF_ELEMENT, 
            BeanDefinitionParserDelegate.IDREF_ELEMENT, 
            BeanDefinitionParserDelegate.VALUE_ELEMENT, 
            BeanDefinitionParserDelegate.NULL_ELEMENT,
            BeanDefinitionParserDelegate.LIST_ELEMENT, 
            BeanDefinitionParserDelegate.SET_ELEMENT, 
            BeanDefinitionParserDelegate.MAP_ELEMENT, 
            BeanDefinitionParserDelegate.ENTRY_ELEMENT, 
            BeanDefinitionParserDelegate.KEY_ELEMENT, 
            BeanDefinitionParserDelegate.PROPS_ELEMENT, 
            BeanDefinitionParserDelegate.PROP_ELEMENT,
            QNAME_ELEMENT };

    protected static final String[] RESERVED_BEAN_ATTRIBUTE_NAMES = { 
            AbstractBeanDefinitionParser.ID_ATTRIBUTE, 
            BeanDefinitionParserDelegate.NAME_ATTRIBUTE, 
            BeanDefinitionParserDelegate.CLASS_ATTRIBUTE,
            BeanDefinitionParserDelegate.PARENT_ATTRIBUTE, 
            BeanDefinitionParserDelegate.DEPENDS_ON_ATTRIBUTE, 
            BeanDefinitionParserDelegate.FACTORY_METHOD_ATTRIBUTE, 
            BeanDefinitionParserDelegate.FACTORY_BEAN_ATTRIBUTE,
            BeanDefinitionParserDelegate.DEPENDENCY_CHECK_ATTRIBUTE, 
            BeanDefinitionParserDelegate.AUTOWIRE_ATTRIBUTE, 
            BeanDefinitionParserDelegate.INIT_METHOD_ATTRIBUTE, 
            BeanDefinitionParserDelegate.DESTROY_METHOD_ATTRIBUTE,
            BeanDefinitionParserDelegate.ABSTRACT_ATTRIBUTE, 
            BeanDefinitionParserDelegate.SINGLETON_ATTRIBUTE, 
            BeanDefinitionParserDelegate.LAZY_INIT_ATTRIBUTE };

    private static final String JAVA_PACKAGE_PREFIX = "java://";

    private static final String BEAN_REFERENCE_PREFIX = "#";
    private static final String NULL_REFERENCE = "#null";

    private Set reservedElementNames = new HashSet(Arrays.asList(RESERVED_ELEMENT_NAMES));
    private Set reservedBeanAttributeNames = new HashSet(Arrays.asList(RESERVED_BEAN_ATTRIBUTE_NAMES));
    protected final NamedConstructorArgs namedConstructorArgs = new NamedConstructorArgs();

    private ParserContext parserContext;
    
    private XBeanQNameHelper qnameHelper;

    public void init() {
    }

    public BeanDefinition parse(Element element, ParserContext parserContext) {
        this.parserContext = parserContext;
        this.qnameHelper = new XBeanQNameHelper(parserContext.getReaderContext());
        BeanDefinitionHolder holder = parseBeanFromExtensionElement(element);
        // Only register components: i.e. first or seconds level beans (or root element if no <beans> element)
        // a 2nd level could be a nested <beans> from Spring 3.1 onwards
        if (element.getParentNode() == element.getOwnerDocument() ||
                element.getParentNode().getParentNode() == element.getOwnerDocument() ||
                element.getParentNode().getParentNode().getParentNode() == element.getOwnerDocument()) {
            BeanDefinitionReaderUtils.registerBeanDefinition(holder, parserContext.getRegistry());
            BeanComponentDefinition componentDefinition = new BeanComponentDefinition(holder);
            parserContext.getReaderContext().fireComponentRegistered(componentDefinition);
        }
        return holder.getBeanDefinition();
    }

    public BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definition, ParserContext parserContext) {
        if (node instanceof org.w3c.dom.Attr && XMLConstants.XMLNS_ATTRIBUTE.equals(node.getLocalName())) {
            return definition; // Ignore xmlns="xxx" attributes
        }
        throw new IllegalArgumentException("Cannot locate BeanDefinitionDecorator for "
                        + (node instanceof Element ? "element" : "attribute") + " [" +
                        node.getLocalName() + "].");
    }

    /**
     * Configures the XmlBeanDefinitionReader to work nicely with extensible XML
     * using this reader implementation.
     */
    public static void configure(AbstractApplicationContext context, XmlBeanDefinitionReader reader) {
        reader.setNamespaceAware(true);
        reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
    }

    /**
     * Registers whatever custom editors we need
     */
    public static void registerCustomEditors(DefaultListableBeanFactory beanFactory) {
        PropertyEditorRegistrar registrar = new PropertyEditorRegistrar() {
            public void registerCustomEditors(PropertyEditorRegistry registry) {
                registry.registerCustomEditor(java.io.File.class, new org.apache.xbean.spring.context.impl.FileEditor());
                registry.registerCustomEditor(java.net.URI.class, new org.apache.xbean.spring.context.impl.URIEditor());
                registry.registerCustomEditor(java.util.Date.class, new org.apache.xbean.spring.context.impl.DateEditor());
                registry.registerCustomEditor(javax.management.ObjectName.class, new org.apache.xbean.spring.context.impl.ObjectNameEditor());
            }
        };

        beanFactory.addPropertyEditorRegistrar(registrar);
    }

    /**
     * Parses the non-standard XML element as a Spring bean definition
     */
    protected BeanDefinitionHolder parseBeanFromExtensionElement(Element element, String parentClass, String property) {
        String uri = element.getNamespaceURI();
        String localName = getLocalName(element);

        MappingMetaData metadata = findNamespaceProperties(uri, localName);
        if (metadata != null) {
            // lets see if we configured the localName to a bean class
            String className = getPropertyDescriptor(parentClass, property).getPropertyType().getName();
            if (className != null) {
                return parseBeanFromExtensionElement(element, metadata, className);
            }
        }
        return null;
    }

    private BeanDefinitionHolder parseBeanFromExtensionElement(Element element, MappingMetaData metadata, String className) {
        Element original = cloneElement(element);
        // lets assume the class name == the package name plus the
        element.setAttributeNS(null, "class", className);
        addSpringAttributeValues(className, element);
        BeanDefinitionHolder definition = parserContext.getDelegate().parseBeanDefinitionElement(element, null);
        addAttributeProperties(definition, metadata, className, original);
        addContentProperty(definition, metadata, element);
        addNestedPropertyElements(definition, metadata, className, element);
        qnameHelper.coerceNamespaceAwarePropertyValues(definition.getBeanDefinition(), element);
        declareLifecycleMethods(definition, metadata, element);
        resolveBeanClass((AbstractBeanDefinition) definition.getBeanDefinition(), definition.getBeanName());
        namedConstructorArgs.processParameters(definition, metadata);
        return definition;
    }

    protected Class resolveBeanClass(AbstractBeanDefinition bd, String beanName) {
        if (bd.hasBeanClass()) {
            return bd.getBeanClass();
        }
        try {
            ResourceLoader rl = parserContext.getReaderContext().getResourceLoader();
            ClassLoader cl = rl != null ? rl.getClassLoader() : null;
            if (cl == null) {
                cl = parserContext.getReaderContext().getReader().getBeanClassLoader();
            }
            if (cl == null) {
                cl = Thread.currentThread().getContextClassLoader();
            }
            if (cl == null) {
                cl = getClass().getClassLoader();
            }
            return bd.resolveBeanClass(cl);
        }
        catch (ClassNotFoundException ex) {
            throw new BeanDefinitionStoreException(bd.getResourceDescription(),
                    beanName, "Bean class [" + bd.getBeanClassName() + "] not found", ex);
        }
        catch (NoClassDefFoundError err) {
            throw new BeanDefinitionStoreException(bd.getResourceDescription(),
                    beanName, "Class that bean class [" + bd.getBeanClassName() + "] depends on not found", err);
        }
    }

    
    /**
     * Parses the non-standard XML element as a Spring bean definition
     */
    protected BeanDefinitionHolder parseBeanFromExtensionElement(Element element) {
        String uri = element.getNamespaceURI();
        String localName = getLocalName(element);

        MappingMetaData metadata = findNamespaceProperties(uri, localName);
        if (metadata != null) {
            // lets see if we configured the localName to a bean class
            String className = metadata.getClassName(localName);
            if (className != null) {
                return parseBeanFromExtensionElement(element, metadata, className);
            } else {
                throw new BeanDefinitionStoreException("Unrecognized xbean element mapping: " + localName + " in namespace " + uri);
            }
        } else {
            if (uri == null) throw new BeanDefinitionStoreException("Unrecognized Spring element: " + localName);
            else throw new BeanDefinitionStoreException("Unrecognized xbean namespace mapping: " + uri);
        }
    }

    protected void addSpringAttributeValues(String className, Element element) {
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0, size = attributes.getLength(); i < size; i++) {
            Attr attribute = (Attr) attributes.item(i);
            String uri = attribute.getNamespaceURI();
            String localName = attribute.getLocalName();

            if (uri != null && (uri.equals(SPRING_SCHEMA) || uri.equals(SPRING_SCHEMA_COMPAT))) {
                element.setAttributeNS(null, localName, attribute.getNodeValue());
            }
        }
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

    /**
     * Parses attribute names and values as being bean property expressions
     */
    protected void addAttributeProperties(BeanDefinitionHolder definition, MappingMetaData metadata, String className,
            Element element) {
        NamedNodeMap attributes = element.getAttributes();
        // First pass on attributes with no namespaces
        for (int i = 0, size = attributes.getLength(); i < size; i++) {
            Attr attribute = (Attr) attributes.item(i);
            String uri = attribute.getNamespaceURI();
            String localName = attribute.getLocalName();
            // Skip namespaces
            if (localName == null || localName.equals("xmlns") || localName.startsWith("xmlns:")) {
                continue;
            }
            // Add attributes with no namespaces
            if (isEmpty(uri) && !localName.equals("class")) {
                boolean addProperty = true;
                if (reservedBeanAttributeNames.contains(localName)) {
                    // should we allow the property to shine through?
                    PropertyDescriptor descriptor = getPropertyDescriptor(className, localName);
                    addProperty = descriptor != null;
                }
                if (addProperty) {
                    addAttributeProperty(definition, metadata, element, attribute);
                }
            }
        }
        // Second pass on attributes with namespaces
        for (int i = 0, size = attributes.getLength(); i < size; i++) {
            Attr attribute = (Attr) attributes.item(i);
            String uri = attribute.getNamespaceURI();
            String localName = attribute.getLocalName();
            // Skip namespaces
            if (localName == null || localName.equals("xmlns") || localName.startsWith("xmlns:")) {
                continue;
            }
            // Add attributs with namespaces matching the element ns
            if (!isEmpty(uri) && uri.equals(element.getNamespaceURI())) {
                boolean addProperty = true;
                if (reservedBeanAttributeNames.contains(localName)) {
                    // should we allow the property to shine through?
                    PropertyDescriptor descriptor = getPropertyDescriptor(className, localName);
                    addProperty = descriptor != null;
                }
                if (addProperty) {
                    addAttributeProperty(definition, metadata, element, attribute);
                }
            }
        }
    }

    protected void addContentProperty(BeanDefinitionHolder definition, MappingMetaData metadata, Element element) {
        String name = metadata.getContentProperty(getLocalName(element));
        if (name != null) {
            String value = getElementText(element);
            addProperty(definition, metadata, element, name, value);
        }
        else {
            StringBuffer buffer = new StringBuffer();
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
            Enumeration enumeration = properties.propertyNames();
            while (enumeration.hasMoreElements()) {
                String propertyName = (String) enumeration.nextElement();
                String propertyEditor = metadata.getPropertyEditor(getLocalName(element), propertyName);
                
                Object value = getValue(properties.getProperty(propertyName), propertyEditor);
                definition.getBeanDefinition().getPropertyValues().addPropertyValue(propertyName, value);
            }
        }
    }

    protected void addAttributeProperty(BeanDefinitionHolder definition, MappingMetaData metadata, Element element,
            Attr attribute) {
        String localName = attribute.getLocalName();
        String value = attribute.getValue();
        addProperty(definition, metadata, element, localName, value);
    }

    /**
     * Add a property onto the current BeanDefinition.
     */
    protected void addProperty(BeanDefinitionHolder definition, MappingMetaData metadata, Element element,
            String localName, String value) {
        String propertyName = metadata.getPropertyName(getLocalName(element), localName);
        String propertyEditor = metadata.getPropertyEditor(getLocalName(element), propertyName);
        if (propertyName != null) {
            definition.getBeanDefinition().getPropertyValues().addPropertyValue(
                            propertyName, getValue(value,propertyEditor));
        }
    }

    protected Object getValue(String value, String propertyEditor) {
        if (value == null)  return null;

        //
        // If value is #null then we are explicitly setting the value null instead of an empty string
        //
        if (NULL_REFERENCE.equals(value)) {
            return null;
        }

        //
        // If value starts with # then we have a ref
        //
        if (value.startsWith(BEAN_REFERENCE_PREFIX)) {
            // strip off the #
            value = value.substring(BEAN_REFERENCE_PREFIX.length());

            // if the new value starts with a #, then we had an excaped value (e.g. ##value)
            if (!value.startsWith(BEAN_REFERENCE_PREFIX)) {
                return new RuntimeBeanReference(value);
            }
        }

        if( propertyEditor!=null ) {
        	PropertyEditor p = createPropertyEditor(propertyEditor);
        	
        	RootBeanDefinition def = new RootBeanDefinition();
        	def.setBeanClass(PropertyEditorFactory.class);
        	def.getPropertyValues().addPropertyValue("propertyEditor", p);
        	def.getPropertyValues().addPropertyValue("value", value);
        	
        	return def;
        }
        
        //
        // Neither null nor a reference
        //
        return value;
    }

    protected PropertyEditor createPropertyEditor(String propertyEditor) {    	
    	ClassLoader cl = Thread.currentThread().getContextClassLoader();
    	if( cl==null ) {
    		cl = XBeanNamespaceHandler.class.getClassLoader();
    	}
    	
    	try {
    		return (PropertyEditor)cl.loadClass(propertyEditor).newInstance();
    	} catch (Throwable e){
    		throw (IllegalArgumentException)new IllegalArgumentException("Could not load property editor: "+propertyEditor).initCause(e);
    	}
	}

    protected String getLocalName(Element element) {
        String localName = element.getLocalName();
        if (localName == null) {
            localName = element.getNodeName();
        }
        return localName;
    }

    /**
     * Lets iterate through the children of this element and create any nested
     * child properties
     */
    protected void addNestedPropertyElements(BeanDefinitionHolder definition, MappingMetaData metadata,
            String className, Element element) {
        NodeList nl = element.getChildNodes();

        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element childElement = (Element) node;
                String uri = childElement.getNamespaceURI();
                String localName = childElement.getLocalName();

                if (!isDefaultNamespace(uri) || !reservedElementNames.contains(localName)) {
                    // we could be one of the following
                    // * the child element maps to a <property> tag with inner
                    // tags being the bean
                    // * the child element maps to a <property><list> tag with
                    // inner tags being the contents of the list
                    // * the child element maps to a <property> tag and is the
                    // bean tag too
                    // * the child element maps to a <property> tag and is a simple
                    // type (String, Class, int, etc).
                    Object value = null;
                    String propertyName = metadata.getNestedListProperty(getLocalName(element), localName);
                    if (propertyName != null) {
                        value = parseListElement(childElement, propertyName);
                    }
                    else {
                        propertyName = metadata.getFlatCollectionProperty(getLocalName(element), localName);
                        if (propertyName != null) {
                            Object def = parserContext.getDelegate().parseCustomElement(childElement);
                            PropertyValue pv = definition.getBeanDefinition().getPropertyValues().getPropertyValue(propertyName);
                            if (pv != null) {
                                Collection l = (Collection) pv.getValue();
                                l.add(def);
                                continue;
                            } else {
                                ManagedList l = new ManagedList();
                                l.add(def);
                                value = l;
                            }
                        } else {
                            propertyName = metadata.getNestedProperty(getLocalName(element), localName);
                            if (propertyName != null) {
                                // lets find the first child bean that parses fine
                                value = parseChildExtensionBean(childElement);
                            }
                        }
                    }

                    if (propertyName == null && metadata.isFlatProperty(getLocalName(element), localName)) {
                       value = parseBeanFromExtensionElement(childElement, className, localName);
                       propertyName = localName;
                    }

                    if (propertyName == null) {
                        value = tryParseNestedPropertyViaIntrospection(metadata, className, childElement);
                        propertyName = localName;
                    }

                    if (value != null) {
                        definition.getBeanDefinition().getPropertyValues().addPropertyValue(propertyName, value);
                    }
                    else
                    {
                        /**
                         * In this case there is no nested property, so just do a normal
                         * addProperty like we do with attributes.
                         */
                        String text = getElementText(childElement);

                        if (text != null) {
                            addProperty(definition, metadata, element, localName, text);
                        }
                    }
                }
            }
        }
    }

    /**
     * Attempts to use introspection to parse the nested property element.
     */
    protected Object tryParseNestedPropertyViaIntrospection(MappingMetaData metadata, String className, Element element) {
        String localName = getLocalName(element);
        PropertyDescriptor descriptor = getPropertyDescriptor(className, localName);
        if (descriptor != null) {
            return parseNestedPropertyViaIntrospection(metadata, element, descriptor.getName(), descriptor.getPropertyType());
        } else {
            return parseNestedPropertyViaIntrospection(metadata, element, localName, Object.class);
        }
    }

    /**
     * Looks up the property decriptor for the given class and property name
     */
    protected PropertyDescriptor getPropertyDescriptor(String className, String localName) {
        BeanInfo beanInfo = qnameHelper.getBeanInfo(className);
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

    /**
     * Attempts to use introspection to parse the nested property element.
     */
    private Object parseNestedPropertyViaIntrospection(MappingMetaData metadata, Element element, String propertyName, Class propertyType) {
        if (isMap(propertyType)) {
            return parseCustomMapElement(metadata, element, propertyName);
        } else if (isCollection(propertyType)) {
            return parseListElement(element, propertyName);
        } else {
            return parseChildExtensionBean(element);
        }
    }

    protected Object parseListElement(Element element, String name) {
        return parserContext.getDelegate().parseListElement(element, null);
    }

    protected Object parseCustomMapElement(MappingMetaData metadata, Element element, String name) {
        Map map = new ManagedMap();

        Element parent = (Element) element.getParentNode();
        String entryName = metadata.getMapEntryName(getLocalName(parent), name);
        String keyName = metadata.getMapKeyName(getLocalName(parent), name);
        String dups = metadata.getMapDupsMode(getLocalName(parent), name);
        boolean flat = metadata.isFlatMap(getLocalName(parent), name);
        String defaultKey = metadata.getMapDefaultKey(getLocalName(parent), name);

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
                    String key = childElement.getAttribute(keyName);
                    if (key == null || key.length() == 0) {
                        key = defaultKey;
                    }
                    if (key == null) {
                        throw new RuntimeException("No key defined for map " + entryName);
                    }

                    Object keyValue = getValue(key, null);

                    Element valueElement = getFirstChildElement(childElement);
                    Object value;
                    if (valueElement != null) {
                        String valueElUri = valueElement.getNamespaceURI();
                        String valueElLocalName = valueElement.getLocalName();
                        if (valueElUri == null || 
                            valueElUri.equals(SPRING_SCHEMA) || 
                            valueElUri.equals(SPRING_SCHEMA_COMPAT) ||
                            valueElUri.equals(BeanDefinitionParserDelegate.BEANS_NAMESPACE_URI)) {
                            if (BeanDefinitionParserDelegate.BEAN_ELEMENT.equals(valueElLocalName)) {
                                value = parserContext.getDelegate().parseBeanDefinitionElement(valueElement, null);
                            } else {
                                value = parserContext.getDelegate().parsePropertySubElement(valueElement, null);
                            }
                        } else {
                            value = parserContext.getDelegate().parseCustomElement(valueElement);
                        }
                    } else {
                        value = getElementText(childElement);
                    }

                    addValueToMap(map, keyValue, value, dups);
                } else if (flat && !isEmpty(uri)) {
                    String key = childElement.getAttribute(keyName);
                    if (key == null || key.length() == 0) {
                        key = defaultKey;
                    }
                    if (key == null) {
                        throw new RuntimeException("No key defined for map entry " + entryName);
                    }
                    Object keyValue = getValue(key, null);
                    childElement.removeAttribute(keyName);
                    BeanDefinitionHolder bdh = parseBeanFromExtensionElement(childElement);
                    addValueToMap(map, keyValue, bdh, dups);
                }
            }
        }
        return map;
    }
    
    protected void addValueToMap(Map map, Object keyValue, Object value, String dups) {
        if (map.containsKey(keyValue)) {
            if ("discard".equalsIgnoreCase(dups)) {
                // Do nothing
            } else if ("replace".equalsIgnoreCase(dups)) {
                map.put(keyValue, value);
            } else if ("allow".equalsIgnoreCase(dups)) {
                List l = new ManagedList();
                l.add(map.get(keyValue));
                l.add(value);
                map.put(keyValue, l);
            } else if ("always".equalsIgnoreCase(dups)) {
                List l = (List) map.get(keyValue);
                l.add(value);
            }
        } else {
            if ("always".equalsIgnoreCase(dups)) {
                List l = (List) map.get(keyValue);
                if (l == null) {
                    l = new ManagedList();
                    map.put(keyValue, l);
                }
                l.add(value);
            } else {
                map.put(keyValue, value);
            }
        }
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

    protected boolean isMap(Class type) {
        return Map.class.isAssignableFrom(type);
    }

    /**
     * Returns true if the given type is a collection type or an array
     */
    protected boolean isCollection(Class type) {
        return type.isArray() || Collection.class.isAssignableFrom(type);
    }

    /**
     * Iterates the children of this element to find the first nested bean
     */
    protected Object parseChildExtensionBean(Element element) {
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element childElement = (Element) node;
                String uri = childElement.getNamespaceURI();
                String localName = childElement.getLocalName();

                if (uri == null || 
                    uri.equals(SPRING_SCHEMA) || 
                    uri.equals(SPRING_SCHEMA_COMPAT) ||
                    uri.equals(BeanDefinitionParserDelegate.BEANS_NAMESPACE_URI)) {
                    if (BeanDefinitionParserDelegate.BEAN_ELEMENT.equals(localName)) {
                        return parserContext.getDelegate().parseBeanDefinitionElement(childElement, null);
                    } else {
                        return parserContext.getDelegate().parsePropertySubElement(childElement, null);
                    }
                } else {
                    Object value = parserContext.getDelegate().parseCustomElement(childElement);
                    if (value != null) {
                        return value;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Uses META-INF/services discovery to find a Properties file with the XML
     * marshaling configuration
     *
     * @param namespaceURI
     *            the namespace URI of the element
     * @param localName
     *            the local name of the element
     * @return the properties configuration of the namespace or null if none
     *         could be found
     */
    protected MappingMetaData findNamespaceProperties(String namespaceURI, String localName) {
        // lets look for the magic prefix
        if (namespaceURI != null && namespaceURI.startsWith(JAVA_PACKAGE_PREFIX)) {
            String packageName = namespaceURI.substring(JAVA_PACKAGE_PREFIX.length());
            return new MappingMetaData(packageName);
        }

        String uri = NamespaceHelper.createDiscoveryPathName(namespaceURI, localName);
        InputStream in = loadResource(uri);
        if (in == null) {
            if (namespaceURI != null && namespaceURI.length() > 0) {
                uri = NamespaceHelper.createDiscoveryPathName(namespaceURI);
                in = loadResource(uri);
                if (in == null) {
                    uri = NamespaceHelper.createDiscoveryOldPathName(namespaceURI);
                    in = loadResource(uri);
                }
            }
        }

        if (in != null) {
            try {
                Properties properties = new Properties();
                properties.load(in);
                return new MappingMetaData(properties);
            }
            catch (IOException e) {
                log.warn("Failed to load resource from uri: " + uri, e);
            }
            finally {
                try {
                    in.close();
                }
                catch (IOException e) {
                    log.warn("Failed to close resource from uri: " + uri, e);
                }
            }
        }
        return null;
    }

    /**
     * Loads the resource from the given URI
     */
    protected InputStream loadResource(String uri) {
        if (System.getProperty("xbean.dir") != null) {
            File f = new File(System.getProperty("xbean.dir") + uri);
            try {
                return new FileInputStream(f);
            } catch (FileNotFoundException e) {
                // Ignore
            }
        }
        // lets try the thread context class loader first
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(uri);
        if (in == null) {
            ClassLoader cl = parserContext.getReaderContext().getReader().getBeanClassLoader();
            if (cl != null) {
                in = cl.getResourceAsStream(uri);
            }
            if (in == null) {
                in = getClass().getClassLoader().getResourceAsStream(uri);
                if (in == null) {
                    log.debug("Could not find resource: " + uri);
                }
            }
        }
        return in;
    }

    protected boolean isEmpty(String uri) {
        return uri == null || uri.length() == 0;
    }

    protected boolean isDefaultNamespace(String namespaceUri) {
        return (!StringUtils.hasLength(namespaceUri) ||
               BeanDefinitionParserDelegate.BEANS_NAMESPACE_URI.equals(namespaceUri)) ||
               SPRING_SCHEMA.equals(namespaceUri) ||
               SPRING_SCHEMA_COMPAT.equals(namespaceUri);
    }

    protected void declareLifecycleMethods(BeanDefinitionHolder definitionHolder, MappingMetaData metaData,
            Element element) {
        BeanDefinition definition = definitionHolder.getBeanDefinition();
        if (definition instanceof AbstractBeanDefinition) {
            AbstractBeanDefinition beanDefinition = (AbstractBeanDefinition) definition;
            if (beanDefinition.getInitMethodName() == null) {
                beanDefinition.setInitMethodName(metaData.getInitMethodName(getLocalName(element)));
            }
            if (beanDefinition.getDestroyMethodName() == null) {
                beanDefinition.setDestroyMethodName(metaData.getDestroyMethodName(getLocalName(element)));
            }
            if (beanDefinition.getFactoryMethodName() == null) {
                beanDefinition.setFactoryMethodName(metaData.getFactoryMethodName(getLocalName(element)));
            }
        }
    }

    // -------------------------------------------------------------------------
    //
    // TODO we could apply the following patches into the Spring code -
    // though who knows if it'll ever make it into a release! :)
    //
    // -------------------------------------------------------------------------
    /*
    protected int parseBeanDefinitions(Element root) throws BeanDefinitionStoreException {
        int beanDefinitionCount = 0;
        if (isEmpty(root.getNamespaceURI()) || root.getLocalName().equals("beans")) {
            NodeList nl = root.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                Node node = nl.item(i);
                if (node instanceof Element) {
                    Element ele = (Element) node;
                    if (IMPORT_ELEMENT.equals(node.getNodeName())) {
                        importBeanDefinitionResource(ele);
                    }
                    else if (ALIAS_ELEMENT.equals(node.getNodeName())) {
                        String name = ele.getAttribute(NAME_ATTRIBUTE);
                        String alias = ele.getAttribute(ALIAS_ATTRIBUTE);
                        getBeanDefinitionReader().getBeanFactory().registerAlias(name, alias);
                    }
                    else if (BEAN_ELEMENT.equals(node.getNodeName())) {
                        beanDefinitionCount++;
                        BeanDefinitionHolder bdHolder = parseBeanDefinitionElement(ele, false);
                        BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getBeanDefinitionReader()
                                .getBeanFactory());
                    }
                    else {
                        BeanDefinitionHolder bdHolder = parseBeanFromExtensionElement(ele);
                        if (bdHolder != null) {
                            beanDefinitionCount++;
                            BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getBeanDefinitionReader()
                                    .getBeanFactory());
                        }
                        else {
                            log.debug("Ignoring unknown element namespace: " + ele.getNamespaceURI() + " localName: "
                                    + ele.getLocalName());
                        }
                    }
                }
            }
        } else {
            BeanDefinitionHolder bdHolder = parseBeanFromExtensionElement(root);
            if (bdHolder != null) {
                beanDefinitionCount++;
                BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getBeanDefinitionReader()
                        .getBeanFactory());
            }
            else {
                log.debug("Ignoring unknown element namespace: " + root.getNamespaceURI() + " localName: " + root.getLocalName());
            }
        }
        return beanDefinitionCount;
    }

    protected BeanDefinitionHolder parseBeanDefinitionElement(Element ele, boolean isInnerBean) throws BeanDefinitionStoreException {
        
        BeanDefinitionHolder bdh = super.parseBeanDefinitionElement(ele, isInnerBean);
        coerceNamespaceAwarePropertyValues(bdh, ele);
        return bdh;
    }

    protected Object parsePropertySubElement(Element element, String beanName) throws BeanDefinitionStoreException {
        String uri = element.getNamespaceURI();
        String localName = getLocalName(element);

        if ((!isEmpty(uri) && !(uri.equals(SPRING_SCHEMA) || uri.equals(SPRING_SCHEMA_COMPAT)))
                || !reservedElementNames.contains(localName)) {
            Object answer = parseBeanFromExtensionElement(element);
            if (answer != null) {
                return answer;
            }
        }
        if (QNAME_ELEMENT.equals(localName) && isQnameIsOnClassPath()) {
            Object answer = parseQNameElement(element);
            if (answer != null) {
                return answer;
            }
        }
        return super.parsePropertySubElement(element, beanName);
    }

    protected Object parseQNameElement(Element element) {
        return QNameReflectionHelper.createQName(element, getElementText(element));
    }
    */

    /**
     * Returns the text of the element
     */
    protected String getElementText(Element element) {
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
