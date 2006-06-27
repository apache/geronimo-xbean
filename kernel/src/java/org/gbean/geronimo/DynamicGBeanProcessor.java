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
package org.gbean.geronimo;

import java.util.Iterator;
import java.util.Map;

import org.apache.geronimo.gbean.DynamicGBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * @version $Revision$ $Date$
 */
public class DynamicGBeanProcessor implements BeanPostProcessor {
    private final String beanName;
    private final Map dynamicProperties;

    public DynamicGBeanProcessor(String beanName, Map dynamicProperties) {
        this.beanName = beanName;
        this.dynamicProperties = dynamicProperties;
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (this.beanName.equals(beanName)) {
            for (Iterator iterator = dynamicProperties.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry entry = (Map.Entry) iterator.next();
                String propertyName = (String) entry.getKey();
                Object propertyValue = entry.getValue();
                try {
                    ((DynamicGBean) bean).setAttribute(propertyName, propertyValue);
                } catch (Exception e) {
                    throw new BeanInitializationException("Error invoking dynamic property setter method:" +
                            " beanName " + beanName +
                            " beanClass " + bean.getClass() +
                            " propertyName " + propertyName +
                            " propertyValue " + propertyValue, e);
                }
            }
        }
        return bean;
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
