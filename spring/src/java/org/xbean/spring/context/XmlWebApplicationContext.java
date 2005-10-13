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
package org.xbean.spring.context;

import java.util.List;
import java.util.Collections;
import java.io.IOException;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.xbean.spring.context.impl.XBeanXmlBeanDefinitionReader;

/**
 * An XBean version of the regular Spring class to provide improved XML
 * handling.
 * 
 * @author James Strachan
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class XmlWebApplicationContext extends org.springframework.web.context.support.XmlWebApplicationContext implements SpringApplicationContext {
    private final List xmlPreprocessors;

    /**
     * Creates a XmlWebApplicationContext which loads the configuration from the a web application context.
     */
    public XmlWebApplicationContext() {
        this.xmlPreprocessors = Collections.EMPTY_LIST;
    }

    /**
     * Creates a XmlWebApplicationContext which loads the configuration from the a web application context.
     * @param xmlPreprocessors the SpringXmlPreprocessors to apply before passing the xml to Spring for processing
     */
    public XmlWebApplicationContext(List xmlPreprocessors) {
        this.xmlPreprocessors = xmlPreprocessors;
    }

    /**
     * {@inheritDoc}
     */
    protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws IOException {
        XmlBeanDefinitionReader beanDefinitionReader = new XBeanXmlBeanDefinitionReader(this, beanFactory, xmlPreprocessors);

        initBeanDefinitionReader(beanDefinitionReader);

        loadBeanDefinitions(beanDefinitionReader);
    }
}
