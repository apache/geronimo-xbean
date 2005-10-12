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
package org.xbean.server.loader;

import org.xbean.kernel.ServiceName;
import org.xbean.kernel.Kernel;

/**
 * Loaders load configurations into a Kernel.
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public interface Loader {
    /**
     * Gets the kernel in which configuraitons are loaded.
     * @return the kernel in which configurations are loaded
     */
    Kernel getKernel();

    /**
     * Loads the specified configuration into the kernel.  The location passed to this method is specific loader
     * implementation. It is important to remember that a loader only loads the configuration into the kernel and
     * does not start the configuration.
     *
     * @param location the location of the configuration
     * @return the service name of the configuration added to the kernel
     * @throws Exception if a problem occurs while loading the configuration
     */
    ServiceName load(String location) throws Exception;
}
