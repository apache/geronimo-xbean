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
 * Indicates that the service factory returned an object from the createService method that is not an instance of every
 * declared type.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class InvalidServiceTypeException extends Exception {
    private final ServiceName serviceName;
    private final Class expectedType;
    private final Class serviceType;

    /**
     * Creates an InvalidServiceType caused by the service with the specified name, which returned an object from the
     * createService method of the specified type that is not an instance of the expected type.
     * @param serviceName the name of the service that returned an object of the wrong type
     * @param expectedType the type that was expected
     * @param serviceType the actual type of the service returned from the factory
     */
    // todo add servicefacotory to the parameters
    public InvalidServiceTypeException(ServiceName serviceName, Class expectedType, Class serviceType) {
        super("Expected service type " + expectedType.getName() + ", but service factory created a " +
                serviceType.getName() + " for service " + serviceName);
        if (serviceName == null) throw new NullPointerException("serviceName is null");
        if (expectedType == null) throw new NullPointerException("expectedType is null");
        if (serviceType == null) throw new NullPointerException("serviceType is null");
        this.serviceName = serviceName;
        this.expectedType = expectedType;
        this.serviceType = serviceType;
    }

    /**
     * Gets the name of the service that returned an object of the wrong type.
     * @return the name of the service that returned an object of the wrong type
     */
    public ServiceName getServiceName() {
        return serviceName;
    }

    /**
     * Gets the type that was expected.
     * @return the type that was expected
     */
    public Class getExpectedType() {
        return expectedType;
    }

    /**
     * Gets the actual type of the service returned from the factory.
     * @return  the actual type of the service returned from the factory
     */
    public Class getServiceType() {
        return serviceType;
    }
}
