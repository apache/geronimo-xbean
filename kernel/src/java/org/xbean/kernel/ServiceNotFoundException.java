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
 * A service with the specified name was not found.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class ServiceNotFoundException extends Exception {
    private final ServiceName serviceName;

    /**
     * Creates a ServiceNotFoundException for the specified service name.
     *
     * @param serviceName the name of the service that was not found.
     */
    public ServiceNotFoundException(ServiceName serviceName) {
        if (serviceName == null) throw new NullPointerException("serviceName is null");
        this.serviceName = serviceName;
    }

    /**
     * Gets the name of the service that was not found.
     *
     * @return the the name of the service that was not found
     */
    public ServiceName getServiceName() {
        return serviceName;
    }
}
