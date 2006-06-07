/**
 *
 * Copyright 2005-2006 The Apache Software Foundation or its licensors, as applicable.
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
package org.apache.xbean.kernel.standard;

import edu.emory.mathcs.backport.java.util.concurrent.Executor;
import org.apache.xbean.kernel.ServiceEvent;
import org.apache.xbean.kernel.ServiceMonitor;

/**
 * The AsyncServiceMonitor delivers service events to a delegate ServiceMonitor asynchronously using an executor.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class AsyncServiceMonitor implements ServiceMonitor {
    private final ServiceMonitor delegate;
    private final Executor executor;

    /**
     * Creates a AsyncServiceMonitor which asynchronously delivers service events to specified delegate
     * ServiceMonitor using the specified executor.
     *
     * @param delegate the service monitor that should recieve the asynchronous events
     * @param executor the executor used to asynchronously deliver the events
     */
    public AsyncServiceMonitor(ServiceMonitor delegate, Executor executor) {
        this.delegate = delegate;
        this.executor = executor;
    }

    /**
     * {@inheritDoc}
     */
    public void serviceRegistered(final ServiceEvent serviceEvent) {
        executor.execute(new Runnable() {
            public void run() {
                delegate.serviceRegistered(serviceEvent);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public void serviceStarting(final ServiceEvent serviceEvent) {
        executor.execute(new Runnable() {
            public void run() {
                delegate.serviceStarting(serviceEvent);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public void serviceWaitingToStart(final ServiceEvent serviceEvent) {
        executor.execute(new Runnable() {
            public void run() {
                delegate.serviceWaitingToStart(serviceEvent);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public void serviceStartError(final ServiceEvent serviceEvent) {
        executor.execute(new Runnable() {
            public void run() {
                delegate.serviceStartError(serviceEvent);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public void serviceRunning(final ServiceEvent serviceEvent) {
        executor.execute(new Runnable() {
            public void run() {
                delegate.serviceRunning(serviceEvent);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public void serviceStopping(final ServiceEvent serviceEvent) {
        executor.execute(new Runnable() {
            public void run() {
                delegate.serviceStopping(serviceEvent);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public void serviceWaitingToStop(final ServiceEvent serviceEvent) {
        executor.execute(new Runnable() {
            public void run() {
                delegate.serviceWaitingToStop(serviceEvent);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public void serviceStopError(final ServiceEvent serviceEvent) {
        executor.execute(new Runnable() {
            public void run() {
                delegate.serviceStopError(serviceEvent);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public void serviceStopped(final ServiceEvent serviceEvent) {
        executor.execute(new Runnable() {
            public void run() {
                delegate.serviceStopped(serviceEvent);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public void serviceUnregistered(final ServiceEvent serviceEvent) {
        executor.execute(new Runnable() {
            public void run() {
                delegate.serviceUnregistered(serviceEvent);
            }
        });
    }
}
