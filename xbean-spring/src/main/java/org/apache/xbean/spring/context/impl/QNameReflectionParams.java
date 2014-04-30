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
package org.apache.xbean.spring.context.impl;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.w3c.dom.Element;

import java.beans.PropertyDescriptor;

/**
 * 
 * @version $Revision: 1.1 $
 */
public class QNameReflectionParams {

    private final AbstractBeanDefinition beanDefinition;
    private final Element element;
    private final PropertyDescriptor[] descriptors;
    private final int index;

    public QNameReflectionParams(AbstractBeanDefinition beanDefinition, Element element,
            PropertyDescriptor[] descriptors, int index) {
        this.beanDefinition = beanDefinition;
        this.element = element;
        this.descriptors = descriptors;
        this.index = index;
    }

    public AbstractBeanDefinition getBeanDefinition() {
        return beanDefinition;
    }

    public PropertyDescriptor[] getDescriptors() {
        return descriptors;
    }

    public Element getElement() {
        return element;
    }

    public int getIndex() {
        return index;
    }

}
