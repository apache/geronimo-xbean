/**
 * 
 * Copyright 2005-2006 The Apache Software Foundation or its licensors,  as applicable.
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
package org.apache.xbean.spring.context;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.SpringVersion;

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
        // Create a new XmlBeanDefinitionReader for the given BeanFactory.
        XmlBeanDefinitionReader beanDefinitionReader = createBeanDefinitionReader(beanFactory);

        // Configure the bean definition reader with this context's
        // resource loading environment.
        beanDefinitionReader.setResourceLoader(this);
        beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));

        // Allow a subclass to provide custom initialization of the reader,
        // then proceed with actually loading the bean definitions.
        initBeanDefinitionReader(beanDefinitionReader);
        loadBeanDefinitions(beanDefinitionReader);
    }
    
    protected XmlBeanDefinitionReader createBeanDefinitionReader(DefaultListableBeanFactory beanFactory) {
        String version = SpringVersion.getVersion();
        String className = "org.apache.xbean.spring.v" + version.charAt(0) + ".XBeanXmlBeanDefinitionReader";
        try {
            Class cl = Class.forName(className);
            Constructor cstr = cl.getConstructor(new Class[] { SpringApplicationContext.class, BeanDefinitionRegistry.class, List.class });
            return (XmlBeanDefinitionReader) cstr.newInstance(new Object[] { this, beanFactory, xmlPreprocessors });
        } catch (Exception e) {
            throw new IllegalStateException("Could not find valid implementation for: " + version);
        }
    }
}
