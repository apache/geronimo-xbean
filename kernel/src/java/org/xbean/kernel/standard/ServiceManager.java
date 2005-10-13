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

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Collections;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicLong;
import edu.emory.mathcs.backport.java.util.concurrent.locks.ReentrantLock;
import org.xbean.kernel.ForcedStopException;
import org.xbean.kernel.IllegalServiceStateException;
import org.xbean.kernel.Kernel;
import org.xbean.kernel.KernelOperationInterruptedException;
import org.xbean.kernel.KernelOperationTimoutException;
import org.xbean.kernel.ServiceCondition;
import org.xbean.kernel.ServiceEvent;
import org.xbean.kernel.ServiceFactory;
import org.xbean.kernel.ServiceMonitor;
import org.xbean.kernel.ServiceName;
import org.xbean.kernel.ServiceNotFoundException;
import org.xbean.kernel.ServiceState;
import org.xbean.kernel.StartStrategies;
import org.xbean.kernel.StartStrategy;
import org.xbean.kernel.StopStrategy;
import org.xbean.kernel.UnregisterServiceException;
import org.xbean.kernel.UnsatisfiedConditionsException;
import org.xbean.kernel.InvalidServiceTypeException;

/**
 * The ServiceManager handles the life cycle of a single service.   The manager is responsible for gaurenteeing that
 * all start conditions have been satisfied before the service is constructed, and that all stop conditions have been
 * satisfied before the service is destroyed.  The ServiceManager can be started and stopped several times, but once
 * destroyed no methods may be called.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class ServiceManager implements Comparable {
    /**
     * The kernel in which this service is registered.
     */
    private final Kernel kernel;

    /**
     * The unique id of this service in the kernel.
     */
    private final long serviceId;

    /**
     * The unique name of this service in the kernel.
     */
    private final ServiceName serviceName;

    /**
     * The factory used to create and destroy the service instance.
     */
    private final ServiceFactory serviceFactory;

    /**
     * The type of service this service manager will create.  This value is cached from the serviceFactory.getT
     */
    private final Set serviceTypes;

    /**
     * The class loader for this service.
     */
    private final ClassLoader classLoader;

    /**
     * The monitor to which we fire service events.  The ServiceManager requires an asynchronous monitor becuse events are
     * fired from within the lock.  This helps to reduce complexity but will cause more services to sit in the
     * {@link ServiceState#STARTING} and {@link ServiceState#STOPPING} states since events are propagated in a separate
     * thread.
     */
    private final ServiceMonitor serviceMonitor;

    /**
     * The service context given to the service factory.  This contans a reference to the kernel, serviceName and
     * classloader.
     */
    private final StandardServiceContext standardServiceContext;

    /**
     * Current state of this service.
     */
    private volatile ServiceState state = ServiceState.STOPPED;

    /**
     * The time the service was started or 0 if not started.
     */
    private volatile long startTime;

    /**
     * The {@link ServiceCondition) objects required to be ready before this service can be completely started.
     */
    private AggregateCondition startCondition;

    /**
     * The {@link ServiceCondition) objects required to be ready before this service can be completely stopped.
     */
    private AggregateCondition stopCondition;

    /**
     * The service instance.
     */
    private volatile Object service;

    /**
     * The single lock we use.
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * The maximum duration to wait for the lock.
     */
    private final long timeoutDuration;

    /**
     * The unit of measure for the {@link #timeoutDuration}.
     */
    private final TimeUnit timeoutUnits;

    /**
     * The name of the operation for which the lock is held; this is used in the reentrant exception message.
     */
    private String currentLockHolderOperation = "NOT-HELD";

    /**
     * Sequence number for service event objects.
     */
    private final AtomicLong eventId = new AtomicLong(0);

    /**
     * If true, when start is successful we will startRecusrive all of the services owned by this service.
     */
    private boolean recursive = false;

    /**
     * Creates a service manager for a single service.
     *
     * @param kernel the kernel in which this wraper will be registered
     * @param serviceId the unique id of this service in the kernel
     * @param serviceName the unique name of this service in the kernel
     * @param serviceFactory the factory used to create and destroy the service instance
     * @param classLoader the class loader for this service
     * @param serviceMonitor the monitor of service events
     * @param timeoutDuration the maximum duration to wait for a lock
     * @param timeoutUnits the unit of measure for the timeoutDuration
     */
    public ServiceManager(Kernel kernel,
            long serviceId,
            ServiceName serviceName,
            ServiceFactory serviceFactory,
            ClassLoader classLoader,
            ServiceMonitor serviceMonitor,
            long timeoutDuration,
            TimeUnit timeoutUnits) {

        this.kernel = kernel;
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.serviceFactory = serviceFactory;
        this.classLoader = classLoader;
        this.serviceMonitor = serviceMonitor;
        this.timeoutDuration = timeoutDuration;
        this.timeoutUnits = timeoutUnits;
        standardServiceContext = new StandardServiceContext(kernel, serviceName, classLoader);
        serviceTypes = Collections.unmodifiableSet(new LinkedHashSet(Arrays.asList(serviceFactory.getTypes())));
    }

    /**
     * Initializes the service.
     *
     * @throws IllegalServiceStateException if the service is not restartable and is disabled
     * @throws UnsatisfiedConditionsException if the service is not restartable and there were unsatisfied start conditions
     * @throws Exception if the service is not restartable and service construction threw an exception
     * @see Kernel#registerService(ServiceName, ServiceFactory, ClassLoader)
     */
    public void initialize() throws IllegalServiceStateException, UnsatisfiedConditionsException, Exception {
        if (!serviceFactory.isRestartable() && !serviceFactory.isEnabled()) {
            throw new IllegalServiceStateException("A disabled non-restartable service factory can not be initalized", serviceName);
        }

        serviceMonitor.serviceRegistered(createServiceEvent());

        // if we are not restartable, we need to start immediately, otherwise we are not going to register this service
        if (!serviceFactory.isRestartable()) {
            try {
                start(false, StartStrategies.UNREGISTER);
            } catch (UnregisterServiceException e) {
                serviceMonitor.serviceUnregistered(createServiceEvent());
                Throwable cause = e.getCause();
                if (cause instanceof Exception) {
                    throw (Exception) cause;
                } else if (cause instanceof Error) {
                    throw (Error) cause;
                } else {
                    throw new AssertionError(cause);
                }
            }

            // a non restartable service uses a special stop conditions object that picks up stop conditions as they
            // are added.  When the stop() method is called on a non-restartable service all of the stop conditions
            // registered with the service factory are initialized (if not already initialized), and the isSatisfied
            // method is called.  This should cause the stop logic of a stop condition to fire.
            lock("initialize");
            try {
                stopCondition = new NonRestartableStopCondition(kernel, serviceName, classLoader, lock, serviceFactory);
            } finally {
                unlock();
            }
        }
    }

    /**
     * Attempts to stop and destroy the service.
     *
     * @param stopStrategy the strategy used to determine how to handle unsatisfied stop conditions
     * @throws IllegalServiceStateException is the service did not stop
     * @throws UnsatisfiedConditionsException if there were unsatisfied stop conditions
     * @see Kernel#unregisterService(ServiceName, StopStrategy)
     */
    public void destroy(StopStrategy stopStrategy) throws IllegalServiceStateException, UnsatisfiedConditionsException {
        // if we are not restartable, we need to stop
        try {
            if (!stop(stopStrategy)) {
                throw new IllegalServiceStateException("Service did not stop", serviceName);
            }
        } catch (UnsatisfiedConditionsException e) {
            throw e;
        }

        if (!serviceFactory.isRestartable()) {
            lock("destroy");
            try {
                if (state != ServiceState.STOPPED) {
                    state = ServiceState.STARTING;
                    serviceMonitor.serviceStopping(createServiceEvent());
                    if (service != null) {
                        try {
                            // destroy the service
                            serviceFactory.destroyService(standardServiceContext);
                        } catch (Throwable e) {
                            serviceMonitor.serviceStopError(createErrorServiceEvent(e));
                        }
                    }

                    destroyAllConditions(serviceMonitor);

                    service = null;
                    startTime = 0;
                    state = ServiceState.STOPPED;
                    serviceMonitor.serviceStopped(createServiceEvent());
                }
            } finally {
                unlock();
            }
        }

        // cool we can unregistered
        serviceMonitor.serviceUnregistered(createServiceEvent());
    }

    /**
     * Gets the unique id of this service in the kernel.
     *
     * @return the unique id of this service in the kernel
     */
    public long getServiceId() {
        return serviceId;
    }

    /**
     * Gets the unique name of this service in the kernel.
     *
     * @return the unique name of this servce in the kernel
     */
    public ServiceName getServiceName() {
        return serviceName;
    }

    /**
     * Gets the types of the service that will be managed by this service manager.
     * @return the types of the service
     */
    public Set getServiceTypes() {
        return serviceTypes;
    }

    /**
     * Gets the factory used to create and destroy the service instance.
     *
     * @return the factory for the service instance
     * @see Kernel#getServiceFactory(ServiceName)
     */
    public ServiceFactory getServiceFactory() {
        return serviceFactory;
    }

    /**
     * Gets the class loader for this service.  This class loader is provided to the service factory in the
     * ServiceContext object.
     *
     * @return the classloader for this service
     * @see Kernel#getClassLoaderFor(ServiceName)
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Gets the service instance.
     *
     * @return the service instance
     * @see Kernel#getService(ServiceName)
     */
    public Object getService() {
        return service;
    }

    /**
     * Gets the current state of this service.
     *
     * @return the current state of this service
     * @see Kernel#getServiceState(ServiceName)
     */
    public ServiceState getState() {
        return state;
    }

    /**
     * Gets the time at which this service entered the STARTING state or 0 if the service is STOPPED.
     *
     * @return the start time or 0 if the service is stopped
     * @see Kernel#getServiceStartTime(ServiceName)
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Attempts to starts the service.
     *
     * @param recursive if start is successful should we start recursive the services owned by this servic
     * @param startStrategy the strategy used to determine how to handle unsatisfied start conditions and start errors
     * @throws IllegalServiceStateException if the service is in a state in which it can not be started
     * @throws UnregisterServiceException if the kernel should unregister this service
     * @throws UnsatisfiedConditionsException if there were unsatisfied start conditions
     * @throws Exception it service creation threw an exception
     * @see Kernel#startService(ServiceName)
     * @see Kernel#startServiceRecursive(ServiceName)
     */
    public void start(boolean recursive, StartStrategy startStrategy) throws IllegalServiceStateException, UnregisterServiceException, UnsatisfiedConditionsException, Exception {
        // verify that it is possible to start this service in the current state before obtaining the lock
        if (!verifyStartable(state)) {
            if (recursive) {
                startOwnedServices(startStrategy);
            }
            return;
        }

        boolean shouldStartRecursive = false;
        lock("start");
        try {
            // update the recursive flag
            this.recursive = this.recursive || recursive;

            Throwable startError = null;
            try {
                //
                // Loop until all start conditions have been satified.  The start strategy can break this loop.
                //
                boolean satisfied = false;
                while (!satisfied) {
                    // do we still want to start?
                    if (!verifyStartable(state)) {
                        // assume someone else called startOwnedServices
                        return;
                    }

                    // if we are in the STOPPED state, we need to move to the STARTING state
                    if (state == ServiceState.STOPPED) {
                        // we are now officially starting
                        state = ServiceState.STARTING;
                        serviceMonitor.serviceStarting(createServiceEvent());

                        // initialize the start conditions
                        startCondition = new AggregateCondition(kernel, serviceName, classLoader, lock, serviceFactory.getStartConditions());
                        startCondition.initialize();
                    }

                    // are we satisfied?
                    Set unsatisfiedConditions = startCondition.getUnsatisfied();
                    satisfied = unsatisfiedConditions.isEmpty();
                    if (!satisfied) {
                        // if the stragegy wants us to wait for conditions to be satisfied, it will return true
                        if (startStrategy.waitForUnsatisfiedConditions(serviceName, unsatisfiedConditions)) {
                            // wait for satisfaction and loop
                            startCondition.awaitSatisfaction();
                        } else {
                            // no wait, notify the monitor and exit
                            serviceMonitor.serviceWaitingToStart(createWaitingServiceEvent(unsatisfiedConditions));
                            return;
                        }
                    }
                }

                // we are ready to create the service
                service = serviceFactory.createService(standardServiceContext);

                // verify that the service implements all of the types
                if (service == null) {
                    throw new NullPointerException("Service factory return null from createService for service " + serviceName);
                }
                for (Iterator iterator = serviceTypes.iterator(); iterator.hasNext();) {
                    Class type = (Class) iterator.next();
                    if (!type.isInstance(service)) {
                        throw new InvalidServiceTypeException(serviceName, type, service.getClass());
                    }
                }

                // success transition to running
                startTime = System.currentTimeMillis();
                state = ServiceState.RUNNING;
                serviceMonitor.serviceRunning(createServiceEvent());

                // should we recursively start our children
                shouldStartRecursive = this.recursive || recursive;
                this.recursive = false;
            } catch (UnsatisfiedConditionsException e) {
                // thrown from waitForUnsatisfiedConditions
                throw e;
            } catch (IllegalServiceStateException e) {
                // this can be thrown while awaiting satisfaction
                throw e;
            } catch (Exception e) {
                startError = e;
            } catch (Error e) {
                startError = e;
            }

            if (startError != null) {
                try {
                    if (startError instanceof UnregisterServiceException) {
                        throw (UnregisterServiceException) startError;
                    } else {
                        // the strategy will normally rethrow the startError, but if it doesn't notify the service monitor
                        startStrategy.startError(serviceName, startError);
                        serviceMonitor.serviceStartError(createErrorServiceEvent(startError));
                    }
                } finally {
                    // we are now STOPPING
                    state = ServiceState.STOPPING;
                    serviceMonitor.serviceStopping(createServiceEvent());

                    // clean up the conditons
                    destroyAllConditions(serviceMonitor);

                    // transition to the STOPPED state
                    service = null;
                    startTime = 0;
                    state = ServiceState.STOPPED;
                    serviceMonitor.serviceStopped(createServiceEvent());
                }
            }
        } finally {
            unlock();
        }


        // startRecursive all of the owned services
        if (shouldStartRecursive) {
            startOwnedServices(startStrategy);
        }
    }

    private void startOwnedServices(StartStrategy startStrategy) throws IllegalServiceStateException, UnsatisfiedConditionsException, Exception {
        Set ownedServices = serviceFactory.getOwnedServices();
        if (ownedServices == null) throw new NullPointerException("serviceFactory.getOwnedServices() returned null");
        for (Iterator iterator = ownedServices.iterator(); iterator.hasNext();) {
            ServiceName ownedService = (ServiceName) iterator.next();
            try {
                kernel.startServiceRecursive(ownedService, startStrategy);
            } catch (ServiceNotFoundException ignored) {
                // this is ok -- service unregistered
            } catch (IllegalServiceStateException ignored) {
                // ownedService is disabled or stopping -- anyway we don't care
            }
        }
    }

    /**
     * Verifies that the service is startable.  This can be used out side a lock to avoid unecessary locking.
     *
     * @param state the state of the service
     * @return true if it is possible to start a service in the specifiec state
     * @throws IllegalServiceStateException if it is illegal to start a service in the specified state
     */
    private boolean verifyStartable(ServiceState state) throws IllegalServiceStateException {
        // if we are alredy in the running state, there is nothing to do
        if (state == ServiceState.RUNNING) {
            return false;
        }

        // if we are in the stopping states, that is an error
        if (state == ServiceState.STOPPING) {
            throw new IllegalServiceStateException("A stopping service can not be started", serviceName);
        }

        // is this service enabled?
        if (state == ServiceState.STOPPED && !serviceFactory.isEnabled()) {
            throw new IllegalServiceStateException("Service is disabled", serviceName);
        }

        return true;
    }

    /**
     * Attempts to stop the service.
     *
     * @param stopStrategy the strategy used to determine how to handle unsatisfied stop conditions
     * @return true if the service was sucessfully stopped; false otherwise
     * @throws UnsatisfiedConditionsException if there were unsatisfied stop conditions
     * @see Kernel#stopService(ServiceName)
     */
    public boolean stop(StopStrategy stopStrategy) throws UnsatisfiedConditionsException {
        // check that we aren't already stopped before attempting to acquire the lock
        ServiceState initialState = state;
        if (initialState == ServiceState.STOPPED) {
            return true;
        }

        lock("stop");
        try {
            try {
                //
                // Loop until all stop conditions have been satified.  The stop strategy can break this loop.
                //
                boolean satisfied = false;
                while (!satisfied) {
                    // do we still want to stop?
                    if (state == ServiceState.STOPPED) {
                        return true;
                    }

                    // if we are not the STOPPING state, transition to it
                    // we check on the stopConditions variable because non-restartable services preset this in the
                    // intialization method
                    if (stopCondition == null) {
                        // we are not officially stopping
                        serviceMonitor.serviceStopping(createServiceEvent());
                        state = ServiceState.STOPPING;

                        // initialize all of the stop conditions
                        stopCondition = new AggregateCondition(kernel, serviceName, classLoader, lock, serviceFactory.getStopConditions());
                        stopCondition.initialize();
                    }

                    // are we satisfied?
                    Set unsatisfiedConditions = stopCondition.getUnsatisfied();
                    satisfied = unsatisfiedConditions.isEmpty();
                    if (!satisfied) {
                        // if the stragegy wants us to wait for conditions to be satisfied, it will return true
                        if (stopStrategy.waitForUnsatisfiedConditions(serviceName, unsatisfiedConditions)) {
                            // wait for satisfaction and loop
                            stopCondition.awaitSatisfaction();
                        } else {
                            // no wait, notify the monitor and exit
                            serviceMonitor.serviceWaitingToStop(createWaitingServiceEvent(unsatisfiedConditions));
                            return false;
                        }
                    }
                }
            } catch (UnsatisfiedConditionsException e) {
                throw e;
            } catch (ForcedStopException e) {
                serviceMonitor.serviceStopError(createErrorServiceEvent(e));
            } catch (Exception e) {
                serviceMonitor.serviceStopError(createErrorServiceEvent(e));
            } catch (Error e) {
                serviceMonitor.serviceStopError(createErrorServiceEvent(e));
            }

            if (serviceFactory.isRestartable()) {
                if (service != null) {
                    try {
                        // destroy the service
                        serviceFactory.destroyService(standardServiceContext);
                    } catch (Throwable e) {
                        serviceMonitor.serviceStopError(createErrorServiceEvent(e));
                    }
                }

                destroyAllConditions(serviceMonitor);

                service = null;
                startTime = 0;
                state = ServiceState.STOPPED;
                serviceMonitor.serviceStopped(createServiceEvent());
            }
            return true;
        } finally {
            unlock();
        }
    }

    private void destroyAllConditions(ServiceMonitor monitor) {
        if (!lock.isHeldByCurrentThread()) {
            throw new IllegalStateException("Current thread must hold lock before calling destroyAllConditions");
        }

        if (startCondition != null) {
            List errors = startCondition.destroy();
            // errors from destroying the start conditions are stop errors because destroy is only called while
            // stopping the service
            for (Iterator iterator = errors.iterator(); iterator.hasNext();) {
                Throwable stopError = (Throwable) iterator.next();
                monitor.serviceStopError(createErrorServiceEvent(stopError));
            }
            startCondition = null;
        }
        if (stopCondition != null) {
            List errors = stopCondition.destroy();
            for (Iterator iterator = errors.iterator(); iterator.hasNext();) {
                Throwable stopError = (Throwable) iterator.next();
                monitor.serviceStopError(createErrorServiceEvent(stopError));
            }
            stopCondition = null;
        }
    }

    /**
     * Obtain the lock for the specified operation.
     *
     * @param operationName name of the operation that lock will be used for - this is only used for exception messages
     * @throws IllegalStateException if thread tries to reenter while holding the lock
     * @throws KernelOperationTimoutException if lock could not be obtained in {@link #timeoutDuration} {@link #timeoutUnits}
     * @throws KernelOperationInterruptedException if the thread was interrupted while waiting for the lock
     */
    private void lock(String operationName) throws IllegalStateException, KernelOperationTimoutException, KernelOperationInterruptedException {
        if (lock.isHeldByCurrentThread()) {
            throw new IllegalStateException("Current thread holds lock for " + currentLockHolderOperation +
                    " and lock can not be reacquired for " + operationName + " on " + serviceName);
        }

        try {
            if (!lock.tryLock(timeoutDuration, timeoutUnits)) {
                throw new KernelOperationTimoutException("Could not obtain lock for " + operationName + " operation on " +
                        serviceName + " within " + timeoutDuration + " " + timeoutUnits.toString().toLowerCase(),
                        serviceName,
                        operationName);
            }
            currentLockHolderOperation = operationName;
        } catch (InterruptedException e) {
            throw new KernelOperationInterruptedException("Interrupted while attempting to obtain lock for " + operationName +
                    " operation on " + serviceName,
                    e,
                    serviceName,
                    operationName);

        }
    }

    /**
     * Unlock the lock and clear the currentLockHolderOperation name.
     */
    private void unlock() {
        if (!lock.isHeldByCurrentThread()) {
            throw new IllegalMonitorStateException("Not owner");
        }

        currentLockHolderOperation = "NOT-HELD";
        lock.unlock();
    }

    private ServiceEvent createServiceEvent() {
        return new ServiceEvent(eventId.getAndIncrement(), kernel, serviceName, serviceFactory, classLoader, service, null, null);
    }

    private ServiceEvent createWaitingServiceEvent(Set unsatisfiedConditions) {
        return new ServiceEvent(eventId.getAndIncrement(), kernel, serviceName, serviceFactory, classLoader, service, null, unsatisfiedConditions);
    }

    private ServiceEvent createErrorServiceEvent(Throwable cause) {
        return new ServiceEvent(eventId.getAndIncrement(), kernel, serviceName, serviceFactory, classLoader, null, cause, null);
    }

    public int hashCode() {
        return (int) (serviceId ^ (serviceId >>> 32));
    }

    public boolean equals(Object o) {
        if (o instanceof ServiceManager) {
            return serviceId == ((ServiceManager)o).serviceId;
        }
        return false;
    }

    public int compareTo(Object o) {
        ServiceManager serviceManager = (ServiceManager) o;

        if (serviceId < serviceManager.serviceId) {
            return -1;
        } else if (serviceId > serviceManager.serviceId) {
            return 1;
        } else {
            return 0;
        }
    }

    public String toString() {
        return "[ServiceManager: serviceId=" + serviceId + ", serviceName=" + serviceName + ", state=" + state + "]";
    }
}
