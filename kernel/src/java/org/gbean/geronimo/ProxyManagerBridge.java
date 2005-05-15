/**
 *
 * Copyright 2004 The Apache Software Foundation
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
package org.gbean.geronimo;

import javax.management.ObjectName;

import org.gbean.proxy.ProxyManager;
import org.gbean.proxy.ProxyFactory;
import org.gbean.kernel.ServiceNotFoundException;

/**
 * @version $Rev$ $Date$
 */
public class ProxyManagerBridge implements org.apache.geronimo.kernel.proxy.ProxyManager {
    private final ProxyManager proxyManager;

    public ProxyManagerBridge(ProxyManager proxyManager) {
        this.proxyManager = proxyManager;
    }

    public org.apache.geronimo.kernel.proxy.ProxyFactory createProxyFactory(Class type) {
        return new ProxyFactoryBridge(proxyManager.createProxyFactory(type));
    }

    public Object createProxy(ObjectName target, Class type) {
        try {
            return proxyManager.createProxy(target, type);
        } catch (ServiceNotFoundException e) {
            throw (IllegalStateException) new IllegalStateException("Service was not loaded: " + target).initCause(e);
        }
    }

    public synchronized void destroyProxy(Object proxy) {
    }

    public boolean isProxy(Object proxy) {
        return proxyManager.isProxy(proxy);
    }

    public synchronized ObjectName getProxyTarget(Object proxy) {
        return proxyManager.getProxyTarget(proxy);
    }

    private class ProxyFactoryBridge implements org.apache.geronimo.kernel.proxy.ProxyFactory {
        private final ProxyFactory proxyFactory;

        public ProxyFactoryBridge(ProxyFactory proxyFactory) {
            this.proxyFactory = proxyFactory;
        }

        public Object createProxy(ObjectName target) {
            try {
                return proxyFactory.createProxy(target);
            } catch (ServiceNotFoundException e) {
                throw (IllegalStateException) new IllegalStateException("Service was not loaded: " + target).initCause(e);
            }
        }
    }
}
