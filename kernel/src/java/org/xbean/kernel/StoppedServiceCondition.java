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

/**
 * This condition that requires another service be in the STOPPED state to be satisfied.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class StoppedServiceCondition implements ServiceCondition {
    private final ServiceName dependency;
    private final DependencyServiceMonitor serviceMonitor = new DependencyServiceMonitor();

    private ServiceConditionContext context;
    private boolean satisfied = true;

    /**
     * Creates a condition that requires the specified service be in the STOPPED state to be satisfied.
     *
     * @param dependency the service that must be stopped
     */
    public StoppedServiceCondition(ServiceName dependency) {
        if (dependency == null) throw new NullPointerException("dependency is null");
        this.dependency = dependency;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void initialize(ServiceConditionContext context) {
        if (context == null) throw new NullPointerException("context is null");

        // if we have no been destroyed, destroy not
        if (this.context != null) {
            destroy();
        }

        this.context = context;

        satisfied = false;
        context.getKernel().addServiceMonitor(serviceMonitor, dependency);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized boolean isSatisfied() {
        if (context == null) {
            // we are not initialized so default to true
            return true;
        }

        if (!satisfied) {
            try {
                if (context.getKernel().getService(dependency) == ServiceState.RUNNING) {
                    satisfied = true;
                    context.getKernel().removeServiceMonitor(serviceMonitor);
                }
            } catch (ServiceNotFoundException ignored) {
                // service hasn't registered yet
            }
        }
        return satisfied;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void destroy() {
        if (context == null) {
            // we are already destroyed
            return;
        }

        context.getKernel().removeServiceMonitor(serviceMonitor);
        context = null;
        satisfied = true;
    }

    private class DependencyServiceMonitor extends NullServiceMonitor {
        public void serviceStopped(ServiceEvent serviceEvent) {
            synchronized (StoppedServiceCondition.this) {
                if (context != null) {
                    // we aren't running anymore
                    return;
                }

                if (!satisfied) {
                    return;
                }

                if (isSatisfied()) {
                    context.setSatisfied();
                }
            }
        }
    }
}
