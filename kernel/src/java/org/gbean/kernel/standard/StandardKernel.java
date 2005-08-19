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

import edu.emory.mathcs.backport.java.util.concurrent.Executor;
import edu.emory.mathcs.backport.java.util.concurrent.Executors;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import org.gbean.kernel.IllegalServiceStateException;
import org.gbean.kernel.Kernel;
import org.gbean.kernel.KernelErrorsError;
import org.gbean.kernel.KernelMonitor;
import org.gbean.kernel.KernelMonitorBroadcaster;
import org.gbean.kernel.ServiceAlreadyExistsException;
import org.gbean.kernel.ServiceFactory;
import org.gbean.kernel.ServiceMonitor;
import org.gbean.kernel.ServiceMonitorBroadcaster;
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
    private final AtomicBoolean running = new AtomicBoolean(true);
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
        this.kernelName = kernelName;
        serviceManagerFactory = new ServiceManagerFactory(this, serviceMonitor, serviceExecutor, timeoutDuration, timeoutUnits);
        serviceManagerRegistry = new ServiceManagerRegistry(serviceManagerFactory);
    }

    /**
     * {@inheritDoc}
     */
    public void destroy() throws KernelErrorsError {
        // if we are already stopped return
        if (!running.compareAndSet(true, false)) {
            return;
        }

        serviceManagerRegistry.destroy();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRunning() {
        return running.get();
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
        ServiceManager serviceManager = getServiceManager(serviceName);
        return serviceManager.getState();
    }

    /**
     * {@inheritDoc}
     */
    public long getServiceStartTime(ServiceName serviceName) throws ServiceNotFoundException {
        ServiceManager serviceManager = getServiceManager(serviceName);
        return serviceManager.getStartTime();
    }

    /**
     * {@inheritDoc}
     */
    public void startService(ServiceName serviceName) throws ServiceNotFoundException, IllegalServiceStateException, UnsatisfiedConditionsException, Exception {
        startService(serviceName, false, StartStrategies.SYNCHRONOUS);
    }

    /**
     * {@inheritDoc}
     */
    public void startService(ServiceName serviceName, StartStrategy startStrategy) throws ServiceNotFoundException, IllegalServiceStateException, UnsatisfiedConditionsException, Exception {
        startService(serviceName, false, startStrategy);
    }

    /**
     * {@inheritDoc}
     */
    public void startServiceRecursive(ServiceName serviceName) throws ServiceNotFoundException, IllegalServiceStateException, UnsatisfiedConditionsException, Exception {
        startService(serviceName, true, StartStrategies.SYNCHRONOUS);
    }

    /**
     * {@inheritDoc}
     */
    public void startServiceRecursive(ServiceName serviceName, StartStrategy startStrategy) throws ServiceNotFoundException, IllegalServiceStateException, UnsatisfiedConditionsException, Exception {
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
        stopService(serviceName, StopStrategies.SYNCHRONOUS);
    }

    /**
     * {@inheritDoc}
     */
    public void stopService(ServiceName serviceName, StopStrategy stopStrategy) throws ServiceNotFoundException, UnsatisfiedConditionsException {
        if (stopStrategy == null) throw new NullPointerException("stopStrategy is null");
        ServiceManager serviceManager = getServiceManager(serviceName);
        serviceManager.stop(stopStrategy);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isServiceEnabled(ServiceName serviceName) throws ServiceNotFoundException {
        ServiceManager serviceManager = getServiceManager(serviceName);
        ServiceFactory serviceFactory = serviceManager.getServiceFactory();
        return serviceFactory.isEnabled();
    }

    /**
     * {@inheritDoc}
     */
    public void setServiceEnabled(ServiceName serviceName, boolean enabled) throws ServiceNotFoundException {
        ServiceManager serviceManager = getServiceManager(serviceName);
        ServiceFactory serviceFactory = serviceManager.getServiceFactory();
        serviceFactory.setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    public Object getService(ServiceName serviceName) throws ServiceNotFoundException, IllegalArgumentException {
        ServiceManager serviceManager = getServiceManager(serviceName);
        return serviceManager.getService();
    }

    /**
     * {@inheritDoc}
     */
    public ServiceFactory getServiceFactory(ServiceName serviceName) throws ServiceNotFoundException {
        ServiceManager serviceManager = getServiceManager(serviceName);
        return serviceManager.getServiceFactory();
    }

    /**
     * {@inheritDoc}
     */
    public ClassLoader getClassLoaderFor(ServiceName serviceName) throws ServiceNotFoundException {
        ServiceManager serviceManager = getServiceManager(serviceName);
        return serviceManager.getClassLoader();
    }

    private ServiceManager getServiceManager(ServiceName serviceName) throws ServiceNotFoundException {
        if (serviceName == null) throw new NullPointerException("serviceName is null");
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
        checkKernelState();
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
        checkKernelState();
        addServiceMonitor(serviceMonitor, null);
    }

    /**
     * {@inheritDoc}
     */
    public void addServiceMonitor(ServiceMonitor serviceMonitor, ServiceName serviceName) {
        if (serviceMonitor == null) throw new NullPointerException("serviceMonitor is null");
        checkKernelState();
        this.serviceMonitor.removeServiceMonitor(serviceMonitor);
    }

    /**
     * {@inheritDoc}
     */
    public void removeServiceMonitor(ServiceMonitor serviceMonitor) {
        if (serviceMonitor == null) throw new NullPointerException("serviceMonitor is null");
        this.serviceMonitor.removeServiceMonitor(serviceMonitor);
    }

    private void checkKernelState() {
        if (!running.get()) {
            throw new IllegalStateException("Kernel is stopped");
        }
    }
}
