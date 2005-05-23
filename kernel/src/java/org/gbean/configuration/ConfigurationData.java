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
package org.gbean.configuration;

import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gbean.repository.Repository;
import org.gbean.service.ServiceFactory;

/**
 * @version $Revision$ $Date$
 */
public class ConfigurationData {
    private static final Log log = LogFactory.getLog(ConfigurationData.class);

    /**
     * URI used to referr to this configuration in the configuration manager
     */
    private URI configurationId;

    /**
     * The uri of the parent of this configuration.  May be null.
     */
    private URI parentId;

    /**
     * The domain name of the configurations.  This is used to autogenerate names for sub components.
     */
    private String domain;

    /**
     * The server name of the configurations.  This is used to autogenerate names for sub components.
     */
    private String server;

    /**
     * List of URIs of jar files on which this configuration is dependent on.
     */
    private final LinkedHashSet dependencies = new LinkedHashSet();

    /**
     * Service factories contained in this configuration.
     */
    private final LinkedHashMap serviceFactories = new LinkedHashMap();

    /**
     * Child configurations of this configuration
     */
    private final List childConfigurations = new ArrayList();

    private ClassLoader classLoader;

    private Collection repositories;

    public ConfigurationData() {
    }

    public ConfigurationData(ConfigurationData configurationData) {
        configurationId = configurationData.configurationId;
        parentId = configurationData.getParentId();
        domain = configurationData.domain;
        server = configurationData.server;
        setDependencies(new ArrayList(configurationData.dependencies));
        setServiceFactories(configurationData.serviceFactories);
        setChildConfigurations(configurationData.childConfigurations);
    }

    public URI getConfigurationId() {
        return configurationId;
    }

    public void setConfigurationId(URI configurationId) {
        this.configurationId = configurationId;
    }

    public URI getParentId() {
        return parentId;
    }

    public void setParentId(URI parentId) {
        this.parentId = parentId;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public List getDependencies() {
        return Collections.unmodifiableList(new ArrayList(dependencies));
    }

    public void setDependencies(List dependencies) {
        if (classLoader != null) {
            throw new IllegalStateException("ClassLoader has already been resolved");
        }
        this.dependencies.clear();
        for (Iterator iterator = dependencies.iterator(); iterator.hasNext();) {
            URI dependency = (URI) iterator.next();
            addDependency(dependency);
        }
    }

    public void addDependency(URI dependency) {
        assert dependency != null;
        if (classLoader != null) {
            throw new IllegalStateException("ClassLoader has already been resolved");
        }
        this.dependencies.add(dependency);
    }

    public Map getServiceFactories() {
        return Collections.unmodifiableMap(serviceFactories);
    }

    public void setServiceFactories(Map serviceFactories) {
        this.serviceFactories.clear();
        for (Iterator iterator = serviceFactories.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            ObjectName name = (ObjectName) entry.getKey();
            ServiceFactory serviceFactory = (ServiceFactory) entry.getValue();
            addServiceFactory(name, serviceFactory);
        }
    }

    public void addServiceFactory(ObjectName name, ServiceFactory serviceFactory) {
        assert serviceFactory != null;
        serviceFactories.put(name, serviceFactory);
    }

    public List getChildConfigurations() {
        return Collections.unmodifiableList(childConfigurations);
    }

    public void setChildConfigurations(List childConfigurations) {
        this.childConfigurations.clear();
        for (Iterator iterator = childConfigurations.iterator(); iterator.hasNext();) {
            ConfigurationData configurationData = (ConfigurationData) iterator.next();
            addChildConfiguration(configurationData);
        }
    }
    public void addChildConfiguration(ConfigurationData configurationData) {
        assert configurationData != null;
        childConfigurations.add(configurationData);
    }

    public Collection getRepositories() {
        return repositories;
    }

    public void setRepositories(Collection repositories) {
        this.repositories = repositories;
    }

    public ClassLoader getClassLoader() {
        if (classLoader == null) {
            URL[] urls = resolveClassPath();
            log.debug("ClassPath for " + configurationId + " resolved to " + Arrays.asList(urls));

            if (parentId != null) {
                throw new UnsupportedOperationException("Parent configurations not supported yet: configurationId=" + configurationId);
            }
            classLoader = new URLClassLoader(urls, getClass().getClassLoader());
        }
        return classLoader;
    }

    private URL[] resolveClassPath() {
        List urls = new LinkedList();
        for (Iterator i = dependencies.iterator(); i.hasNext();) {
            URI uri = (URI) i.next();
            URL url = null;
            for (Iterator j = repositories.iterator(); j.hasNext();) {
                Repository repository = (Repository) j.next();
                if (repository.containsResource(uri)) {
                    url = repository.getResource(uri);
                    break;
                }
            }
            if (url == null) {
                throw new MissingDependencyException("Unable to resolve dependency " + uri);
            }
            urls.add(url);
        }
        return (URL[]) urls.toArray(new URL[urls.size()]);
    }
}
