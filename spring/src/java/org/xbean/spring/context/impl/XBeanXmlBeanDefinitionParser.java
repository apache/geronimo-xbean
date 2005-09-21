/**
 * 
 * Copyright 2005 LogicBlaze, Inc. http://www.logicblaze.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 * 
 **/
package org.xbean.spring.context.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.DefaultXmlBeanDefinitionParser;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * An enhanced XML parser capable of handling custom XML schemas.
 * 
 * @author James Strachan
 * @version $Revision: 1.1 $
 */
public class XBeanXmlBeanDefinitionParser extends DefaultXmlBeanDefinitionParser {
    public static final String META_INF_PREFIX = "META-INF/services/org/xbean/spring/";

    private static final Log log = LogFactory.getLog(XBeanXmlBeanDefinitionParser.class);

    /**
     * All the reserved Spring XML element names which cannot be overloaded by
     * an XML extension
     */
    protected static final String[] RESERVED_ELEMENT_NAMES = { "beans", DESCRIPTION_ELEMENT, IMPORT_ELEMENT, ALIAS_ELEMENT, BEAN_ELEMENT,
            CONSTRUCTOR_ARG_ELEMENT, PROPERTY_ELEMENT, LOOKUP_METHOD_ELEMENT, REPLACED_METHOD_ELEMENT, ARG_TYPE_ELEMENT, REF_ELEMENT, IDREF_ELEMENT,
            VALUE_ELEMENT, NULL_ELEMENT, LIST_ELEMENT, SET_ELEMENT, MAP_ELEMENT, ENTRY_ELEMENT, KEY_ELEMENT, PROPS_ELEMENT, PROP_ELEMENT };

    protected static final String[] RESERVED_BEAN_ATTRIBUTE_NAMES = { ID_ATTRIBUTE, NAME_ATTRIBUTE, CLASS_ATTRIBUTE, PARENT_ATTRIBUTE, DEPENDS_ON_ATTRIBUTE,
            FACTORY_METHOD_ATTRIBUTE, FACTORY_BEAN_ATTRIBUTE, DEPENDENCY_CHECK_ATTRIBUTE, AUTOWIRE_ATTRIBUTE, INIT_METHOD_ATTRIBUTE, DESTROY_METHOD_ATTRIBUTE,
            ABSTRACT_ATTRIBUTE, SINGLETON_ATTRIBUTE, LAZY_INIT_ATTRIBUTE };

    private static final String JAVA_PACKAGE_PREFIX = "java://";

    private Set reservedElementNames = new HashSet(Arrays.asList(RESERVED_ELEMENT_NAMES));
    private Set reservedBeanAttributeNames = new HashSet(Arrays.asList(RESERVED_BEAN_ATTRIBUTE_NAMES));

    /**
     * Configures the XmlBeanDefinitionReader to work nicely with extensible XML
     * using this reader implementation.
     */
    public static void configure(XmlBeanDefinitionReader reader) {
        reader.setValidating(false);
        reader.setNamespaceAware(true);
        reader.setParserClass(XBeanXmlBeanDefinitionParser.class);
    }

    /**
     * Parses the non-standard XML element as a Spring bean definition
     */
    protected BeanDefinitionHolder parseBeanFromExtensionElement(Element element) {
        String uri = element.getNamespaceURI();
        String localName = element.getLocalName();

        MappingMetaData metadata = findNamespaceProperties(uri, localName);
        if (metadata != null) {
            // lets see if we configured the localName to a bean class
            String className = metadata.getClassName(localName);
            if (className != null) {
                // lets assume the class name == the package name plus the
                element.setAttributeNS(null, "class", className);
                BeanDefinitionHolder definition = parseBeanDefinitionElement(element, false);
                addAttributeProperties(definition, metadata, className, element);
                addNestedPropertyElements(definition, metadata, className, element);
                return definition;
            }
        }
        return null;
    }

    /**
     * Parses attribute names and values as being bean property expressions
     */
    protected void addAttributeProperties(BeanDefinitionHolder definition, MappingMetaData metadata, String className, Element element) {
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0, size = attributes.getLength(); i < size; i++) {
            Attr attribute = (Attr) attributes.item(i);
            String uri = attribute.getNamespaceURI();
            String localName = attribute.getLocalName();

            if (localName == null || localName.equals("xmlns") || localName.startsWith("xmlns:")) {
                continue;
            }

            // we could use namespaced attributes to differentiate real spring
            // attributes from namespace-specific attributes
            if (((isEmpty(uri)) && !reservedBeanAttributeNames.contains(localName)) || (!isEmpty(uri) && !uri.equals("http://www.w3.org/2000/xmlns/"))) {
                addAttributeProperty(definition, metadata, element, attribute);
            }
        }
    }

    protected void addAttributeProperty(BeanDefinitionHolder definition, MappingMetaData metadata, Element element, Attr attribute) {
        String localName = attribute.getName();
        String value = attribute.getValue();
        if (value != null) {
            String propertyName = metadata.getPropertyName(element.getLocalName(), localName);
            if (propertyName != null) {
                definition.getBeanDefinition().getPropertyValues().addPropertyValue(propertyName, value);
            }
        }
    }

    /**
     * Lets iterate through the children of this element and create any nested
     * child properties
     */
    protected void addNestedPropertyElements(BeanDefinitionHolder definition, MappingMetaData metadata, String className, Element element) {
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element childElement = (Element) node;
                String uri = childElement.getNamespaceURI();
                String localName = childElement.getLocalName();

                if (!isEmpty(uri) || !reservedElementNames.contains(localName)) {
                    // we could be one of the following
                    // * the child element maps to a <property> tag with inner
                    // tags being the bean
                    // * the child element maps to a <property><list> tag with
                    // inner tags being the contents of the list
                    // * the child element maps to a <property> tag and is the
                    // bean tag too
                    Object value = null;
                    String propertyName = metadata.getNestedListProperty(element.getLocalName(), localName);
                    if (propertyName != null) {
                        value = parseListElement(childElement, propertyName);
                    }
                    else {
                        propertyName = metadata.getNestedProperty(element.getLocalName(), localName);
                        if (propertyName != null) {
                            // lets find the first child bean that parses fine
                            value = parseChildExtensionBean(childElement);
                        }
                    }
                    if (propertyName == null) {
                        value = tryParseNestedPropertyViaIntrospection(metadata, className, childElement);
                        propertyName = localName;
                    }
                    if (value != null) {
                        definition.getBeanDefinition().getPropertyValues().addPropertyValue(propertyName, value);
                    }
                }
            }
        }
    }

    /**
     * Attempts to use introspection to parse the nested property element.
     */
    protected Object tryParseNestedPropertyViaIntrospection(MappingMetaData metadata, String className, Element element) {
        Class type = null;
        String localName = element.getLocalName();
        try {
            type = loadClass(className);
        }
        catch (ClassNotFoundException e) {
            throw new BeanDefinitionStoreException("Failed to load type: " + className + ". Reason: " + e, e);
        }
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(type);
            if (beanInfo != null) {
                PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();
                for (int i = 0; i < descriptors.length; i++) {
                    PropertyDescriptor descriptor = descriptors[i];
                    if (descriptor.getWriteMethod() != null) {
                        String name = descriptor.getName();
                        if (name.equals(localName)) {
                            return parseNestedPropertyViaIntrospection(metadata, className, element, descriptor);
                        }
                    }
                }
            }
        }
        catch (IntrospectionException e) {
            throw new BeanDefinitionStoreException("Failed to introspect type: " + className + ". Reason: " + e, e);
        }
        return null;
    }

    /**
     * Attempts to use introspection to parse the nested property element.
     */
    protected Object parseNestedPropertyViaIntrospection(MappingMetaData metadata, String className, Element element, PropertyDescriptor descriptor) {
        String name = descriptor.getName();
        if (isCollection(descriptor.getPropertyType())) {
            return parseListElement(element, name);
        }
        else {
            return parseChildExtensionBean(element);
        }
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

                if (!isEmpty(uri) || !reservedElementNames.contains(localName)) {
                    Object value = parseBeanFromExtensionElement(childElement);
                    if (value != null) {
                        return value;
                    }
                }
                else if (isEmpty(uri)) {
                    if (BEAN_ELEMENT.equals(localName)) {
                        return parseBeanDefinitionElement(childElement, true);
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

        String uri = META_INF_PREFIX + createDiscoveryPathName(namespaceURI, localName);
        InputStream in = loadResource(uri);
        if (in == null) {
            if (namespaceURI != null && namespaceURI.length() > 0) {
                in = loadResource(META_INF_PREFIX + createDiscoveryPathName(namespaceURI));
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
        }
        return null;
    }

    /**
     * Loads the resource from the given URI
     */
    protected InputStream loadResource(String uri) {
        // lets try the thread context class loader first
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(uri);
        if (in == null) {
            in = getClass().getClassLoader().getResourceAsStream(uri);
            if (in == null) {
                logger.debug("Could not find resource: " + uri);
            }
        }
        return in;
    }

    /**
     * Converts the namespace and localName into a valid path name we can use on
     * the classpath to discover a text file
     */
    protected String createDiscoveryPathName(String uri, String localName) {
        if (isEmpty(uri)) {
            return localName;
        }
        return createDiscoveryPathName(uri) + "/" + localName;
    }

    /**
     * Converts the namespace and localName into a valid path name we can use on
     * the classpath to discover a text file
     */
    protected String createDiscoveryPathName(String uri) {
        // TODO proper encoding required
        // lets replace any dodgy characters
        return uri.replaceAll("://", "/").replace(':', '/').replace(' ', '_');
    }

    /**
     * protected void preprocessXml(BeanDefinitionReader reader, Element root,
     * Resource resource) throws BeanDefinitionStoreException { String localName =
     * root.getNodeName(); String uri = root.getNamespaceURI(); boolean
     * extensible = true; if (uri == null || uri.length() == 0) { if
     * (reservedElementNames.contains(localName)) { extensible = false; } } if
     * (extensible) { // lets see if we have a custom XML processor
     * ElementProcessor handler = findElementProcessor(uri, localName); if
     * (handler != null) { handler.processElement(root, reader, resource); } } //
     * lets recurse into any children NodeList nl = root.getChildNodes(); for
     * (int i = 0; i < nl.getLength(); i++) { Node node = nl.item(i); if (node
     * instanceof Element) { Element element = (Element) node;
     * preprocessXml(reader, element, resource); } } }
     */

    /**
     * Attempts to load the class on the current thread context class loader or
     * the class loader which loaded us
     */
    protected Class loadClass(String name) throws ClassNotFoundException {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(name);
        }
        catch (ClassNotFoundException e) {
            return getClass().getClassLoader().loadClass(name);
        }
    }

    protected boolean isEmpty(String uri) {
        return uri == null || uri.length() == 0;
    }

    // -------------------------------------------------------------------------
    //
    // TODO we could apply the following patches into the Spring code -
    // though who knows if it'll ever make it into a release! :)
    // 
    // -------------------------------------------------------------------------
    protected int parseBeanDefinitions(Element root) throws BeanDefinitionStoreException {
        NodeList nl = root.getChildNodes();
        int beanDefinitionCount = 0;
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
                    BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getBeanDefinitionReader().getBeanFactory());
                }
                else {
                    BeanDefinitionHolder bdHolder = parseBeanFromExtensionElement(ele);
                    if (bdHolder != null) {
                        beanDefinitionCount++;
                        BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getBeanDefinitionReader().getBeanFactory());
                    }
                    else {
                        log.debug("Ignoring unknown element namespace: " + ele.getNamespaceURI() + " localName: " + ele.getLocalName());
                    }
                }
            }
        }
        return beanDefinitionCount;
    }

    protected Object parsePropertySubElement(Element element, String beanName) throws BeanDefinitionStoreException {
        String uri = element.getNamespaceURI();
        String localName = element.getLocalName();

        if (!isEmpty(uri) || !reservedElementNames.contains(localName)) {
            Object answer = parseBeanFromExtensionElement(element);
            if (answer != null) {
                return answer;
            }
        }
        return super.parsePropertySubElement(element, beanName);
    }

}
