/**
 *
 * Copyright 2005 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.xbean.spring.context.impl;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;
import org.xbean.spring.context.SpringApplicationContext;
import org.xbean.spring.context.SpringXmlPreprocessor;

import java.util.Iterator;
import java.util.List;

/**
 * XBeanXmlBeanDefinitionReader extends XmlBeanDefinitionReader adds support for SpringXMLPreprocessors which can
 * modify the DOM before it is passed to Spring for reading.  This allows for extra information to be added into the
 * Spring configuration file that is processed and removed before Spring sees the xml.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class XBeanXmlBeanDefinitionReader extends XmlBeanDefinitionReader {
    private final SpringApplicationContext applicationContext;
    private final List xmlPreprocessors;

    /**
     * Creates a XBeanXmlBeanDefinitionReader for the specified applcationContext and beanFactory which will apply
     * the xmlPreprocessors before passing the DOM to Spring for processing.
     * @param applicationContext the application context for which the bean definitons will be loaded
     * @param beanFactory the beanFactory that services will be loaded
     * @param xmlPreprocessors the preprocessors to apply the DOM before passing to Spring for processing
     */
    public XBeanXmlBeanDefinitionReader(SpringApplicationContext applicationContext, BeanDefinitionRegistry beanFactory, List xmlPreprocessors) {
        super(beanFactory);
        this.applicationContext = applicationContext;
        this.xmlPreprocessors = xmlPreprocessors;
        setValidating(false);
        setNamespaceAware(true);
        setParserClass(XBeanXmlBeanDefinitionParser.class);

        setResourceLoader(applicationContext);
        setEntityResolver(new ResourceEntityResolver(applicationContext));
    }

    /**
     * Gets the application context for which the bean definitons will be loaded.
     * @return the application context for which the bean definitons will be loaded
     */
    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * {@inheritDoc}
     */
    public int registerBeanDefinitions(Document doc, Resource resource) throws BeansException {
        preprocess(doc);
        XBeanXmlBeanDefinitionParser parser = new XBeanXmlBeanDefinitionParser();
        return parser.registerBeanDefinitions(this, doc, resource);
    }

    private void preprocess(Document doc) {
        for (Iterator iterator = xmlPreprocessors.iterator(); iterator.hasNext();) {
            SpringXmlPreprocessor preprocessor = (SpringXmlPreprocessor) iterator.next();
            preprocessor.preprocess(applicationContext, this, doc);
        }
    }

}
