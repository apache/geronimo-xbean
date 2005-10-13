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

import java.io.Serializable;

/**
 * The state of services within the Kernel.  The state model is directly adapted from the J2EE Management Specification
 * (JSR 77) with the removal of the FAILED state.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public final class ServiceState implements Serializable {
    private static final long serialVersionUID = -2629672602273580572L;

    /**
     * This state indicates that the service is attempting to start but has not fully started yet.  Normally, a service
     * in this state is waiting for a required service to enter the RUNNING state.
     */
    public static final ServiceState STARTING = new ServiceState((byte) 0, "STARTING");

    /**
     * This state indicates that the service is in the normal operational state.
     */
    public static final ServiceState RUNNING = new ServiceState((byte) 1, "RUNNING");

    /**
     * This state indicates that the service is attempting to stop but has not fully stopped yet.  Normally, a service
     * in this state because another service is still usind this service.
     */
    public static final ServiceState STOPPING = new ServiceState((byte) 2, "STOPPING");

    /**
     * This state indicates that the service is stopped and not operational.
     */
    public static final ServiceState STOPPED = new ServiceState((byte) 3, "STOPPED");

    /**
     * A quick index for looking up service states.
     */
    private static final ServiceState[] serviceStateIndex = new ServiceState[]{STARTING, RUNNING, STOPPING, STOPPED};

    static {
        for (int i = 0; i < serviceStateIndex.length; i++) {
            ServiceState serviceState = serviceStateIndex[i];
            if (serviceState.getIndex() != i) {
                throw new AssertionError(serviceState + " state index is " + serviceState.getIndex() +
                    ", but is located at index " + i + " in the serviceStateIndex");
            }
        }
    }

    /**
     * Converts the state index into corresponding state name.
     *
     * @param state the state index
     * @return the name of the state
     * @throws IllegalArgumentException if the state index is not 0, 1, 2 or 3
     */
    public static ServiceState getServiceState(int state) throws IllegalArgumentException {
        if (state < 0 || state >= serviceStateIndex.length) {
            throw new IllegalArgumentException("Unknown state " + state);
        }
        return serviceStateIndex[state];
    }

    /**
     * Converts the state name in the corresponding state index.  This method performs a case insensitive comparison.
     *
     * @param state the state name
     * @return the state index
     * @throws IllegalArgumentException if the state index is not STARTING, RUNNING, STOPPING or FAILED
     */
    public static ServiceState parseServiceState(String state) {
        if (state == null) throw new NullPointerException("state is null");
        if (STARTING.toString().equalsIgnoreCase(state)) {
            return STARTING;
        } else if (RUNNING.toString().equalsIgnoreCase(state)) {
            return RUNNING;
        } else if (STOPPING.toString().equalsIgnoreCase(state)) {
            return STOPPING;
        } else if (STOPPED.toString().equalsIgnoreCase(state)) {
            return STOPPED;
        } else {
            throw new IllegalArgumentException("Unknown state " + state);
        }
    }

    private final byte index;
    private final transient String name;

    private ServiceState(byte index, String name) {
        this.index = index;
        this.name = name;
    }

    /**
     * Gets the unique index of this state.  This index can be be used to retrieve this state instance using the
     * getServiceState(int) method.
     *
     * @return the unique index of this state
     */
    public int getIndex() {
        return index;
    }

    /**
     * The unique name of this state.  This uppercase name can be used to retrieve this state instance using the
     * parseServiceState(String).
     *
     * @return the unique name of this state
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the name of this state.
     *
     * @return the unique name of this state
     */
    public String toString() {
        return name;
    }

    private Object readResolve() {
        return serviceStateIndex[index];
    }
}
