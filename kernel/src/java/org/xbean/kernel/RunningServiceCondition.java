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
 * This condition that requires another service be in the RUNNING state to be satisfied.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class RunningServiceCondition implements ServiceCondition {
    private final ServiceName dependency;
    private final boolean ownedRelationship;
    private final boolean stopOnServiceShutdown;
    private final DependencyServiceMonitor serviceMonitor = new DependencyServiceMonitor();

    private ServiceConditionContext context;
    private boolean satisfied = true;
    private StoppedServiceCondition stoppedServiceCondition;

    /**
     * Creates a condition that requires the specified service be in the RUNNING state to be satisfied.
     *
     * @param dependency the service that must be running
     * @param ownedRelationship if true the condition will register the relationship
     * @param stopOnServiceShutdown if the our service should be stopped when the specified service shutsdown
     */
    public RunningServiceCondition(ServiceName dependency, boolean ownedRelationship, boolean stopOnServiceShutdown) {
        if (dependency == null) throw new NullPointerException("dependency is null");
        this.dependency = dependency;
        this.ownedRelationship = ownedRelationship;
        this.stopOnServiceShutdown = stopOnServiceShutdown;
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
        if (ownedRelationship) {
            // todo register owned relationship
        }

        if (stopOnServiceShutdown) {
            stoppedServiceCondition = new StoppedServiceCondition(context.getServiceName());
        }
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
                // grab a synchronized lock on the service factory to assure that the state doesn't change while
                // adding the dependency.... the kernel will grab the same lock when getting the stop dependencies
                ServiceFactory serviceFactory = context.getKernel().getServiceFactory(dependency);
                synchronized (serviceFactory) {
                    if (context.getKernel().getService(dependency) == ServiceState.RUNNING) {
                        if (stopOnServiceShutdown) {
                            serviceFactory.addStopCondition(stoppedServiceCondition);
                        }
                        satisfied = true;
                        context.getKernel().removeServiceMonitor(serviceMonitor);
                    }
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
        if (ownedRelationship) {
            // todo unregister owned relationship
        }
        if (stopOnServiceShutdown) {
            stoppedServiceCondition.destroy();
            stoppedServiceCondition = null;
        }
    }

    private class DependencyServiceMonitor extends NullServiceMonitor {
        public void serviceRunning(ServiceEvent serviceEvent) {
            synchronized (RunningServiceCondition.this) {
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
