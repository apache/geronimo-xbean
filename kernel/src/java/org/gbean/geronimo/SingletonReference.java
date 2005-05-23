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
import java.util.Iterator;
import java.util.Set;
import javax.management.ObjectName;

import org.gbean.service.ServiceContext;
import org.gbean.kernel.runtime.ServiceInstanceUtil;
import org.gbean.kernel.ServiceNotFoundException;
import org.gbean.kernel.ClassLoading;
import org.gbean.proxy.ProxyManager;
import org.gbean.spring.ServiceContextThreadLocal;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * @version $Rev: 71492 $ $Date: 2004-11-14 21:31:50 -0800 (Sun, 14 Nov 2004) $
 */
public class SingletonReference implements FactoryBean, Serializable {
    public static BeanDefinitionHolder createBeanDefinition(String propertyName, Set patterns, String referenceType) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition(SingletonReference.class, 0);
        ConstructorArgumentValues args = beanDefinition.getConstructorArgumentValues();
        args.addIndexedArgumentValue(0, propertyName, String.class.getName());
        args.addIndexedArgumentValue(1, patterns, Set.class.getName());
        args.addIndexedArgumentValue(2, referenceType, String.class.getName());
        return new BeanDefinitionHolder(beanDefinition, SingletonReference.class.getName());
    }

    /**
     * Name of this reference.
     */
    private final String name;

    /**
     * Proxy type which is injected into the service.
     */
    private final String objectTypeName;

    /**
     * The target objectName patterns to watch for a connection.
     */
    private final Set patterns;

    /**
     * Proxy type which is injected into the service.
     */
    private transient Class objectType;

    public SingletonReference(String name, Set patterns, String objectTypeName) {
        this.name = name;
        this.patterns = patterns;
        this.objectTypeName = objectTypeName;
    }

    public final String getName() {
        return name;
    }

    public Set getPatterns() {
        return patterns;
    }

    public final Class getObjectType() {
        ServiceContext serviceContext = ServiceContextThreadLocal.get();
        if (serviceContext == null) {
            throw new IllegalStateException("Service context has not been set");
        }
        synchronized (this) {
            if (objectType == null) {
                try {
                    objectType = ClassLoading.loadClass(objectTypeName, serviceContext.getClassLoader());
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
                objectType = ClassLoading.loadClass(objectTypeName, serviceContext.getClassLoader());
            }
        }

        Set targets = ServiceInstanceUtil.getRunningTargets(serviceContext.getKernel(), patterns);
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
