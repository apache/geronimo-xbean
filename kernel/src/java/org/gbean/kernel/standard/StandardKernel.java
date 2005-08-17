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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.emory.mathcs.backport.java.util.concurrent.Callable;
import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutionException;
import edu.emory.mathcs.backport.java.util.concurrent.Executor;
import edu.emory.mathcs.backport.java.util.concurrent.Executors;
import edu.emory.mathcs.backport.java.util.concurrent.Future;
import edu.emory.mathcs.backport.java.util.concurrent.FutureTask;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import org.gbean.kernel.IllegalServiceStateException;
import org.gbean.kernel.Kernel;
import org.gbean.kernel.KernelErrorsError;
import org.gbean.kernel.KernelMonitor;
import org.gbean.kernel.KernelOperationInterruptedException;
import org.gbean.kernel.ServiceAlreadyExistsException;
import org.gbean.kernel.ServiceEvent;
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
    private final Map serviceManagers = new HashMap();

    /**
     * The monitors for service events.
     */
    private final Map serviceMonitors = new HashMap();

    /**
     * The monitors of kernel events.
     */
    private final List kernelMonitors = new CopyOnWriteArrayList();

    /**
     * This monitor broadcasts events to the listeners registered for service.
     */
    private final ServiceMonitor serviceMonitorBroadcaster = new ServiceMonitorBroadcaster();

    /**
     * Events service events are sent asynchronously using this executor.
     */
    private final Executor serviceExecutor = Executors.newCachedThreadPool();

    /**
     * If true, the kernel is still running.
     */
    private AtomicBoolean running = new AtomicBoolean(true);

    /**
     * The maximum duration to wait for a lock.
     */
    private final long timeoutDuration;

    /**
     * The unit of measure for the {@link #timeoutDuration}.
     */
    private final TimeUnit timeoutUnits;

    /**
     * Creates a kernel using the specified name.
     *
     * @param kernelName the unique name of this kernel
     */
    public StandardKernel(String kernelName) {
        this.kernelName = kernelName;
        timeoutDuration = 30;
        timeoutUnits = TimeUnit.SECONDS;
    }

    /**
     * {@inheritDoc} 
     */
    public void destroy() throws KernelErrorsError {
        // if we are already stopped return
        if (!running.compareAndSet(true, false)) {
            return;
        }

        // we gather all errors that occur during shutdown and throw them as on huge exception
        List errors = new ArrayList();

        List managerFutures;
        synchronized (serviceManagers) {
            managerFutures = new ArrayList();
            serviceManagers.clear();
        }

        List managers = new ArrayList(managerFutures.size());
        for (Iterator iterator = managerFutures.iterator(); iterator.hasNext();) {
            Future future = (Future) iterator.next();
            try {
                managers.add(future.get());
            } catch (InterruptedException e) {
                // ignore -- this should not happen
                errors.add(new AssertionError(e));
            } catch (ExecutionException e) {
                // good -- one less manager to deal with
            }
        }

        // Be nice and try to stop asynchronously
        for (Iterator iterator = managers.iterator(); iterator.hasNext();) {
            ServiceManager serviceManager = (ServiceManager) iterator.next();
            try {
                serviceManager.stop(StopStrategies.ASYNCHRONOUS);
            } catch (UnsatisfiedConditionsException e) {
                // this should not happen in with an asynchronous strategy
                errors.add(new AssertionError(e));
            } catch (RuntimeException e) {
                errors.add(new AssertionError(e));
            } catch (Error e) {
                errors.add(new AssertionError(e));
            }
        }

        // Be really nice and try to stop asynchronously again
        for (Iterator iterator = managers.iterator(); iterator.hasNext();) {
            ServiceManager serviceManager = (ServiceManager) iterator.next();
            try {
                serviceManager.stop(StopStrategies.ASYNCHRONOUS);
            } catch (UnsatisfiedConditionsException e) {
                // this should not happen in with an asynchronous strategy
                errors.add(new AssertionError(e));
            } catch (RuntimeException e) {
                errors.add(new AssertionError(e));
            } catch (Error e) {
                errors.add(new AssertionError(e));
            }
        }

        // We have been nice enough now nuke them
        for (Iterator iterator = managers.iterator(); iterator.hasNext();) {
            ServiceManager serviceManager = (ServiceManager) iterator.next();
            try {
                serviceManager.stop(StopStrategies.FORCE);
            } catch (UnsatisfiedConditionsException e) {
                // this should not happen in with an force strategy
                errors.add(new AssertionError(e));
            } catch (RuntimeException e) {
                errors.add(new AssertionError(e));
            } catch (Error e) {
                errors.add(new AssertionError(e));
            }
        }

        // All managers are gaurenteed to be destroyed now
        for (Iterator iterator = managers.iterator(); iterator.hasNext();) {
            ServiceManager serviceManager = (ServiceManager) iterator.next();
            try {
                serviceManager.destroy(StopStrategies.FORCE);
            } catch (UnsatisfiedConditionsException e) {
                // this should not happen, because we force stopped
                errors.add(new AssertionError(e));
            } catch (IllegalServiceStateException e) {
                // this should not happen, because we force stopped
                errors.add(new AssertionError(e));
            } catch (RuntimeException e) {
                errors.add(new AssertionError(e));
            } catch (Error e) {
                errors.add(new AssertionError(e));
            }
        }

        if (!errors.isEmpty()) {
            throw new KernelErrorsError(errors);
        }
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
        assert serviceName != null : "serviceName is null";
        assert serviceFactory != null : "serviceFactory is null";
        assert classLoader != null : "classLoader is null";
        if (!running.get()) {
            throw new IllegalStateException("Kernel is stopped");
        }

        FutureTask registrationTask = null;

        //
        // This loop will continue until we put our registrationTask in the serviceManagers map.  If at any point,
        // we discover that there is already a service registered under the specified service name, we will throw
        // a ServiceAlreadyExistsException exiting this method.
        //
        while (registrationTask == null) {
            Future existingRegistration;
            synchronized (serviceManagers) {
                existingRegistration = (Future) serviceManagers.get(serviceName);

                // if we do not have an existing registration or the existing registration task is complete
                // we can create the new registration task; otherwise we need to wait for the existing registration to
                // finish out side of the synchronized lock on serviceManagers.
                if (existingRegistration == null || existingRegistration.isDone()) {
                    // if we have a valid existing registration, throw a ServiceAlreadyExistsException
                    if (existingRegistration != null) {
                        try {
                            boolean alreadyRegistered = (existingRegistration.get() != null);
                            if (alreadyRegistered) {
                                throw new ServiceAlreadyExistsException(serviceName);
                            }
                        } catch (InterruptedException e) {
                            throw new KernelOperationInterruptedException(e, serviceName, "registerService");
                        } catch (ExecutionException e) {
                            // the previous registration threw an exception.. we can continure as normal
                        }
                    }

                    // we are ready to register our serviceManager
                    existingRegistration = null;
                    registrationTask = new FutureTask(new RegisterServiceManager(serviceName, serviceFactory, classLoader));
                    serviceManagers.put(serviceName, registrationTask);
                }
            }

            // If there is an unfinished exiting registration task, wait until it is done executing
            if (existingRegistration != null) {
                try {
                    existingRegistration.get();
                    // we don't throw an error here because we want to check in the synchronized block that this
                    // future is still registered in the serviceManagers map
                } catch (InterruptedException e) {
                    throw new KernelOperationInterruptedException(e, serviceName, "registerService");
                } catch (ExecutionException e) {
                    // good
                }
            }
        }

        // run our registration task and check the results
        registrationTask.run();
        try {
            // if initialization completed successfully, this method will not throw an exception
            registrationTask.get();
        } catch (InterruptedException e) {
            throw new KernelOperationInterruptedException(e, serviceName, "registerService");
        } catch (ExecutionException e) {
            throw new ServiceRegistrationException(serviceName, e.getCause());
        }
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
        assert serviceName != null : "serviceName is null";

        FutureTask unregistrationTask = null;
        final Throwable[] exceptionHandle = new Throwable[0];

        //
        // This loop will continue until we put our unregistrationTask in the serviceManagers map.  If at any point,
        // we discover that there actually is not a service registered under the specified service name, we will throw
        // a ServiceNotFoundException exiting this method.
        //
        while (unregistrationTask == null) {
            Future existingRegistration;
            synchronized (serviceManagers) {
                existingRegistration = (Future) serviceManagers.get(serviceName);
                if (existingRegistration == null) {
                    throw new ServiceNotFoundException(serviceName);
                }

                // if existing registration is done running, we can destroy it
                if (existingRegistration.isDone()) {
                    ServiceManager serviceManager = null;
                    try {
                        serviceManager = (ServiceManager) existingRegistration.get();
                    } catch (InterruptedException e) {
                        throw new KernelOperationInterruptedException(e, serviceName, "unregisterService");
                    } catch (ExecutionException e) {
                        // good
                    }

                    // if there isn't a registered manager that is an exception
                    if (serviceManager == null) {
                        throw new ServiceNotFoundException(serviceName);
                    }

                    // we are ready to register our serviceManager
                    existingRegistration = null;
                    unregistrationTask = new FutureTask(new UnregisterServiceManager(serviceManager, stopStrategy, exceptionHandle));
                    serviceManagers.put(serviceName, unregistrationTask);
                }
            }


            // If there is an unfinished exiting registration task, wait until it is done executing
            if (existingRegistration != null) {
                try {
                    existingRegistration.get();
                    // we don't throw an error here because we want to check in the synchronized block that this
                    // future is still registered in the serviceManagers map
                } catch (InterruptedException e) {
                    throw new KernelOperationInterruptedException(e, serviceName, "unregisterService");
                } catch (ExecutionException e) {
                    // good
                }
            }
        }

        unregistrationTask.run();
        try {
            // if get returns any value other then null, the unregistration failed
            if (unregistrationTask.get() != null) {
                synchronized (exceptionHandle) {
                    // the root exception is contained in the exception handle
                    throw new ServiceRegistrationException(serviceName, exceptionHandle[0]);
                }
            }
        } catch (InterruptedException e) {
            throw new KernelOperationInterruptedException(e, serviceName, "unregisterService");
        } catch (ExecutionException e) {
            // this won't happen
            throw new AssertionError(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRegistered(ServiceName serviceName) {
        Future serviceManagerFuture;
        synchronized (serviceManagers) {
            serviceManagerFuture = (Future) serviceManagers.get(serviceName);
        }
        try {
            // the service is registered if we have a non-null future value
            return serviceManagerFuture != null && serviceManagerFuture.get() != null;
        } catch (InterruptedException e) {
            throw new KernelOperationInterruptedException(e, serviceName, "isRegistered");
        } catch (ExecutionException e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public ServiceState getServiceState(ServiceName serviceName) throws ServiceNotFoundException {
        return getServiceManager(serviceName).getState();
    }

    /**
     * {@inheritDoc}
     */
    public long getServiceStartTime(ServiceName serviceName) throws ServiceNotFoundException {
        return getServiceManager(serviceName).getStartTime();
    }

    /**
     * {@inheritDoc}
     */
    public void startService(ServiceName serviceName) throws ServiceNotFoundException, IllegalServiceStateException, UnsatisfiedConditionsException, Exception {
        startService(serviceName, StartStrategies.SYNCHRONOUS);
    }

    /**
     * {@inheritDoc}
     */
    public void startService(ServiceName serviceName, StartStrategy startStrategy) throws ServiceNotFoundException, IllegalServiceStateException, UnsatisfiedConditionsException, Exception {
        ServiceManager serviceManager;
        if (!running.get()) {
            throw new IllegalStateException("Kernel is stopped");
        }
        serviceManager = getServiceManager(serviceName);
        try {
            serviceManager.start(false, startStrategy);
        } catch (UnregisterServiceException e) {
            unregisterService(serviceName);
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
    public void startServiceRecursive(ServiceName serviceName) throws ServiceNotFoundException, IllegalServiceStateException, UnsatisfiedConditionsException, Exception {
        startServiceRecursive(serviceName, StartStrategies.SYNCHRONOUS);
    }

    /**
     * {@inheritDoc}
     */
    public void startServiceRecursive(ServiceName serviceName, StartStrategy startStrategy) throws ServiceNotFoundException, IllegalServiceStateException, UnsatisfiedConditionsException, Exception {
        ServiceManager serviceManager;
        if (!running.get()) {
            throw new IllegalStateException("Kernel is stopped");
        }
        serviceManager = getServiceManager(serviceName);
        try {
            serviceManager.start(true, startStrategy);
        } catch (UnregisterServiceException e) {
            unregisterService(serviceName);
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
        ServiceManager serviceManager = getServiceManager(serviceName);
        serviceManager.stop(stopStrategy);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isServiceEnabled(ServiceName serviceName) throws ServiceNotFoundException {
        return getServiceFactory(serviceName).isEnabled();
    }

    /**
     * {@inheritDoc}
     */
    public void setServiceEnabled(ServiceName serviceName, boolean enabled) throws ServiceNotFoundException {
        getServiceFactory(serviceName).setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    public Object getService(ServiceName serviceName) throws ServiceNotFoundException, IllegalArgumentException {
        return getServiceManager(serviceName).getService();
    }

    /**
     * {@inheritDoc}
     */
    public ServiceFactory getServiceFactory(ServiceName serviceName) throws ServiceNotFoundException {
        return getServiceManager(serviceName).getServiceFactory();
    }

    /**
     * {@inheritDoc}
     */
    public ClassLoader getClassLoaderFor(ServiceName serviceName) throws ServiceNotFoundException {
        return getServiceManager(serviceName).getClassLoader();
    }

    private ServiceManager getServiceManager(ServiceName serviceName) throws ServiceNotFoundException {
        Future serviceManagerFuture;
        synchronized (serviceManagers) {
            serviceManagerFuture = (Future) serviceManagers.get(serviceName);
        }

        // this service has no future
        if (serviceManagerFuture == null) {
            throw new ServiceNotFoundException(serviceName);
        }

        try {
            ServiceManager serviceManager = (ServiceManager) serviceManagerFuture.get();
            if (serviceManager == null) {
                throw new ServiceNotFoundException(serviceName);
            }
            return serviceManager;
        } catch (InterruptedException e) {
            throw new KernelOperationInterruptedException(e, serviceName, "getServiceManager");
        } catch (ExecutionException e) {
            // registration threw an exception which means it didn't register
            throw new ServiceNotFoundException(serviceName);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addKernelMonitor(KernelMonitor kernelMonitor) {
        kernelMonitors.add(kernelMonitor);
    }

    /**
     * {@inheritDoc}
     */
    public void removeKernelMonitor(KernelMonitor kernelMonitor) {
        kernelMonitors.remove(kernelMonitor);
    }

    private List fireServiceNotificationError(ServiceMonitor serviceMonitor, ServiceEvent serviceEvent, Throwable throwable) {
        List errors = new ArrayList();
        for (Iterator iterator = kernelMonitors.iterator(); iterator.hasNext();) {
            KernelMonitor kernelMonitor = (KernelMonitor) iterator.next();
            try {
                kernelMonitor.serviceNotificationError(serviceMonitor, serviceEvent, throwable);
            } catch (RuntimeException ignored) {
                // ignore - we did our best to notify the world
            } catch (Error e) {
                errors.add(e);
            }
        }
        return errors;
    }

    /**
     * {@inheritDoc}
     */
    public void addServiceMonitor(ServiceMonitor serviceMonitor) {
        addServiceMonitor(serviceMonitor, null);
    }

    /**
     * {@inheritDoc}
     */
    public void addServiceMonitor(ServiceMonitor serviceMonitor, ServiceName serviceName) {
        synchronized (serviceMonitors) {
            Set monitors = (Set) serviceMonitors.get(serviceName);
            if (monitors == null) {
                monitors = new HashSet();
                serviceMonitors.put(serviceName, serviceMonitor);
            }
            monitors.add(serviceMonitor);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeServiceMonitor(ServiceMonitor serviceMonitor) {
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

    private Set getServiceMonitors(ServiceName serviceName) {
        synchronized (serviceMonitors) {
            Set monitors = new HashSet();
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

    private class ServiceMonitorBroadcaster implements ServiceMonitor {
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

        public void serviceStarting(ServiceEvent serviceEvent) {
            List errors = new ArrayList();
            Set serviceMonitors = getServiceMonitors(serviceEvent.getServiceName());
            for (Iterator iterator = serviceMonitors.iterator(); iterator.hasNext();) {
                ServiceMonitor serviceMonitor = (ServiceMonitor) iterator.next();
                try {
                    serviceMonitor.serviceStarting(serviceEvent);
                } catch (Exception e) {
                    errors.addAll(fireServiceNotificationError(serviceMonitor, serviceEvent, e));
                }
            }
            if (!errors.isEmpty()) {
                throw new KernelErrorsError(errors);
            }
        }

        public void serviceWaitingToStart(ServiceEvent serviceEvent) {
            List errors = new ArrayList();
            Set serviceMonitors = getServiceMonitors(serviceEvent.getServiceName());
            for (Iterator iterator = serviceMonitors.iterator(); iterator.hasNext();) {
                ServiceMonitor serviceMonitor = (ServiceMonitor) iterator.next();
                try {
                    serviceMonitor.serviceWaitingToStart(serviceEvent);
                } catch (Exception e) {
                    errors.addAll(fireServiceNotificationError(serviceMonitor, serviceEvent, e));
                }
            }
            if (!errors.isEmpty()) {
                throw new KernelErrorsError(errors);
            }
        }

        public void serviceStartError(ServiceEvent serviceEvent) {
            List errors = new ArrayList();
            Set serviceMonitors = getServiceMonitors(serviceEvent.getServiceName());
            for (Iterator iterator = serviceMonitors.iterator(); iterator.hasNext();) {
                ServiceMonitor serviceMonitor = (ServiceMonitor) iterator.next();
                try {
                    serviceMonitor.serviceStartError(serviceEvent);
                } catch (Exception e) {
                    errors.addAll(fireServiceNotificationError(serviceMonitor, serviceEvent, e));
                }
            }
            if (!errors.isEmpty()) {
                throw new KernelErrorsError(errors);
            }
        }

        public void serviceRunning(ServiceEvent serviceEvent) {
            List errors = new ArrayList();
            Set serviceMonitors = getServiceMonitors(serviceEvent.getServiceName());
            for (Iterator iterator = serviceMonitors.iterator(); iterator.hasNext();) {
                ServiceMonitor serviceMonitor = (ServiceMonitor) iterator.next();
                try {
                    serviceMonitor.serviceRunning(serviceEvent);
                } catch (Exception e) {
                    errors.addAll(fireServiceNotificationError(serviceMonitor, serviceEvent, e));
                }
            }
            if (!errors.isEmpty()) {
                throw new KernelErrorsError(errors);
            }
        }

        public void serviceStopping(ServiceEvent serviceEvent) {
            List errors = new ArrayList();
            Set serviceMonitors = getServiceMonitors(serviceEvent.getServiceName());
            for (Iterator iterator = serviceMonitors.iterator(); iterator.hasNext();) {
                ServiceMonitor serviceMonitor = (ServiceMonitor) iterator.next();
                try {
                    serviceMonitor.serviceStopping(serviceEvent);
                } catch (Exception e) {
                    errors.addAll(fireServiceNotificationError(serviceMonitor, serviceEvent, e));
                }
            }
            if (!errors.isEmpty()) {
                throw new KernelErrorsError(errors);
            }
        }

        public void serviceWaitingToStop(ServiceEvent serviceEvent) {
            List errors = new ArrayList();
            Set serviceMonitors = getServiceMonitors(serviceEvent.getServiceName());
            for (Iterator iterator = serviceMonitors.iterator(); iterator.hasNext();) {
                ServiceMonitor serviceMonitor = (ServiceMonitor) iterator.next();
                try {
                    serviceMonitor.serviceWaitingToStop(serviceEvent);
                } catch (Exception e) {
                    errors.addAll(fireServiceNotificationError(serviceMonitor, serviceEvent, e));
                }
            }
            if (!errors.isEmpty()) {
                throw new KernelErrorsError(errors);
            }
        }

        public void serviceStopError(ServiceEvent serviceEvent) {
            List errors = new ArrayList();
            Set serviceMonitors = getServiceMonitors(serviceEvent.getServiceName());
            for (Iterator iterator = serviceMonitors.iterator(); iterator.hasNext();) {
                ServiceMonitor serviceMonitor = (ServiceMonitor) iterator.next();
                try {
                    serviceMonitor.serviceStopError(serviceEvent);
                } catch (Exception e) {
                    errors.addAll(fireServiceNotificationError(serviceMonitor, serviceEvent, e));
                }
            }
            if (!errors.isEmpty()) {
                throw new KernelErrorsError(errors);
            }
        }

        public void serviceStopped(ServiceEvent serviceEvent) {
            List errors = new ArrayList();
            Set serviceMonitors = getServiceMonitors(serviceEvent.getServiceName());
            for (Iterator iterator = serviceMonitors.iterator(); iterator.hasNext();) {
                ServiceMonitor serviceMonitor = (ServiceMonitor) iterator.next();
                try {
                    serviceMonitor.serviceStopped(serviceEvent);
                } catch (Exception e) {
                    errors.addAll(fireServiceNotificationError(serviceMonitor, serviceEvent, e));
                }
            }
            if (!errors.isEmpty()) {
                throw new KernelErrorsError(errors);
            }
        }

        public void serviceUnregistered(ServiceEvent serviceEvent) {
            List errors = new ArrayList();
            Set serviceMonitors = getServiceMonitors(serviceEvent.getServiceName());
            for (Iterator iterator = serviceMonitors.iterator(); iterator.hasNext();) {
                ServiceMonitor serviceMonitor = (ServiceMonitor) iterator.next();
                try {
                    serviceMonitor.serviceUnregistered(serviceEvent);
                } catch (Exception e) {
                    errors.addAll(fireServiceNotificationError(serviceMonitor, serviceEvent, e));
                }
            }
            if (!errors.isEmpty()) {
                throw new KernelErrorsError(errors);
            }
        }
    }

    private class RegisterServiceManager implements Callable {
        private final ServiceName serviceName;
        private final ServiceFactory serviceFactory;
        private final ClassLoader classLoader;

        private RegisterServiceManager(ServiceName serviceName, ServiceFactory serviceFactory, ClassLoader classLoader) {
            this.serviceName = serviceName;
            this.serviceFactory = serviceFactory;
            this.classLoader = classLoader;
        }

        public Object call() throws Exception {
            ServiceManager serviceManager = new ServiceManager(StandardKernel.this,
                    serviceName,
                    serviceFactory,
                    classLoader,
                    serviceMonitorBroadcaster,
                    serviceExecutor,
                    timeoutDuration,
                    timeoutUnits);

            try {
                serviceManager.initialize();
                return serviceManager;
            } catch (Exception e) {
                // non-restartable service factories must be started from the initialize method, and this throws exceptions
                synchronized (serviceManagers) {
                    serviceManagers.remove(serviceName);
                }
                throw new ServiceRegistrationException(serviceName, e);
            }
        }
    }

    private class UnregisterServiceManager implements Callable {
        private final ServiceManager serviceManager;
        private final StopStrategy stopStrategy;
        private final Throwable[] futureThrowable;

        private UnregisterServiceManager(ServiceManager serviceManager, StopStrategy stopStrategy, Throwable[] futureThrowable) {
            this.serviceManager = serviceManager;
            this.stopStrategy = stopStrategy;
            this.futureThrowable = futureThrowable;
        }

        public Object call() {
            try {
                serviceManager.destroy(stopStrategy);
                synchronized (serviceManagers) {
                    serviceManagers.remove(serviceManager.getServiceName());
                }
                return null;
            } catch (Throwable e) {
                // we did not destroy the service... save the exception and return the service manager
                // so it remains registered with the kernel
                synchronized (futureThrowable) {
                    futureThrowable[0] = e;
                }
                return serviceManager;
            }
        }
    }
}
