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

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.management.ObjectName;

import org.gbean.kernel.ClassLoading;
import org.gbean.kernel.KernelUtil;
import org.gbean.kernel.ServiceNotFoundException;
import org.gbean.proxy.ProxyManager;
import org.gbean.service.ServiceContext;
import org.gbean.spring.DependencyProvider;
import org.gbean.spring.ServiceContextThreadLocal;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * @version $Rev: 71492 $ $Date: 2004-11-14 21:31:50 -0800 (Sun, 14 Nov 2004) $
 */
public class SingletonReference implements FactoryBean, DependencyProvider, Serializable {
    public static BeanDefinitionHolder createBeanDefinition(String name, Set patterns, String referenceType) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition(SingletonReference.class, 0);
        MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();
        propertyValues.addPropertyValue("name", name);
        propertyValues.addPropertyValue("patterns", patterns);
        propertyValues.addPropertyValue("referenceType", referenceType);
        return new BeanDefinitionHolder(beanDefinition, SingletonReference.class.getName());
    }

    public static Map getDependencies(RootBeanDefinition beanDefinition) {
        if (!beanDefinition.getBeanClass().equals(SingletonReference.class)) {
            throw new IllegalArgumentException("Bean definition is for another bean type:" +
                    " expected=" + SingletonReference.class.getName() +
                    " actual=" + beanDefinition.getBeanClass().getName());
        }
        MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();
        if (!propertyValues.contains("name")) {
            throw new IllegalArgumentException("Bean definition does not contain a name property");
        }
        String name = (String) propertyValues.getPropertyValue("name").getValue();
        if (!propertyValues.contains("patterns")) {
            throw new IllegalArgumentException("Bean definition does not contain a patterns property: name=" + name);
        }
        Set patterns = (Set) propertyValues.getPropertyValue("patterns").getValue();
        return Collections.singletonMap(name, patterns);
    }

    /**
     * Name of this reference.
     */
    private String name;

    /**
     * Proxy type which is injected into the service.
     */
    private String referenceType;

    /**
     * The target objectName patterns to watch for a connection.
     */
    private Set patterns;

    /**
     * Proxy type which is injected into the service.
     */
    private transient Class objectType;

    public SingletonReference() {
    }

    public SingletonReference(String name, Set patterns, String referenceType) {
        this.name = name;
        this.patterns = patterns;
        this.referenceType = referenceType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public Set getPatterns() {
        return patterns;
    }

    public void setPatterns(Set patterns) {
        this.patterns = patterns;
    }

    public Map getDependencies() {
        return Collections.singletonMap(name, patterns);
    }

    public final Class getObjectType() {
        ServiceContext serviceContext = ServiceContextThreadLocal.get();
        if (serviceContext == null) {
            throw new IllegalStateException("Service context has not been set");
        }
        synchronized (this) {
            if (objectType == null) {
                try {
                    objectType = ClassLoading.loadClass(referenceType, serviceContext.getClassLoader());
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("Could not load singleton reference object type");
                }
            }
        }
        return objectType;
    }

    public synchronized final Object getObject() throws ClassNotFoundException {
        ServiceContext serviceContext = ServiceContextThreadLocal.get();
        if (serviceContext == null) {
            throw new IllegalStateException("Service context has not been set");
        }
        synchronized (this) {
            if (objectType == null) {
                objectType = ClassLoading.loadClass(referenceType, serviceContext.getClassLoader());
            }
        }

        Set targets = KernelUtil.getRunningServiceNames(serviceContext.getKernel(), patterns);
        if (targets.size() != 1) {
            throw new IllegalStateException("Invalid reference: name=" + name + ", targetCount=" + targets.size() + ", patterns=" + getPatternsText());
        }

        // add a dependency on our target and create the proxy
        ObjectName target = (ObjectName) targets.iterator().next();
        ProxyManager proxyManager = null;
        try {
            proxyManager = ProxyManager.findProxyManager(serviceContext.getKernel());
        } catch (ServiceNotFoundException e) {
            throw new IllegalStateException("ProxyManger has not been loaded");

        }

        try {
            return proxyManager.createProxy(target, objectType, this);
        } catch (ServiceNotFoundException e) {
            throw new IllegalStateException("Referenced object was unregistered before a proxy could be created: name=" + name + ", targetCount=" + targets.size() + ", patterns=" + getPatternsText());
        }
    }

    public boolean isSingleton() {
        return true;
    }

    private String getPatternsText() {
        StringBuffer buf = new StringBuffer();
        for (Iterator iterator = patterns.iterator(); iterator.hasNext();) {
            ObjectName objectName = (ObjectName) iterator.next();
            buf.append(objectName.getCanonicalName()).append(" ");
        }
        return buf.toString();
    }
}
