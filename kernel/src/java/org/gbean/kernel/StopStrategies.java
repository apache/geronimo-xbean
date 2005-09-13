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
package org.gbean.kernel;

import java.util.Set;

/**
 * This class contains the built-in common stop startegies.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public final class StopStrategies {
    private StopStrategies() {
    }

    /**
     * This strategy attempts to immedately stop the service.  When there are unsatisfied conditions, this strategy
     * will leave the service in the STOPPING state, and throw an UnsatisfiedConditionsException
     * to the caller.
     */
    public static final StopStrategy SYNCHRONOUS = new Synchronous();

    private static class Synchronous implements StopStrategy {
        public boolean waitForUnsatisfiedConditions(ServiceName serviceName, Set conditions) throws UnsatisfiedConditionsException {
            throw new UnsatisfiedConditionsException("Unsatisfied stop conditions", serviceName, conditions);
        }

    }

    /**
     * This strategy attempts to stop the service asynchronously.  When there are unsatisfied conditions, this strategy
     * will leave the service in the STOPPING state, and caller will not recieve any exceptions.
     */
    public static final StopStrategy ASYNCHRONOUS = new Asynchronous();

    private static class Asynchronous implements StopStrategy {
        public boolean waitForUnsatisfiedConditions(ServiceName serviceName, Set conditions) {
            return false;
        }

    }

    /**
     * This strategy wait until the service stops.  This strategy blocks until all unsatisfied conditons
     * are satisfied.
     */
    public static final StopStrategy BLOCK = new Block();

    private static class Block implements StopStrategy {
        public boolean waitForUnsatisfiedConditions(ServiceName serviceName, Set conditions) {
            return true;
        }

    }

    /**
     * This strategy forceable stops the service.  This strategy ignores all unsatisfied conditons.
     */
    public static final StopStrategy FORCE = new Force();

    private static class Force implements StopStrategy {
        public boolean waitForUnsatisfiedConditions(ServiceName serviceName, Set conditions) throws ForcedStopException {
            throw new ForcedStopException(serviceName, conditions);
        }

    }
}
