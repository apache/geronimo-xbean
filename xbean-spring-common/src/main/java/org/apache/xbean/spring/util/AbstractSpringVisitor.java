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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Default do nothing implementation of SpringVisitor.
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public abstract class AbstractSpringVisitor implements SpringVisitor {
    public void visitBeanFactory(ConfigurableListableBeanFactory beanRegistry, Object data) throws BeansException {
        String[] beanNames = beanRegistry.getBeanDefinitionNames();
        for (int i = 0; i < beanNames.length; i++) {
            String beanName = beanNames[i];
            visitBeanDefinition(beanName, beanRegistry.getBeanDefinition(beanName), data);
        }
    }

    public void visitBeanDefinitionHolder(BeanDefinitionHolder beanDefinitionHolder, Object data) throws BeansException {
        visitBeanDefinition(beanDefinitionHolder.getBeanName(), beanDefinitionHolder.getBeanDefinition(), data);
    }

    public void visitBeanDefinition(String beanName, BeanDefinition beanDefinition, Object data) throws BeansException {
        visitBeanDefinition(beanDefinition, data);
    }

    public void visitBeanDefinition(BeanDefinition beanDefinition, Object data) throws BeansException {
        visitConstructorArgumentValues(beanDefinition.getConstructorArgumentValues(), data);
        visitMutablePropertyValues(beanDefinition.getPropertyValues(), data);
    }

    public void visitMutablePropertyValues(MutablePropertyValues propertyValues, Object data) throws BeansException {
        PropertyValue[] values = propertyValues.getPropertyValues();
        for (int i = 0; i < values.length; i++) {
            visitPropertyValue(values[i], data);
        }
    }

    public void visitConstructorArgumentValues(ConstructorArgumentValues constructorArgumentValues, Object data) throws BeansException {
        Map indexedArgumentValues = constructorArgumentValues.getIndexedArgumentValues();
        for (Iterator iterator = indexedArgumentValues.values().iterator(); iterator.hasNext();) {
            visitConstructorArgumentValue((ConstructorArgumentValues.ValueHolder) iterator.next(), data);
        }
        List genericArgumentValues = constructorArgumentValues.getGenericArgumentValues();
        for (Iterator iterator = genericArgumentValues.iterator(); iterator.hasNext();) {
            visitConstructorArgumentValue((ConstructorArgumentValues.ValueHolder) iterator.next(), data);
        }
    }

    public void visitConstructorArgumentValue(ConstructorArgumentValues.ValueHolder valueHolder, Object data) throws BeansException {
        visitNext(valueHolder.getValue(), data);
    }

    public void visitPropertyValue(PropertyValue propertyValue, Object data) throws BeansException {
        visitNext(propertyValue.getValue(), data);
    }

    public void visitRuntimeBeanReference(RuntimeBeanReference beanReference, Object data) throws BeansException {
    }

    public void visitCollection(Collection collection, Object data)  throws BeansException {
        for (Iterator iterator = collection.iterator(); iterator.hasNext();) {
            visitNext(iterator.next(), data);
        }
    }

    public void visitMap(Map map, Object data)  throws BeansException {
        for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            visitNext(entry.getKey(), data);
            visitNext(entry.getValue(), data);
        }
    }

    public void visitObject(Object value, Object data)  throws BeansException {
    }

    protected void visitNext(Object value, Object data) throws BeansException {
        if (value == null) {
            return;
        }

        if (value instanceof ConfigurableListableBeanFactory) {
            visitBeanFactory((ConfigurableListableBeanFactory) value, data);
        } else if (value instanceof BeanDefinitionHolder) {
            visitBeanDefinitionHolder((BeanDefinitionHolder) value, data);
        } else if (value instanceof BeanDefinition) {
            visitBeanDefinition((BeanDefinition) value, data);
        } else if (value instanceof ConstructorArgumentValues) {
            visitConstructorArgumentValues((ConstructorArgumentValues) value, data);
        } else if (value instanceof ConstructorArgumentValues.ValueHolder) {
            visitConstructorArgumentValue((ConstructorArgumentValues.ValueHolder) value, data);
        } else if (value instanceof MutablePropertyValues) {
            visitMutablePropertyValues((MutablePropertyValues) value, data);
        } else if (value instanceof PropertyValue) {
            visitPropertyValue((PropertyValue) value, data);
        } else if (value instanceof RuntimeBeanReference) {
            visitRuntimeBeanReference((RuntimeBeanReference) value, data);
        } else if (value instanceof Map) {
            visitMap((Map) value, data);
        } else if (value instanceof Collection) {
            visitCollection((Collection) value, data);
        } else {
            visitObject(value, data);
        }
    }

}
