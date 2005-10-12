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

/**
 * A ServiceContion represents a prerequsite for a service to start or stop.  A condition can be added to a service with
 * the {@link ServiceFactory#addStartCondition(ServiceCondition)} or
 * {@link ServiceFactory#addStopCondition(ServiceCondition)} methods.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public interface ServiceCondition {
    /**
     * Initializes the condition.  The conition is now allowed reserve unique resources and start threads.
     * mehtod should never block the thread nor should it throw any exceptions.
     * </p>
     * Note: this method is called from within a critical lock within the kernel, so do not block the thread or
     * call back into the kernel.  This method should never throw an exception.
     *
     * @param context context information for this condition
     */
    void initialize(ServiceConditionContext context);

    /**
     * Gets statisfied state of this conditon.  Once a condition returns true from this method it is assumed to be satisfied until
     * destroyed and reinitialized.
     * </p>
     * Note: this method is called from within a critical lock within the kernel, so do not block the thread or
     * call back into the kernel.  This method should never throw an exception.
     *
     * @return true if this condition is satisfied; false otherwise
     */
    boolean isSatisfied();

    /**
     * Destroys the condition.  The condition must release all resources and stop any started threads.
     * </p>
     * Note: this method is called from within a critical lock within the kernel, so do not block the thread or
     * call back into the kernel.  This method should never throw an exception.
     */
    void destroy();
}
