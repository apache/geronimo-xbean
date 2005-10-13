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

import edu.emory.mathcs.backport.java.util.concurrent.Executor;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import org.xbean.kernel.Kernel;
import org.xbean.kernel.ServiceFactory;
import org.xbean.kernel.ServiceName;

/**
 * The ServiceManagerFactory handles the construction ServiceManagers.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class ServiceManagerFactory {
    /**
     * The kernel in which the service will be bound.
     */
    private final Kernel kernel;

    /**
     * This monitor broadcasts events to the listeners registered for service.
     */
    private final ServiceMonitorBroadcaster serviceMonitor;

    /**
     * Events service events are sent asynchronously using this executor.
     */
    private final Executor serviceExecutor;

    /**
     * The maximum duration to wait for a service event to complete.
     */
    private final long timeoutDuration;

    /**
     * The unit of measure for the {@link #timeoutDuration}.
     */
    private final TimeUnit timeoutUnits;

    /**
     * Creates a ServiceManagerFactory.
     *
     * @param kernel the kernel in which the service will be registered
     * @param serviceMonitor the service monitor used for all services created by this factory
     * @param serviceExecutor the executor available to the service manager
     * @param timeoutDuration the maximum duration to wait for a service event to complete
     * @param timeoutUnits the unit of measure for the timeoutDuration
     */
    public ServiceManagerFactory(Kernel kernel, ServiceMonitorBroadcaster serviceMonitor, Executor serviceExecutor, long timeoutDuration, TimeUnit timeoutUnits) {
        this.kernel = kernel;
        this.serviceMonitor = serviceMonitor;
        this.serviceExecutor = serviceExecutor;
        this.timeoutDuration = timeoutDuration;
        this.timeoutUnits = timeoutUnits;
    }

    /**
     * Creates a ServiceManager.
     *
     * @param serviceId the id of the service
     * @param serviceName the name of the service
     * @param serviceFactory the factory for the service
     * @param classLoader the classloader for the service
     * @return a new service manager
     */
    public ServiceManager createServiceManager(long serviceId, ServiceName serviceName, ServiceFactory serviceFactory, ClassLoader classLoader) {
        return new ServiceManager(kernel,
                serviceId,
                serviceName,
                serviceFactory,
                classLoader,
                new AsyncServiceMonitor(serviceMonitor, serviceExecutor),
                timeoutDuration,
                timeoutUnits);
    }
}
