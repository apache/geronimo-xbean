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

import edu.emory.mathcs.backport.java.util.concurrent.locks.Condition;
import edu.emory.mathcs.backport.java.util.concurrent.locks.Lock;
import org.gbean.kernel.Kernel;
import org.gbean.kernel.ServiceConditionContext;
import org.gbean.kernel.ServiceName;

/**
 * This is the service context used by the service manager.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class StandardServiceConditionContext implements ServiceConditionContext {
    /**
     * The kernel in which the service is registered.
     */
    private final Kernel kernel;

    /**
     * The unique name of the service in the kernel.
     */
    private final ServiceName serviceName;

    /**
     * The class loader for the service.
     */
    private final ClassLoader classLoader;

    /**
     * The lock that must be acquired before signaling the condition.
     */
    private final Lock lock;

    /**
     * The condition to signal when the {@link #setSatisfied()} method is called.
     */
    private final Condition condition;

    /**
     * Has this condition been satisfied?  Once satisfied a condition is always considered satisfied.
     */
    private boolean satisfied = false;

    /**
     * Creates a service context for the specified service.
     *
     * @param kernel the kernel in which the service is registered
     * @param serviceName the name of the service
     * @param classLoader the class loader for the service
     * @param lock the lock for the service manager
     * @param condition the condition that should be notified when the {@link #setSatisfied()} method is called
     */
    public StandardServiceConditionContext(Kernel kernel, ServiceName serviceName, ClassLoader classLoader, Lock lock, Condition condition) {
        this.kernel = kernel;
        this.serviceName = serviceName;
        this.classLoader = classLoader;
        this.lock = lock;
        this.condition = condition;
    }

    /**
     * {@inheritDoc}
     */
    public Kernel getKernel() {
        return kernel;
    }

    /**
     * {@inheritDoc}
     */
    public ServiceName getServiceName() {
        return serviceName;
    }

    /**
     * {@inheritDoc}
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Gets the satisfied status of this condition.  Once satisfied a condition is considered satisfied until destroyed
     * and reinitialized.  The ServiceManager uses the StandardServiceConditionContext to track the status of conditions
     * so it will call setSatisfied() when the condition returns true from isSatisfied().
     *
     * @return satisfied status of this condition
     */
    public boolean isSatisfied() {
        return satisfied;
    }

    /**
     * {@inheritDoc}
     */
    public void setSatisfied() {
        lock.lock();
        try {
            if (!satisfied) {
                satisfied = true;
                condition.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }
}
