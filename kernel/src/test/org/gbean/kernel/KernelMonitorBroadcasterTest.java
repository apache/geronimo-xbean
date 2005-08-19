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
package org.gbean.kernel;

import junit.framework.TestCase;
import org.gbean.kernel.standard.StandardKernel;

/**
 * Tests the KernelMonitorBroadcaster.
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class KernelMonitorBroadcasterTest extends TestCase {
    private static final ServiceMonitor SERVICE_MONITOR = new NullServiceMonitor();
    private static final ServiceEvent SERVICE_EVENT = new ServiceEvent(0,
            new StandardKernel("test"),
            new StringServiceName("service-name"),
            new StaticServiceFactory(new Object()),
            ClassLoader.getSystemClassLoader(), null);
    private static final Throwable THROWABLE = new Throwable("test throwable");

    private MockKernelMonitor kernelMonitor1 = new MockKernelMonitor();
    private MockKernelMonitor kernelMonitor2 = new MockKernelMonitor();
    private MockKernelMonitor kernelMonitor3 = new MockKernelMonitor();
    private MockKernelMonitor kernelMonitor4 = new MockKernelMonitor();
    private KernelMonitorBroadcaster kernelMonitorBroadcaster = new KernelMonitorBroadcaster();

    /**
     * Test that events are fired to all registered monitors.
     * Strategy:
     * <ul><li>
     *      Add the monitors
     * </li><li>
     *      Fire an event
     * </li><li>
     *      Verify that the event was called on each monitor
     * </li></ul>
     */
    public void testFireEvent() {
        kernelMonitorBroadcaster.addKernelMonitor(kernelMonitor1);
        kernelMonitorBroadcaster.addKernelMonitor(kernelMonitor2);
        kernelMonitorBroadcaster.addKernelMonitor(kernelMonitor3);
        kernelMonitorBroadcaster.addKernelMonitor(kernelMonitor4);

        kernelMonitorBroadcaster.serviceNotificationError(SERVICE_MONITOR, SERVICE_EVENT, THROWABLE);

        assertTrue(kernelMonitor1.wasServiceNotificationErrorCalled());
        assertTrue(kernelMonitor2.wasServiceNotificationErrorCalled());
        assertTrue(kernelMonitor3.wasServiceNotificationErrorCalled());
        assertTrue(kernelMonitor4.wasServiceNotificationErrorCalled());
    }

    /**
     * Test that if a monitor is added more then once it only recieves the event once.
     * Strategy:
     * <ul><li>
     *      Add a monitor several times
     * </li><li>
     *      Fire an event
     * </li><li>
     *      Verify that the monitor was only notified once
     * </li></ul>
     */
    public void testDoubleAdd() {
        kernelMonitorBroadcaster.addKernelMonitor(kernelMonitor1);
        kernelMonitorBroadcaster.addKernelMonitor(kernelMonitor1);
        kernelMonitorBroadcaster.addKernelMonitor(kernelMonitor1);

        kernelMonitorBroadcaster.serviceNotificationError(SERVICE_MONITOR, SERVICE_EVENT, THROWABLE);

        // note the mock monitor asserts that it is only called once before requreing a reset
        assertTrue(kernelMonitor1.wasServiceNotificationErrorCalled());
    }

    /**
     * Test that events are not fired to a monitor that has been removed.
     * Strategy:
     * <ul><li>
     *      Add the monitors
     * </li><li>
     *      Fire an event
     * </li><li>
     *      Verify that the event was called on each monitor
     * </li><li>
     *      Removes a monitors
     * </li><li>
     *      Fire an event
     * </li><li>
     *      Verify that the event was called on each monitor except the removed monitor
     * </li></ul>
     */
    public void testRemove() {
        kernelMonitorBroadcaster.addKernelMonitor(kernelMonitor1);
        kernelMonitorBroadcaster.addKernelMonitor(kernelMonitor2);
        kernelMonitorBroadcaster.addKernelMonitor(kernelMonitor3);
        kernelMonitorBroadcaster.addKernelMonitor(kernelMonitor4);

        kernelMonitorBroadcaster.serviceNotificationError(SERVICE_MONITOR, SERVICE_EVENT, THROWABLE);

        assertTrue(kernelMonitor1.wasServiceNotificationErrorCalled());
        assertTrue(kernelMonitor2.wasServiceNotificationErrorCalled());
        assertTrue(kernelMonitor3.wasServiceNotificationErrorCalled());
        assertTrue(kernelMonitor4.wasServiceNotificationErrorCalled());

        kernelMonitor1.rest();
        kernelMonitor2.rest();
        kernelMonitor3.rest();
        kernelMonitor4.rest();

        kernelMonitorBroadcaster.removeKernelMonitor(kernelMonitor2);

        kernelMonitorBroadcaster.serviceNotificationError(SERVICE_MONITOR, SERVICE_EVENT, THROWABLE);

        assertTrue(kernelMonitor1.wasServiceNotificationErrorCalled());
        assertFalse(kernelMonitor2.wasServiceNotificationErrorCalled());
        assertTrue(kernelMonitor3.wasServiceNotificationErrorCalled());
        assertTrue(kernelMonitor4.wasServiceNotificationErrorCalled());
    }

    /**
     * Tests that no exceptions are thrown if an attempt is made to remove a monitor not registered.
     */
    public void testRemoveUnassociated() {
        kernelMonitorBroadcaster.removeKernelMonitor(kernelMonitor1);        
    }

    private static class MockKernelMonitor implements KernelMonitor {
        private boolean serviceNotificationErrorCalled = false;

        private void rest() {
            serviceNotificationErrorCalled = false;
        }

        public boolean wasServiceNotificationErrorCalled() {
            return serviceNotificationErrorCalled;
        }

        public void serviceNotificationError(ServiceMonitor serviceMonitor, ServiceEvent serviceEvent, Throwable throwable) {
            assertFalse(serviceNotificationErrorCalled);
            serviceNotificationErrorCalled = true;
            assertSame(SERVICE_MONITOR, serviceMonitor);
            assertSame(SERVICE_EVENT, serviceEvent);
            assertSame(THROWABLE, throwable);
        }
    }


}
