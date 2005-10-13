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
 * The NullServiceMonitor is a simple implementation of ServiceMonitor containing a noop implementaion of
 * each callback.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class NullServiceMonitor implements ServiceMonitor {
    /**
     * {@inheritDoc}
     */
    public void serviceRegistered(ServiceEvent serviceEvent) {
    }

    /**
     * {@inheritDoc}
     */
    public void serviceStarting(ServiceEvent serviceEvent) {
    }

    /**
     * {@inheritDoc}
     */
    public void serviceWaitingToStart(ServiceEvent serviceEvent) {
    }

    /**
     * {@inheritDoc}
     */
    public void serviceStartError(ServiceEvent serviceEvent) {
    }

    /**
     * {@inheritDoc}
     */
    public void serviceRunning(ServiceEvent serviceEvent) {
    }

    /**
     * {@inheritDoc}
     */
    public void serviceStopping(ServiceEvent serviceEvent) {
    }

    /**
     * {@inheritDoc}
     */
    public void serviceWaitingToStop(ServiceEvent serviceEvent) {
    }

    /**
     * {@inheritDoc}
     */
    public void serviceStopError(ServiceEvent serviceEvent) {
    }

    /**
     * {@inheritDoc}
     */
    public void serviceStopped(ServiceEvent serviceEvent) {
    }

    /**
     * {@inheritDoc}
     */
    public void serviceUnregistered(ServiceEvent serviceEvent) {
    }
}
