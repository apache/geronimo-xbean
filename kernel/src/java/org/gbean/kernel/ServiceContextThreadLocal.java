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

/**
 * ServiceContextThreadLocal carries the ServiceContext on the Thread during service construction and destruction.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public final class ServiceContextThreadLocal {
    private ServiceContextThreadLocal() {
    }

    private static final ThreadLocal THREAD_LOCAL = new ThreadLocal();

    /**
     * Gets the ServiceContext associated with the current thread.
     * @return the ServiceContext associated with the current thread
     */
    public static ServiceContext get() {
        return (ServiceContext) THREAD_LOCAL.get();
    }

    /**
     * Assocates the specified ServiceContext with the current thread.
     * @param serviceContext the service context to associate with the current thread
     */
    public static void set(ServiceContext serviceContext) {
        THREAD_LOCAL.set(serviceContext);
    }
}
