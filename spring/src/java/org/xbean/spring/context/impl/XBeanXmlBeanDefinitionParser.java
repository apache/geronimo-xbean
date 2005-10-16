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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.DefaultXmlBeanDefinitionParser;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.AbstractApplicationContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import javax.xml.namespace.QName;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * An enhanced XML parser capable of handling custom XML schemas.
 * 
 * @author James Strachan
 * @version $Id$
 * @since 2.0
 */
public class XBeanXmlBeanDefinitionParser extends DefaultXmlBeanDefinitionParser {

    public static final String SPRING_SCHEMA = "http://xbean.org/schemas/spring/1.0";

    static {
        PropertyEditorHelper.registerCustomEditors();
    }

    private static final Log log = LogFactory.getLog(XBeanXmlBeanDefinitionParser.class);

    /**
     * All the reserved Spring XML element names which cannot be overloaded by
     * an XML extension
     */
    protected static final String[] RESERVED_ELEMENT_NAMES = { "beans", DESCRIPTION_ELEMENT, IMPORT_ELEMENT,
            ALIAS_ELEMENT, BEAN_ELEMENT, CONSTRUCTOR_ARG_ELEMENT, PROPERTY_ELEMENT, LOOKUP_METHOD_ELEMENT,
            REPLACED_METHOD_ELEMENT, ARG_TYPE_ELEMENT, REF_ELEMENT, IDREF_ELEMENT, VALUE_ELEMENT, NULL_ELEMENT,
            LIST_ELEMENT, SET_ELEMENT, MAP_ELEMENT, ENTRY_ELEMENT, KEY_ELEMENT, PROPS_ELEMENT, PROP_ELEMENT };

    protected static final String[] RESERVED_BEAN_ATTRIBUTE_NAMES = { ID_ATTRIBUTE, NAME_ATTRIBUTE, CLASS_ATTRIBUTE,
            PARENT_ATTRIBUTE, DEPENDS_ON_ATTRIBUTE, FACTORY_METHOD_ATTRIBUTE, FACTORY_BEAN_ATTRIBUTE,
            DEPENDENCY_CHECK_ATTRIBUTE, AUTOWIRE_ATTRIBUTE, INIT_METHOD_ATTRIBUTE, DESTROY_METHOD_ATTRIBUTE,
            ABSTRACT_ATTRIBUTE, SINGLETON_ATTRIBUTE, LAZY_INIT_ATTRIBUTE };

    private static final String JAVA_PACKAGE_PREFIX = "java://";

    private static final String BEAN_REFERENCE_SUFFIX = "-ref";

    private Set reservedElementNames = new HashSet(Arrays.asList(RESERVED_ELEMENT_NAMES));
    private Set reservedBeanAttributeNames = new HashSet(Arrays.asList(RESERVED_BEAN_ATTRIBUTE_NAMES));
    protected final NamedConstructorArgs namedConstructorArgs = new NamedConstructorArgs();

    /**
     * Configures the XmlBeanDefinitionReader to work nicely with extensible XML
     * using this reader implementation.
     */
    public static void configure(AbstractApplicationContext context, XmlBeanDefinitionReader reader) {
        reader.setValidating(false);
        reader.setNamespaceAware(true);
        reader.setParserClass(XBeanXmlBeanDefinitionParser.class);
    }

    /**
     * Registers whatever custom editors we need
     */
    public static void registerCustomEditors(DefaultListableBeanFactory beanFactory) {
        PropertyEditorHelper.registerCustomEditors();
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
                Element original = cloneElement(element);
                // lets assume the class name == the package name plus the
                element.setAttributeNS(null, "class", className);
                addSpringAttributeValues(className, element);
                BeanDefinitionHolder definition = parseBeanDefinitionElement(element, false);
                addAttributeProperties(definition, metadata, className, original);
                addNestedPropertyElements(definition, metadata, className, element);
                addInlinedPropertiesFile(definition, metadata, className, element);
                coerceNamespaceAwarePropertyValues(definition, element);
                declareLifecycleMethods(definition, metadata, element);
                namedConstructorArgs.processParameters(definition, metadata);
                return definition;
            }
        }
        return null;
    }

    protected void addSpringAttributeValues(String className, Element element) {
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0, size = attributes.getLength(); i < size; i++) {
            Attr attribute = (Attr) attributes.item(i);
            String uri = attribute.getNamespaceURI();
            String localName = attribute.getLocalName();

            if (uri != null && uri.equals(SPRING_SCHEMA)) {
                element.setAttributeNS(null, localName, attribute.getNodeValue());
            }
        }
    }

    /**
     * Creates a clone of the element and its attribute (though not its content)
     */
    protected Element cloneElement(Element element) {
        Element answer = element.getOwnerDocument().createElement(getLocalName(element));
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
        for (int i = 0, size = attributes.getLength(); i < size; i++) {
            Attr attribute = (Attr) attributes.item(i);
            String uri = attribute.getNamespaceURI();
            String localName = attribute.getLocalName();

            if (localName == null || localName.equals("xmlns") || localName.startsWith("xmlns:")) {
                continue;
            }

            // we could use namespaced attributes to differentiate real spring
            // attributes from namespace-specific attributes
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
    }

    protected void addAttributeProperty(BeanDefinitionHolder definition, MappingMetaData metadata, Element element,
            Attr attribute) {
        String localName = attribute.getName();
        String value = attribute.getValue();
        if (value != null) {
            if (localName.endsWith(BEAN_REFERENCE_SUFFIX)) {
                localName = localName.substring(0, localName.length() - BEAN_REFERENCE_SUFFIX.length());
                String propertyName = metadata.getPropertyName(getLocalName(element), localName);
                if (propertyName != null) {
                    definition.getBeanDefinition().getPropertyValues().addPropertyValue(propertyName,
                            new RuntimeBeanReference(value));
                }
            }
            else {
                String propertyName = metadata.getPropertyName(getLocalName(element), localName);
                if (propertyName != null) {
                    definition.getBeanDefinition().getPropertyValues().addPropertyValue(propertyName, value);
                }
            }
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

                if (!isEmpty(uri) || !reservedElementNames.contains(localName)) {
                    // we could be one of the following
                    // * the child element maps to a <property> tag with inner
                    // tags being the bean
                    // * the child element maps to a <property><list> tag with
                    // inner tags being the contents of the list
                    // * the child element maps to a <property> tag and is the
                    // bean tag too
                    Object value = null;
                    String propertyName = metadata.getNestedListProperty(getLocalName(element), localName);
                    if (propertyName != null) {
                        value = parseListElement(childElement, propertyName);
                    }
                    else {
                        propertyName = metadata.getNestedProperty(getLocalName(element), localName);
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
        String localName = getLocalName(element);
        PropertyDescriptor descriptor = getPropertyDescriptor(className, localName);
        if (descriptor != null) {
            return parseNestedPropertyViaIntrospection(metadata, className, element, descriptor);
        }
        return null;
    }

    /**
     * Parses a Properties file from the text node inside an element and adds
     * the contents as properties of this bean. Only valid for elements
     * containing a single text node and no sub-elements
     */
    protected void addInlinedPropertiesFile(BeanDefinitionHolder definition, MappingMetaData metadata,
            String className, Element element) {
        NodeList childNodes = element.getChildNodes();
        if (childNodes.getLength() == 1 && childNodes.item(0) instanceof Text) {
            Text text = (Text) childNodes.item(0);
            ByteArrayInputStream in = new ByteArrayInputStream(text.getData().getBytes());
            Properties properties = new Properties();
            try {
                properties.load(in);
            }
            catch (IOException e) {
                return;
            }
            Enumeration enumeration = properties.propertyNames();
            while (enumeration.hasMoreElements()) {
                String name = (String) enumeration.nextElement();
                Object value = properties.getProperty(name);
                definition.getBeanDefinition().getPropertyValues().addPropertyValue(name, value);
            }
        }
    }

    /**
     * Any namespace aware property values (such as QNames) need to be coerced
     * while we still have access to the XML Element from which its value comes -
     * so lets do that now before we trash the DOM and just have the bean
     * definition.
     */
    protected void coerceNamespaceAwarePropertyValues(BeanDefinitionHolder definitionHolder, Element element) {
        BeanDefinition definition = definitionHolder.getBeanDefinition();
        if (definition instanceof AbstractBeanDefinition) {
            AbstractBeanDefinition bd = (AbstractBeanDefinition) definition;
            // lets check for any QName types
            BeanInfo beanInfo = getBeanInfo(bd.getBeanClassName());
            if (beanInfo != null) {
                PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();
                for (int i = 0; i < descriptors.length; i++) {
                    PropertyDescriptor descriptor = descriptors[i];
                    if (descriptor.getWriteMethod() != null
                            && descriptor.getPropertyType().isAssignableFrom(QName.class)) {
                        String name = descriptor.getName();
                        MutablePropertyValues propertyValues = bd.getPropertyValues();
                        PropertyValue propertyValue = propertyValues.getPropertyValue(name);
                        if (propertyValue != null) {
                            Object value = propertyValue.getValue();
                            if (value instanceof String) {
                                propertyValues.removePropertyValue(propertyValue);
                                propertyValues.addPropertyValue(name, QNameHelper.createQName(element, (String) value));
                            }
                        }
                    }
                }
            }
        }
    }

    protected BeanInfo getBeanInfo(String className) throws BeanDefinitionStoreException {
        BeanInfo info = null;
        Class type = null;
        try {
            type = loadClass(className);
        }
        catch (ClassNotFoundException e) {
            throw new BeanDefinitionStoreException("Failed to load type: " + className + ". Reason: " + e, e);
        }
        try {
            info = Introspector.getBeanInfo(type);
        }
        catch (IntrospectionException e) {
            throw new BeanDefinitionStoreException("Failed to introspect type: " + className + ". Reason: " + e, e);
        }
        return info;
    }

    /**
     * Looks up the property decriptor for the given class and property name
     */
    protected PropertyDescriptor getPropertyDescriptor(String className, String localName) {
        BeanInfo beanInfo = getBeanInfo(className);
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
    protected Object parseNestedPropertyViaIntrospection(MappingMetaData metadata, String className, Element element,
            PropertyDescriptor descriptor) {
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

        String uri = NamespaceHelper.createDiscoveryPathName(namespaceURI, localName);
        InputStream in = loadResource(uri);
        if (in == null) {
            if (namespaceURI != null && namespaceURI.length() > 0) {
                in = loadResource(NamespaceHelper.createDiscoveryPathName(namespaceURI));
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
     * Attempts to load the class on the current thread context class loader or
     * the class loader which loaded us
     */
    protected Class loadClass(String name) throws ClassNotFoundException {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            try {
                return contextClassLoader.loadClass(name);
            }
            catch (ClassNotFoundException e) {
            }
        }
        return getClass().getClassLoader().loadClass(name);
    }

    protected boolean isEmpty(String uri) {
        return uri == null || uri.length() == 0;
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
    protected int parseBeanDefinitions(Element root) throws BeanDefinitionStoreException {
        int beanDefinitionCount = 0;
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
        return beanDefinitionCount;
    }

    protected Object parsePropertySubElement(Element element, String beanName) throws BeanDefinitionStoreException {
        String uri = element.getNamespaceURI();
        String localName = getLocalName(element);

        if (!isEmpty(uri) || !reservedElementNames.contains(localName)) {
            Object answer = parseBeanFromExtensionElement(element);
            if (answer != null) {
                return answer;
            }
        }
        return super.parsePropertySubElement(element, beanName);
    }
}
