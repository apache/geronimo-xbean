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

import java.util.Collections;
import java.util.Set;

/**
 * Signafies that a StopStrategies would like the kernel to ignore any unsatified stop conditions and continue to
 * destroy the service.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class ForcedStopException extends Exception {
    private final ServiceName serviceName;
    private final Set unsatisfiedConditions;

    /**
     * Creates a ForcedStopException for the specified service name.
     *
     * @param serviceName the name of the service that is to be forceably stopped
     * @param unsatisfiedConditions the unsatisfied conditions that will be ignored
     */
    public ForcedStopException(ServiceName serviceName, Set unsatisfiedConditions) {
        super("Forced stop and ignored unsatisfied conditons:" +
                " serviceName=" + serviceName +
                ", unsatisfiedConditions=" + unsatisfiedConditions);
        if (serviceName == null) throw new NullPointerException("serviceName is null");
        if (unsatisfiedConditions == null) throw new NullPointerException("unsatisfiedConditions is null");
        this.serviceName = serviceName;
        this.unsatisfiedConditions = Collections.unmodifiableSet(unsatisfiedConditions);
    }

    /**
     * Gets the name of the service that is to be forceably stopped.
     *
     * @return the service name
     */
    public ServiceName getServiceName() {
        return serviceName;
    }

    /**
     * Gets the conditions that were unsatified when the exception was thrown.
     *
     * @return the unsatified conditions that were ignored
     */
    public Set getUnsatisfiedConditions() {
        return unsatisfiedConditions;
    }
}
