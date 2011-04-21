/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.xbean.osgi.bundle.util;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;

/**
 * DelegatingBundleReference is based on {@link BundleReference} and provides
 * access to {@link DelegatingBundle}.  
 */
public interface DelegatingBundleReference extends BundleReference {

    /**
     * Returns the bundle associated with this classloader.
     * 
     * In most cases the bundle associated with the classloader is a regular framework bundle. 
     * However, in some cases the bundle associated with the classloader is a {@link DelegatingBundle}.
     * In such cases, the <tt>unwrap</tt> parameter controls whether this function returns the
     * {@link DelegatingBundle} instance or the main application bundle backing with the {@link DelegatingBundle}.
     *
     * @param unwrap If true and if the bundle associated with this classloader is a {@link DelegatingBundle}, 
     *        this function will return the main application bundle backing with the {@link DelegatingBundle}. 
     *        Otherwise, the bundle associated with this classloader is returned as is.
     * @return The bundle associated with this classloader.
     */
    public Bundle getBundle(boolean unwrap);

}
