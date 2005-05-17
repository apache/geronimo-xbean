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
package org.gbean.kernel.simple;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.gbean.service.ServiceContext;
import org.gbean.service.ServiceFactory;

/**
 * @version $Revision$ $Date$
 */
public class SimpleServiceFactory implements ServiceFactory {
    private final Object service;
    private final Map dependencies = new HashMap();
    private boolean enabled = true;

    public SimpleServiceFactory(Object service) {
        this.service = service;
    }

    public Map getDependencies() {
        return dependencies;
    }

    public void addDependency(String name, Set patterns) {
        dependencies.put(name, patterns);
    }

    public Object createService(ServiceContext serviceContext) throws Exception {
        if (service instanceof SimpleLifecycle) {
            ((SimpleLifecycle) service).start();
        }
        return service;
    }

    public void destroyService(ServiceContext serviceContext, Object service) {
        if (service != this.service) {
            throw new IllegalArgumentException("Wrong service instance");
        }
        if (service instanceof SimpleLifecycle) {
            ((SimpleLifecycle) service).stop();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
