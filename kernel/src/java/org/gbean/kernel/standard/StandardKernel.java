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
package org.gbean.kernel.standard;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import edu.emory.mathcs.backport.java.util.concurrent.Executor;
import edu.emory.mathcs.backport.java.util.concurrent.Executors;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.locks.Lock;
import edu.emory.mathcs.backport.java.util.concurrent.locks.ReentrantLock;
import edu.emory.mathcs.backport.java.util.concurrent.locks.Condition;
import org.gbean.kernel.IllegalServiceStateException;
import org.gbean.kernel.Kernel;
import org.gbean.kernel.KernelErrorsError;
import org.gbean.kernel.KernelMonitor;
import org.gbean.kernel.ServiceAlreadyExistsException;
import org.gbean.kernel.ServiceFactory;
import org.gbean.kernel.ServiceMonitor;
import org.gbean.kernel.ServiceName;
import org.gbean.kernel.ServiceNotFoundException;
import org.gbean.kernel.ServiceRegistrationException;
import org.gbean.kernel.ServiceState;
import org.gbean.kernel.StartStrategies;
import org.gbean.kernel.StartStrategy;
import org.gbean.kernel.StopStrategies;
import org.gbean.kernel.StopStrategy;
import org.gbean.kernel.UnregisterServiceException;
import org.gbean.kernel.UnsatisfiedConditionsException;
import org.gbean.kernel.KernelFactory;

/**
 * The standard kernel implementation.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class StandardKernel implements Kernel {
    /**
     * The unique name of this kernel.
     */
    private final String kernelName;

    /**
     * The registered service managers.
     */
    private final ServiceManagerRegistry serviceManagerRegistry;

    /**
     * Tracks and broadcasts kernel evnents to the registered listeners.
     */
    private final KernelMonitorBroadcaster kernelMonitor = new KernelMonitorBroadcaster();

    /**
     * This monitor broadcasts events to the listeners registered for service.
     */
    private final ServiceMonitorBroadcaster serviceMonitor = new ServiceMonitorBroadcaster(kernelMonitor);

    /**
     * If true, the kernel is still running.
     */
    private boolean running = true;

    /**
     * Lock that should be acquired before accessing the running boolean flag.
     */
    private final Lock destroyLock = new ReentrantLock();

    /**
     * The condition that is notified when the kernel has been destroyed.
     */
    private final Condition destroyCondition = destroyLock.newCondition();

    /**
     * Creates the service managers with handle service lifecycle.
     */
    private ServiceManagerFactory serviceManagerFactory;

    /**
     * Creates a kernel using the specified name.
     *
     * @param kernelName the unique name of this kernel
     */
    public StandardKernel(String kernelName) {
        this(kernelName, Executors.newCachedThreadPool(), 30, TimeUnit.SECONDS);
    }

    /**
     * Creates a kernel using the specified name.
     *
     * @param kernelName the unique name of this kernel
     * @param serviceExecutor the executor to use for asynchronous service operations
     * @param timeoutDuration the maximum duration to wait for a service event to complete
     * @param timeoutUnits the unit of measure for the timeoutDuration
     */
    public StandardKernel(String kernelName, Executor serviceExecutor, long timeoutDuration, TimeUnit timeoutUnits) {
        if (kernelName == null) throw new NullPointerException("kernelName is null");
        if (kernelName.length() ==0) throw new IllegalArgumentException("kernelName must be atleast one character long");
        if (serviceExecutor == null) throw new NullPointerException("serviceExecutor is null");
        if (timeoutUnits == null) throw new NullPointerException("timeoutUnits is null");

        this.kernelName = kernelName;
        serviceManagerFactory = new ServiceManagerFactory(this, serviceMonitor, serviceExecutor, timeoutDuration, timeoutUnits);
        serviceManagerRegistry = new ServiceManagerRegistry(serviceManagerFactory);
    }

    /**
     * {@inheritDoc}
     */
    public void destroy() throws KernelErrorsError {
        destroyLock.lock();
        try {
            // if we are already stopped simply return
            if (!running) {
                return;
            }
            running = false;
        } finally {
            destroyLock.unlock();
        }

        // destroy all services
        serviceManagerRegistry.destroy();

        // remove this kernel from the kernel factory registry
        KernelFactory.destroyInstance(this);

        // notify threads waiting for destroy to complete
        destroyLock.lock();
        try {
            destroyCondition.signalAll();
        } finally {
            destroyLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void waitForDestruction() {
        destroyLock.lock();
        try {
            // if we are already stopped simply return
            if (!running) {
                return;
            }

            // wait until destroy completes
            destroyCondition.awaitUninterruptibly();
        } finally {
            destroyLock.unlock();
        }

    }

    /**
     * {@inheritDoc}
     */
    public boolean isRunning() {
        destroyLock.lock();
        try {
            return running;
        } finally {
            destroyLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getKernelName() {
        return kernelName;
    }

    /**
     * {@inheritDoc}
     */
    public void registerService(ServiceName serviceName, ServiceFactory serviceFactory, ClassLoader classLoader) throws ServiceAlreadyExistsException, ServiceRegistrationException {
        if (serviceName == null) throw new NullPointerException("serviceName is null");
        if (serviceFactory == null) throw new NullPointerException("serviceFactory is null");
        if (classLoader == null) throw new NullPointerException("classLoader is null");
        if (!isRunning()) {
            throw new ServiceRegistrationException(serviceName, new IllegalStateException("Kernel is destroyed"));
        }

        serviceManagerRegistry.registerService(serviceName, serviceFactory, classLoader);
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterService(ServiceName serviceName) throws ServiceNotFoundException, ServiceRegistrationException {
        if (serviceName == null) throw new NullPointerException("serviceName is null");
        unregisterService(serviceName, StopStrategies.SYNCHRONOUS);
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterService(ServiceName serviceName, StopStrategy stopStrategy) throws ServiceNotFoundException, ServiceRegistrationException {
        if (serviceName == null) throw new NullPointerException("serviceName is null");
        if (stopStrategy == null) throw new NullPointerException("stopStrategy is null");
        if (!isRunning()) {
            return;
        }

        serviceManagerRegistry.unregisterService(serviceName, stopStrategy);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRegistered(ServiceName serviceName) {
        if (serviceName == null) throw new NullPointerException("serviceName is null");
        if (!isRunning()) {
            return false;
        }

        return serviceManagerRegistry.isRegistered(serviceName);
    }

    /**
     * {@inheritDoc}
     */
    public ServiceState getServiceState(ServiceName serviceName) throws ServiceNotFoundException {
        if (serviceName == null) throw new NullPointerException("serviceName is null");
        ServiceManager serviceManager = getServiceManager(serviceName);
        return serviceManager.getState();
    }

    /**
     * {@inheritDoc}
     */
    public long getServiceStartTime(ServiceName serviceName) throws ServiceNotFoundException {
        if (serviceName == null) throw new NullPointerException("serviceName is null");
        ServiceManager serviceManager = getServiceManager(serviceName);
        return serviceManager.getStartTime();
    }

    /**
     * {@inheritDoc}
     */
    public void startService(ServiceName serviceName) throws ServiceNotFoundException, IllegalServiceStateException, UnsatisfiedConditionsException, Exception {
        if (serviceName == null) throw new NullPointerException("serviceName is null");
        startService(serviceName, false, StartStrategies.SYNCHRONOUS);
    }

    /**
     * {@inheritDoc}
     */
    public void startService(ServiceName serviceName, StartStrategy startStrategy) throws ServiceNotFoundException, IllegalServiceStateException, UnsatisfiedConditionsException, Exception {
        if (serviceName == null) throw new NullPointerException("serviceName is null");
        if (startStrategy == null) throw new NullPointerException("startStrategy is null");
        startService(serviceName, false, startStrategy);
    }

    /**
     * {@inheritDoc}
     */
    public void startServiceRecursive(ServiceName serviceName) throws ServiceNotFoundException, IllegalServiceStateException, UnsatisfiedConditionsException, Exception {
        if (serviceName == null) throw new NullPointerException("serviceName is null");
        startService(serviceName, true, StartStrategies.SYNCHRONOUS);
    }

    /**
     * {@inheritDoc}
     */
    public void startServiceRecursive(ServiceName serviceName, StartStrategy startStrategy) throws ServiceNotFoundException, IllegalServiceStateException, UnsatisfiedConditionsException, Exception {
        if (serviceName == null) throw new NullPointerException("serviceName is null");
        if (startStrategy == null) throw new NullPointerException("startStrategy is null");
        startService(serviceName, true, startStrategy);
    }

    private void startService(ServiceName serviceName, boolean recursive, StartStrategy startStrategy) throws Exception {
        if (startStrategy == null) throw new NullPointerException("startStrategy is null");
        ServiceManager serviceManager = getServiceManager(serviceName);
        try {
            serviceManager.start(recursive, startStrategy);
        } catch (UnregisterServiceException e) {
            try {
                unregisterService(serviceName, StopStrategies.FORCE);
            } catch (ServiceNotFoundException ignored) {
                // that is weird, but what ever
            } catch (ServiceRegistrationException ignored) {
                // we are alredy throwing an exception so ignore this one
            }
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new AssertionError(cause);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stopService(ServiceName serviceName) throws ServiceNotFoundException, UnsatisfiedConditionsException {
        if (serviceName == null) throw new NullPointerException("serviceName is null");
        stopService(serviceName, StopStrategies.SYNCHRONOUS);
    }

    /**
     * {@inheritDoc}
     */
    public void stopService(ServiceName serviceName, StopStrategy stopStrategy) throws ServiceNotFoundException, UnsatisfiedConditionsException {
        if (serviceName == null) throw new NullPointerException("serviceName is null");
        if (stopStrategy == null) throw new NullPointerException("stopStrategy is null");
        ServiceManager serviceManager = getServiceManager(serviceName);
        serviceManager.stop(stopStrategy);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isServiceEnabled(ServiceName serviceName) throws ServiceNotFoundException {
        if (serviceName == null) throw new NullPointerException("serviceName is null");
        ServiceManager serviceManager = getServiceManager(serviceName);
        ServiceFactory serviceFactory = serviceManager.getServiceFactory();
        return serviceFactory.isEnabled();
    }

    /**
     * {@inheritDoc}
     */
    public void setServiceEnabled(ServiceName serviceName, boolean enabled) throws ServiceNotFoundException {
        if (serviceName == null) throw new NullPointerException("serviceName is null");
        ServiceManager serviceManager = getServiceManager(serviceName);
        ServiceFactory serviceFactory = serviceManager.getServiceFactory();
        serviceFactory.setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    public Object getService(ServiceName serviceName) throws ServiceNotFoundException, IllegalArgumentException {
        if (serviceName == null) throw new NullPointerException("serviceName is null");
        ServiceManager serviceManager = getServiceManager(serviceName);
        return serviceManager.getService();
    }

    /**
     * {@inheritDoc}
     */
    public Object getService(Class type) {
        if (type == null) throw new NullPointerException("type is null");
        if (!isRunning()) {
            return null;
        }

        Object service = serviceManagerRegistry.getService(type);
        return service;
    }

    /**
     * {@inheritDoc}
     */
    public List getServices(Class type) {
        if (type == null) throw new NullPointerException("type is null");
        if (!isRunning()) {
            return null;
        }

        List services = serviceManagerRegistry.getServices(type);
        return services;
    }

    /**
     * {@inheritDoc}
     */
    public ServiceFactory getServiceFactory(ServiceName serviceName) throws ServiceNotFoundException {
        if (serviceName == null) throw new NullPointerException("serviceName is null");
        ServiceManager serviceManager = getServiceManager(serviceName);
        return serviceManager.getServiceFactory();
    }

    /**
     * {@inheritDoc}
     */
    public ServiceFactory getServiceFactory(Class type) {
        if (type == null) throw new NullPointerException("type is null");
        if (!isRunning()) {
            return null;
        }

        ServiceManager serviceManager = serviceManagerRegistry.getServiceManager(type);
        return serviceManager.getServiceFactory();
    }

    /**
     * {@inheritDoc}
     */
    public List getServiceFactories(Class type) {
        if (type == null) throw new NullPointerException("type is null");
        if (!isRunning()) {
            return null;
        }

        List serviceManagers = serviceManagerRegistry.getServiceManagers(type);
        List serviceFactories = new ArrayList(serviceManagers.size());
        for (Iterator iterator = serviceManagers.iterator(); iterator.hasNext();) {
            ServiceManager serviceManager = (ServiceManager) iterator.next();
            serviceFactories.add(serviceManager.getServiceFactory());
        }
        return serviceFactories;
    }

    /**
     * {@inheritDoc}
     */
    public ClassLoader getClassLoaderFor(ServiceName serviceName) throws ServiceNotFoundException {
        if (serviceName == null) throw new NullPointerException("serviceName is null");
        ServiceManager serviceManager = getServiceManager(serviceName);
        return serviceManager.getClassLoader();
    }

    private ServiceManager getServiceManager(ServiceName serviceName) throws ServiceNotFoundException {
        if (!isRunning()) {
            throw new ServiceNotFoundException(serviceName);
        }

        ServiceManager serviceManager = serviceManagerRegistry.getServiceManager(serviceName);
        return serviceManager;
    }

    /**
     * {@inheritDoc}
     */
    public void addKernelMonitor(KernelMonitor kernelMonitor) {
        if (kernelMonitor == null) throw new NullPointerException("kernelMonitor is null");
        if (!isRunning()) {
            throw new IllegalStateException("Kernel is stopped");
        }
        this.kernelMonitor.addKernelMonitor(kernelMonitor);
    }

    /**
     * {@inheritDoc}
     */
    public void removeKernelMonitor(KernelMonitor kernelMonitor) {
        if (kernelMonitor == null) throw new NullPointerException("kernelMonitor is null");
        this.kernelMonitor.removeKernelMonitor(kernelMonitor);
    }

    /**
     * {@inheritDoc}
     */
    public void addServiceMonitor(ServiceMonitor serviceMonitor) {
        if (serviceMonitor == null) throw new NullPointerException("serviceMonitor is null");
        if (!isRunning()) {
            throw new IllegalStateException("Kernel is stopped");
        }
        addServiceMonitor(serviceMonitor, null);
    }

    /**
     * {@inheritDoc}
     */
    public void addServiceMonitor(ServiceMonitor serviceMonitor, ServiceName serviceName) {
        if (serviceMonitor == null) throw new NullPointerException("serviceMonitor is null");
        if (serviceName == null) throw new NullPointerException("serviceName is null");
        if (!isRunning()) {
            throw new IllegalStateException("Kernel is stopped");
        }
        this.serviceMonitor.addServiceMonitor(serviceMonitor, serviceName);
    }

    /**
     * {@inheritDoc}
     */
    public void removeServiceMonitor(ServiceMonitor serviceMonitor) {
        if (serviceMonitor == null) throw new NullPointerException("serviceMonitor is null");
        this.serviceMonitor.removeServiceMonitor(serviceMonitor);
    }
}
