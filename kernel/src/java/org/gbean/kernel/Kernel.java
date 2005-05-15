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
package org.gbean.kernel;

import java.util.Set;
import java.util.Date;
import javax.management.ObjectName;

import org.gbean.service.ServiceFactory;

/**
 * @version $Revision$ $Date$
 */
public interface Kernel {
    /**
     * The name used by a Kernel to register itself when it boots.
     */
    ObjectName KERNEL = ServiceName.createName(":j2eeType=Kernel");

    /**
     * Get the name of this kernel
     *
     * @return the name of this kernel
     */
    String getKernelName();

    /**
     * Load a specific service into this kernel.
     * This is intended for applications that are embedding the kernel.
     *
     * @param serviceFactory the service to load
     * @param classLoader the class loader to use to load the service
     * @throws ServiceAlreadyExistsException if the name is already used
     */
    void loadService(ObjectName name, ServiceFactory serviceFactory, ClassLoader classLoader) throws ServiceAlreadyExistsException;

    /**
     * Is there a service registered with the kernel under the specified name?
     * @param name the name to check
     * @return true if there is a service registered under the specified name; false otherwise
     */
    boolean isLoaded(ObjectName name);

    /**
     * Start a specific service.
     *
     * @param name the service to start
     * @throws ServiceNotFoundException if the service could not be found
     * @throws IllegalStateException If the service is disabled
     */
    void startService(ObjectName name) throws ServiceNotFoundException, IllegalStateException;

    /**
     * Start a specific service and its children.
     *
     * @param name the service to start
     * @throws ServiceNotFoundException if the service could not be found
     * @throws IllegalStateException If the service is disabled
     */
    void startRecursiveService(ObjectName name) throws ServiceNotFoundException, IllegalStateException;

    /**
     * Stop a specific service.
     *
     * @param name the service to stop
     * @throws ServiceNotFoundException if the service could not be found
     * @throws IllegalStateException If the service is disabled
     */
    void stopService(ObjectName name) throws ServiceNotFoundException, IllegalStateException;

    /**
     * Unload a specific service.
     * This is intended for applications that are embedding the kernel.
     *
     * @param name the name of the service to unregister
     * @throws ServiceNotFoundException if the service could not be found
     */
    void unloadService(ObjectName name) throws ServiceNotFoundException, IllegalStateException;

    /**
     * Gets a service instance.  This method should be use with extreme caution, as this method
     * returns hard reference to the instance which if handled improperly will lead to memory
     * leaks.
     * @param name the name of the object to fetch
     * @return the service instance
     * @throws ServiceNotFoundException if the service is not loaded in to the kernel
     * @throws IllegalStateException if the service is not in the RUNNING or STOPPING states
     */
    Object getService(ObjectName name) throws ServiceNotFoundException, IllegalStateException;

    /**
     * Gets the state of the specified service.
     * @param name the name of the service
     * @return the state of the service
     * @throws ServiceNotFoundException if the service could not be found
     */
    int getServiceState(ObjectName name) throws ServiceNotFoundException;

    /**
     * Gets the time the specified service was started
     * @param name the name of the service
     * @return the start time of the service or 0 if not running
     * @throws ServiceNotFoundException if the service could not be found
     */
    long getServiceStartTime(ObjectName name) throws ServiceNotFoundException;

    /**
     * Is the specified service enabled?
     * @param name the name if the service
     * @return true if the service is enabled
     * @throws ServiceNotFoundException if the service could not be found
     */
    boolean isServiceEnabled(ObjectName name) throws ServiceNotFoundException;

    /**
     * Sets the eneabled status of the specified service.  A disabled service can not be started, and
     * will not be started via startRecursive.
     * @param name the name if the service
     * @param enabled the new enabled status
     * @throws ServiceNotFoundException if the service could not be found
     */
    void setServiceEnabled(ObjectName name, boolean enabled) throws ServiceNotFoundException;

    /**
     * Gets the ClassLoader used to register the specified service
     * @param name the name of the service from which the class loader should be extracted
     * @return the class loader associated with the specified service
     * @throws ServiceNotFoundException if the specified service is not registered with the kernel
     */
    ClassLoader getClassLoaderFor(ObjectName name) throws ServiceNotFoundException;

    /**
     * Return the ServiceFactory for a registered service instance.
     * @param name the name of the service whose info should be returned
     * @return the info for that instance
     * @throws ServiceNotFoundException if there is no instance with the supplied name
     */
    ServiceFactory getServiceFactory(ObjectName name) throws ServiceNotFoundException;

    /**
     * Returns a Set of all services matching the object name pattern
     * @return a List of javax.management.ObjectName of matching services registered with this kernel
     */
    Set listServices(ObjectName pattern);

    /**
     * Returns a Set of all services matching the set of object name pattern
     * @return a List of javax.management.ObjectName of matching services registered with this kernel
     */
    Set listServices(Set patterns);

    /**
     * Brings the kernel online
     * @throws Exception if the kernel can not boot
     */
    void boot() throws Exception;

    /**
     * Returns the time this kernel was last booted.
     * @return the time this kernel was last booted; null if the kernel has not been
     */
    Date getBootTime();

    /**
     * Registers a runnable to execute when the kernel is shutdown
     * @param hook a runnable to execute when the kernel is shutdown
     */
    void registerShutdownHook(Runnable hook);

    /**
     * Unregisters a runnable from the list to execute when the kernel is shutdown
     * @param hook the runnable that should be removed
     */
    void unregisterShutdownHook(Runnable hook);

    /**
     * Stops the kernel
     */
    void shutdown();

    /**
     * Has the kernel been booted
     * @return true if the kernel has been booted; false otherwise
     */
    boolean isRunning();

    /**
     * Adds a listener for all lifecycle events any service matching the pattern
     *
     * This is equivalent to addLifecycleListener(lifecycleListener, Collections.singleton(pattern))
     *
     * @param lifecycleListener the listener instance
     * @param pattern the pattern used to filter events
     */
    void addLifecycleListener(LifecycleListener lifecycleListener, ObjectName pattern);

    /**
     * Registers a listener to revieve life cycle events for a set of object name patterns.
     * @param lifecycleListener the listener that will receive life cycle events
     * @param patterns a set of ObjectName patterns
     */
    void addLifecycleListener(LifecycleListener lifecycleListener, Set patterns);

    /**
     * Removes the listener from all notifications.
     * @param lifecycleListener the listener to unregister
     */
    void removeLifecycleListener(LifecycleListener lifecycleListener);
}
