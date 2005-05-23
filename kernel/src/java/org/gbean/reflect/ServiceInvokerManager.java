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
package org.gbean.reflect;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gbean.kernel.Kernel;
import org.gbean.kernel.LifecycleAdapter;
import org.gbean.kernel.ServiceName;
import org.gbean.kernel.ServiceNotFoundException;
import org.gbean.kernel.simple.SimpleLifecycle;

/**
 * @version $Revision$ $Date$
 */
public class ServiceInvokerManager implements SimpleLifecycle {
    private static final Log log = LogFactory.getLog(ServiceInvokerManager.class);
    private static final ObjectName ALL = ServiceName.createName("*:*");
    private final Kernel kernel;
    private final Map registry = new HashMap();
    private final ServiceRegistrationListener lifecycleListener;

    public ServiceInvokerManager(Kernel kernel) {
        this.kernel = kernel;
        lifecycleListener = new ServiceRegistrationListener();
    }

    public synchronized void start() {
        kernel.addLifecycleListener(lifecycleListener, ALL);
        Set allNames = kernel.listServices(ALL);
        for (Iterator iterator = allNames.iterator(); iterator.hasNext();) {
            ObjectName objectName = (ObjectName) iterator.next();
            try {
                register(objectName);
            } catch (Exception e) {
                log.info("Unable to create service invoker for " + objectName, e);
            }
        }
    }

    public synchronized ServiceInvoker getServiceInvoker(ObjectName objectName) throws ServiceNotFoundException, IllegalStateException {
        ServiceInvoker serviceInvoker = (ServiceInvoker) registry.get(objectName);
        if (serviceInvoker != null) {
            serviceInvoker.assureRunning();
        } else {
            register(objectName);
            serviceInvoker = (ServiceInvoker) registry.get(objectName);
        }

        if (serviceInvoker == null) {
            throw new ServiceNotFoundException(objectName.getCanonicalName());
        }
        return serviceInvoker;
    }

    public synchronized void stop() {
        kernel.removeLifecycleListener(lifecycleListener);
        registry.clear();
    }

    private void register(ObjectName objectName) throws ServiceNotFoundException, IllegalStateException {
        synchronized (this) {
            if (registry.containsKey(objectName)) {
                return;
            }

            ServiceInvoker serviceInvoker = new ServiceInvoker(kernel, objectName);
            serviceInvoker.start();
            registry.put(objectName, serviceInvoker);
        }
    }

    private void unregister(ObjectName objectName) {
        synchronized (this) {
            ServiceInvoker serviceInvoker = (ServiceInvoker) registry.remove(objectName);
            if (serviceInvoker != null) {
                serviceInvoker.stop();
            }
        }
    }

    private class ServiceRegistrationListener extends LifecycleAdapter {
        public void running(ObjectName objectName) {
            try {
                register(objectName);
            } catch (Exception e) {
                log.info("Unable to create service invoker for " + objectName, e);
            }
        }

        public void stopped(ObjectName objectName) {
            unregister(objectName);
        }

        public void unloaded(ObjectName objectName) {
            unregister(objectName);
        }
    }
}
