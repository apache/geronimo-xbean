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
package org.gbean.geronimo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gbean.kernel.Kernel;
import org.gbean.kernel.LifecycleAdapter;
import org.gbean.kernel.LifecycleListener;
import org.gbean.kernel.ServiceNotFoundException;
import org.gbean.kernel.KernelUtil;
import org.gbean.proxy.ProxyFactory;
import org.gbean.proxy.ProxyManager;
import org.springframework.beans.factory.FactoryBean;

/**
 * @version $Rev$ $Date$
 */
public class ProxyReferenceCollection implements org.apache.geronimo.gbean.ReferenceCollection, FactoryBeanProvider {
    private static final Log log = LogFactory.getLog(ProxyReferenceCollection.class);
    private final FactoryBean factoryBean;
    private final String name;
    private final ProxyFactory factory;
    private final Map proxies = new HashMap();
    private final Set listeners = new HashSet();
    private boolean stopped = false;
    private LifecycleListener listener;
    private final Kernel kernel;

    public ProxyReferenceCollection(FactoryBean factoryBean, String name, Class type, Kernel kernel, Set patterns) {
        this.factoryBean = factoryBean;
        this.name = name;
        this.kernel = kernel;
        try {
            factory = ProxyManager.findProxyManager(kernel).createProxyFactory(type);
        } catch (ServiceNotFoundException e) {
            throw new IllegalStateException("No ProxyManager available in kernel");
        }

        Set targets = KernelUtil.getRunningServiceNames(kernel, patterns);
        for (Iterator iterator = targets.iterator(); iterator.hasNext();) {
            addTarget((ObjectName) iterator.next());
        }

        listener = new CollectionReferenceLifecycleListener();
        kernel.addLifecycleListener(listener, patterns);
    }

    public FactoryBean getFactoryBean() {
        return factoryBean;
    }

    public String getName() {
        return name;
    }

    public synchronized void destroy() {
        stopped = true;
        proxies.clear();
        listeners.clear();

        if (listener != null) {
            kernel.removeLifecycleListener(listener);
            listener = null;
        }
    }

    private void addTarget(ObjectName target) {
        Object proxy = null;
        ArrayList listenerCopy;
        synchronized (this) {
            // if this is not a new target return
            if (proxies.containsKey(target)) {
                return;
            }

            // create and add the proxy
            try {
                proxy = factory.createProxy(target);
            } catch (ServiceNotFoundException e) {
                // service was removed before we could add it
            }
            proxies.put(target, proxy);

            // make a snapshot of the listeners
            listenerCopy = new ArrayList(listeners);
        }

        // fire the member added event
        for (Iterator iterator = listenerCopy.iterator(); iterator.hasNext();) {
            org.apache.geronimo.gbean.ReferenceCollectionListener listener = (org.apache.geronimo.gbean.ReferenceCollectionListener) iterator.next();
            try {
                listener.memberAdded(new org.apache.geronimo.gbean.ReferenceCollectionEvent(name, proxy));
            } catch (Throwable t) {
                log.error("Listener threw exception", t);
            }
        }
    }

    private void removeTarget(ObjectName target) {
        Object proxy;
        ArrayList listenerCopy;
        synchronized (this) {
            // remove the proxy
            proxy = proxies.remove(target);

            // if this was not a target return
            if (proxy == null) {
                return;
            }

            // make a snapshot of the listeners
            listenerCopy = new ArrayList(listeners);
        }

        // fire the member removed event
        for (Iterator iterator = listenerCopy.iterator(); iterator.hasNext();) {
            org.apache.geronimo.gbean.ReferenceCollectionListener listener = (org.apache.geronimo.gbean.ReferenceCollectionListener) iterator.next();
            try {
                listener.memberRemoved(new org.apache.geronimo.gbean.ReferenceCollectionEvent(name, proxy));
            } catch (Throwable t) {
                log.error("Listener threw exception", t);
            }
        }
    }

    public synchronized boolean isStopped() {
        return stopped;
    }

    public synchronized void addReferenceCollectionListener(org.apache.geronimo.gbean.ReferenceCollectionListener listener) {
        listeners.add(listener);
    }

    public synchronized void removeReferenceCollectionListener(org.apache.geronimo.gbean.ReferenceCollectionListener listener) {
        listeners.remove(listener);
    }

    public synchronized int size() {
        if (stopped) {
            return 0;
        }
        return proxies.size();
    }

    public synchronized boolean isEmpty() {
        if (stopped) {
            return true;
        }
        return proxies.isEmpty();
    }

    public synchronized boolean contains(Object o) {
        if (stopped) {
            return false;
        }
        return proxies.containsValue(o);
    }

    public synchronized Iterator iterator() {
        if (stopped) {
            return new Iterator() {
                public boolean hasNext() {
                    return false;
                }

                public Object next() {
                    throw new NoSuchElementException();
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        return new Iterator() {
            // copy the proxies, so the client can iterate without concurrent modification
            // this is necssary since the client has nothing to synchronize on
            private final Iterator iterator = new ArrayList(proxies.values()).iterator();

            public boolean hasNext() {
                return iterator.hasNext();
            }

            public Object next() {
                return iterator.next();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public synchronized Object[] toArray() {
        if (stopped) {
            return new Object[0];
        }
        return proxies.values().toArray();
    }

    public synchronized Object[] toArray(Object a[]) {
        if (stopped) {
            if (a.length > 0) {
                a[0] = null;
            }
            return a;
        }
        return proxies.values().toArray(a);
    }

    public synchronized boolean containsAll(Collection c) {
        if (stopped) {
            return c.isEmpty();
        }
        return proxies.values().containsAll(c);
    }

    public boolean add(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    private class CollectionReferenceLifecycleListener extends LifecycleAdapter {
        public void running(ObjectName objectName) {
            addTarget(objectName);
        }

        public void stopping(ObjectName objectName) {
            removeTarget(objectName);
        }

        public void stopped(ObjectName objectName) {
            removeTarget(objectName);
        }

        public void unloaded(ObjectName objectName) {
            removeTarget(objectName);
        }
    }
}
