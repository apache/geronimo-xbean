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
package org.apache.xbean.spring.context.v2;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.SystemPropertyUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XBeanBeanDefinitionDocumentReader extends DefaultBeanDefinitionDocumentReader {

    protected BeanDefinitionParserDelegate createHelper(XmlReaderContext readerContext, Element root) {
        BeanDefinitionParserDelegate delegate = XBeanV2Helper.createParser(readerContext);
        delegate.initDefaults(root);
        return delegate;
    }

    protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
        String namespaceUri = root.getNamespaceURI();
        if (!DomUtils.nodeNameEquals(root, "beans") && 
            !delegate.isDefaultNamespace(namespaceUri)) {
            try {
                try {
                    Method m = BeanDefinitionParserDelegate.class.getMethod("parseCustomElement", new Class[] { Element.class });
                    m.invoke(delegate, new Object[] { root });
                } catch (NoSuchMethodException e) {
                    try {
                        Method m = BeanDefinitionParserDelegate.class.getMethod("parseCustomElement", new Class[] { Element.class, boolean.class });
                        m.invoke(delegate, new Object[] { root, Boolean.FALSE });
                    } catch (NoSuchMethodException e2) {
                        throw new IllegalStateException(e);
                    }
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
                throw new RuntimeException(e);
            }
        } else if (DomUtils.nodeNameEquals(root, "beans")) {
            NodeList nl = root.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                Node node = nl.item(i);
                if (node instanceof Element) {
                    Element ele = (Element) node;
                    String childNamespaceUri = ele.getNamespaceURI();
                    if (delegate.isDefaultNamespace(childNamespaceUri)) {
                        parseDefaultElement(ele, delegate);
                    }
                    else {
                        delegate.parseCustomElement(ele);
                    }
                }
            }
        } else {
            super.parseBeanDefinitions(root, delegate);
        }
    }
    
    private void parseDefaultElement(Element ele, BeanDefinitionParserDelegate delegate) {
        if (DomUtils.nodeNameEquals(ele, IMPORT_ELEMENT)) {
            importBeanDefinitionResource(ele);
        }
        else if (DomUtils.nodeNameEquals(ele, ALIAS_ELEMENT)) {
            processAliasRegistration(ele);
        }
        else if (DomUtils.nodeNameEquals(ele, BEAN_ELEMENT)) {
            processBeanDefinition(ele, delegate);
        }
    }

    /**
     * Parse an "import" element and load the bean definitions
     * from the given resource into the bean factory.
     */
    protected void importBeanDefinitionResource(Element ele) {
        String location = ele.getAttribute(RESOURCE_ATTRIBUTE);
        if (!StringUtils.hasText(location)) {
            getReaderContext().error("Resource location must not be empty", ele);
            return;
        }

        // Resolve system properties: e.g. "${user.dir}"
        location = SystemPropertyUtils.resolvePlaceholders(location);

        if (ResourcePatternUtils.isUrl(location)) {
            int importCount = getReaderContext().getReader().loadBeanDefinitions(location);
            if (logger.isDebugEnabled()) {
                logger.debug("Imported " + importCount + " bean definitions from URL location [" + location + "]");
            }
        }
        else {
            // No URL -> considering resource location as relative to the current file.
            try {
                Resource relativeResource = getReaderContext().getResource().createRelative(location);
                int importCount = getReaderContext().getReader().loadBeanDefinitions(relativeResource);
                if (logger.isDebugEnabled()) {
                    logger.debug("Imported " + importCount + " bean definitions from relative location [" + location + "]");
                }
            }
            catch (IOException ex) {
                getReaderContext().error(
                        "Invalid relative resource location [" + location + "] to import bean definitions from", ele, null, ex);
            }
        }

        getReaderContext().fireImportProcessed(location, extractSource(ele));
    }

    /**
     * Process the given alias element, registering the alias with the registry.
     */
    protected void processAliasRegistration(Element ele) {
        String name = ele.getAttribute(NAME_ATTRIBUTE);
        String alias = ele.getAttribute(ALIAS_ATTRIBUTE);
        boolean valid = true;
        if (!StringUtils.hasText(name)) {
            getReaderContext().error("Name must not be empty", ele);
            valid = false;
        }
        if (!StringUtils.hasText(alias)) {
            getReaderContext().error("Alias must not be empty", ele);
            valid = false;
        }
        if (valid) {
            try {
                getReaderContext().getRegistry().registerAlias(name, alias);
            }
            catch (BeanDefinitionStoreException ex) {
                getReaderContext().error(ex.getMessage(), ele);
            }
            getReaderContext().fireAliasRegistered(name, alias, extractSource(ele));
        }
    }

    /**
     * Process the given bean element, parsing the bean definition
     * and registering it with the registry.
     */
    protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
        BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
        if (bdHolder != null) {
            bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
            // Register the final decorated instance.
            BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
            // Send registration event.
            getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
        }
    }


}
