/**
 *
 * Copyright 2005 GBean.org
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

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.kernel.management.State;
import org.apache.geronimo.kernel.config.ConfigurationClassLoader;
import org.gbean.configuration.Configuration;
import org.gbean.configuration.ConfigurationInfo;
import org.gbean.kernel.Kernel;
import org.gbean.kernel.simple.SimpleLifecycle;
import org.gbean.service.ServiceFactory;

/**
 * @version $Revision$ $Date$
 */
public class SpringConfiguration implements Configuration, SimpleLifecycle {
    private static final Log log = LogFactory.getLog(SpringConfiguration.class);

    public static ObjectName getConfigurationObjectName(URI configId) throws MalformedObjectNameException {
        return new ObjectName("geronimo.config:name=" + ObjectName.quote(configId.toString()));
    }

    private final Kernel kernel;

    private final ObjectName objectName;

    private final URI configurationId;

    private final String domain;

    private final String server;

    private final Map serviceFactories;

    private final ClassLoader classLoader;

    public SpringConfiguration(Kernel kernel,
            ConfigurationInfo configurationInfo,
            Map serviceFactories,
            ClassLoader classLoader) throws Exception {

        this.kernel = kernel;
        this.configurationId = configurationInfo.getConfigurationId();
        this.objectName = getConfigurationObjectName(configurationId);

        this.domain = configurationInfo.getDomain();
        this.server = configurationInfo.getServer();

        this.serviceFactories = serviceFactories;

        this.classLoader = classLoader;
    }

    public ObjectName getObjectName() {
        return objectName;
    }

    public URI getConfigurationId() {
        return configurationId;
    }

    public URI getParentId() {
        return null;
    }

    public String getDomain() {
        return domain;
    }

    public String getServer() {
        return server;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    // here for compatability with geronimo
    public ConfigurationClassLoader getConfigurationClassLoader() {
        return (ConfigurationClassLoader) classLoader;
    }

    public Set getServiceNames() {
        return new HashSet(serviceFactories.keySet());
    }

    public void start() throws Exception {
        for (Iterator iterator = serviceFactories.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            ObjectName serviceName = (ObjectName) entry.getKey();
            ServiceFactory serviceFactory = (ServiceFactory) entry.getValue();
            serviceFactory.addDependency("GBeanConfiguration", Collections.singleton(objectName));

            kernel.loadService(serviceName, serviceFactory, classLoader);
            kernel.startService(serviceName);
            int serviceState = kernel.getServiceState(serviceName);
            String msg = serviceName.getCanonicalName() + " - " + State.fromInt(serviceState);
            log.info(msg);
        }

        log.info("Started configuration " + configurationId);
    }

    public void stop() {
        log.info("Stopping configuration " + configurationId);

        // unregister all services
        for (Iterator i = serviceFactories.keySet().iterator(); i.hasNext();) {
            ObjectName serviceName = (ObjectName) i.next();
            try {
                log.trace("Unload GBean " + serviceName);
                kernel.unloadService(serviceName);
            } catch (Exception e) {
                // ignore
                log.warn("Could not unload service " + serviceName, e);
            }
        }
    }
}
