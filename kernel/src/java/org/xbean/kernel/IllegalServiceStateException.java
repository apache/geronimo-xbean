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
 * Indicates an operation was called on a service in a state that does not allow that operation to be called.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class IllegalServiceStateException extends Exception {
    private final ServiceName serviceName;

    /**
     * Creates an IllegalServiceStateException.
     *
     * @param message information about why the service is in an illegal state
     * @param serviceName the name of the service
     */
    public IllegalServiceStateException(String message, ServiceName serviceName) {
        super(message + ": " + serviceName);
        if (serviceName == null) throw new NullPointerException("serviceName is null");
        this.serviceName = serviceName;
    }

    /**
     * Gets the name of the service that caused this exception.
     *
     * @return the service name
     */
    public ServiceName getServiceName() {
        return serviceName;
    }
}
