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
package org.gbean.kernel;

import java.util.Collections;
import java.util.Set;

/**
 * This class holds information about a service event.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class ServiceEvent {
    private final long eventId;
    private final Kernel kernel;
    private final ServiceName serviceName;
    private final ServiceFactory serviceFactory;
    private final ClassLoader classLoader;
    private final Object service;
    private final Throwable cause;
    private final Set unsatisfiedConditions;

    /**
     * Creates a service event.
     *
     * @param eventId the sequence number for this event
     * @param kernel the kernel in which the service is registered
     * @param serviceName the name of the service
     * @param serviceFactory the factory for the service
     * @param classLoader the class loader for the service
     * @param service the service instance if it exists
     */
    public ServiceEvent(long eventId, Kernel kernel, ServiceName serviceName, ServiceFactory serviceFactory, ClassLoader classLoader, Object service) {
        this.eventId = eventId;
        this.kernel = kernel;
        this.serviceName = serviceName;
        this.serviceFactory = serviceFactory;
        this.classLoader = classLoader;
        this.service = service;
        cause = null;
        unsatisfiedConditions = null;
    }

    /**
     * Creates an error service event.
     *
     * @param eventId the sequence number for this event
     * @param kernel the kernel in which the service is registered
     * @param serviceName the name of the service
     * @param serviceFactory the factory for the service
     * @param classLoader the class loader for the service
     * @param cause the error that occured
     */
    public ServiceEvent(long eventId, Kernel kernel, ServiceName serviceName, ServiceFactory serviceFactory, ClassLoader classLoader, Throwable cause) {
        this.eventId = eventId;
        this.kernel = kernel;
        this.serviceName = serviceName;
        this.serviceFactory = serviceFactory;
        this.classLoader = classLoader;
        this.cause = cause;
        service = null;
        unsatisfiedConditions = null;
    }

    /**
     * Creates a waiting service event.
     *
     * @param eventId the sequence number for this event
     * @param kernel the kernel in which the service is registered
     * @param serviceName the name of the service
     * @param serviceFactory the factory for the service
     * @param classLoader the class loader for the service
     * @param service the service instance if it exists
     * @param unsatisfiedConditions the unsatified conditions
     */
    public ServiceEvent(long eventId, Kernel kernel, ServiceName serviceName, ServiceFactory serviceFactory, ClassLoader classLoader, Object service, Set unsatisfiedConditions) {
        this.eventId = eventId;
        this.kernel = kernel;
        this.serviceName = serviceName;
        this.serviceFactory = serviceFactory;
        this.classLoader = classLoader;
        this.service = service;
        this.unsatisfiedConditions = Collections.unmodifiableSet(unsatisfiedConditions);
        cause = null;
    }

    /**
     * Gets the sequence number for this event.  A service gaurentees that events will occur in increasing order with out
     * skipping any numbers.
     *
     * @return the sequence number for this event
     */
    public long getEventId() {
        return eventId;
    }

    /**
     * Gets the kernel in which the service is registered.
     *
     * @return the kernel in which the servce is registerd
     */
    public Kernel getKernel() {
        return kernel;
    }

    /**
     * Gets the name of the service.
     *
     * @return the name of the service
     */
    public ServiceName getServiceName() {
        return serviceName;
    }

    /**
     * Gets the service factory for the service.
     *
     * @return the service factory for the service
     */
    public ServiceFactory getServiceFactory() {
        return serviceFactory;
    }

    /**
     * Gets the class loader for the service.
     *
     * @return the class loader for the service
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Gets the service instance or null if the service doesn't exist.
     *
     * @return the service instance
     */
    public Object getService() {
        return service;
    }

    /**
     * Gets the error that was thrown during startup or shutdown.  This is available only in error events.
     *
     * @return the error
     */
    public Throwable getCause() {
        return cause;
    }

    /**
     * Gets the unsatified dependencies that cause the service to wait.   This is available only in waiting events.
     *
     * @return the unsatified dependencies that cause the service to wait
     */
    public Set getUnsatisfiedConditions() {
        return unsatisfiedConditions;
    }
}
