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

import java.util.Iterator;
import java.util.Set;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gbean.kernel.Kernel;
import org.gbean.kernel.DependencyManager;
import org.gbean.kernel.LifecycleListener;
import org.gbean.kernel.LifecycleAdapter;

/**
 * @version $Rev: 71492 $ $Date: 2004-11-14 21:31:50 -0800 (Sun, 14 Nov 2004) $
 */
public class ServiceDependency {
    private static final Log log = LogFactory.getLog(ServiceDependency.class);

    /**
     * The ServiceInstance to which this reference belongs.
     */
    private final ServiceInstance serviceInstance;

    /**
     * The target objectName patterns to watch for a connection.
     */
    private final Set patterns;

    /**
     * Descriptive name of dependency
     */
    private final String name;

    /**
     * The kernel to which the reference is bound.
     */
    private final Kernel kernel;

    /**
     * The dependency manager of the kernel.
     */
    private final DependencyManager dependencyManager;

    /**
     * Is the ServiceInstance waitng for me to start?
     */
    private boolean waiting = false;

    /**
     * The object to which the proxy is bound
     */
    private ObjectName proxyTarget;

    /**
     * Our listener for lifecycle events
     */
    private LifecycleListener listener;

    /**
     * Why is the dependency in the current state
     */
    private String statusDescription;

    public ServiceDependency(ServiceInstance serviceInstance, String name, Set patterns, Kernel kernel, DependencyManager dependencyManager) {
        this.serviceInstance = serviceInstance;
        this.name = name;
        this.patterns = patterns;
        this.kernel = kernel;
        this.dependencyManager = dependencyManager;
        statusDescription = "NOT STARTED: " + getDescription();
    }

    public String getName() {
        return name;
    }

    public Set getPatterns() {
        return patterns;
    }

    public boolean isWaiting() {
        return waiting;
    }

    public String getStatusDescription() {
        return statusDescription;
    }

    public synchronized boolean start() {
        // We only need to start if there are patterns and we don't already have a proxy
        if (proxyTarget == null) {
            if (listener == null) {
                listener = new DependencyLifecycleListener();
                try {
                    kernel.addLifecycleListener(listener, patterns);
                } catch (Error e) {
                    throw e;
                } catch (RuntimeException e) {
                    throw e;
                }
            }

            //
            // We must have exactally one running target
            //
            ObjectName objectName = serviceInstance.getObjectName();
            Set services = ServiceInstanceUtil.getRunningTargets(kernel, patterns);
            if (services.size() == 0) {
                waiting = true;
                statusDescription = "WAITING: no targest " + getDescription() + ", patterns=" + getPatternsText();
                log.debug(statusDescription);
                return false;
            } else if (services.size() > 1) {
                waiting = true;
                statusDescription = "WAITING: to many targets " + getDescription() + ", patterns=" + getPatternsText();
                log.debug(statusDescription);
                return false;
            }

            //
            // ready to start
            //
            waiting = false;

            // stop all services that would match our patterns from starting
            dependencyManager.addStartHolds(objectName, patterns);

            // add a dependency on our target
            proxyTarget = (ObjectName) services.iterator().next();
            dependencyManager.addDependency(objectName, proxyTarget);

            statusDescription = "READY: " + getDescription();
        }

        return true;
    }

    public synchronized void stop() {
        waiting = false;
        ObjectName objectName = serviceInstance.getObjectName();
        dependencyManager.removeStartHolds(objectName, patterns);

        if (proxyTarget != null) {
            dependencyManager.removeDependency(objectName, proxyTarget);
            proxyTarget = null;
        }

        if (listener != null) {
            kernel.removeLifecycleListener(listener);
            listener = null;
        }

        statusDescription = "STOPPED: " + getDescription();
    }

    private synchronized void checkStatus() {
        Set services = ServiceInstanceUtil.getRunningTargets(kernel, patterns);

        // if we are running, and we now have two valid targets, which is an illegal state so we need to fail
        // todo fix me
        if (serviceInstance.getState() == ServiceState.RUNNING_INDEX && services.size() != 1) {
            serviceInstance.stop();
        } else if (waiting) {
            if (services.size() == 1) {
                // the service was waiting for me and not there is now just one target
                waiting = false;
                serviceInstance.start();
            }
        }
    }

    private String getPatternsText() {
        StringBuffer buf = new StringBuffer();
        for (Iterator iterator = patterns.iterator(); iterator.hasNext();) {
            ObjectName objectName = (ObjectName) iterator.next();
            buf.append(objectName.getCanonicalName()).append(" ");
        }
        return buf.toString();
    }

    private String getDescription() {
        return "depependency=" + name + ", serviceInstance " + serviceInstance.getObjectName().getCanonicalName();
    }

    private class DependencyLifecycleListener extends LifecycleAdapter {
        public void running(ObjectName objectName) {
            checkStatus();
        }

        public void stopped(ObjectName objectName) {
            checkStatus();
        }

        public void unloaded(ObjectName objectName) {
            checkStatus();
        }
    }
}
