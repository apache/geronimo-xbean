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
import org.gbean.kernel.standard.KernelMonitorBroadcaster;

/**
 * Tests the KernelMonitorBroadcaster.
 *
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
            ClassLoader.getSystemClassLoader(), 
            null,
            null,
            null);
    private static final Throwable THROWABLE = new Throwable("test throwable");

    private static final int MONITOR_COUNT = 4;
    private MockKernelMonitor[] kernelMonitors = new MockKernelMonitor[MONITOR_COUNT];
    private KernelMonitorBroadcaster kernelMonitorBroadcaster = new KernelMonitorBroadcaster();

    protected void setUp() throws Exception {
        super.setUp();
        for (int i = 0; i < kernelMonitors.length; i++) {
            kernelMonitors[i] = new MockKernelMonitor("monitor-" + i);
            kernelMonitorBroadcaster.addKernelMonitor(kernelMonitors[i]);
        }
    }

    /**
     * Test that events are fired to all registered monitors.
     * Strategy:
     * <ul><li>
     * Add the monitors
     * </li><li>
     * Fire an event
     * </li><li>
     * Verify that the event was called on each monitor
     * </li></ul>
     */
    public void testFireEvent() {
        kernelMonitorBroadcaster.serviceNotificationError(SERVICE_MONITOR, SERVICE_EVENT, THROWABLE);
        assertNotificationCorrect();
    }

    private void assertNotificationCorrect() {
        for (int i = 0; i < kernelMonitors.length; i++) {
            MockKernelMonitor kernelMonitor = kernelMonitors[i];
            assertTrue(kernelMonitor.wasNotificationCalled());
        }
    }

    /**
     * Test that if a monitor is added more then once it only recieves the event once.
     * Strategy:
     * <ul><li>
     * Add a monitor several times
     * </li><li>
     * Fire an event
     * </li><li>
     * Verify that the monitor was only notified once
     * </li></ul>
     */
    public void testDoubleAdd() {
        kernelMonitorBroadcaster.addKernelMonitor(kernelMonitors[1]);
        kernelMonitorBroadcaster.addKernelMonitor(kernelMonitors[1]);
        kernelMonitorBroadcaster.addKernelMonitor(kernelMonitors[1]);

        kernelMonitorBroadcaster.serviceNotificationError(SERVICE_MONITOR, SERVICE_EVENT, THROWABLE);

        // note the mock monitor asserts that it is only called once before requreing a reset
        assertNotificationCorrect();
    }

    /**
     * Test that events are not fired to a monitor that has been removed.
     * Strategy:
     * <ul><li>
     * Add the monitors
     * </li><li>
     * Fire an event
     * </li><li>
     * Verify that the event was called on each monitor
     * </li><li>
     * Removes a monitors
     * </li><li>
     * Fire an event
     * </li><li>
     * Verify that the event was called on each monitor except the removed monitor
     * </li></ul>
     */
    public void testRemove() {
        kernelMonitorBroadcaster.serviceNotificationError(SERVICE_MONITOR, SERVICE_EVENT, THROWABLE);

        assertNotificationCorrect();

        for (int i = 0; i < kernelMonitors.length; i++) {
            MockKernelMonitor kernelMonitor = kernelMonitors[i];
            kernelMonitor.rest();
        }

        kernelMonitorBroadcaster.removeKernelMonitor(kernelMonitors[1]);

        kernelMonitorBroadcaster.serviceNotificationError(SERVICE_MONITOR, SERVICE_EVENT, THROWABLE);

        for (int i = 0; i < kernelMonitors.length; i++) {
            MockKernelMonitor kernelMonitor = kernelMonitors[i];
            if (i == 1) {
                assertFalse(kernelMonitor.wasNotificationCalled());
            } else {
                assertTrue(kernelMonitor.wasNotificationCalled());
            }
        }
    }

    /**
     * Tests that no exceptions are thrown if an attempt is made to remove a monitor not registered.
     */
    public void testRemoveUnassociated() {
        kernelMonitorBroadcaster.removeKernelMonitor(new MockKernelMonitor("unassociated monitor"));
    }

    private static class MockKernelMonitor implements KernelMonitor {
        private final String name;
        private boolean notificationCalled = false;

        private MockKernelMonitor(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }

        private void rest() {
            notificationCalled = false;
        }

        public boolean wasNotificationCalled() {
            return notificationCalled;
        }

        public void serviceNotificationError(ServiceMonitor serviceMonitor, ServiceEvent serviceEvent, Throwable throwable) {
            assertFalse(notificationCalled);
            notificationCalled = true;
            assertSame(SERVICE_MONITOR, serviceMonitor);
            assertSame(SERVICE_EVENT, serviceEvent);
            assertSame(THROWABLE, throwable);
        }
    }
}