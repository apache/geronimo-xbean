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
package org.xbean.kernel;

/**
 * This interface defines the interface used to monitor kernel events.  A KernelMonitor can be registered with the
 * kernel using the {@link Kernel#addKernelMonitor(KernelMonitor)} method.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public interface KernelMonitor {
    /**
     * An error occured with notifiying a service monitor.
     *
     * @param serviceMonitor the monitor that threw the exception
     * @param serviceEvent the event that was being processed
     * @param throwable the exception that was thrown
     */
    void serviceNotificationError(ServiceMonitor serviceMonitor, ServiceEvent serviceEvent, Throwable throwable);
}
