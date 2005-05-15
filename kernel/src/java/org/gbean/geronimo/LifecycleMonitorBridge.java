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

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import javax.management.ObjectName;

import org.gbean.kernel.Kernel;
import org.gbean.kernel.LifecycleListener;

/**
 * @version $Rev$ $Date$
 */
public class LifecycleMonitorBridge implements org.apache.geronimo.kernel.lifecycle.LifecycleMonitor {
    private final Kernel kernel;
    private final Map listenerBridgeMap = new HashMap();

    public LifecycleMonitorBridge(Kernel kernel) {
        this.kernel = kernel;
    }

    public void addLifecycleListener(org.apache.geronimo.kernel.lifecycle.LifecycleListener listener, ObjectName pattern) {
        LifecycleListenerBridge listenerBridge = getListenerBridge(listener);
        kernel.addLifecycleListener(listenerBridge, pattern);
    }

    public void addLifecycleListener(org.apache.geronimo.kernel.lifecycle.LifecycleListener listener, Set patterns) {
        LifecycleListenerBridge listenerBridge = getListenerBridge(listener);
        kernel.addLifecycleListener(listenerBridge, patterns);
    }

    public void removeLifecycleListener(org.apache.geronimo.kernel.lifecycle.LifecycleListener listener) {
        LifecycleListenerBridge listenerBridge = getListenerBridge(listener);
        if (listenerBridge != null) {
            kernel.removeLifecycleListener(listenerBridge);
        }
    }

    private LifecycleListenerBridge getListenerBridge(org.apache.geronimo.kernel.lifecycle.LifecycleListener listener) {
        synchronized (listenerBridgeMap) {
            LifecycleListenerBridge listenerBridge = (LifecycleListenerBridge) listenerBridgeMap.get(listener);
            if (listenerBridge == null) {
                listenerBridge = new LifecycleListenerBridge(listener);
                listenerBridgeMap.put(listener, listenerBridge);
            }
            return listenerBridge;
        }
    }


    private static class LifecycleListenerBridge implements LifecycleListener {
        private final org.apache.geronimo.kernel.lifecycle.LifecycleListener lifecycleListener;

        public LifecycleListenerBridge(org.apache.geronimo.kernel.lifecycle.LifecycleListener lifecycleListener) {
            this.lifecycleListener = lifecycleListener;
        }

        public void loaded(ObjectName objectName) {
            lifecycleListener.loaded(objectName);
        }

        public void starting(ObjectName objectName) {
            lifecycleListener.starting(objectName);
        }

        public void running(ObjectName objectName) {
            lifecycleListener.running(objectName);
        }

        public void stopping(ObjectName objectName) {
            lifecycleListener.stopping(objectName);
        }

        public void stopped(ObjectName objectName) {
            lifecycleListener.stopped(objectName);
        }

        public void unloaded(ObjectName objectName) {
            lifecycleListener.unloaded(objectName);
        }
    }
}
