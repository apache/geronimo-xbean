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
package org.gbean.kernel.standard;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import edu.emory.mathcs.backport.java.util.concurrent.locks.Lock;
import org.gbean.kernel.Kernel;
import org.gbean.kernel.ServiceCondition;
import org.gbean.kernel.ServiceFactory;
import org.gbean.kernel.ServiceName;

/**
 * A special sub-class of AggregateCondition used to manage the stop conditions of a non-restartable service.  This class
 * will update stop conditions to reflect the stop conditions currently registered with the service factory, when the
 * initialized or getUnsatisfied methods are called.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class NonRestartableStopCondition extends AggregateCondition {
    private final ServiceFactory serviceFactory;

    /**
     * Creates a NonRestartableStopCondition.
     *
     * @param kernel the kernel in which the service is registered
     * @param serviceName the name of the service
     * @param classLoader the class loader for the service
     * @param lock the lock for the service manager
     * @param serviceFactory the service factory for the service
     */
    public NonRestartableStopCondition(Kernel kernel, ServiceName serviceName, ClassLoader classLoader, Lock lock, ServiceFactory serviceFactory) {
        super(kernel, serviceName, classLoader, lock, Collections.EMPTY_SET);
        this.serviceFactory = serviceFactory;
    }

    /**
     * Throws UnsupportedOperationException.  Initialize is not a valid operation for a NonRestartableStopCondition
     *
     * @throws UnsupportedOperationException always
     */
    public synchronized void initialize() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("initialize should never be called on a NonRestartableStopCondition");
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Set getUnsatisfied() {
        updateConditions();
        return super.getUnsatisfied();
    }

    private void updateConditions() {
        if (isDestroyed()) throw new IllegalStateException("destroyed");

        Set conditions = getConditions();

        // add the new conditions
        Set stopConditions = serviceFactory.getStopConditions();
        for (Iterator iterator = stopConditions.iterator(); iterator.hasNext();) {
            ServiceCondition condition = (ServiceCondition) iterator.next();
            if (!conditions.contains(condition)) {
                addCondition(condition);
            }
        }

        // remove the conditions that were dropped
        for (Iterator iterator = conditions.iterator(); iterator.hasNext();) {
            ServiceCondition serviceCondition = (ServiceCondition) iterator.next();
            if (!stopConditions.contains(serviceCondition)) {
                removeCondition(serviceCondition);
            }
        }
    }

}
