/**
 *
 * Copyright 2005 (C) The original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.xbean.jmx;

import java.util.HashMap;
import java.util.Map;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;

import org.xbean.jmx.config.JMXServiceConfig;
import org.xbean.kernel.Kernel;
import org.xbean.kernel.NullServiceMonitor;
import org.xbean.kernel.ServiceEvent;
import org.xbean.kernel.ServiceName;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;


/**
 * @version $Revision: $ $Date: $
 */
public class JMXServiceMonitor extends NullServiceMonitor {

    private final static Log log = LogFactory.getLog(JMXServiceMonitor.class);
    private final static JMXStrategyFinder FINDER = new JMXStrategyFinder("META-INF/org.xbean.jmx.StrategyFinder/");
    private final Map configurations = new HashMap();
    private final Map strategies = new HashMap();
    private final MBeanServer server;

    public JMXServiceMonitor(Kernel kernel, MBeanServer server) {
        if (kernel == null) throw new IllegalArgumentException("kernel is null");
        if (server == null) throw new IllegalArgumentException("server is null");

        kernel.addServiceMonitor(this);
        this.server = server;
    }

    /**
     * {@inheritDoc}
     */
    public void serviceStarting(ServiceEvent serviceEvent) {
        Cache entry = (Cache) configurations.get(serviceEvent.getServiceName());
        if (entry != null) {
            try {
                server.registerMBean(entry.strategy.wrapService(serviceEvent.getService(), entry.configuration),
                                     entry.configuration.getObjectName());
            } catch (InstanceAlreadyExistsException doNothing) {
                log.error("Starting " + serviceEvent.getServiceName(), doNothing);
            } catch (MBeanRegistrationException doNothing) {
                log.error("Starting " + serviceEvent.getServiceName(), doNothing);
            } catch (NotCompliantMBeanException doNothing) {
                log.error("Starting " + serviceEvent.getServiceName(), doNothing);
            } catch (JMXServiceException doNothing) {
                log.error("Starting " + serviceEvent.getServiceName(), doNothing);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void serviceStopping(ServiceEvent serviceEvent) {
        Cache entry = (Cache) configurations.get(serviceEvent.getServiceName());
        if (entry != null) {
            try {
                server.unregisterMBean(entry.configuration.getObjectName());
                entry.strategy.unwrapService(serviceEvent.getService(), entry.configuration);
            } catch (InstanceNotFoundException doNothing) {
                log.error("Starting " + serviceEvent.getServiceName(), doNothing);
            } catch (MBeanRegistrationException doNothing) {
                log.error("Starting " + serviceEvent.getServiceName(), doNothing);
            }
        }
    }

    /**
     * Add a service configuration to the monitor.  The method loads the wrapping
     * strategy, if it has not been loaded already.
     *
     * @param service of the service to be wrapped.
     * @param config  the details of how the service is to be wrapped.
     * @throws JMXServiceException if no wrapping strategy exists for the config.
     */
    public void addServiceConfig(ServiceName service, JMXServiceConfig config) throws JMXServiceException {
        try {
            String name = config.getStrategy();
            JMXWrappingStrategy strategy = (JMXWrappingStrategy) strategies.get(name);
            if (strategy == null) {
                strategy = FINDER.newInstance(name);
                strategies.put(name, strategy);
            }
            configurations.put(service, new Cache(config, strategy));
        } catch (JMXServiceException doNothing) {
            log.error("Adding " + service, doNothing);
            throw doNothing;
        }
    }

    private class Cache {
        final JMXServiceConfig configuration;
        final JMXWrappingStrategy strategy;

        public Cache(JMXServiceConfig configuration, JMXWrappingStrategy strategy) {
            this.configuration = configuration;
            this.strategy = strategy;
        }
    }
}
