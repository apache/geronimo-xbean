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
package org.xbean.server.spring.configuration;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.xbean.spring.util.SpringVisitor;
import org.xbean.spring.util.AbstractSpringVisitor;

/**
 * LifecycleDetector is a Spring configuration post processor that automatically sets the init and destroy methods
 * on the bean definition based on the type of the bean class.
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class LifecycleDetector implements BeanFactoryPostProcessor {
    private final Map lifecycleMap = new LinkedHashMap();

    /**
     * Gets the lifecycle interfaces used by this detectors.
     * @return the lifecycle interfaces used by this detectors
     */
    public List getLifecycleInterfaces() {
        List lifecycleInterfaces = new LinkedList();
        for (Iterator iterator = lifecycleMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            Class type = (Class) entry.getKey();
            LifecycleMethods lifecycleMethods = (LifecycleMethods) entry.getValue();
            lifecycleInterfaces.add(new LifecycleInfo(type, lifecycleMethods.initMethodName, lifecycleMethods.destroyMethodName));
        }
        return lifecycleInterfaces;
    }

    /**
     * Sets the lifecycle interfaces used by this detectors.
     * @param lifecycleInterfaces the lifecycle interfaces used by this detectors
     */
    public void setLifecycleInterfaces(List lifecycleInterfaces) {
        lifecycleMap.clear();
        for (Iterator iterator = lifecycleInterfaces.iterator(); iterator.hasNext();) {
            addLifecycleInterface((LifecycleInfo) iterator.next());
        }
    }

    /**
     * Adds a lifecycle interface to this detector.
     * @param type the lifecycle interface
     * @param initMethodName the name of the init method
     * @param destroyMethodName the name of the destroy method
     */
    public void addLifecycleInterface(Class type, String initMethodName, String destroyMethodName) {
        lifecycleMap.put(type, new LifecycleMethods(initMethodName, destroyMethodName));
    }

    /**
     * Adds a lifecycle interface to this detector.
     * @param lifecycleInfo the lifecycle interfface info
     */
    public void addLifecycleInterface(LifecycleInfo lifecycleInfo) {
        lifecycleMap.put(lifecycleInfo.getType(),
                new LifecycleMethods(lifecycleInfo.getInitMethodName(), lifecycleInfo.getDestroyMethodName()));
    }

    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        SpringVisitor visitor = new AbstractSpringVisitor() {
            public void visitBeanDefinition(BeanDefinition beanDefinition, Object data) throws BeansException {
                super.visitBeanDefinition(beanDefinition, data);

                if (!(beanDefinition instanceof RootBeanDefinition)) {
                    return;
                }
                RootBeanDefinition rootBeanDefinition = ((RootBeanDefinition) beanDefinition);
                Class beanType = rootBeanDefinition.getBeanClass();
                for (Iterator iterator = lifecycleMap.entrySet().iterator(); iterator.hasNext();) {
                    Map.Entry entry = (Map.Entry) iterator.next();
                    Class lifecycleInterface = (Class) entry.getKey();
                    LifecycleMethods value = (LifecycleMethods) entry.getValue();
                    if (lifecycleInterface.isAssignableFrom(beanType)) {
                        if (rootBeanDefinition.getInitMethodName() == null) {
                            rootBeanDefinition.setInitMethodName(value.initMethodName);
                        }
                        if (rootBeanDefinition.getDestroyMethodName() == null) {
                            rootBeanDefinition.setDestroyMethodName(value.destroyMethodName);
                        }
                        if (rootBeanDefinition.getInitMethodName() != null && rootBeanDefinition.getDestroyMethodName() != null) {
                            return;
                        }
                    }
                }
            }
        };
        visitor.visitBeanFactory(beanFactory, null);
    }

    private static class LifecycleMethods {
        private String initMethodName;
        private String destroyMethodName;

        public LifecycleMethods(String initMethodName, String destroyMethodName) {
            this.initMethodName = initMethodName;
            this.destroyMethodName = destroyMethodName;
        }
    }
}
