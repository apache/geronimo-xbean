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
package org.gbean.reflect;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.management.ObjectName;

import org.gbean.kernel.Kernel;
import org.gbean.kernel.LifecycleAdapter;
import org.gbean.kernel.ServiceName;
import org.gbean.kernel.ServiceNotFoundException;
import org.gbean.kernel.simple.SimpleLifecycle;

/**
 * @version $Revision$ $Date$
 */
public class ServiceInvokerManager implements SimpleLifecycle {
    private static final ObjectName ALL = ServiceName.createName("*:*");
    private final Kernel kernel;
    private final Map registry = new HashMap();

    public ServiceInvokerManager(Kernel kernel) {
        this.kernel = kernel;
    }

    public synchronized ServiceInvoker getServiceInvoker(ObjectName objectName) throws ServiceNotFoundException {
        ServiceInvoker serviceInvoker = (ServiceInvoker) registry.get(objectName);
        if (serviceInvoker != null) {
            serviceInvoker.updateState();
        } else {
            if (kernel.isLoaded(objectName)) {
                register(objectName);
                serviceInvoker = (ServiceInvoker) registry.get(objectName);
            }
        }

        if (serviceInvoker == null) {
            throw new ServiceNotFoundException(objectName.getCanonicalName());
        }
        return serviceInvoker;
    }

    public void start() {
        kernel.addLifecycleListener(new ServiceRegistrationListener(), ALL);

        HashSet invokers = new HashSet();
        synchronized (this) {
            Set allNames = kernel.listServices(ALL);
            for (Iterator iterator = allNames.iterator(); iterator.hasNext();) {
                ObjectName objectName = (ObjectName) iterator.next();
                if (registry.containsKey(objectName)) {
                    // instance already registered
                    continue;
                }
                ServiceInvoker serviceInvoker = new ServiceInvoker(kernel, objectName);
                registry.put(objectName, serviceInvoker);
                invokers.add(serviceInvoker);
            }
        }
        for (Iterator iterator = invokers.iterator(); iterator.hasNext();) {
            ServiceInvoker serviceInvoker = (ServiceInvoker) iterator.next();
            serviceInvoker.start();
        }
    }

    public void stop() {
        // unregister all of our GBeans from the MBeanServer
        HashSet invokers;
        synchronized (this) {
            invokers = new HashSet(registry.values());
            registry.clear();
        }
        for (Iterator iterator = invokers.iterator(); iterator.hasNext();) {
            ServiceInvoker serviceInvoker = (ServiceInvoker) iterator.next();
            serviceInvoker.stop();
        }
    }

    private void register(ObjectName objectName) {
        ServiceInvoker serviceInvoker = null;
        synchronized (this) {
            if (registry.containsKey(objectName)) {
                return;
            }
            serviceInvoker = new ServiceInvoker(kernel, objectName);
            registry.put(objectName, serviceInvoker);
        }
        serviceInvoker.start();
    }

    private void unregister(ObjectName objectName) {
        ServiceInvoker serviceInvoker = null;
        synchronized (this) {
            serviceInvoker = (ServiceInvoker) registry.remove(objectName);
            if (serviceInvoker == null) {
                return;
            }
        }

        serviceInvoker.stop();
    }

    private class ServiceRegistrationListener extends LifecycleAdapter {
        public void loaded(ObjectName objectName) {
            register(objectName);
        }

        public void unloaded(ObjectName objectName) {
            unregister(objectName);
        }
    }
}
