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
package org.gbean.beans;

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
import org.gbean.kernel.runtime.ServiceInstanceUtil;

/**
 * @version $Rev$ $Date$
 */
public class LiveHashSet implements LiveCollection, Set {
    private static final Log log = LogFactory.getLog(LiveHashSet.class);
    private final Kernel kernel;
    private final String name;
    private final Set patterns;
    private final Map members = new HashMap();
    private final Set listeners = new HashSet();
    private boolean stopped = true;
    private LifecycleListener listener;

    public LiveHashSet(Kernel kernel, String name, Set patterns) {
        this.kernel = kernel;
        this.name = name;
        this.patterns = patterns;
    }

    public void start() {
        synchronized (this) {
            if (!stopped) {
                throw new IllegalStateException("Already started");
            }
        }

        Set targets = ServiceInstanceUtil.getRunningTargets(kernel, patterns);
        for (Iterator iterator = targets.iterator(); iterator.hasNext();) {
            addTarget((ObjectName) iterator.next());
        }

        listener = new CollectionReferenceLifecycleListener();
        kernel.addLifecycleListener(listener, patterns);
        stopped = false;
    }

    public String getName() {
        return name;
    }

    public synchronized void destroy() {
        stopped = true;
        members.clear();
        listeners.clear();

        if (listener != null) {
            kernel.removeLifecycleListener(listener);
            listener = null;
        }
    }

    private void addTarget(ObjectName target) {
        Object service = null;
        ArrayList listenerCopy;
        synchronized (this) {
            // if this is not a new target return
            if (members.containsKey(target)) {
                return;
            }

            service = getServiceReference(target);
            if (service == null) {
                return;
            }
            members.put(target, service);

            // make a snapshot of the listeners
            listenerCopy = new ArrayList(listeners);
        }

        // fire the member added event
        for (Iterator iterator = listenerCopy.iterator(); iterator.hasNext();) {
            LiveCollectionListener listener = (LiveCollectionListener) iterator.next();
            try {
                listener.memberAdded(new LiveCollectionEvent(name, service));
            } catch (Throwable t) {
                log.error("Listener threw exception", t);
            }
        }
    }

    protected Object getServiceReference(ObjectName target) {
        try {
            return kernel.getService(target);
        } catch (ServiceNotFoundException e) {
            // service was removed before we could add it
            return null;
        }
    }

    private void removeTarget(ObjectName target) {
        Object service;
        ArrayList listenerCopy;
        synchronized (this) {
            // remove the proxy
            service = members.remove(target);

            // if this was not a target return
            if (service == null) {
                return;
            }

            // make a snapshot of the listeners
            listenerCopy = new ArrayList(listeners);
        }

        // fire the member removed event
        for (Iterator iterator = listenerCopy.iterator(); iterator.hasNext();) {
            LiveCollectionListener listener = (LiveCollectionListener) iterator.next();
            try {
                listener.memberRemoved(new LiveCollectionEvent(name, service));
            } catch (Throwable t) {
                log.error("Listener threw exception", t);
            }
        }
    }

    public synchronized boolean isStopped() {
        return stopped;
    }

    public synchronized void addReferenceCollectionListener(LiveCollectionListener listener) {
        listeners.add(listener);
    }

    public synchronized void removeReferenceCollectionListener(LiveCollectionListener listener) {
        listeners.remove(listener);
    }

    public synchronized int size() {
        if (stopped) {
            return 0;
        }
        return members.size();
    }

    public synchronized boolean isEmpty() {
        if (stopped) {
            return true;
        }
        return members.isEmpty();
    }

    public synchronized boolean contains(Object o) {
        if (stopped) {
            return false;
        }
        return members.containsValue(o);
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
            private final Iterator iterator = new ArrayList(members.values()).iterator();

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
        return members.values().toArray();
    }

    public synchronized Object[] toArray(Object a[]) {
        if (stopped) {
            if (a.length > 0) {
                a[0] = null;
            }
            return a;
        }
        return members.values().toArray(a);
    }

    public synchronized boolean containsAll(Collection c) {
        if (stopped) {
            return c.isEmpty();
        }
        return members.values().containsAll(c);
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
