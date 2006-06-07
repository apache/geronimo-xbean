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
package org.apache.xbean.kernel;

/**
 * This interface is used to monitor service lifecycle events.  A ServiceMonitor can be registered with a kernel using
 * {@link Kernel#addServiceMonitor(ServiceMonitor)} or {@link Kernel#addServiceMonitor(ServiceMonitor, ServiceName)}.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public interface ServiceMonitor {
    /**
     * A new service has been registered with the kernel.
     *
     * @param serviceEvent the event information
     */
    void serviceRegistered(ServiceEvent serviceEvent);

    /**
     * A service has entered the STARTING state.
     *
     * @param serviceEvent the event information
     */
    void serviceStarting(ServiceEvent serviceEvent);

    /**
     * A service is waiting to start because some start conditions are unsatified.
     *
     * @param serviceEvent the event information
     * @see ServiceEvent#getUnsatisfiedConditions()
     */
    void serviceWaitingToStart(ServiceEvent serviceEvent);

    /**
     * An error occured while calling creating the service.
     *
     * @param serviceEvent the event information
     * @see ServiceEvent#getCause()
     */
    void serviceStartError(ServiceEvent serviceEvent);

    /**
     * A service has entered the RUNNING state.
     *
     * @param serviceEvent the event information
     */
    void serviceRunning(ServiceEvent serviceEvent);

    /**
     * A service has entered the RUNNING state.
     *
     * @param serviceEvent the event information
     */
    void serviceStopping(ServiceEvent serviceEvent);

    /**
     * A service is waiting to stop because some stop condition is unsatified.
     *
     * @param serviceEvent the event information
     * @see ServiceEvent#getUnsatisfiedConditions()
     */
    void serviceWaitingToStop(ServiceEvent serviceEvent);

    /**
     * An error occured while calling destroying the service.
     *
     * @param serviceEvent the event information
     * @see ServiceEvent#getCause()
     */
    void serviceStopError(ServiceEvent serviceEvent);

    /**
     * A service has entered the STOPPED state.
     *
     * @param serviceEvent the event information
     */
    void serviceStopped(ServiceEvent serviceEvent);

    /**
     * A service has been unregistered from the kernel.
     *
     * @param serviceEvent the event information
     */
    void serviceUnregistered(ServiceEvent serviceEvent);
}
