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
package org.xbean.kernel;

import java.util.List;

/**
 * This iterface defines the API for managing and monitoring service life-cycle. A kernel can be constructed with the
 * following code:
 * <p><blockquote><pre>
 * Kernel kernel = KernelFactory.newInstance().createKernel(name);
 * </pre></blockquote>
 * Services can be registered, unregistered, started and
 * stopped.  The lifecycle model is loosly based on the J2ee Management Specification (JSR 77).  All lifecycle
 * transitions are broadcasted via a ServiceMonitor.
 * <p/>
 * Each kernel must have a name that is unique with in the KernelFactory (there should only be one KernelFactory per
 * VM but class loader tricks can result in several KernelFactory)
 * <p/>
 * This class is loosely based on the J2ee management MEJB and JMX MBeanServer interfaces.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public interface Kernel {
    /**
     * Destroys this kernel.  This method causes all services to be stopped and unregistered.
     */
    void destroy();

    /**
     * Waits for the kernel to be destroyed.
     */
    void waitForDestruction();

    /**
     * Gets the running status of the kernel.  Services can not be registered or started on a stopped kernel.
     *
     * @return true if the kernel is running; false otherwise
     */
    boolean isRunning();

    /**
     * Gets the unique name of this kernel within the KernelFactory.
     *
     * @return the unique name of this kernel
     */
    String getKernelName();

    /**
     * Registers a service with this kernel.  If the service is restartable, it will enter the server in the
     * STOPPED state.  If a service is not restartable, the kernel will assure that all
     * dependencies are satisfied and service will enter the kernel in the  RUNNING state.  If a
     * dependency for a non-restartable service is not immediately satisfiable, this method will throw a
     * ServiceRegistrationException.
     *
     * @param serviceName the unique name of the service in the kernel
     * @param serviceFactory the factory used to create the service
     * @param classLoader the class loader to use for this service
     * @throws ServiceAlreadyExistsException if service is already registered with the specified name
     * @throws ServiceRegistrationException if the service is not restartable and an error occured while starting the service
     */
    void registerService(ServiceName serviceName, ServiceFactory serviceFactory, ClassLoader classLoader) throws ServiceAlreadyExistsException, ServiceRegistrationException;

    /**
     * Unregisters a service from this kernel.  The kernel will attempt to stop the service using the
     * SYNCHRONOUS stop strategy, but if it can not stop the service a
     * ServiceRegistrationException will be thrown containing an UnsatisfiedConditionsException.
     *
     * @param serviceName the unique name of the service in the kernel
     * @throws ServiceNotFoundException if there is no service registered under the specified name
     * @throws ServiceRegistrationException if the service could not be stopped
     */
    void unregisterService(ServiceName serviceName) throws ServiceNotFoundException, ServiceRegistrationException;

    /**
     * Unregisters a service from this kernel.  The kernel will attempt to stop the service using the specified stop
     * strategy, but if it can not stop the service a ServiceRegistrationException will be thrown containing
     * either an UnsatisfiedConditionsException or a IllegalServiceStateException.
     *
     * @param serviceName the unique name of the service in the kernel
     * @param stopStrategy the strategy that determines how unsatisfied conditions are handled
     * @throws ServiceNotFoundException if there is no service registered under the specified name
     * @throws ServiceRegistrationException if the service could not be stopped
     */
    void unregisterService(ServiceName serviceName, StopStrategy stopStrategy) throws ServiceNotFoundException, ServiceRegistrationException;

    /**
     * Determines if there is a service registered under the specified name.
     *
     * @param serviceName the unique name of the service
     * @return true if there is a service registered with the specified name; false otherwise
     */
    boolean isRegistered(ServiceName serviceName);

    /**
     * Gets the ServiceState of the specified service.  If the service is not restartable, this method will
     * always return RUNNING.
     *
     * @param serviceName the unique name of the service in the kernel
     * @return the curren ServiceState of the service
     * @throws ServiceNotFoundException if there is no service registered under the specified name
     */
    ServiceState getServiceState(ServiceName serviceName) throws ServiceNotFoundException;

    /**
     * Gets the time the specified service entered the RUNNING state since the epoch
     * (January 1, 1970, 00:00:00) in milliseconds.  If the service is in the STOPPED or
     * STARTING states, this method will return 0.
     *
     * @param serviceName the unique name of the service in the kernel
     * @return the time the service started in milliseconds since January 1, 1970, 00:00:00
     * @throws ServiceNotFoundException if there is no service registered under the specified name
     */
    long getServiceStartTime(ServiceName serviceName) throws ServiceNotFoundException;

    /**
     * Immediately starts the service using the SYNCHRONOUS start strategy.  Any exception throw
     * from service constuction is throw directly from this method.  If a start condition can not be immediately
     * satisfied, a UnsatisfiedConditionsException will be thrown.  If a service already in the
     * RUNNING state, or is not restartable, this method is a noop.  If the service
     * is in the STOPPING state an IllegalServiceStateException will be thrown.  If the
     * service is disabled, this method will throw an IllegalServiceStateException.
     * <p/>
     * This method has no effect on as service that is not restartable.
     *
     * @param serviceName the unique name of the service to start
     * @throws ServiceNotFoundException if there is no service registered under the specified name
     * @throws IllegalServiceStateException if the service is restartable and is in the STOPPING state or if the
     * service is disabled
     * @throws UnsatisfiedConditionsException if some of the start conditions can not be immediately satisfied
     * @throws Exception if service construction threw an Exception
     */
    void startService(ServiceName serviceName) throws ServiceNotFoundException, IllegalServiceStateException, UnsatisfiedConditionsException, Exception;

    /**
     * Immediately starts the service using the specified start strategy.
     * <p/>
     * The start strategy determines how any exception thrown from service constuction is handled including throwing
     * the exception  directly from this method.
     * <p/>
     * The start strategy determines what to do if a start condition can not be immediately satisfied. Possibilities
     * include throwing an UnsatisfiedConditionsException, blocking, leaving the service in the
     * RUNNING state, or unregistering the service.
     * <p/>
     * If a service already in the RUNNING state, or is not restartable, this method is a noop.
     * If the service is in the STOPPING state an IllegalServiceStateException will be
     * thrown.  If the service is disabled, this method will throw an IllegalServiceStateException.
     * <p/>
     * This method has no effect on as service that is not restartable.
     *
     * @param serviceName the unique name of the service to start
     * @param startStrategy the strategy that determines how unsatisfied conditions and construction exceptions are handled
     * @throws ServiceNotFoundException if there is no service registered under the specified name
     * @throws IllegalServiceStateException if the service is restartable and is in the STOPPING state or if the
     * service is disabled
     * @throws UnsatisfiedConditionsException if some of the start conditions can not be immediately satisfied
     * @throws Exception if service construction threw an Exception
     */
    void startService(ServiceName serviceName, StartStrategy startStrategy) throws ServiceNotFoundException, IllegalServiceStateException, UnsatisfiedConditionsException, Exception;

    /**
     * Immediately starts the service, and if the start ultimately completes successfully, all services owned by the
     * specified service, all services that are owned by those services, and so on, will be started using the
     * startServiceRecursive(ServiceName) method.
     *
     * @param serviceName the unique name of the service to start recursively
     * @throws ServiceNotFoundException if there is no service registered under the specified name
     * @throws IllegalServiceStateException if the service is restartable and is in the STOPPING state or if the
     * service is disabled
     * @throws UnsatisfiedConditionsException if some of the start conditions can not be immediately satisfied
     * @throws Exception if service construction threw an Exception
     */
    void startServiceRecursive(ServiceName serviceName) throws ServiceNotFoundException, IllegalServiceStateException, UnsatisfiedConditionsException, Exception;

    /**
     * Immediately starts the service, and if the start ultimately completes successfully, all services owned by the
     * specified service, all services that are owned by those services, and so on, will be started using the
     * startServiceRecursive(ServiceName, StartStrategy) method.
     *
     * @param serviceName the unique name of the service to start recursively
     * @param startStrategy the strategy that determines how unsatisfied conditions and construction exceptions are handled
     * @throws ServiceNotFoundException if there is no service registered under the specified name
     * @throws IllegalServiceStateException if the service is restartable and is in the STOPPING state or if the
     * service is disabled
     * @throws UnsatisfiedConditionsException if some of the start conditions can not be immediately satisfied
     * @throws Exception if service construction threw an Exception
     */
    void startServiceRecursive(ServiceName serviceName, StartStrategy startStrategy) throws ServiceNotFoundException, IllegalServiceStateException, UnsatisfiedConditionsException, Exception;

    /**
     * Immediately stops the service using the SYNCHRONOUS stop strategy.  If a stop condition can
     * not be immediately satisfied, an UnsatisfiedConditionsException will be thrown.  If a service already in
     * the STOPPED state, this method is a noop.
     * <p/>
     * If the service is not restartable, this method only attempts to satify the stop conditions.  This is useful for
     * stopping all dependent services of a non-restartable service before unregistering the service.
     *
     * @param serviceName the unique name of the service to stop
     * @throws ServiceNotFoundException if there is no service registered under the specified name
     */
    void stopService(ServiceName serviceName) throws ServiceNotFoundException, UnsatisfiedConditionsException;

    /**
     * Immediately stops the service using the specified stop strategy.  If a stop condition can not be immediately
     * satisfied, an UnsatisfiedConditionsException will be thrown.  If a service already in the
     * STOPPED state, this method is a noop.
     * <p/>
     * If the service is not restartable, this method only attempts to satify the stop conditions.  This is useful for
     * stopping all dependent services of a non-restartable service before unregistering the service.
     *
     * @param serviceName the unique name of the service to stop
     * @param stopStrategy the strategy that determines how unsatisfied conditions are handled
     * @throws ServiceNotFoundException if there is no service registered under the specified name
     */
    void stopService(ServiceName serviceName, StopStrategy stopStrategy) throws ServiceNotFoundException, UnsatisfiedConditionsException;

    /**
     * Determines if the service can be instantiated in a kernel.  A disabled restartable service can not be
     * started.  This method is equivalent to:
     * <p><blockquote><pre>
     *     kernel.getServiceFactory(serviceName).isEnabled();
     * </pre></blockquote>
     * <p/>
     *
     * @param serviceName the unique name of the service
     * @return true if the service factory is enabled; false otherwise
     * @throws ServiceNotFoundException if there is no service registered under the specified name
     */
    boolean isServiceEnabled(ServiceName serviceName) throws ServiceNotFoundException;

    /**
     * Sets the enabled status of a service.   A disabled restartable service can not be started.  This state has
     * no effect on a service that is already started, but if a running service is disabled, it can not be restarted.
     * This method is equivalent to:
     * <p><blockquote><pre>
     *     kernel.getServiceFactory(serviceName).setEnabled(enabled);
     * </pre></blockquote>
     * <p/>
     *
     * @param serviceName the unique name of the service
     * @param enabled the new enabled state of this factory
     * @throws ServiceNotFoundException if there is no service registered under the specified name
     */
    void setServiceEnabled(ServiceName serviceName, boolean enabled) throws ServiceNotFoundException;

    /**
     * Gets the service registered under the specified name.  If the service is not in the RUNNING,
     * or STARTING state this method will throw an IllegalArgumentException.
     *
     * @param serviceName the unique name of the service
     * @return the service associated with the specified name
     * @throws ServiceNotFoundException if there is no service registered under the specified name
     * @throws IllegalArgumentException if the service is not in the RUNNING, or STARTING state
     */
    Object getService(ServiceName serviceName) throws ServiceNotFoundException, IllegalArgumentException;

    /**
     * Gets the first running service registered with the kernel that is an instance of the specified type.  If no
     * running services are instances of the specified type, null is returned.
     *
     * @param type the of the desired service
     * @return the first registered service that is an instance of the specified type and is running
     */
    Object getService(Class type);

    /**
     * Gets the all of running service registered with the kernel that are an instances of the specified type.  If no
     * running services are instances of the specified type, an empty list is returned
     *
     * @param type the of the desired service
     * @return the registered services that are instances of the specified type and are running 
     */
    List getServices(Class type);

    /**
     * Gets the service factory registered under the specified name.
     *
     * @param serviceName the unique name of the service
     * @return the service factory associated with the specified name
     * @throws ServiceNotFoundException if there is no service registered under the specified name
     */
    ServiceFactory getServiceFactory(ServiceName serviceName) throws ServiceNotFoundException;

    /**
     * Gets the first service factory registered with the kernel that creates an instance of the specified type.
     * If no service factories create an instance of the specified type, null is returned.
     *
     * @param type the of the desired service
     * @return the first service factory registered with the kernel that creates an instance of the specified type
     */
    ServiceFactory getServiceFactory(Class type);

    /**
     * Gets the all of the service factories registered with the kernel that create an instances of the specified type.
     * If no service factories create an instance of the specified type, an empty list is returned.
     *
     * @param type the of the desired service
     * @return the registered services that are instances of the specified type and are running
     */
    List getServiceFactories(Class type);

    /**
     * Gets the class loader associated with the specifed service.
     *
     * @param serviceName the unique name of the service
     * @return the class loader associated with the specified name
     * @throws ServiceNotFoundException if there is no service registered under the specified name
     */
    ClassLoader getClassLoaderFor(ServiceName serviceName) throws ServiceNotFoundException;

    /**
     * Adds a kernel monitor.
     *
     * @param kernelMonitor the kernel monitor to add
     */
    void addKernelMonitor(KernelMonitor kernelMonitor);

    /**
     * Removes a kernel monitor.
     *
     * @param kernelMonitor the kernel monitor to remove
     */
    void removeKernelMonitor(KernelMonitor kernelMonitor);

    /**
     * Adds a service monitor for all services registered with the kernel.  This method is equivalent to:
     * <p><blockquote><pre>
     *     addServiceMonitor(serviceMonitor, null);
     * </pre></blockquote>
     * <p/>
     * Note: the order in which service monitors are notified is not specified.
     *
     * @param serviceMonitor the service monitor to add
     */
    void addServiceMonitor(ServiceMonitor serviceMonitor);

    /**
     * Adds a service monitor for a specific service.
     * <p/>
     * Note: the order in which service monitors are notified is not specified.
     *
     * @param serviceMonitor the service monitor to add
     * @param serviceName the unique name of the service to monitor or null to monitor all services
     */
    void addServiceMonitor(ServiceMonitor serviceMonitor, ServiceName serviceName);

    /**
     * Removes a service monitor.
     *
     * @param serviceMonitor the service monitor to remove
     */
    void removeServiceMonitor(ServiceMonitor serviceMonitor);
}
