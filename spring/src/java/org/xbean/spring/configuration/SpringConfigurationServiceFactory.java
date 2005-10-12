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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

import org.xbean.kernel.AbstractServiceFactory;
import org.xbean.kernel.ServiceCondition;
import org.xbean.kernel.ServiceConditionContext;
import org.xbean.kernel.ServiceContext;
import org.xbean.kernel.ServiceFactory;
import org.xbean.kernel.ServiceName;
import org.xbean.spring.context.SpringApplicationContext;

/**
 * SpringConfigurationServiceFactory is manages the creation and destruction of a SpringConfiguration.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class SpringConfigurationServiceFactory extends AbstractServiceFactory {
    private final SpringApplicationContext applicationContext;
    private final ConfigurationStopCondition configurationStopCondition;
    private SpringConfiguration springConfiguration;

    /**
     * Creates a SpringConfigurationServiceFactory that wraps the specified application context.
     * @param applicationContext the application context for this configuration
     */
    public SpringConfigurationServiceFactory(SpringApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        configurationStopCondition = new ConfigurationStopCondition();
        addStopCondition(configurationStopCondition);
    }

    /**
     * Gets the unique id if this configuration.
     * @return the unique id if this configuration
     */
    public String getId() {
        return applicationContext.getDisplayName();
    }

    /**
     * Gets the application context wrapped by this configuration.  Use caution when modifiying this context as it can
     * effect the running state of services.
     * @return the application context wrapped by this configuration
     */
    public SpringApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * {@inheritDoc}
     */
    public Class[] getTypes() {
        return new Class[]{SpringConfiguration.class};
    }

    /**
     * SpringConfigurationServiceFactory is restartable so this method always returns true.
     * @return true
     */
    public boolean isRestartable() {
        return true;
    }

    /**
     * Gets the ServiceNames of the services defined in the application context if the configuration has been started,
     * otherwise this method returns an empty set.
     *
     * @return the ServiceNames of the services defined in the application context if the configuration has been started
     */
    public Set getOwnedServices() {
        if (springConfiguration != null) {
            return new HashSet(springConfiguration.getServiceFactories().keySet());
        }
        return Collections.EMPTY_SET;
    }

    /**
     * {@inheritDoc}
     */
    public Object createService(ServiceContext serviceContext) throws Exception {
        springConfiguration = new SpringConfiguration(applicationContext, serviceContext.getKernel());

        // add owned service stop conditions
        Set ownedServices = springConfiguration.getServiceFactories().keySet();
        for (Iterator iterator = springConfiguration.getServiceFactories().entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            ServiceName serviceName = (ServiceName) entry.getKey();
            ServiceFactory serviceFactory = (ServiceFactory) entry.getValue();
            if (ownedServices.contains(serviceName)) {
                serviceFactory.addStopCondition(configurationStopCondition.createOwnedServiceStopCondition());
            }
        }

        return springConfiguration;
    }

    /**
     * {@inheritDoc}
     */
    public void destroyService(ServiceContext serviceContext) {
        if (springConfiguration != null) {
            springConfiguration.destroy();
            springConfiguration = null;
        }
    }

    private static class ConfigurationStopCondition implements ServiceCondition {
        private final List ownedServiceConditions = new ArrayList();

        public synchronized void initialize(ServiceConditionContext context) {
            for (Iterator iterator = ownedServiceConditions.iterator(); iterator.hasNext();) {
                OwnedServiceCondition ownedServiceCondition = (OwnedServiceCondition) iterator.next();
                ownedServiceCondition.setSatisfied();
            }
        }

        public boolean isSatisfied() {
            return true;
        }

        public synchronized void destroy() {
            for (Iterator iterator = ownedServiceConditions.iterator(); iterator.hasNext();) {
                OwnedServiceCondition ownedServiceCondition = (OwnedServiceCondition) iterator.next();
                ownedServiceCondition.setSatisfied();
            }
        }

        public ServiceCondition createOwnedServiceStopCondition() {
            ServiceCondition ownedServiceCondition = new OwnedServiceCondition();
            ownedServiceConditions.add(ownedServiceCondition);
            return ownedServiceCondition;
        }

        private static class OwnedServiceCondition implements ServiceCondition {
            private boolean satisfied = false;
            private ServiceConditionContext context;

            public synchronized void initialize(ServiceConditionContext context) {
                this.context = context;
            }

            public synchronized boolean isSatisfied() {
                return satisfied;
            }

            private void setSatisfied() {
                this.satisfied = true;
                if (context != null) {
                    context.setSatisfied();
                }
            }

            public synchronized void destroy() {
                context = null;
            }
        }
    }
}
