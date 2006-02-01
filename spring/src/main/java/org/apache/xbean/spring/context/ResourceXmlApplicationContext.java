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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.apache.xbean.spring.context.impl.XBeanXmlBeanDefinitionReader;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * An XBean version of a regular Spring ApplicationContext which takes a
 * {@link Resource} as a parameter to load the application context
 * 
 * @author James Strachan
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class ResourceXmlApplicationContext extends AbstractXmlApplicationContext implements SpringApplicationContext {
    private final List xmlPreprocessors;
    private final Resource resource;

    /**
     * Creates a ResourceXmlApplicationContext which loads the configuration from the specified Resource.
     * @param resource the resource from which the configuration is loaded
     */
    public ResourceXmlApplicationContext(Resource resource) {
        this(resource, Collections.EMPTY_LIST);
    }
  
    /**
     * Creates a ResourceXmlApplicationContext which loads the configuration from the specified Resource.
     * @param resource the resource from which the configuration is loaded
     * @param xmlPreprocessors the SpringXmlPreprocessors to apply before passing the xml to Spring for processing
     */
    public ResourceXmlApplicationContext(Resource resource, List xmlPreprocessors) {
        super();
        this.xmlPreprocessors = xmlPreprocessors;
        this.resource = resource;
        refresh();
    }

    public ResourceXmlApplicationContext(Resource resource, ApplicationContext parent) {
        this(resource, Collections.EMPTY_LIST, parent);
    }

    public ResourceXmlApplicationContext(Resource resource,  List xmlPreprocessors, ApplicationContext parent) {
        super(parent);
        this.xmlPreprocessors = xmlPreprocessors;
        this.resource = resource;
        refresh();
    }

    protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws IOException {
        XmlBeanDefinitionReader beanDefinitionReader = new XBeanXmlBeanDefinitionReader(this, beanFactory, xmlPreprocessors);

        initBeanDefinitionReader(beanDefinitionReader);

        loadBeanDefinitions(beanDefinitionReader);
    }

    /**
     * {@inheritDoc}
     */
    protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws BeansException, IOException {
        reader.loadBeanDefinitions(resource);
    }

    /**
     * {@inheritDoc}
     */
    protected String[] getConfigLocations() {
        return null;
    }
}
