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
package org.gbean.spring;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Collection;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * @version $Revision$ $Date$
 */
public abstract class AbstractSpringVisitor implements SpringVisitor {
    public void visitBeanFactory(ConfigurableListableBeanFactory beanRegistry) throws BeansException {
        String[] beanNames = beanRegistry.getBeanDefinitionNames();
        for (int i = 0; i < beanNames.length; i++) {
            String beanName = beanNames[i];
            visitBeanDefinition(beanName, beanRegistry.getBeanDefinition(beanName));
        }
    }

    public void visitBeanDefinition(String beanName, BeanDefinition beanDefinition) throws BeansException {
        visitBeanDefinition(beanDefinition);
    }

    public void visitBeanDefinition(BeanDefinition beanDefinition) throws BeansException {
        visitConstructorArgumentValues(beanDefinition.getConstructorArgumentValues());
        visitMutablePropertyValues(beanDefinition.getPropertyValues());
    }

    public void visitMutablePropertyValues(MutablePropertyValues propertyValues) throws BeansException {
        PropertyValue[] values = propertyValues.getPropertyValues();
        for (int i = 0; i < values.length; i++) {
            visitPropertyValue(values[i]);
        }
    }

    public void visitConstructorArgumentValues(ConstructorArgumentValues constructorArgumentValues) throws BeansException {
        Map indexedArgumentValues = constructorArgumentValues.getIndexedArgumentValues();
        for (Iterator iterator = indexedArgumentValues.values().iterator(); iterator.hasNext();) {
            visitConstructorArgumentValues((ConstructorArgumentValues.ValueHolder) iterator.next());
        }
        Set genericArgumentValues = constructorArgumentValues.getGenericArgumentValues();
        for (Iterator iterator = genericArgumentValues.iterator(); iterator.hasNext();) {
            visitConstructorArgumentValues((ConstructorArgumentValues.ValueHolder) iterator.next());
        }
    }

    public void visitConstructorArgumentValues(ConstructorArgumentValues.ValueHolder valueHolder) throws BeansException {
        visitNext(valueHolder.getValue());
    }

    public void visitPropertyValue(PropertyValue propertyValue) throws BeansException {
        visitNext(propertyValue.getValue());
    }

    public void visitRuntimeBeanReference(RuntimeBeanReference beanReference) throws BeansException {
    }

    public void visitCollection(Collection collection)  throws BeansException {
        for (Iterator iterator = collection.iterator(); iterator.hasNext();) {
            visitNext(iterator.next());
        }
    }

    public void visitMap(Map map)  throws BeansException {
        for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            visitNext(entry.getKey());
            visitNext(entry.getValue());
        }
    }

    public void visitObject(Object value)  throws BeansException {
    }

    protected void visitNext(Object value) throws BeansException {
        if (value == null) {
            return;
        }

        if (value instanceof ConfigurableListableBeanFactory) {
            visitBeanFactory((ConfigurableListableBeanFactory) value);
        } else if (value instanceof BeanDefinition) {
            visitBeanDefinition((BeanDefinition) value);
        } else if (value instanceof ConstructorArgumentValues) {
            visitConstructorArgumentValues((ConstructorArgumentValues) value);
        } else if (value instanceof ConstructorArgumentValues.ValueHolder) {
            visitConstructorArgumentValues((ConstructorArgumentValues.ValueHolder) value);
        } else if (value instanceof MutablePropertyValues) {
            visitMutablePropertyValues((MutablePropertyValues) value);
        } else if (value instanceof PropertyValue) {
            visitPropertyValue((PropertyValue) value);
        } else if (value instanceof RuntimeBeanReference) {
            visitRuntimeBeanReference((RuntimeBeanReference) value);
        } else if (value instanceof Map) {
            visitMap((Map) value);
        } else if (value instanceof Collection) {
            visitCollection((Collection) value);
        } else {
            visitObject(value);
        }
    }

}
