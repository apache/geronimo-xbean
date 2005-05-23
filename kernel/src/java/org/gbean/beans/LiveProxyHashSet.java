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
package org.gbean.beans;

import java.util.Set;
import javax.management.ObjectName;

import org.gbean.kernel.Kernel;
import org.gbean.kernel.ServiceNotFoundException;
import org.gbean.proxy.ProxyFactory;
import org.gbean.proxy.ProxyManager;

/**
 * @version $Rev$ $Date$
 */
public class LiveProxyHashSet extends LiveHashSet {
    private final ProxyFactory factory;

    public LiveProxyHashSet(Kernel kernel, String name, Set patterns, Class type) {
        super (kernel, name, patterns);

        try {
            factory = ProxyManager.findProxyManager(kernel).createProxyFactory(type);
        } catch (ServiceNotFoundException e) {
            throw new IllegalStateException("No ProxyManager available in kernel");
        }
    }

    protected Object getServiceReference(ObjectName target) {
        try {
            return factory.createProxy(target);
        } catch (ServiceNotFoundException e) {
            // service was removed before we could add it
            return null;
        }
    }
}
