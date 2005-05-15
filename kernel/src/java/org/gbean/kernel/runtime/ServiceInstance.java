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

package org.gbean.kernel.runtime;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gbean.kernel.DependencyManager;
import org.gbean.kernel.Kernel;
import org.gbean.kernel.ServiceNotFoundException;
import org.gbean.service.ServiceContext;
import org.gbean.service.ServiceFactory;

/**
 * @version $Rev: 106387 $ $Date: 2004-11-23 22:16:54 -0800 (Tue, 23 Nov 2004) $
 */
public final class ServiceInstance {
    private static final Log log = LogFactory.getLog(ServiceInstance.class);

    private static final int DESTROYED = 0;
    private static final int CREATING = 1;
    private static final int RUNNING = 2;
    private static final int DESTROYING = 3;

    /**
     * The kernel in which this server is registered.
     */
    private final Kernel kernel;

    /**
     * The factory used to create the actual object instance.
     */
    private ServiceFactory serviceFactory;

    /**
     * This handles all state transiitions for this instance.
     */
    private final ServiceInstanceState serviceInstanceState;

    /**
     * The single listener to which we broadcast lifecycle change events.
     */
    private final LifecycleBroadcaster lifecycleBroadcaster;

    /**
     * The context given to the instance
     */
    private final ServiceContext serviceContext;

    /**
     * The classloader used for all invocations and creating targets.
     */
    private final ClassLoader classLoader;

    /**
     * Has this instance been destroyed?
     */
    private boolean dead = false;

    /**
     * The state of the internal service instance that we are wrapping.
     */
    private int instanceState = DESTROYED;

    /**
     * Target instance of this service instance
     */
    private Object target;

    /**
     * The time this application started.
     */
    private long startTime;

    private Set dependencies;
    private final ObjectName objectName;

    public ServiceInstance(ObjectName objectName,
            ServiceFactory serviceFactory,
            Kernel kernel,
            DependencyManager dependencyManager,
            LifecycleBroadcaster lifecycleBroadcaster,
            ClassLoader classLoader) {

        this.objectName = objectName;
        this.serviceFactory = serviceFactory;

        // add the dependencies
        Set tempDependencies = new HashSet();
        for (Iterator iterator = serviceFactory.getDependencies().entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String dependencyName = (String) entry.getKey();
            Set patterns = (Set) entry.getValue();
            tempDependencies.add(new ServiceDependency(this, dependencyName, patterns, kernel, dependencyManager));
        }
        this.dependencies = Collections.unmodifiableSet(tempDependencies);

        this.kernel = kernel;
        this.lifecycleBroadcaster = lifecycleBroadcaster;
        this.serviceInstanceState = new ServiceInstanceState(objectName, kernel, dependencyManager, this, lifecycleBroadcaster);
        this.classLoader = classLoader;

        serviceContext = new ServiceInstanceContext(this);
    }

    public void init() {
        lifecycleBroadcaster.fireLoadedEvent();
    }

    public void destroy() throws ServiceNotFoundException {
        synchronized (this) {
            if (dead) {
                // someone beat us to the punch... this instance should have never been found in the first place
                throw new ServiceNotFoundException(objectName.getCanonicalName());
            }
            dead = true;
        }

        // if the bean is already stopped or failed, this will do nothing; otherwise it will shutdown the bean
        serviceInstanceState.stop();

        // tell everyone we are done
        lifecycleBroadcaster.fireUnloadedEvent();
    }

    /**
     * The kernel in which this instance is mounted
     * @return the kernel
     */
    public Kernel getKernel() {
        return kernel;
    }

    /**
     * The class loader used to build this service.  This class loader is set into the thread context
     * class loader before callint the target instace.
     *
     * @return the class loader used to build this service
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public synchronized Object getInstance() {
        return target;
    }

    /**
     * Has this service instance been destroyed. An destroyed service can no longer be used.
     *
     * @return true if the service has been destroyed
     */
    public synchronized boolean isDead() {
        return dead;
    }

    public final ObjectName getObjectName() {
        return objectName;
    }

    public ServiceFactory getServiceFactory() {
        return serviceFactory;
    }

    /**
     * Is this service enabled.  A disabled service can not be started.
     *
     * @return true if the service is enabled and can be started
     */
    public synchronized final boolean isEnabled() {
        return serviceFactory.isEnabled();
    }

    /**
     * Changes the enabled status.
     *
     * @param enabled the new enabled flag
     */
    public synchronized final void setEnabled(boolean enabled) {
        serviceFactory.setEnabled(enabled);
    }

    public synchronized final long getStartTime() {
        return startTime;
    }

    public int getState() {
        return serviceInstanceState.getState();
    }


    /**
     * Moves this service to the starting state and then attempts to move this service immediately
     * to the running state.
     *
     * @throws IllegalStateException If the service is disabled
     */
    public final void start() {
        synchronized (this) {
            if (dead) {
                throw new IllegalStateException("A dead service can not be started: objectName=" + objectName);
            }
            if (!isEnabled()) {
                throw new IllegalStateException("A disabled service can not be started: objectName=" + objectName);
            }
        }
        serviceInstanceState.start();
    }

    /**
     * Starts this ServiceInstance and then attempts to start all of its start dependent children.
     *
     * @throws IllegalStateException If the service is disabled
     */
    public final void startRecursive() {
        synchronized (this) {
            if (dead) {
                throw new IllegalStateException("A dead service can not be started: objectName=" + objectName);
            }
            if (!isEnabled()) {
                throw new IllegalStateException("A disabled service can not be started: objectName=" + objectName);
            }
        }
        serviceInstanceState.startRecursive();
    }

    /**
     * Moves this service to the STOPPING state, calls stop on all start dependent children, and then attempt
     * to move this service to the STOPPED state.
     */
    public final void stop() {
        serviceInstanceState.stop();
    }

    boolean createInstance() throws Exception {
        synchronized (this) {
            // first check we are still in the correct state to start
            if (instanceState == CREATING || instanceState == RUNNING) {
                // another thread already completed starting
                return false;
            } else if (instanceState == DESTROYING) {
                // this should never ever happen... this method is protected by the ServiceInstanceState class which should
                // prevent stuff like this happening, but check anyway
                throw new IllegalStateException("A stopping instance can not be started until fully stopped");
            }
            assert instanceState == DESTROYED;

            // Call all start on every reference.  This way the dependecies are held until we can start
            boolean allStarted = true;
            for (Iterator iterator = dependencies.iterator(); iterator.hasNext();) {
                ServiceDependency dependency = (ServiceDependency) iterator.next();
                allStarted = dependency.start() && allStarted;
            }
            if (!allStarted) {
                return false;
            }

            // we are definately going to (try to) start... if this fails the must clean up these variables
            instanceState = CREATING;
            startTime = System.currentTimeMillis();
        }

        Object instance = null;
        try {
            instance = serviceFactory.createService(serviceContext);

            // all done... we are now fully running
            synchronized (this) {
                target = instance;
                instanceState = RUNNING;
                this.notifyAll();
            }

            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            // something went wrong... we need to destroy this instance
            synchronized (this) {
                instanceState = DESTROYING;
            }

            serviceFactory.destroyService(serviceContext, instance);

            // bean has been notified... drop our reference
            synchronized (this) {
                for (Iterator iterator = dependencies.iterator(); iterator.hasNext();) {
                    ServiceDependency dependency = (ServiceDependency) iterator.next();
                    dependency.stop();
                }
                target = null;
                instanceState = DESTROYED;
                startTime = 0;
                this.notifyAll();
            }

            if (t instanceof Exception) {
                throw (Exception) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            } else {
                throw new Error(t);
            }
        }
    }

    boolean destroyInstance() throws Exception {
        Object instance;
        synchronized (this) {
            // if the instance is being created we need to wait
            //  for it to finish before we can try to stop it
            while (instanceState == CREATING) {
                // todo should we limit this wait?  If so, how do we configure the wait time?
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    // clear the interrupted flag
                    Thread.interrupted();
                    // rethrow the interrupted exception.... someone was sick of us waiting
                    throw e;
                }
            }

            if (instanceState == DESTROYING || instanceState == DESTROYED) {
                // another thread is already stopping or has already stopped
                return false;
            }
            assert instanceState == RUNNING;

            // we are definately going to stop... if this fails we must clean up these variables
            instanceState = DESTROYING;
            instance = target;
        }

        try {
            // we notify the bean before removing our dependencies so the dependencies can be called back while stopping
            serviceFactory.destroyService(serviceContext, instance);
        } finally {
            // bean has been notified... drop the dependencies
            synchronized (this) {
                for (Iterator iterator = dependencies.iterator(); iterator.hasNext();) {
                    ServiceDependency dependency = (ServiceDependency) iterator.next();
                    dependency.stop();
                }
                target = null;
                instanceState = DESTROYED;
                startTime = 0;
            }
        }
        return true;
    }

    private static final class ServiceInstanceContext implements ServiceContext {
        /**
         * The ServiceInstance which owns the target.
         */
        private final ServiceInstance serviceInstance;

        /**
         * Creates a new context for a target.
         *
         * @param serviceInstance the ServiceInstance
         */
        public ServiceInstanceContext(ServiceInstance serviceInstance) {
            this.serviceInstance = serviceInstance;
        }

        public Kernel getKernel() {
            return serviceInstance.getKernel();
        }

        public String getObjectName() {
            return serviceInstance.getObjectName().getCanonicalName();
        }

        public ClassLoader getClassLoader() {
            return serviceInstance.getClassLoader();
        }

        public int getState() {
            return serviceInstance.getState();
        }

        public void stop() throws Exception {
            synchronized (serviceInstance) {
                if (serviceInstance.instanceState == CREATING) {
                    throw new IllegalStateException("Stop can not be called until instance is fully started");
                } else if (serviceInstance.instanceState == DESTROYING) {
                    log.debug("Stop ignored.  Service is already being stopped");
                    return;
                } else if (serviceInstance.instanceState == DESTROYED) {
                    log.debug("Stop ignored.  Service is already stopped");
                    return;
                }
            }
            serviceInstance.stop();
        }
    }

    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof ServiceInstance == false) return false;
        return objectName.equals(((ServiceInstance) obj).objectName);
    }

    public int hashCode() {
        return objectName.hashCode();
    }

    public String toString() {
        return objectName.getCanonicalName();
    }
}
