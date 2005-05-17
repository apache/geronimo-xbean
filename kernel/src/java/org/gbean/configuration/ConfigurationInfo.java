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
package org.gbean.configuration;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * @version $Revision$ $Date$
 */
public class ConfigurationInfo {
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

    public ConfigurationInfo() {
    }

    public ConfigurationInfo(ConfigurationInfo configurationInfo) {
        configurationId = configurationInfo.configurationId;
        parentId = configurationInfo.getParentId();
        domain = configurationInfo.domain;
        server = configurationInfo.server;
        setDependencies(new ArrayList(configurationInfo.dependencies));
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
        this.dependencies.clear();
        for (Iterator iterator = dependencies.iterator(); iterator.hasNext();) {
            URI dependency = (URI) iterator.next();
            addDependency(dependency);
        }
    }

    public void addDependency(URI dependency) {
        assert dependency != null;
        this.dependencies.add(dependency);
    }
}
