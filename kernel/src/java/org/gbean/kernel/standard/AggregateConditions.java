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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.emory.mathcs.backport.java.util.concurrent.locks.Condition;
import edu.emory.mathcs.backport.java.util.concurrent.locks.Lock;
import org.gbean.kernel.Kernel;
import org.gbean.kernel.ServiceCondition;
import org.gbean.kernel.ServiceConditionContext;
import org.gbean.kernel.ServiceName;

/**
 * Aggregates a set of ServiceConditions together so the ServiceManager can treat them as a single unit.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class AggregateConditions {
    /**
     * The service conditions to satisfy.
     */
    protected final Set conditions;

    /**
     * The condition object which is used to singal when a condition has been satisfied asynchronously.
     */
    private final Condition condition;

    /**
     * The context object passed to service conditions in the
     * {@link ServiceCondition#initialize(ServiceConditionContext)} method.
     */
    protected final ServiceConditionContext context;

    /**
     * The destroyed flag.
     */
    protected boolean destroyed = false;

    /**
     * Creates an aggregate service condition.
     *
     * @param kernel the kernel in which the service is registered
     * @param serviceName the name of the service
     * @param classLoader the class loader for the service
     * @param conditions the conditions
     * @param lock the lock for the service manager
     */
    public AggregateConditions(Kernel kernel, ServiceName serviceName, ClassLoader classLoader, Set conditions, Lock lock) {
        this.conditions = conditions;
        condition = lock.newCondition();
        context = new StandardServiceConditionContext(kernel, serviceName, classLoader, lock, condition);
    }

    /**
     * Initializes the conditions.
     */
    public void initialize() {
        if (destroyed) throw new IllegalStateException("destroyed");

        for (Iterator iterator = conditions.iterator(); iterator.hasNext();) {
            ServiceCondition serviceCondition = (ServiceCondition) iterator.next();
            serviceCondition.initialize(context);
        }
    }

    /**
     * Gets the unsatisfied conditions.
     *
     * @return the unstatisfied conditions
     */
    public Set getUnsatisfied() {
        if (destroyed) throw new IllegalStateException("destroyed");

        Set unsatisfied = new HashSet();
        for (Iterator iterator = conditions.iterator(); iterator.hasNext();) {
            ServiceCondition serviceCondition = (ServiceCondition) iterator.next();
            if (!serviceCondition.isSatisfied()) {
                unsatisfied.add(serviceCondition);
            }
        }

        // notify anyone awaiting satisfaction
        if (unsatisfied.isEmpty()) {
            condition.signalAll();
        }
        return unsatisfied;
    }

    /**
     * Destroys all condtions.
     *
     * @return a list of the Exceptions or Errors that occured while destroying the conditon objects.
     */
    public List destroy() {
        List stopErrors = new ArrayList();
        if (!destroyed) {
            destroyed = true;
            for (Iterator iterator = conditions.iterator(); iterator.hasNext();) {
                ServiceCondition serviceCondition = (ServiceCondition) iterator.next();
                try {
                    serviceCondition.destroy();
                } catch (RuntimeException stopError) {
                    stopErrors.add(stopError);
                } catch (Error stopError) {
                    stopErrors.add(stopError);
                }
            }
            // notify anyone awaiting satisfaction
            condition.signalAll();
        }
        return stopErrors;
    }

    /**
     * Causes the current thread to wait until the conditons is satisfied.
     *
     * @throws InterruptedException if the thread is interrupted
     */
    public void awaitSatisfaction() throws InterruptedException {
        while (!destroyed) {
            Set unsatisfied = new HashSet();
            for (Iterator iterator = conditions.iterator(); iterator.hasNext();) {
                ServiceCondition serviceCondition = (ServiceCondition) iterator.next();
                if (!serviceCondition.isSatisfied()) {
                    unsatisfied.add(serviceCondition);
                }
            }
            if (unsatisfied.isEmpty()) {
                return;
            }

            condition.await();
        }
    }
}
