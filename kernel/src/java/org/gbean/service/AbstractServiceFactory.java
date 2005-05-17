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
package org.gbean.service;

import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.HashMap;

/**
 * @version $Revision$ $Date$
 */
public abstract class AbstractServiceFactory implements ServiceFactory {
    private boolean enabled = true;
    private Map dependencies = new HashMap();

    public Map getDependencies() {
        return dependencies;
    }

    public void addDependency(String name, Set patterns) {
        dependencies.put(name, patterns);
    }

    public void destroyService(ServiceContext serviceContext, Object service) {
    }

    public Set getPropertyNames() {
        return Collections.EMPTY_SET;
    }

    public Map getProperties() {
        return Collections.EMPTY_MAP;
    }

    public void setProperties(Map properties) {
        assert properties != null;
        if (properties.size() > 0) {
            throw new IllegalArgumentException("This service factory does not have any properties");
        }
    }

    public Object getProperty(String name) {
        throw new IllegalArgumentException("This service factory does not have any properties");
    }

    public void setProperty(String name, Object value) {
        throw new IllegalArgumentException("This service factory does not have any properties");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
