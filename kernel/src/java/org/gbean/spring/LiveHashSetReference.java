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

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import javax.management.ObjectName;

import org.gbean.beans.LiveHashSet;
import org.gbean.service.ServiceContext;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * @version $Rev: 71492 $ $Date: 2004-11-14 21:31:50 -0800 (Sun, 14 Nov 2004) $
 */
public class LiveHashSetReference implements FactoryBean, Serializable {
    public static BeanDefinitionHolder createBeanDefinition(String name, Set patterns) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition(LiveHashSetReference.class, 0);
        MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();
        propertyValues.addPropertyValue("name", name);
        propertyValues.addPropertyValue("patterns", patterns);
        return new BeanDefinitionHolder(beanDefinition, LiveHashSetReference.class.getName());
    }

    /**
     * Name of this reference.
     */
    private String name;

    /**
     * The target objectName patterns to watch for a connection.
     */
    private Set patterns;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set getPatterns() {
        return patterns;
    }

    public void setPatterns(Set patterns) {
        this.patterns = patterns;
    }

    public void setPattern(ObjectName pattern) {
        this.patterns = Collections.singleton(pattern);
    }

    public final Class getObjectType() {
        return Set.class;
    }

    public synchronized final Object getObject() {
        ServiceContext serviceContext = ServiceContextThreadLocal.get();
        if (serviceContext == null) {
            throw new IllegalStateException("Service context has not been set");
        }
        LiveHashSet liveHashSet = new LiveHashSet(serviceContext.getKernel(), name, patterns);
        liveHashSet.start();
        return liveHashSet;
    }

    public boolean isSingleton() {
        return true;
    }
}
