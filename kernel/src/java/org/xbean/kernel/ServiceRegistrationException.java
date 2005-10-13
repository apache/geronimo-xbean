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
 * A problem occured while attempting to register or unregister an exception.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class ServiceRegistrationException extends Exception {
    private final ServiceName serviceName;

    /**
     * Creates a ServiceRegistrationException for the specified service caused by the specified Throwable.
     *
     * @param serviceName the name of the service that was being registered or unregistered.
     * @param cause the reason the registeration problem occured
     */
    public ServiceRegistrationException(ServiceName serviceName, Throwable cause) {
        super(cause);
        if (serviceName == null) throw new NullPointerException("serviceName is null");
        this.serviceName = serviceName;
    }

    /**
     * Gets the name of the service that had a registration problem.
     *
     * @return the the name of the service that had a registration problem
     */
    public ServiceName getServiceName() {
        return serviceName;
    }
}
