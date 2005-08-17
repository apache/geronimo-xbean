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
 * This class contains the built-in common start startegies.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public final class StartStrategies {
    private StartStrategies() {
    }

    /**
     * This strategy attempts to immedately start the service.  When there are unsatisfied conditions, this strategy
     * will leave the service in the STARTING state, and throw an UnsatisfiedConditionsException
     * to the caller.  When there is a start error, the service will be destroyed and the exception will be rethrown to
     * the caller.
     */
    public static final StartStrategy SYNCHRONOUS = new Synchronous();

    private static class Synchronous implements StartStrategy {
        public boolean waitForUnsatisfiedConditions(ServiceName serviceName, Set conditions) throws UnsatisfiedConditionsException {
            throw new UnsatisfiedConditionsException("Unsatisfied start conditions", serviceName, conditions);
        }

        public void startError(ServiceName serviceName, Throwable startError) throws Exception {
            if (startError instanceof Exception) {
                throw (Exception) startError;
            } else if (startError instanceof Error) {
                throw (Error) startError;
            } else {
                throw new AssertionError(startError);
            }
        }
    }

    /**
     * This strategy attempts to start the service asynchronously.  When there are unsatisfied conditions, this strategy
     * will leave the service in the STARTING state, and caller will not recieve any exceptions.
     * When there is a start error the service will be destroyed adn the exception will be sent to the service montior.
     * The caller will not recieve any start exception.
     */
    public static final StartStrategy ASYNCHRONOUS = new Asynchronous();

    private static class Asynchronous implements StartStrategy {
        public boolean waitForUnsatisfiedConditions(ServiceName serviceName, Set conditions) {
            return false;
        }

        public void startError(ServiceName serviceName, Throwable startError) {
        }
    }

    /**
     * This strategy wait until the service start.  This strategy blocks until all unsatisfied conditons
     * are satisfied.  When there is a start error, the service will be destroyed and the exception will be rethrown to
     * the caller.
     */
    public static final StartStrategy BLOCK = new Block();

    private static class Block implements StartStrategy {
        public boolean waitForUnsatisfiedConditions(ServiceName serviceName, Set conditions) {
            return true;
        }

        public void startError(ServiceName serviceName, Throwable startError) throws Exception {
            if (startError instanceof Exception) {
                throw (Exception) startError;
            } else if (startError instanceof Error) {
                throw (Error) startError;
            } else {
                throw new AssertionError(startError);
            }
        }
    }

    /**
     * This strategy attempts to start the service immedately.  When there are unsatisfied conditions or a start error
     * the dervice will be destroyed and unregistered.  In this case an UnsatisfiedConditionsException or
     * the start error will be thrown to the caller.
     */
    public static final StartStrategy UNREGISTER = new Unregister();

    private static class Unregister implements StartStrategy {
        public boolean waitForUnsatisfiedConditions(ServiceName serviceName, Set conditions) throws UnregisterServiceException {
            UnsatisfiedConditionsException userException = new UnsatisfiedConditionsException("Unsatisfied start conditions", serviceName, conditions);
            throw new UnregisterServiceException(serviceName, userException);
        }

        public void startError(ServiceName serviceName, Throwable startError) throws UnregisterServiceException {
            throw new UnregisterServiceException(serviceName, startError);
        }
    }
}
