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
package org.xbean.kernel.standard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.emory.mathcs.backport.java.util.concurrent.locks.Condition;
import edu.emory.mathcs.backport.java.util.concurrent.locks.Lock;
import org.xbean.kernel.Kernel;
import org.xbean.kernel.ServiceCondition;
import org.xbean.kernel.ServiceName;

/**
 * Aggregates a set of ServiceConditions together so the ServiceManager can treat them as a single unit.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class AggregateCondition {
    private final Kernel kernel;
    private final ServiceName serviceName;
    private final ClassLoader classLoader;
    private final Lock lock;
    private final Map conditions = new HashMap();
    private final Condition satisfiedSignal;
    private boolean destroyed = false;

    /**
     * Creates an aggregate condition.
     *
     * @param kernel the kernel in which the service is registered
     * @param serviceName the name of the service
     * @param classLoader the class loader for the service
     * @param lock the lock for the service manager
     * @param conditions the conditions
     */
    public AggregateCondition(Kernel kernel, ServiceName serviceName, ClassLoader classLoader, Lock lock, Set conditions) {
        this.kernel = kernel;
        this.serviceName = serviceName;
        this.classLoader = classLoader;
        this.lock = lock;
        satisfiedSignal = lock.newCondition();

        // add the conditions to the registry
        if (conditions == null) throw new NullPointerException("conditions is null");
        for (Iterator iterator = conditions.iterator(); iterator.hasNext();) {
            ServiceCondition serviceCondition = (ServiceCondition) iterator.next();
            addCondition(serviceCondition);
        }
    }

    /**
     * Gets a snapshot of the current conditions.
     *
     * @return a snapshot of the current conditions
     */
    protected Set getConditions() {
        return new HashSet(conditions.keySet());
    }

    /**
     * Adds a new condition if not already registered.
     *
     * @param condition the new condition
     */
    protected final void addCondition(ServiceCondition condition) {
        if (!conditions.containsKey(condition)) {
            StandardServiceConditionContext context = new StandardServiceConditionContext(kernel, serviceName, classLoader, lock, satisfiedSignal);
            condition.initialize(context);
            conditions.put(condition, context);
        }
    }

    /**
     * Removes a condition from the registry if present.
     *
     * @param condition the condition to remove
     */
    protected final void removeCondition(ServiceCondition condition) {
        if (conditions.remove(condition) != null) {
            condition.destroy();
        }
    }

    /**
     * Initializes the conditions.
     */
    public void initialize() {
        if (destroyed) throw new IllegalStateException("destroyed");

        for (Iterator iterator = conditions.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            ServiceCondition condition = (ServiceCondition) entry.getKey();
            StandardServiceConditionContext context = (StandardServiceConditionContext) entry.getValue();
            condition.initialize(context);
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
        for (Iterator iterator = conditions.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            ServiceCondition condition = (ServiceCondition) entry.getKey();
            StandardServiceConditionContext context = (StandardServiceConditionContext) entry.getValue();
            if (!context.isSatisfied()) {
                if (condition.isSatisfied()) {
                    // the condition is satisfied
                    // record this fact in the context
                    context.setSatisfied();
                } else {
                    unsatisfied.add(condition);
                }
            }
        }

        // notify anyone awaiting satisfaction
        if (unsatisfied.isEmpty()) {
            satisfiedSignal.signalAll();
        }
        return unsatisfied;
    }

    /**
     * Gets the destroyed status.
     *
     * @return true if this AggregateCondition been destroyed; false otherwise
     */
    public boolean isDestroyed() {
        return destroyed;
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
            for (Iterator iterator = conditions.keySet().iterator(); iterator.hasNext();) {
                ServiceCondition condition = (ServiceCondition) iterator.next();
                try {
                    condition.destroy();
                } catch (RuntimeException stopError) {
                    stopErrors.add(stopError);
                } catch (Error stopError) {
                    stopErrors.add(stopError);
                }
            }
            // notify anyone awaiting satisfaction
            satisfiedSignal.signalAll();
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
            if (getUnsatisfied().isEmpty()) {
                return;
            }
            satisfiedSignal.await();
        }
    }
}
