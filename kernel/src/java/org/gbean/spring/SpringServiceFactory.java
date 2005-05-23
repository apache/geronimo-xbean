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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.gbean.service.ConfigurableServiceFactory;
import org.gbean.service.ServiceContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;

/**
 * @version $Revision$ $Date$
 */
public class SpringServiceFactory implements ConfigurableServiceFactory {
    private static final String ROOT_BEAN_DEFINITION = "RootBeanDefinition";
    private final Map dependencies = new LinkedHashMap();
    private final Map objectNameMap;
    private RootBeanDefinition beanDefinition;
    private GenericApplicationContext applicationContext;
    private boolean enabled = true;

    public SpringServiceFactory(RootBeanDefinition beanDefinition, Map objectNameMap) throws Exception {
        this.beanDefinition = beanDefinition;
        this.objectNameMap = objectNameMap;

        extractDependencies(beanDefinition);
    }

    private void extractDependencies(RootBeanDefinition beanDefinition) throws Exception {
        SpringVisitor springVisitor = new AbstractSpringVisitor() {
            public void visitBeanDefinition(BeanDefinition beanDefinition) throws BeansException {
                super.visitBeanDefinition(beanDefinition);
                if (beanDefinition instanceof RootBeanDefinition) {
                    RootBeanDefinition rootBeanDefinition = (RootBeanDefinition) beanDefinition;
                    String[] dependsOn = rootBeanDefinition.getDependsOn();
                    if (dependsOn != null) {
                        for (int i = 0; i < dependsOn.length; i++) {
                            String dependency = dependsOn[i];
                            try {
                                dependencies.put(dependency, Collections.singleton(getObjectName(dependency)));
                            } catch (MalformedObjectNameException e) {
                                throw new BeanDefinitionValidationException("Depends on name could not be converted to an objectName: dependency=" + dependency, e);
                            }
                        }
                    }
                }
            }

            public void visitRuntimeBeanReference(RuntimeBeanReference beanReference) throws BeansException {
                super.visitRuntimeBeanReference(beanReference);
                String dependency = beanReference.getBeanName();
                try {
                    dependencies.put(dependency, Collections.singleton(getObjectName(dependency)));
                } catch (MalformedObjectNameException e) {
                    throw new BeanDefinitionValidationException("Bean ref name could not be converted to an objectName: refName=" + dependency, e);
                }
            }
        };
        springVisitor.visitBeanDefinition(beanDefinition);
    }

    public RootBeanDefinition getBeanDefinition() {
        return beanDefinition;
    }

    public void setBeanDefinition(RootBeanDefinition beanDefinition) {
        this.beanDefinition = beanDefinition;
    }

    public Map getDependencies() {
        return dependencies;
    }

    public void addDependency(String name, Set patterns) {
        dependencies.put(name, patterns);
    }

    public Object createService(final ServiceContext serviceContext) throws Exception {
        Object service = null;
        try {
            ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
            ServiceContext oldServiceContext = ServiceContextThreadLocal.get();
            try {
                Thread.currentThread().setContextClassLoader(serviceContext.getClassLoader());
                ServiceContextThreadLocal.set(serviceContext);

                ApplicationContext parent = new ApplicationContext() {
                    public ApplicationContext getParent() {
                        throw new UnsupportedOperationException();
                    }

                    public String getDisplayName() {
                        throw new UnsupportedOperationException();
                    }

                    public long getStartupDate() {
                        throw new UnsupportedOperationException();
                    }

                    public void publishEvent(ApplicationEvent event) {
//                        throw new UnsupportedOperationException();
                    }

                    public boolean containsBeanDefinition(String beanName) {
                        throw new UnsupportedOperationException();
                    }

                    public int getBeanDefinitionCount() {
                        throw new UnsupportedOperationException();
                    }

                    public String[] getBeanDefinitionNames() {
                        throw new UnsupportedOperationException();
                    }

                    public String[] getBeanDefinitionNames(Class type) {
                        throw new UnsupportedOperationException();
                    }

                    public String[] getBeanNamesForType(Class type) {
                        throw new UnsupportedOperationException();
                    }

                    public String[] getBeanNamesForType(Class type, boolean includePrototypes, boolean includeFactoryBeans) {
                        throw new UnsupportedOperationException();
                    }

                    public Map getBeansOfType(Class type) throws BeansException {
                        throw new UnsupportedOperationException();
                    }

                    public Map getBeansOfType(Class type, boolean includePrototypes, boolean includeFactoryBeans) throws BeansException {
                        throw new UnsupportedOperationException();
                    }

                    public Object getBean(String name) throws BeansException {
                        throw new UnsupportedOperationException();
                    }

                    public Object getBean(String name, Class requiredType) throws BeansException {
                        ObjectName objectName = null;
                        try {
                            objectName = getObjectName(name);
                        } catch (MalformedObjectNameException e) {
                            throw (NoSuchBeanDefinitionException) new NoSuchBeanDefinitionException(name, "Could not create an objectname for the specified name").initCause(e);
                        }

                        try {
                            Object service = serviceContext.getKernel().getService(objectName);
                            return service;
                        } catch (Exception e) {
                            throw (NoSuchBeanDefinitionException) new NoSuchBeanDefinitionException(name, "Kernel threw an exception").initCause(e);
                        }
                    }

                    public boolean containsBean(String name) {
                        throw new UnsupportedOperationException();
                    }

                    public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
                        throw new UnsupportedOperationException();
                    }

                    public Class getType(String name) throws NoSuchBeanDefinitionException {
                        throw new UnsupportedOperationException();
                    }

                    public String[] getAliases(String name) throws NoSuchBeanDefinitionException {
                        throw new UnsupportedOperationException();
                    }

                    public BeanFactory getParentBeanFactory() {
                        throw new UnsupportedOperationException();
                    }

                    public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
                        throw new UnsupportedOperationException();
                    }

                    public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
                        throw new UnsupportedOperationException();
                    }

                    public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
                        throw new UnsupportedOperationException();
                    }

                    public Resource[] getResources(String locationPattern) {
                        throw new UnsupportedOperationException();
                    }

                    public Resource getResource(String location) {
                        throw new UnsupportedOperationException();
                    }
                };
                applicationContext = new GenericApplicationContext(parent);
                applicationContext.registerBeanDefinition(serviceContext.getObjectName(), beanDefinition);
                applicationContext.refresh();
                service = applicationContext.getBean(serviceContext.getObjectName());

            } finally {
                Thread.currentThread().setContextClassLoader(oldClassLoader);
                ServiceContextThreadLocal.set(oldServiceContext);
            }

        } catch (Throwable t) {
            applicationContext = null;

            if (t instanceof Exception) {
                throw (Exception) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            } else {
                throw new Error(t);
            }
        }

        return service;
    }

    private ObjectName getObjectName(String name) throws MalformedObjectNameException {
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

    public void destroyService(ServiceContext serviceContext, Object service) {
        if (applicationContext != null) {
            applicationContext.close();
            applicationContext.destroy();
            applicationContext = null;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Set getPropertyNames() {
        return Collections.singleton(ROOT_BEAN_DEFINITION);
    }

    public Object getProperty(String propertyName) {
        if (ROOT_BEAN_DEFINITION.equals(propertyName)) {
            return beanDefinition;
        }

        throw new IllegalArgumentException("Unknown property: " + propertyName);
    }

    public void setProperty(String propertyName, Object persistentValue) {
        if (ROOT_BEAN_DEFINITION.equals(propertyName)) {
            beanDefinition = (RootBeanDefinition) persistentValue;
        }

        throw new IllegalArgumentException("Unknown property: " + propertyName);
    }
}
