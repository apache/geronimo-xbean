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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * @version $Revision$ $Date$
 */
public class LifecycleDetector implements BeanFactoryPostProcessor {
    private static final Map lifecycleMap = new LinkedHashMap();

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

    public void setLifecycleInterfaces(List lifecycleInterfaces) {
        lifecycleMap.clear();
        for (Iterator iterator = lifecycleInterfaces.iterator(); iterator.hasNext();) {
            addLifecycleInterface((LifecycleInfo) iterator.next());
        }
    }

    public void addLifecycleInterface(Class type, String initMethodName, String destroyMethodName) {
        lifecycleMap.put(type, new LifecycleMethods(initMethodName, destroyMethodName));
    }

    public void addLifecycleInterface(LifecycleInfo lifecycleInfo) {
        lifecycleMap.put(lifecycleInfo.getType(),
                new LifecycleMethods(lifecycleInfo.getInitMethodName(), lifecycleInfo.getDestroyMethodName()));
    }

    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        SpringVisitor visitor = new AbstractSpringVisitor() {
            public void visitBeanDefinition(BeanDefinition beanDefinition) throws BeansException {
                super.visitBeanDefinition(beanDefinition);

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
        visitor.visitBeanFactory(beanFactory);
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
