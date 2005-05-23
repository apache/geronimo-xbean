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
package org.gbean.service;

import org.gbean.kernel.Kernel;

/**
 * @version $Revision$ $Date$
 */
public interface ServiceContext {
    /**
     * Gets the object name under which this bean is registered
     * @return the object name of this bean
     */
    String getObjectName();

    /**
     * Gets the kernel in which this bean is registered
     * @return the kernel in which this bean is register
     */
    Kernel getKernel();

    /**
     * Gets the class loader used to construct this bean
     * @return thhe class loader in which this bean was constructed
     */
    ClassLoader getClassLoader();

    /**
     * Gets the state of this component as an int.
     * The int return is required by the JSR77 specification.
     *
     * @return the current state of this component
     */
    int getState();

    /**
     * Attempt to bring the component into the fully stopped state. If an exception occurs while
     * stopping the component, the component is automaticaly failed.
     * <p/>
     * There is no guarantee that the service will be stopped when the method returns.
     *
     * @throws Exception if a problem occurs while stopping the component
     */
    void stop() throws Exception;
}
