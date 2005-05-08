/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
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

package org.gbean.beans;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

import org.apache.geronimo.kernel.ClassLoading;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * @version $Rev: 71492 $ $Date: 2004-11-14 21:31:50 -0800 (Sun, 14 Nov 2004) $
 */
public class CollectionReference implements FactoryBean, Serializable {
    public static BeanDefinitionHolder createBeanDefinition(String propertyName, Set patterns, String referenceType) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition(CollectionReference.class, 0);
        ConstructorArgumentValues args = beanDefinition.getConstructorArgumentValues();
        args.addIndexedArgumentValue(0, propertyName, String.class.getName());
        args.addIndexedArgumentValue(1, patterns, Set.class.getName());
        args.addIndexedArgumentValue(2, referenceType, String.class.getName());
        return new BeanDefinitionHolder(beanDefinition, CollectionReference.class.getName());
    }

    /**
     * Name of this reference.
     */
    private final String name;

    /**
     * Proxy type which is injected into the GBeanInstance.
     */
    private final String proxyTypeName;

    /**
     * The target objectName patterns to watch for a connection.
     */
    private final Set patterns;

    public CollectionReference(String name, Set patterns, String proxyTypeName) {
        this.name = name;
        this.patterns = patterns;
        this.proxyTypeName = proxyTypeName;
    }

    public final String getName() {
        return name;
    }

    public String getProxyTypeName() {
        return proxyTypeName;
    }

    public Set getPatterns() {
        return patterns;
    }

    public final Class getObjectType() {
        return Collection.class;
    }

    public synchronized final Object getObject() throws ClassNotFoundException {
        GBeanContext beanContext = (GBeanContext) GBeanContext.threadLocal.get();
        if (beanContext == null) {
            throw new IllegalStateException("Geronimo bean context has not been set for CollectionReference: " + name);
        }
        Class proxyType = ClassLoading.loadClass(proxyTypeName, beanContext.getClassLoader());
        return new ProxyReferenceCollection(this, name, proxyType, beanContext.getKernel(), patterns);
    }

    public boolean isSingleton() {
        return true;
    }
}
