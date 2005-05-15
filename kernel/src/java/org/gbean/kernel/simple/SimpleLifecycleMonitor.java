/**
 *
 * Copyright 2005 GBean.org
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

package org.gbean.kernel.simple;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gbean.kernel.LifecycleListener;
import org.gbean.kernel.runtime.LifecycleBroadcaster;

/**
 * @version $Rev: 71492 $ $Date: 2004-11-14 21:31:50 -0800 (Sun, 14 Nov 2004) $
 */
public class SimpleLifecycleMonitor {
    private static final Log log = LogFactory.getLog(SimpleLifecycleMonitor.class);

    private final Map boundListeners = new LinkedHashMap();
    private final Map listenerPatterns = new LinkedHashMap();

    public void start() {
    }

    /**
     * Frees all resources associated with the monitor.  No futher events will be processed.
     */
    public synchronized void stop() {
        boundListeners.clear();
        listenerPatterns.clear();
    }

    /**
     * Create a lifecycle broadcaster for the specified bean.  This is typically given to the
     * instance manager to broadcast lifecycle changes
     * @param objectName the name of the object for which a broadcaster should be created
     * @return the lifecycle broadcaster
     */
    public LifecycleBroadcaster createLifecycleBroadcaster(ObjectName objectName) {
        return new SimpleLifecycleBroadcaster(objectName);
    }

    /**
     * Registers a listener to revieve life cycle events for a set of object name patterns.
     * @param listener the listener that will receive life cycle events
     * @param patterns a set of ObjectName patterns
     */
    public synchronized void addLifecycleListener(LifecycleListener listener, Set patterns) {
        for (Iterator patternIterator = patterns.iterator(); patternIterator.hasNext();) {
            ObjectName pattern = (ObjectName) patternIterator.next();
            for (Iterator iterator = boundListeners.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry entry = (Map.Entry) iterator.next();
                ObjectName source = (ObjectName) entry.getKey();
                if (pattern.apply(source)) {
                    List listeners = (List) entry.getValue();
                    listeners.add(listener);
                }
            }
        }
        listenerPatterns.put(listener, patterns);
    }

    /**
     * Removes the listener from all notifications.
     * @param listener the listener to unregister
     */
    public synchronized void removeLifecycleListener(LifecycleListener listener) {
        for (Iterator iterator = boundListeners.values().iterator(); iterator.hasNext();) {
            List listeners = (List) iterator.next();
            listeners.remove(listener);
        }
        listenerPatterns.remove(listener);
    }

    private synchronized void addSource(ObjectName source) {
        if (boundListeners.containsKey(source)) {
            // alreayd registered
            return;
        }

        // find all listeners interested in events from this source
        List listeners = new LinkedList();
        for (Iterator listenerIterator = listenerPatterns.entrySet().iterator(); listenerIterator.hasNext();) {
            Map.Entry entry = (Map.Entry) listenerIterator.next();
            Set patterns = (Set) entry.getValue();
            for (Iterator patternIterator = patterns.iterator(); patternIterator.hasNext();) {
                ObjectName pattern = (ObjectName) patternIterator.next();
                if (pattern.apply(source)) {
                    LifecycleListener listener = (LifecycleListener) entry.getKey();
                    listeners.add(listener);
                }
            }
        }

        boundListeners.put(source, listeners);
    }

    private synchronized void removeSource(ObjectName source) {
        boundListeners.remove(source);
    }

    private synchronized List getTargets(ObjectName source) {
        List targets = (List) boundListeners.get(source);
        if (targets == null) {
            // no one is interested in this event
            return Collections.EMPTY_LIST;
        } else {
            return new LinkedList(targets);
        }
    }

    private void fireLoadedEvent(ObjectName objectName) {
        List targets = getTargets(objectName);
        for (Iterator iterator = targets.iterator(); iterator.hasNext();) {
            LifecycleListener listener = (LifecycleListener) iterator.next();
            try {
                listener.loaded(objectName);
            } catch (Throwable e) {
                log.warn("Exception occured while notifying listener", e);
            }
        }
    }

    private void fireStartingEvent(ObjectName source) {
        List targets = getTargets(source);
        for (Iterator iterator = targets.iterator(); iterator.hasNext();) {
            LifecycleListener listener = (LifecycleListener) iterator.next();
            try {
                listener.starting(source);
            } catch (Throwable e) {
                log.warn("Exception occured while notifying listener", e);
            }
        }
    }

    private void fireRunningEvent(ObjectName source) {
        List targets = getTargets(source);
        for (Iterator iterator = targets.iterator(); iterator.hasNext();) {
            LifecycleListener listener = (LifecycleListener) iterator.next();
            try {
                listener.running(source);
            } catch (Throwable e) {
                log.warn("Exception occured while notifying listener", e);
            }
        }
    }

    private void fireStoppingEvent(ObjectName source) {
        List targets = getTargets(source);
        Collections.reverse(targets);
        for (Iterator iterator = targets.iterator(); iterator.hasNext();) {
            LifecycleListener listener = (LifecycleListener) iterator.next();
            try {
                listener.stopping(source);
            } catch (Throwable e) {
                log.warn("Exception occured while notifying listener", e);
            }
        }
    }

    private void fireStoppedEvent(ObjectName source) {
        List targets = getTargets(source);
        Collections.reverse(targets);
        for (Iterator iterator = targets.iterator(); iterator.hasNext();) {
            LifecycleListener listener = (LifecycleListener) iterator.next();
            try {
                listener.stopped(source);
            } catch (Throwable e) {
                log.warn("Exception occured while notifying listener", e);
            }
        }
    }

    private void fireUnloadedEvent(ObjectName source) {
        List targets = getTargets(source);
        Collections.reverse(targets);
        for (Iterator iterator = targets.iterator(); iterator.hasNext();) {
            LifecycleListener listener = (LifecycleListener) iterator.next();
            try {
                listener.unloaded(source);
            } catch (Throwable e) {
                log.warn("Exception occured while notifying listener", e);
            }
        }
    }

    private class SimpleLifecycleBroadcaster implements LifecycleBroadcaster {
        private final ObjectName objectName;

        public SimpleLifecycleBroadcaster(ObjectName objectName) {
            this.objectName = objectName;
        }

        public void fireLoadedEvent() {
            addSource(objectName);
            SimpleLifecycleMonitor.this.fireLoadedEvent(objectName);
        }

        public void fireStartingEvent() {
            SimpleLifecycleMonitor.this.fireStartingEvent(objectName);
        }

        public void fireRunningEvent() {
            SimpleLifecycleMonitor.this.fireRunningEvent(objectName);
        }

        public void fireStoppingEvent() {
            SimpleLifecycleMonitor.this.fireStoppingEvent(objectName);
        }

        public void fireStoppedEvent() {
            SimpleLifecycleMonitor.this.fireStoppedEvent(objectName);
        }

        public void fireUnloadedEvent() {
            SimpleLifecycleMonitor.this.fireUnloadedEvent(objectName);
            removeSource(objectName);
        }
    }
}
