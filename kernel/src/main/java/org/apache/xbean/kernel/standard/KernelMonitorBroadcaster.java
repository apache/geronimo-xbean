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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;
import org.apache.xbean.kernel.KernelMonitor;
import org.apache.xbean.kernel.ServiceMonitor;
import org.apache.xbean.kernel.ServiceEvent;
import org.apache.xbean.kernel.KernelErrorsError;

/**
 * The KernelMonitorBroadcaster broadcasts kernel events to registered kernel monitors.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class KernelMonitorBroadcaster implements KernelMonitor {
    /**
     * The monitors of kernel events.
     */
    private final CopyOnWriteArrayList kernelMonitors = new CopyOnWriteArrayList();

    /**
     * Adds a kernel monitor.
     *
     * @param kernelMonitor the kernel monitor to add
     */
    public void addKernelMonitor(KernelMonitor kernelMonitor) {
        kernelMonitors.addIfAbsent(kernelMonitor);
    }

    /**
     * Removes a kernel monitor.
     *
     * @param kernelMonitor the kernel monitor to remove
     */
    public void removeKernelMonitor(KernelMonitor kernelMonitor) {
        kernelMonitors.remove(kernelMonitor);
    }

    /**
     * {@inheritDoc}
     */
    public void serviceNotificationError(ServiceMonitor serviceMonitor, ServiceEvent serviceEvent, Throwable throwable) {
        List errors = new ArrayList();
        for (Iterator iterator = kernelMonitors.iterator(); iterator.hasNext();) {
            KernelMonitor kernelMonitor = (KernelMonitor) iterator.next();
            try {
                kernelMonitor.serviceNotificationError(serviceMonitor, serviceEvent, throwable);
            } catch (RuntimeException ignored) {
                // ignore - we did our best to notify the world
            } catch (Error e) {
                errors.add(e);
            }
        }
        if (!errors.isEmpty()) {
            throw new KernelErrorsError(errors);
        }
    }
}
