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

import java.util.Set;

/**
 * The StopStrategy interface is used to assist the kernel in determining how to handle problems that occur while
 * stoping a service.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public interface StopStrategy {
    /**
     * Determines if the kernel should wait for the unsatified conditions to be satisfied.
     *
     * @param serviceName the name of the service that has the unsatisfied condtions
     * @param conditions the unsatisfied condtions
     * @return true if the kernel should wait for the conditions to be satisfied; false if the strategy would like
     *         silently leave the service in the stopping state
     * @throws UnsatisfiedConditionsException the the strategy would like to leave the service in the stopping state
     * and throw an exception the caller
     * @throws ForcedStopException if the strategy would like to ignore the unsatisfied conditions and continue to
     * destroy the service
     */
    boolean waitForUnsatisfiedConditions(ServiceName serviceName, Set conditions) throws UnsatisfiedConditionsException, ForcedStopException;
}
