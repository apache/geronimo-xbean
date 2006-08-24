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
package org.apache.xbean.spring.util;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;

import java.util.Collection;
import java.util.Map;

/**
 * Walks a spring bean factory tree.
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public interface SpringVisitor {
    void visitBeanFactory(ConfigurableListableBeanFactory beanRegistry, Object data) throws BeansException;

    void visitBeanDefinition(String beanName, BeanDefinition beanDefinition, Object data) throws BeansException;

    void visitBeanDefinition(BeanDefinition beanDefinition, Object data) throws BeansException;

    void visitMutablePropertyValues(MutablePropertyValues propertyValues, Object data) throws BeansException;

    void visitConstructorArgumentValues(ConstructorArgumentValues constructorArgumentValues, Object data) throws BeansException;

    void visitConstructorArgumentValue(ConstructorArgumentValues.ValueHolder valueHolder, Object data) throws BeansException;

    void visitPropertyValue(PropertyValue propertyValue, Object data) throws BeansException;

    void visitRuntimeBeanReference(RuntimeBeanReference beanReference, Object data) throws BeansException;

    void visitCollection(Collection collection, Object data)  throws BeansException;

    void visitMap(Map map, Object data)  throws BeansException;

    void visitObject(Object value, Object data) throws BeansException;

    void visitBeanDefinitionHolder(BeanDefinitionHolder beanDefinitionHolder, Object data) throws BeansException;
}
