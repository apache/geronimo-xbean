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
package org.apache.xbean.spring.context;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.xbean.spring.context.impl.XBeanHelper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;

/**
 * An XBean version of the regular Spring class to provide improved XML
 * handling.
 * 
 * @author James Strachan
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class FileSystemXmlApplicationContext extends org.springframework.context.support.FileSystemXmlApplicationContext implements SpringApplicationContext {
    private final List xmlPreprocessors;

    /**
     * Creates a FileSystemXmlApplicationContext which loads the configuration at the specified location on the file system.
     * @param configLocation the location of the configuration file on the class path
     * @throws BeansException if a problem occurs while reading the configuration
     */
    public FileSystemXmlApplicationContext(String configLocation) throws BeansException {
        this(new String[] {configLocation}, true, null, Collections.EMPTY_LIST);
    }

    /**
     * Creates a FileSystemXmlApplicationContext which loads the configuration at the specified locations on the file system.
     * @param configLocations the locations of the configuration files on the class path
     * @throws BeansException if a problem occurs while reading the configuration
     */
    public FileSystemXmlApplicationContext(String[] configLocations) throws BeansException {
        this(configLocations, true, null, Collections.EMPTY_LIST);
    }

    /**
     * Creates a FileSystemXmlApplicationContext which loads the configuration at the specified locations on the file system.
     * @param configLocations the locations of the configuration files on the class path
     * @param refresh if true the configurations are immedately loaded; otherwise the configurations are not loaded
     * until refresh() is called
     * @throws BeansException if a problem occurs while reading the configuration
     */
    public FileSystemXmlApplicationContext(String[] configLocations, boolean refresh) throws BeansException {
        this(configLocations, refresh, null, Collections.EMPTY_LIST);
    }

    /**
     * Creates a FileSystemXmlApplicationContext which loads the configuration at the specified locations on the file system.
     * @param configLocations the locations of the configuration files on the class path
     * @param parent the parent of this application context
     * @throws BeansException if a problem occurs while reading the configuration
     */
    public FileSystemXmlApplicationContext(String[] configLocations, ApplicationContext parent) throws BeansException {
        this(configLocations, true, parent, Collections.EMPTY_LIST);
    }

    /**
     * Creates a FileSystemXmlApplicationContext which loads the configuration at the specified locations on the file system.
     * @param configLocations the locations of the configuration files on the class path
     * @param refresh if true the configurations are immedately loaded; otherwise the configurations are not loaded
     * until refresh() is called
     * @param parent the parent of this application context
     * @throws BeansException if a problem occurs while reading the configuration
     */
    public FileSystemXmlApplicationContext(String[] configLocations, boolean refresh, ApplicationContext parent) throws BeansException {
        this(configLocations, refresh, parent, Collections.EMPTY_LIST);
    }

    /**
     * Creates a FileSystemXmlApplicationContext which loads the configuration at the specified location on the file system.
     * @param configLocation the location of the configuration file on the class path
     * @param xmlPreprocessors the SpringXmlPreprocessors to apply before passing the xml to Spring for processing
     * @throws BeansException if a problem occurs while reading the configuration
     */
    public FileSystemXmlApplicationContext(String configLocation, List xmlPreprocessors) throws BeansException {
        this(new String[] {configLocation}, true, null, xmlPreprocessors);
    }

    /**
     * Creates a FileSystemXmlApplicationContext which loads the configuration at the specified locations on the file system.
     * @param configLocations the locations of the configuration files on the class path
     * @param xmlPreprocessors the SpringXmlPreprocessors to apply before passing the xml to Spring for processing
     * @throws BeansException if a problem occurs while reading the configuration
     */
    public FileSystemXmlApplicationContext(String[] configLocations, List xmlPreprocessors) throws BeansException {
        this(configLocations, true, null, xmlPreprocessors);
    }

    /**
     * Creates a FileSystemXmlApplicationContext which loads the configuration at the specified locations on the file system.
     * @param configLocations the locations of the configuration files on the class path
     * @param refresh if true the configurations are immedately loaded; otherwise the configurations are not loaded
     * until refresh() is called
     * @param xmlPreprocessors the SpringXmlPreprocessors to apply before passing the xml to Spring for processing
     * @throws BeansException if a problem occurs while reading the configuration
     */
    public FileSystemXmlApplicationContext(String[] configLocations, boolean refresh, List xmlPreprocessors) throws BeansException {
        this(configLocations, refresh, null, xmlPreprocessors);
    }

    /**
     * Creates a FileSystemXmlApplicationContext which loads the configuration at the specified locations on the file system.
     * @param configLocations the locations of the configuration files on the class path
     * @param parent the parent of this application context
     * @param xmlPreprocessors the SpringXmlPreprocessors to apply before passing the xml to Spring for processing
     * @throws BeansException if a problem occurs while reading the configuration
     */
    public FileSystemXmlApplicationContext(String[] configLocations, ApplicationContext parent, List xmlPreprocessors) throws BeansException {
        this(configLocations, true, parent, xmlPreprocessors);
    }

    /**
     * Creates a FileSystemXmlApplicationContext which loads the configuration at the specified locations on the file system.
     * @param configLocations the locations of the configuration files on the class path
     * @param refresh if true the configurations are immedately loaded; otherwise the configurations are not loaded
     * until refresh() is called
     * @param parent the parent of this application context
     * @param xmlPreprocessors the SpringXmlPreprocessors to apply before passing the xml to Spring for processing
     * @throws BeansException if a problem occurs while reading the configuration
     */
    public FileSystemXmlApplicationContext(String[] configLocations, boolean refresh, ApplicationContext parent, List xmlPreprocessors) throws BeansException {
        super(configLocations, false, parent);
        this.xmlPreprocessors = xmlPreprocessors;
        if (refresh) {
            refresh();
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws IOException {
        // Create a new XmlBeanDefinitionReader for the given BeanFactory.
        XmlBeanDefinitionReader beanDefinitionReader = XBeanHelper.createBeanDefinitionReader(this, beanFactory, xmlPreprocessors);

        // Configure the bean definition reader with this context's
        // resource loading environment.
        beanDefinitionReader.setResourceLoader(this);
        beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));

        // Allow a subclass to provide custom initialization of the reader,
        // then proceed with actually loading the bean definitions.
        initBeanDefinitionReader(beanDefinitionReader);
        loadBeanDefinitions(beanDefinitionReader);
    }
    
}
