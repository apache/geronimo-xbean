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
package org.xbean.spring.configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.gbean.kernel.Kernel;
import org.gbean.kernel.ServiceAlreadyExistsException;
import org.gbean.kernel.ServiceName;
import org.gbean.kernel.ServiceNotFoundException;
import org.gbean.kernel.ServiceRegistrationException;
import org.gbean.kernel.StaticServiceFactory;
import org.gbean.kernel.StringServiceName;
import org.springframework.beans.BeansException;
import org.xbean.spring.context.SpringApplicationContext;
import org.xbean.spring.loader.SpringLoader;

/**
 * SpringConfiguration that registers and unregisters services that have been defined in a SpringApplicationContext.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class SpringConfiguration {
    private final SpringApplicationContext applicationContext;
    private final Map serviceFactories;
    private final Kernel kernel;

    /**
     * Creates a SpringConfiguration that registers and unregisters services that have been defined in a Spring ApplicationContext.
     * @param applicationContext the application context from which services are registered
     * @param kernel the kernel in which services are registered and unregistered
     * @throws Exception if a problem occurs while registering the services fromt he application context
     */
    public SpringConfiguration(SpringApplicationContext applicationContext, Kernel kernel) throws Exception {
        this.applicationContext = applicationContext;
        this.kernel = kernel;

        ClassLoader classLoader = getClassLoader(applicationContext);
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);

        try {
            // register the configuration file from source
            applicationContext.refresh();

            // build a map from bean name to service name
            Map serviceNameIndex = buildServiceNameIndex(applicationContext);

            // Use Spring to create all of the beans
            Map factories = new HashMap(serviceNameIndex.size());
            for (Iterator iterator = serviceNameIndex.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry entry = (Map.Entry) iterator.next();
                String beanName = (String) entry.getKey();
                ServiceName serviceName = (ServiceName) entry.getValue();

                Object bean = applicationContext.getBean(beanName);
                StaticServiceFactory serviceFactory = new StaticServiceFactory(bean);
                factories.put(serviceName, serviceFactory);
            }
            serviceFactories = Collections.unmodifiableMap(factories);

            // register each bean with the kernel
            for (Iterator iterator = serviceFactories.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry entry = (Map.Entry) iterator.next();
                ServiceName serviceName = (ServiceName) entry.getKey();
                StaticServiceFactory serviceFactory = (StaticServiceFactory) entry.getValue();
                kernel.registerService(serviceName, serviceFactory, classLoader);
            }

        } catch (BeansException e) {
            applicationContext.close();
            throw e;
        } catch (ServiceAlreadyExistsException e) {
            destroy();
            throw e;
        } catch (ServiceRegistrationException e) {
            destroy();
            throw e;
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    /**
     * Gets the unique identifier of this configuration.
     * @return the unique identifier of this configuration
     */
    public String getId() {
        return applicationContext.getDisplayName();
    }

    /**
     * Gets the service factories for the services defined in this configuration by ServiceName.
     * @return the service factories for the services defined in this configuration by ServiceName
     */
    public Map getServiceFactories() {
        return serviceFactories;
    }

    /**
     * Unregisters all of the services registered with the kernel in the constructor.
     */
    public void destroy() {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader(applicationContext));
        try {
            for (Iterator iterator = serviceFactories.keySet().iterator(); iterator.hasNext();) {
                ServiceName serviceName = (ServiceName) iterator.next();
                try {
                    kernel.unregisterService(serviceName);
                } catch (ServiceNotFoundException e) {
                    // Doesn't matter
                } catch (ServiceRegistrationException e) {
                    // todo ignored...
                }
            }

            applicationContext.close();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private static Map buildServiceNameIndex(SpringApplicationContext applicationContext) {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        Map serviceNameIndex = new HashMap(beanNames.length);
        for (int i = 0; i < beanNames.length; i++) {
            String beanName = beanNames[i];
            ServiceName serviceName = new StringServiceName(beanName);
            serviceNameIndex.put(beanName, serviceName);
        }
        return serviceNameIndex;
    }

    private static ClassLoader getClassLoader(SpringApplicationContext applicationContext) {
        ClassLoader classLoader = applicationContext.getClassLoader();
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        if (classLoader == null) {
            classLoader = SpringLoader.class.getClassLoader();
        }
        return classLoader;
    }
}
