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
package org.gbean.jmx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.JMRuntimeException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gbean.kernel.Kernel;
import org.gbean.kernel.LifecycleAdapter;
import org.gbean.kernel.ServiceName;
import org.gbean.kernel.ServiceNotFoundException;
import org.gbean.kernel.simple.SimpleLifecycle;
import org.gbean.reflect.ServiceInvokerManager;

/**
 * @version $Revision$ $Date$
 */
public class JMXBridge implements SimpleLifecycle {
    private static final ObjectName ALL = ServiceName.createName("*:*");
    private static final Log log = LogFactory.getLog(JMXBridge.class);

    private final HashMap registry = new HashMap();
    private final Kernel kernel;
    private final ServiceInvokerManager serviceInvokerManager;
    private final MBeanServer mbeanServer;

    public JMXBridge(Kernel kernel, ServiceInvokerManager serviceInvokerManager) {
        this.kernel = kernel;
        this.serviceInvokerManager = serviceInvokerManager;
        mbeanServer = MBeanServerFactory.createMBeanServer(kernel.getKernelName());
    }

    public JMXBridge(Kernel kernel, ServiceInvokerManager serviceInvokerManager, String mbeanServerId) {
        this.kernel = kernel;
        this.serviceInvokerManager = serviceInvokerManager;
        ArrayList servers = MBeanServerFactory.findMBeanServer(mbeanServerId);
        if (servers.size() == 0) {
            throw new IllegalStateException("No MBeanServers were found with the agent id " + mbeanServerId);
        } else if (servers.size() > 1) {
            throw new IllegalStateException(servers.size() + " MBeanServers were found with the agent id " + mbeanServerId);
        }
        mbeanServer = (MBeanServer) servers.get(0);
    }

    public JMXBridge(Kernel kernel, ServiceInvokerManager serviceInvokerManager, MBeanServer mbeanServer) {
        this.kernel = kernel;
        this.serviceInvokerManager = serviceInvokerManager;
        this.mbeanServer = mbeanServer;
    }

    public void start() {
        kernel.addLifecycleListener(new ServiceRegistrationListener(), ALL);

        HashMap beans = new HashMap();
        synchronized (this) {
            Set allNames = kernel.listServices(ALL);
            for (Iterator iterator = allNames.iterator(); iterator.hasNext();) {
                ObjectName objectName = (ObjectName) iterator.next();
                if (registry.containsKey(objectName)) {
                    // instance already registered
                    continue;
                }
                try {
                    ServiceMBean serviceMBean = new ServiceMBean(kernel, serviceInvokerManager, objectName);
                    registry.put(objectName, serviceMBean);
                    beans.put(objectName, serviceMBean);
                } catch (ServiceNotFoundException e) {
                    // ignore - service died on us
                }
            }
        }
        for (Iterator iterator = beans.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            ObjectName objectName = (ObjectName) entry.getKey();
            ServiceMBean serviceMBean = (ServiceMBean) entry.getValue();
            try {
                mbeanServer.registerMBean(serviceMBean, objectName);
            } catch (InstanceAlreadyExistsException e) {
                // ignore - service already has an mbean shadow object
            } catch (Exception e) {
                log.info("Unable to register MBean shadow object for service: " + objectName, unwrapJMException(e));
            }
        }
    }

    public void stop() {
        // unregister all of our mbeans from the MBeanServer
        Map beans;
        synchronized (this) {
            beans = new HashMap(registry);
            registry.clear();
        }
        for (Iterator i = beans.keySet().iterator(); i.hasNext();) {
            ObjectName objectName = (ObjectName) i.next();
            try {
                mbeanServer.unregisterMBean(objectName);
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private void register(ObjectName objectName) {
        try {
            ServiceMBean serviceMBean = null;
            synchronized (this) {
                if (registry.containsKey(objectName)) {
                    return;
                }
                serviceMBean = new ServiceMBean(kernel, serviceInvokerManager, objectName);
                registry.put(objectName, serviceMBean);
            }
            mbeanServer.registerMBean(serviceMBean, objectName);
        } catch (InstanceAlreadyExistsException e) {
            // ignore - service already has a mbean shadow object
        } catch (Exception e) {
            log.info("Unable to register MBean shadow object for service", unwrapJMException(e));
        }
    }

    private void unregister(ObjectName objectName) {
        synchronized (this) {
            if (registry.remove(objectName) == null) {
                return;
            }
        }

        try {
            mbeanServer.unregisterMBean(objectName);
        } catch (InstanceNotFoundException e) {
            // ignore - something else may have unregistered us
            // if there truely is no service then we will catch it below whwn we call the superclass
        } catch (Exception e) {
            log.info("Unable to unregister MBean shadow object for service", unwrapJMException(e));
        }
    }

    private Throwable unwrapJMException(Throwable cause) {
        while ((cause instanceof JMException || cause instanceof JMRuntimeException) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
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
