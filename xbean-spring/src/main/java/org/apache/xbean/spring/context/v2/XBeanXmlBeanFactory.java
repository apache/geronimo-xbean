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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.Resource;

import java.util.Collections;
import java.util.List;

public class XBeanXmlBeanFactory extends DefaultListableBeanFactory {

    /**
     * Create a new XBeanXmlBeanFactory with the given resource,
     * which must be parsable using DOM.
     * @param resource XML resource to load bean definitions from
     * @throws BeansException in case of loading or parsing errors
     */
    public XBeanXmlBeanFactory(Resource resource) throws BeansException {
        this(resource, null, Collections.EMPTY_LIST);
    }

    /**
     * Create a new XBeanXmlBeanFactory with the given input stream,
     * which must be parsable using DOM.
     * @param resource XML resource to load bean definitions from
     * @param parentBeanFactory parent bean factory
     * @throws BeansException in case of loading or parsing errors
     */
    public XBeanXmlBeanFactory(Resource resource, BeanFactory parentBeanFactory) throws BeansException {
        this(resource, parentBeanFactory, Collections.EMPTY_LIST);
    }

    /**
     * Create a new XBeanXmlBeanFactory with the given input stream,
     * which must be parsable using DOM.
     * @param resource XML resource to load bean definitions from
     * @param xmlPreprocessors the preprocessors to apply the DOM before passing to Spring for processing
     * @throws BeansException in case of loading or parsing errors
     */
    public XBeanXmlBeanFactory(Resource resource, List xmlPreprocessors) throws BeansException {
        this(resource, null, xmlPreprocessors);
    }

    /**
     * Create a new XBeanXmlBeanFactory with the given input stream,
     * which must be parsable using DOM.
     * @param resource XML resource to load bean definitions from
     * @param parentBeanFactory parent bean factory
     * @param xmlPreprocessors the preprocessors to apply the DOM before passing to Spring for processing
     * @throws BeansException in case of loading or parsing errors
     */
    public XBeanXmlBeanFactory(Resource resource, BeanFactory parentBeanFactory, List xmlPreprocessors) throws BeansException {
        super(parentBeanFactory);
        XBeanXmlBeanDefinitionReader reader = new XBeanXmlBeanDefinitionReader(null, this, xmlPreprocessors);
        reader.loadBeanDefinitions(resource);
    }

}
