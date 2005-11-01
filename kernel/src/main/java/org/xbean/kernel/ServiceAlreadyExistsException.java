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
 * Signifies that an attempt was made to register a service using a name that already has a service registered.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class ServiceAlreadyExistsException extends Exception {
    private final ServiceName serviceName;

    /**
     * Creates a ServiceAlreadyExistsException for the specified service name.
     *
     * @param serviceName the name of the service that already exists
     */
    public ServiceAlreadyExistsException(ServiceName serviceName) {
        if (serviceName == null) throw new NullPointerException("name is null");
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
