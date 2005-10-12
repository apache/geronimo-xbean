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

import java.util.Set;

/**
 * A service factory is responsible for construction and destruction of a single service.  A service factory provides
 * the kernel the start conditions, stopped conditions, owned services and enabled status of the service.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public interface ServiceFactory {
    /**
     * Gets the types of the service this service factory will create.  These types is used to index the service within
     * the kernel.  It is a start error to return an object from create service that is not an instance of every type.
     * This is the only type used to index the service, so if the service factory returns a subclass of this type from
     * createService, the subtypes will now be reflected in the index.
     *
     * @return the type of the service this service factory will create
     */
    Class[] getTypes();

    /**
     * A restartable service can be started and stopped repeatedly in the kernel.  A service that is not restartable
     * immediately enters the RUNNING state when registered with the kernel, and can not be started or stopped.
     *
     * @return true if this service can be started and stopped; false otherwise
     */
    boolean isRestartable();

    /**
     * Determines if the service can be instantiated in a kernel.  A disabled restartable service can not be
     * started and a disabled non-restartable service can not be loaded into a kernel.
     *
     * @return true if the service factory is enabled; false otherwise
     */
    boolean isEnabled();

    /**
     * Sets the enabled status of this service factory.   A disabled restartable service can not be
     * started and a disabled non-restartable service can not be loaded into a kernel.
     *
     * @param enabled the new enabled state of this factory
     */
    void setEnabled(boolean enabled);

    /**
     * Get an unmodifable snapshot of the conditions that must be satisfied before this service can be started.
     *
     * @return the start conditions of this service
     */
    Set getStartConditions();

    /**
     * Adds start condition to this service.
     *
     * @param startCondition the new start condition
     * @throws NullPointerException if startCondition is null
     */
    void addStartCondition(ServiceCondition startCondition) throws NullPointerException;

    /**
     * Removes a start condition from this service.
     *
     * @param startCondition the start condition to remove
     * @throws NullPointerException if startCondition is null
     */
    void removeStartCondition(ServiceCondition startCondition) throws NullPointerException;

    /**
     * Get an unmodifable snapshot of the conditions that must be satisfied before this service can be stopped.
     *
     * @return the stop conditions of this service
     */
    Set getStopConditions();

    /**
     * Adds stop condition to this service.
     *
     * @param stopCondition the new stop condition
     * @throws NullPointerException if stopCondition is null
     */
    void addStopCondition(ServiceCondition stopCondition) throws NullPointerException;

    /**
     * Removes a stop condition from this service.
     *
     * @param stopCondition the stop condition to remove
     * @throws NullPointerException if stopCondition is null
     */
    void removeStopCondition(ServiceCondition stopCondition) throws NullPointerException;

    /**
     * Gets the names of services owned by this service.  This information is used for the startRecursive method on the
     * kernel.  When a servcie is started with startRecursive all owned services will be started with startRecursive.
     *
     * @return the names of the services owned by this service
     */
    Set getOwnedServices();

    /**
     * Creates the service instance.
     *
     * @param serviceContext context information for the new service
     * @return the service instance
     * @throws Exception if a problem occurs during construction
     */
    Object createService(ServiceContext serviceContext) throws Exception;

    /**
     * Destroys the service instance.
     *
     * @param serviceContext the context information for the service
     */
    void destroyService(ServiceContext serviceContext);
}
