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
package org.gbean.kernel.runtime;

import java.util.Iterator;
import java.util.Set;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gbean.kernel.Kernel;
import org.gbean.kernel.DependencyManager;
import org.gbean.kernel.LifecycleListener;
import org.gbean.kernel.ServiceNotFoundException;
import org.gbean.kernel.LifecycleAdapter;

/**
 * @version $Rev$ $Date$
 */
public final class ServiceInstanceState {
    private static final Log log = LogFactory.getLog(ServiceInstanceState.class);

    /**
     * The ServiceInstance in which this server is registered.
     */
    private final ServiceInstance serviceInstance;

    /**
     * The kernel in which this server is registered.
     */
    private final Kernel kernel;

    /**
     * The unique name of this service.
     */
    private final ObjectName objectName;

    /**
     * The dependency manager
     */
    private final DependencyManager dependencyManager;

    /**
     * The broadcaster of lifecycle events
     */
    private final LifecycleBroadcaster lifecycleBroadcaster;

    /**
     * The listener for the of the object blocking the start of this service.
     * When the blocker dies we attempt to start.
     */
    private LifecycleListener blockerListener;

    // This must be volatile otherwise getState must be synchronized which will result in deadlock as dependent
    // objects check if each other are in one state or another (i.e., classic A calls B while B calls A)
    private volatile ServiceState state = ServiceState.STOPPED;

    ServiceInstanceState(ObjectName objectName, Kernel kernel, DependencyManager dependencyManager, ServiceInstance serviceInstance, LifecycleBroadcaster lifecycleBroadcaster) {
        this.objectName = objectName;
        this.kernel = kernel;
        this.dependencyManager = dependencyManager;
        this.serviceInstance = serviceInstance;
        this.lifecycleBroadcaster = lifecycleBroadcaster;
    }

    /**
     * Moves this service to the starting state and then attempts to move this service immediately
     * to the running state.
     * <p/>
     * Note:  This method cannot be called while the current thread holds a synchronized lock on this service,
     * because this method sends lifecycle notifications. Sending a general notification from a synchronized block
     * is a bad idea and therefore not allowed.
     */
    public void start() {
        assert !Thread.holdsLock(this): "This method cannot be called while holding a synchronized lock on this";

        // Move to the starting state
        ServiceState originalState;
        synchronized (this) {
            originalState = getStateInstance();
            if (originalState == ServiceState.RUNNING) {
                return;
            }
            // only try to change states if we are not already starting
            if (originalState != ServiceState.STARTING) {
                setStateInstance(ServiceState.STARTING);
            }
        }

        // only fire a notification if we are not already starting
        if (originalState != ServiceState.STARTING) {
            lifecycleBroadcaster.fireStartingEvent();
        }

        attemptFullStart();
    }

    /**
     * Starts this service and then attempts to start all of its start dependent children.
     * <p/>
     * Note:  This method cannot be call while the current thread holds a synchronized lock on this service,
     * because this method sends lifecycle notifications.  Sending a general notification from a synchronized block
     * is a bad idea and therefore not allowed.
     */
    public void startRecursive() {
        assert !Thread.holdsLock(this): "This method cannot be called while holding a synchronized lock on this";

        ServiceState state = getStateInstance();
        if (state != ServiceState.STOPPED) {
            // Cannot startRecursive while in the stopping state
            // Dain: I don't think we can throw an exception here because there is no way for the caller
            // to lock the instance and check the state before calling
            return;
        }

        // get myself starting
        start();

        // startRecursive all of objects that depend on me
        Set dependents = dependencyManager.getChildren(objectName);
        for (Iterator iterator = dependents.iterator(); iterator.hasNext();) {
            ObjectName dependent = (ObjectName) iterator.next();
            try {
                if (kernel.isServiceEnabled(dependent)) {
                    kernel.startRecursiveService(dependent);
                }
            } catch (ServiceNotFoundException e) {
                // this is ok the service died before we could start it
                continue;
            } catch (Exception e) {
                // the is something wrong with this service... skip it
                continue;
            }
        }
    }

    /**
     * Moves this service to the STOPPING state, calls stop on all start dependent children, and then attempt
     * to move this service to the STOPPED state.
     * <p/>
     * Note:  This method can not be call while the current thread holds a syncronized lock on this service,
     * because this method sends lifecycle notifications.  Sending a general notification from a synchronized block
     * is a bad idea and therefore not allowed.
     */
    public void stop() {
        assert !Thread.holdsLock(this): "This method cannot be called while holding a synchronized lock on this";

        // move to the stopping state
        ServiceState originalState;
        synchronized (this) {
            originalState = getStateInstance();
            if (originalState == ServiceState.STOPPED) {
                return;
            }

            // only try to change states if we are not already stopping
            if (originalState != ServiceState.STOPPING) {
                setStateInstance(ServiceState.STOPPING);
            }
        }

        // only fire a notification if we are not already stopping
        if (originalState != ServiceState.STOPPING) {
            lifecycleBroadcaster.fireStoppingEvent();
        }

        // Don't try to stop dependents from within a synchronized block... this should reduce deadlocks

        // stop all of my dependent objects
        Set dependents = dependencyManager.getChildren(objectName);
        for (Iterator iterator = dependents.iterator(); iterator.hasNext();) {
            ObjectName child = (ObjectName) iterator.next();
            try {
                log.trace("Checking if child is running: child=" + child);
                if (kernel.getServiceState(child) == ServiceState.RUNNING_INDEX) {
                    log.trace("Stopping child: child=" + child);
                    kernel.stopService(child);
                    log.trace("Stopped child: child=" + child);
                }
            } catch (Exception ignore) {
                // not a big deal... did my best
            }
        }

        attemptFullStop();
    }

    /**
     * Attempts to bring the component into running state. If an Exception occurs while
     * starting the component, the component will be failed.
     * <p/>
     * <p/>
     * Note: Do not call this from within a synchronized block as it makes may send a lifecycle notification
     */
    void attemptFullStart() {
        assert !Thread.holdsLock(this): "This method cannot be called while holding a synchronized lock on this";

        synchronized (this) {
            // if we are still trying to start and can start now... start
            if (getStateInstance() != ServiceState.STARTING) {
                return;
            }

            if (blockerListener != null) {
                log.trace("Cannot run because service is still being blocked");
                return;
            }

            // check if an service is blocking us from starting
            final ObjectName blocker = dependencyManager.checkBlocker(objectName);
            if (blocker != null) {
                blockerListener = new LifecycleAdapter() {

                    public void stopped(ObjectName objectName) {
                        checkBlocker(objectName);
                    }

                    public void unloaded(ObjectName objectName) {
                        checkBlocker(objectName);
                    }

                    private void checkBlocker(ObjectName objectName) {
                        synchronized (ServiceInstanceState.this) {
                            if (!objectName.equals(blocker)) {
                                // it did not start so just exit this method
                                return;
                            }

                            // it started, so remove the blocker and attempt a full start
                            kernel.removeLifecycleListener(this);
                            ServiceInstanceState.this.blockerListener = null;
                        }

                        try {
                            attemptFullStart();
                        } catch (Exception e) {
                            log.warn("A problem occured while attempting to start", e);
                        }
                    }
                };
                // register the listener and return
                kernel.addLifecycleListener(blockerListener, blocker);
                return;
            }

            // check if all of the services we depend on are running
            Set parents = dependencyManager.getParents(objectName);
            for (Iterator i = parents.iterator(); i.hasNext();) {
                ObjectName parent = (ObjectName) i.next();
                if (!kernel.isLoaded(parent)) {
                    log.trace("Cannot run because parent is not registered: parent=" + parent);
                    return;
                }
                try {
                    log.trace("Checking if parent is running: parent=" + parent);
                    if (kernel.getServiceState(parent) != ServiceState.RUNNING_INDEX) {
                        log.trace("Cannot run because parent is not running: parent=" + parent);
                        return;
                    }
                    log.trace("Parent is running: parent=" + parent);
                } catch (ServiceNotFoundException e) {
                    // depended on instance was removed bewteen the register check and the invoke
                    log.trace("Cannot run because parent is not registered: parent=" + parent);
                    return;
                } catch (Exception e) {
                    // problem getting the attribute, parent has most likely failed
                    log.trace("Cannot run because an error occurred while checking if parent is running: parent=" + parent);
                    return;
                }
            }
        }

        try {
            // try to create the instance
            if (!serviceInstance.createInstance()) {
                // instance is not ready to start... this is normally caused by references
                // not being available, but could be because someone alreayd started the service.
                // in another thread.  The reference will log a debug message about why
                // it could not start
                return;
            }
        } catch (Throwable t) {
            // oops there was a problem, stop the service
            setStateInstance(ServiceState.STOPPING);
            lifecycleBroadcaster.fireStoppingEvent();
            setStateInstance(ServiceState.STOPPED);
            lifecycleBroadcaster.fireStoppedEvent();

            log.error("Error while starting; Service is now in the STOPPED state: objectName=\"" + objectName + "\"", t);
            return;
        }

        // started successfully... notify everyone else
        setStateInstance(ServiceState.RUNNING);
        lifecycleBroadcaster.fireRunningEvent();
    }

    /**
     * Attempt to bring the component into the fully stopped state.
     * If an exception occurs while stopping the component, the component will be failed.
     * <p/>
     * <p/>
     * Note: Do not call this from within a synchronized block as it may send a lifecycle notification
     */
    void attemptFullStop() {
        assert !Thread.holdsLock(this): "This method cannot be called while holding a synchronized lock on this";

        // check if we are able to stop
        synchronized (this) {
            // if we are still trying to stop...
            if (getStateInstance() != ServiceState.STOPPING) {
                return;
            }

            // check if all of the service depending on us are stopped
            Set children = dependencyManager.getChildren(objectName);
            for (Iterator i = children.iterator(); i.hasNext();) {
                ObjectName child = (ObjectName) i.next();
                if (kernel.isLoaded(child)) {
                    try {
                        log.trace("Checking if child is stopped: child=" + child);
                        int state = kernel.getServiceState(child);
                        if (state == ServiceState.RUNNING_INDEX) {
                            log.trace("Cannot stop because child is still running: child=" + child);
                            return;
                        }
                    } catch (ServiceNotFoundException e) {
                        // depended on instance was removed between the register check and the invoke
                    } catch (Exception e) {
                        // problem getting the attribute, depended on bean has most likely failed
                        log.trace("Cannot run because an error occurred while checking if child is stopped: child=" + child);
                        return;
                    }
                }
            }
        }

        // all is clear to stop... try to stop
        try {
            if (!serviceInstance.destroyInstance()) {
                // instance is not ready to stop... this is because another thread has
                // already stopped the service.
                return;
            }
        } catch (Throwable t) {
            log.error("Error while stopping; Service is now in the STOPPED state: objectName=\"" + objectName + "\"", t);
        } finally {
            // we are always stopped at this point
            setStateInstance(ServiceState.STOPPED);
            lifecycleBroadcaster.fireStoppedEvent();
        }
    }

    public int getState() {
        return state.getIndex();
    }

    public ServiceState getStateInstance() {
        return state;
    }

    /**
     * Set the Component state.
     *
     * @param newState the target state to transition
     * @throws IllegalStateException Thrown if the transition is not supported by the lifecycle contract.
     */
    private synchronized void setStateInstance(ServiceState newState) throws IllegalStateException {
        switch (state.getIndex()) {
            case ServiceState.STOPPED_INDEX:
                switch (newState.getIndex()) {
                    case ServiceState.STARTING_INDEX:
                        break;
                    case ServiceState.STOPPED_INDEX:
                    case ServiceState.RUNNING_INDEX:
                    case ServiceState.STOPPING_INDEX:
                        throw new IllegalStateException("Cannot transition to " + newState + " state from " + state);
                }
                break;

            case ServiceState.STARTING_INDEX:
                switch (newState.getIndex()) {
                    case ServiceState.RUNNING_INDEX:
                    case ServiceState.STOPPING_INDEX:
                        break;
                    case ServiceState.STOPPED_INDEX:
                    case ServiceState.STARTING_INDEX:
                        throw new IllegalStateException("Cannot transition to " + newState + " state from " + state);
                }
                break;

            case ServiceState.RUNNING_INDEX:
                switch (newState.getIndex()) {
                    case ServiceState.STOPPING_INDEX:
                        break;
                    case ServiceState.STOPPED_INDEX:
                    case ServiceState.STARTING_INDEX:
                    case ServiceState.RUNNING_INDEX:
                        throw new IllegalStateException("Cannot transition to " + newState + " state from " + state);
                }
                break;

            case ServiceState.STOPPING_INDEX:
                switch (newState.getIndex()) {
                    case ServiceState.STOPPED_INDEX:
                        break;
                    case ServiceState.STARTING_INDEX:
                    case ServiceState.RUNNING_INDEX:
                    case ServiceState.STOPPING_INDEX:
                        throw new IllegalStateException("Cannot transition to " + newState + " state from " + state);
                }
                break;

        }
        log.debug(toString() + " State changed from " + state + " to " + newState);
        state = newState;
    }

    public String toString() {
        return "ServiceInstanceState for: " + objectName;
    }

}
