/**
 *
 * Copyright 2005-2006 The Apache Software Foundation or its licensors, as applicable.
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
package org.apache.xbean.kernel.standard;

import org.apache.xbean.kernel.Kernel;
import org.apache.xbean.kernel.ServiceContext;
import org.apache.xbean.kernel.ServiceFactory;
import org.apache.xbean.kernel.ServiceName;

/**
 * The standard service context implementation.  This is passed to the service factory in the
 * {@link ServiceFactory#createService(ServiceContext)} and {@link ServiceFactory#destroyService(ServiceContext)}
 * methods.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class StandardServiceContext implements ServiceContext {
    private final Kernel kernel;
    private final ServiceName serviceName;
    private final ClassLoader classLoader;

    /**
     * Creates the standard service context implementation.
     *
     * @param kernel the kernel in which the service is registered
     * @param serviceName the name of the service
     * @param classLoader the class loader for the service
     */
    public StandardServiceContext(Kernel kernel, ServiceName serviceName, ClassLoader classLoader) {
        this.kernel = kernel;
        this.serviceName = serviceName;
        this.classLoader = classLoader;
    }

    /**
     * {@inheritDoc}
     */
    public Kernel getKernel() {
        return kernel;
    }

    /**
     * {@inheritDoc}
     */
    public ServiceName getServiceName() {
        return serviceName;
    }

    /**
     * {@inheritDoc}
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }
}
