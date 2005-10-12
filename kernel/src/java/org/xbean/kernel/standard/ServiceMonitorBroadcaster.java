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
package org.xbean.kernel.standard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xbean.kernel.ServiceMonitor;
import org.xbean.kernel.KernelMonitor;
import org.xbean.kernel.ServiceName;
import org.xbean.kernel.ServiceEvent;
import org.xbean.kernel.KernelErrorsError;

/**
 * The ServiceMonitorBroadcaster broadcasts kernel events to registered service monitors.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class ServiceMonitorBroadcaster implements ServiceMonitor {
    /**
     * The monitors for service events.
     */
    private final Map serviceMonitors = new LinkedHashMap();

    /**
     * The monitor we notify when we get an exception from a service monitor.
     */
    private final KernelMonitor kernelMonitor;

    /**
     * Creates a ServiceMonitorBroadcaster that notifies the specified kernel monitor when an error occurs while
     * notifying the registered service monitors.
     *
     * @param kernelMonitor the monitor to notify when an error occurs while notifying the registered service monitors
     */
    public ServiceMonitorBroadcaster(KernelMonitor kernelMonitor) {
        if (kernelMonitor == null) throw new NullPointerException("kernelMonitor is null");
        this.kernelMonitor = kernelMonitor;
    }

    /**
     * Adds a service monitor for a specific service, or if the specified service name is null, a global monitor.
     * <p/>
     * Note: the order in which service monitors are notified is not specified.
     *
     * @param serviceMonitor the service monitor to add
     * @param serviceName the unique name of the service to monitor or null to monitor all services
     */
    public void addServiceMonitor(ServiceMonitor serviceMonitor, ServiceName serviceName) {
        if (serviceMonitor == null) throw new NullPointerException("serviceMonitor is null");
        synchronized (serviceMonitors) {
            Set monitors = (Set) serviceMonitors.get(serviceName);
            if (monitors == null) {
                monitors = new LinkedHashSet();
                serviceMonitors.put(serviceName, monitors);
            }
            monitors.add(serviceMonitor);
        }
    }

    /**
     * Removes a service monitor.
     *
     * @param serviceMonitor the service monitor to remove
     */
    public void removeServiceMonitor(ServiceMonitor serviceMonitor) {
        if (serviceMonitor == null) throw new NullPointerException("serviceMonitor is null");
        synchronized (serviceMonitors) {
            for (Iterator iterator = serviceMonitors.values().iterator(); iterator.hasNext();) {
                Set monitors = (Set) iterator.next();
                monitors.remove(serviceMonitor);
                if (monitors.isEmpty()) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Gets the service monitors registered to recieve events for the specified service.  This will include all global
     * monitors and service specific monitors.
     *
     * @param serviceName the name of the service
     * @return the monitors registerd to recieve events for the specified service
     */
    private Set getServiceMonitors(ServiceName serviceName) {
        synchronized (serviceMonitors) {
            Set monitors = new LinkedHashSet();
            Set globalMonitors = (Set) serviceMonitors.get(null);
            if (globalMonitors != null) {
                monitors.addAll(globalMonitors);
            }
            Set specificMonitors = (Set) serviceMonitors.get(serviceName);
            if (specificMonitors != null) {
                monitors.addAll(specificMonitors);
            }
            return monitors;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void serviceRegistered(ServiceEvent serviceEvent) {
        List errors = new ArrayList();
        Set serviceMonitors = getServiceMonitors(serviceEvent.getServiceName());
        for (Iterator iterator = serviceMonitors.iterator(); iterator.hasNext();) {
            ServiceMonitor serviceMonitor = (ServiceMonitor) iterator.next();
            try {
                serviceMonitor.serviceRegistered(serviceEvent);
            } catch (Throwable e) {
                errors.addAll(fireServiceNotificationError(serviceMonitor, serviceEvent, e));
            }
        }
        if (!errors.isEmpty()) {
            throw new KernelErrorsError(errors);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void serviceStarting(ServiceEvent serviceEvent) {
        List errors = new ArrayList();
        Set serviceMonitors = getServiceMonitors(serviceEvent.getServiceName());
        for (Iterator iterator = serviceMonitors.iterator(); iterator.hasNext();) {
            ServiceMonitor serviceMonitor = (ServiceMonitor) iterator.next();
            try {
                serviceMonitor.serviceStarting(serviceEvent);
            } catch (Throwable e) {
                errors.addAll(fireServiceNotificationError(serviceMonitor, serviceEvent, e));
            }
        }
        if (!errors.isEmpty()) {
            throw new KernelErrorsError(errors);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void serviceWaitingToStart(ServiceEvent serviceEvent) {
        List errors = new ArrayList();
        Set serviceMonitors = getServiceMonitors(serviceEvent.getServiceName());
        for (Iterator iterator = serviceMonitors.iterator(); iterator.hasNext();) {
            ServiceMonitor serviceMonitor = (ServiceMonitor) iterator.next();
            try {
                serviceMonitor.serviceWaitingToStart(serviceEvent);
            } catch (Throwable e) {
                errors.addAll(fireServiceNotificationError(serviceMonitor, serviceEvent, e));
            }
        }
        if (!errors.isEmpty()) {
            throw new KernelErrorsError(errors);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void serviceStartError(ServiceEvent serviceEvent) {
        List errors = new ArrayList();
        Set serviceMonitors = getServiceMonitors(serviceEvent.getServiceName());
        for (Iterator iterator = serviceMonitors.iterator(); iterator.hasNext();) {
            ServiceMonitor serviceMonitor = (ServiceMonitor) iterator.next();
            try {
                serviceMonitor.serviceStartError(serviceEvent);
            } catch (Throwable e) {
                errors.addAll(fireServiceNotificationError(serviceMonitor, serviceEvent, e));
            }
        }
        if (!errors.isEmpty()) {
            throw new KernelErrorsError(errors);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void serviceRunning(ServiceEvent serviceEvent) {
        List errors = new ArrayList();
        Set serviceMonitors = getServiceMonitors(serviceEvent.getServiceName());
        for (Iterator iterator = serviceMonitors.iterator(); iterator.hasNext();) {
            ServiceMonitor serviceMonitor = (ServiceMonitor) iterator.next();
            try {
                serviceMonitor.serviceRunning(serviceEvent);
            } catch (Throwable e) {
                errors.addAll(fireServiceNotificationError(serviceMonitor, serviceEvent, e));
            }
        }
        if (!errors.isEmpty()) {
            throw new KernelErrorsError(errors);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void serviceStopping(ServiceEvent serviceEvent) {
        List errors = new ArrayList();
        Set serviceMonitors = getServiceMonitors(serviceEvent.getServiceName());
        for (Iterator iterator = serviceMonitors.iterator(); iterator.hasNext();) {
            ServiceMonitor serviceMonitor = (ServiceMonitor) iterator.next();
            try {
                serviceMonitor.serviceStopping(serviceEvent);
            } catch (Throwable e) {
                errors.addAll(fireServiceNotificationError(serviceMonitor, serviceEvent, e));
            }
        }
        if (!errors.isEmpty()) {
            throw new KernelErrorsError(errors);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void serviceWaitingToStop(ServiceEvent serviceEvent) {
        List errors = new ArrayList();
        Set serviceMonitors = getServiceMonitors(serviceEvent.getServiceName());
        for (Iterator iterator = serviceMonitors.iterator(); iterator.hasNext();) {
            ServiceMonitor serviceMonitor = (ServiceMonitor) iterator.next();
            try {
                serviceMonitor.serviceWaitingToStop(serviceEvent);
            } catch (Throwable e) {
                errors.addAll(fireServiceNotificationError(serviceMonitor, serviceEvent, e));
            }
        }
        if (!errors.isEmpty()) {
            throw new KernelErrorsError(errors);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void serviceStopError(ServiceEvent serviceEvent) {
        List errors = new ArrayList();
        Set serviceMonitors = getServiceMonitors(serviceEvent.getServiceName());
        for (Iterator iterator = serviceMonitors.iterator(); iterator.hasNext();) {
            ServiceMonitor serviceMonitor = (ServiceMonitor) iterator.next();
            try {
                serviceMonitor.serviceStopError(serviceEvent);
            } catch (Throwable e) {
                errors.addAll(fireServiceNotificationError(serviceMonitor, serviceEvent, e));
            }
        }
        if (!errors.isEmpty()) {
            throw new KernelErrorsError(errors);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void serviceStopped(ServiceEvent serviceEvent) {
        List errors = new ArrayList();
        Set serviceMonitors = getServiceMonitors(serviceEvent.getServiceName());
        for (Iterator iterator = serviceMonitors.iterator(); iterator.hasNext();) {
            ServiceMonitor serviceMonitor = (ServiceMonitor) iterator.next();
            try {
                serviceMonitor.serviceStopped(serviceEvent);
            } catch (Throwable e) {
                errors.addAll(fireServiceNotificationError(serviceMonitor, serviceEvent, e));
            }
        }
        if (!errors.isEmpty()) {
            throw new KernelErrorsError(errors);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void serviceUnregistered(ServiceEvent serviceEvent) {
        List errors = new ArrayList();
        Set serviceMonitors = getServiceMonitors(serviceEvent.getServiceName());
        for (Iterator iterator = serviceMonitors.iterator(); iterator.hasNext();) {
            ServiceMonitor serviceMonitor = (ServiceMonitor) iterator.next();
            try {
                serviceMonitor.serviceUnregistered(serviceEvent);
            } catch (Throwable e) {
                errors.addAll(fireServiceNotificationError(serviceMonitor, serviceEvent, e));
            }
        }
        if (!errors.isEmpty()) {
            throw new KernelErrorsError(errors);
        }
    }

    private List fireServiceNotificationError(ServiceMonitor serviceMonitor, ServiceEvent serviceEvent, Throwable throwable) {
        try {
            kernelMonitor.serviceNotificationError(serviceMonitor, serviceEvent, throwable);
        } catch (RuntimeException ignored) {
            // ignore - we did our best to notify the world
        } catch (KernelErrorsError e) {
            return e.getErrors();
        } catch (Error e) {
            return Collections.singletonList(e);
        }
        return Collections.EMPTY_LIST;
    }
}
