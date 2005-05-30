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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * @version $Revision$ $Date$
 */
public final class SpringUtil {
    private SpringUtil() {
    }

    public static Map extractDependencies(RootBeanDefinition beanDefinition, final Map objectNameMap) throws Exception {
        final Map dependencies = new LinkedHashMap();
        SpringVisitor springVisitor = new AbstractSpringVisitor() {
            public void visitBeanDefinition(BeanDefinition beanDefinition, Object data) throws BeansException {
                Map dependencies = (Map) data;
                super.visitBeanDefinition(beanDefinition, data);
                if (beanDefinition instanceof RootBeanDefinition) {
                    RootBeanDefinition rootBeanDefinition = (RootBeanDefinition) beanDefinition;
                    String[] dependsOn = rootBeanDefinition.getDependsOn();
                    if (dependsOn != null) {
                        for (int i = 0; i < dependsOn.length; i++) {
                            String dependency = dependsOn[i];
                            try {
                                dependencies.put(dependency, Collections.singleton(getObjectName(objectNameMap, dependency)));
                            } catch (MalformedObjectNameException e) {
                                throw new BeanDefinitionValidationException("Depends on name could not be converted to an objectName: dependency=" + dependency, e);
                            }
                        }
                    }

                    Class beanClass = rootBeanDefinition.getBeanClass();
                    Method method = null;
                    try {
                        method = beanClass.getDeclaredMethod("getDependencies", new Class[]{RootBeanDefinition.class});
                    } catch (NoSuchMethodException e) {
                        // ignored mehtod doesn't exist most of the time
                    }
                    if (method != null) {
                        try {
                            dependencies.putAll((Map) method.invoke(beanClass, new Object[]{rootBeanDefinition}));
                        } catch (Exception e) {
                            throw new BeanDefinitionValidationException("Unable to get dependencies from root bean definition: " + rootBeanDefinition, e);
                        }
                    }
                }
            }

            public void visitRuntimeBeanReference(RuntimeBeanReference beanReference, Object data) throws BeansException {
                super.visitRuntimeBeanReference(beanReference, data);
                String dependency = beanReference.getBeanName();
                try {
                    Map dependencies = (Map) data;
                    dependencies.put(dependency, Collections.singleton(getObjectName(objectNameMap, dependency)));
                } catch (MalformedObjectNameException e) {
                    throw new BeanDefinitionValidationException("Bean ref name could not be converted to an objectName: refName=" + dependency, e);
                }
            }

            public void visitObject(Object value, Object data) throws BeansException {
                Map dependencies = (Map) data;
                super.visitObject(value, data);
                if (value instanceof DependencyProvider) {
                    DependencyProvider dependencyProvider = (DependencyProvider) value;
                    dependencies.putAll(dependencyProvider.getDependencies());
                }
            }
        };
        springVisitor.visitBeanDefinition(beanDefinition, dependencies);
        return dependencies;
    }

    public static ObjectName getObjectName(Map objectNameMap, String name) throws MalformedObjectNameException {
        if (name.indexOf(":") < 0) {
            ObjectName objectName = (ObjectName) objectNameMap.get(name);
            if (objectName == null) {
                throw new NoSuchBeanDefinitionException(name, "No object name definded for service");
            }

            return objectName;
        } else {
            return new ObjectName(name);
        }
    }
}
