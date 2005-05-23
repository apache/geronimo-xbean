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

import java.io.Serializable;

/**
 * @version $Rev: 54804 $ $Date: 2004-10-14 14:09:29 -0700 (Thu, 14 Oct 2004) $
 */
public final class ServiceState implements Serializable {
    public static final int STARTING_INDEX = 0;
    public static final int RUNNING_INDEX = 1;
    public static final int STOPPING_INDEX = 2;
    public static final int STOPPED_INDEX = 3;

    public static final ServiceState STARTING = new ServiceState("starting", STARTING_INDEX);
    public static final ServiceState RUNNING = new ServiceState("running", RUNNING_INDEX);
    public static final ServiceState STOPPING = new ServiceState("stopping", STOPPING_INDEX);
    public static final ServiceState STOPPED = new ServiceState("stopped", STOPPED_INDEX);

    private static final ServiceState[] fromInt = {STARTING, RUNNING, STOPPING, STOPPED};

    /**
     * Get a State from an int index
     *
     * @param index int index of the state
     * @return The State instance or null if no such State.
     */
    public static ServiceState fromIndex(int index) {
        if (index < 0 || index >= fromInt.length) {
            return null;
        }
        return fromInt[index];
    }

    public static String toString(int state) {
        if (state < 0 || state >= fromInt.length) {
            throw new IllegalArgumentException("State must be between 0 and " + fromInt.length);
        }
        return fromInt[state].name;
    }

    /**
     * The user readable name of this state.
     */
    private final String name;

    /**
     * The index of this state.
     */
    private final int index;

    private ServiceState(String name, int index) {
        this.name = name;
        this.index = index;
    }

    /**
     * Gets the integer index value of this state
     */
    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return name;
    }

    private Object readResolve() {
        return fromInt[index];
    }
}
