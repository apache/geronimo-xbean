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
package org.xbean.kernel.standard;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import junit.framework.TestCase;
import org.xbean.kernel.KernelErrorsError;
import org.xbean.kernel.KernelMonitor;
import org.xbean.kernel.NullServiceMonitor;
import org.xbean.kernel.ServiceEvent;
import org.xbean.kernel.ServiceMonitor;
import org.xbean.kernel.ServiceName;
import org.xbean.kernel.StaticServiceFactory;
import org.xbean.kernel.StringServiceName;

/**
 * Tests ServiceMonitorBroadcaster.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class ServiceMonitorBroadcasterTest extends TestCase {
    private static final StandardKernel KERNEL = new StandardKernel("test");
    private static final StaticServiceFactory SERVICE_FACTORY = new StaticServiceFactory(new Object());
    private static final ClassLoader SYSTEM_CLASS_LOADER = ClassLoader.getSystemClassLoader();

    private static final int SERVICE_REGISTERED = 1 << 0;
    private static final int SERVICE_STARTING = 1 << 1;
    private static final int SERVICE_WAITING_TO_START = 1 << 2;
    private static final int SERVICE_START_ERROR = 1 << 3;
    private static final int SERVICE_RUNNING = 1 << 4;
    private static final int SERVICE_STOPPING = 1 << 5;
    private static final int SERVICE_WAITING_TO_STOP = 1 << 6;
    private static final int SERVICE_STOP_ERROR = 1 << 7;
    private static final int SERVICE_STOPPED = 1 << 8;
    private static final int SERVICE_UNREGISTERED = 1 << 9;
    private static final int[] NOTIFICATION_TYPES = new int[]{
        SERVICE_REGISTERED,
        SERVICE_STARTING,
        SERVICE_WAITING_TO_START,
        SERVICE_START_ERROR,
        SERVICE_RUNNING,
        SERVICE_STOPPING,
        SERVICE_WAITING_TO_STOP,
        SERVICE_STOP_ERROR,
        SERVICE_STOPPED,
        SERVICE_UNREGISTERED,
    };


    private static final int SERVICE_COUNT = 4;
    private static final ServiceName[] serviceName = new ServiceName[SERVICE_COUNT];
    private static final ServiceEvent[] serviceEvent = new ServiceEvent[SERVICE_COUNT];
    private final Set[] expectedMonitors = new Set[SERVICE_COUNT];
    private final Set[] unexpectedMonitors = new Set[SERVICE_COUNT];

    private final MockKernelMonitor kernelMonitor = new MockKernelMonitor();
    private final ServiceMonitorBroadcaster serviceMonitorBroadcaster = new ServiceMonitorBroadcaster(kernelMonitor);
    private MockServiceMonitor[] globalMonitors;
    private MockServiceMonitor[][] serviceMonitors;
    private MockServiceMonitor evenMonitor;
    private MockServiceMonitor oddMonitor;

    private Throwable throwable;
    private Throwable kernelThrowable;

    /**
     * Fires all events on every service and verifies.
     */
    public void testFireEvent() {
        fireAllEvents();
    }

    /**
     * Test that if a monitor is added more then once it only recieves the event once.
     * Strategy:
     * <ul><li>
     * Add a monitor several times
     * </li><li>
     * Fire all events and verify
     * </li></ul>
     */
    public void testMultipleAdd() {
        serviceMonitorBroadcaster.addServiceMonitor(globalMonitors[0], null);
        serviceMonitorBroadcaster.addServiceMonitor(globalMonitors[0], null);
        serviceMonitorBroadcaster.addServiceMonitor(globalMonitors[0], null);
        serviceMonitorBroadcaster.addServiceMonitor(serviceMonitors[1][0], serviceName[1]);
        serviceMonitorBroadcaster.addServiceMonitor(serviceMonitors[1][0], serviceName[1]);
        serviceMonitorBroadcaster.addServiceMonitor(serviceMonitors[1][0], serviceName[1]);

        fireAllEvents();
    }

    /**
     * Test that events are not fired to a monitor that has been removed.
     * Strategy:
     * </li><li>
     * Fire all events and verify
     * </li><li>
     * Removes some monitors
     * </li><li>
     * Fire all events and verify
     * </li></ul>
     */
    public void testRemove() {
        fireAllEvents();

        serviceMonitorBroadcaster.removeServiceMonitor(globalMonitors[0]);
        for (int i = 0; i < expectedMonitors.length; i++) {
            Set expectedMonitor = expectedMonitors[i];
            expectedMonitor.remove(globalMonitors[0]);
        }
        for (int i = 0; i < unexpectedMonitors.length; i++) {
            Set expectedMonitor = unexpectedMonitors[i];
            expectedMonitor.add(globalMonitors[0]);
        }

        serviceMonitorBroadcaster.removeServiceMonitor(serviceMonitors[1][0]);
        expectedMonitors[1].remove(serviceMonitors[1][0]);
        unexpectedMonitors[1].add(serviceMonitors[1][0]);

        fireAllEvents();
    }

    /**
     * Tests that no exceptions are thrown if an attempt is made to remove a monitor not registered.
     */
    public void testRemoveUnassociated() {
        serviceMonitorBroadcaster.removeServiceMonitor(new NullServiceMonitor());
    }

    /**
     * Test that when a service monitor throws a RuntimeException the kernel monitor is notified.
     * Strategy:
     * </li><li>
     * Set all monitors to throw a RuntimeException
     * </li><li>
     * Fire all events and verify
     * </li></ul>
     */
    public void testNotificationRuntimeException() {
        throwable = new RuntimeException("notification exception");
        fireAllEvents();
    }

    /**
     * Test that when a service monitor throws an Error the kernel monitor is notified.
     * Strategy:
     * </li><li>
     * Set all monitors to throw an Error
     * </li><li>
     * Fire all events and verify
     * </li></ul>
     */
    public void testNotificationError() {
        throwable = new Error("notification error");
        fireAllEvents();
    }

    /**
     * Tests that when a kernel monitor throws a RuntimeException that it is ignored.
     * </li><li>
     * Set all service monitors to throw a RuntimeException
     * </li><li>
     * Set kernel monitor to throw a RuntimeException
     * </li><li>
     * Fire all events and verify
     * </li></ul>
     */
    public void testKernelMonitorException() {
        throwable = new RuntimeException("notification exception");
        kernelThrowable = new RuntimeException("kernel exception");
        fireAllEvents();
    }

    /**
     * Tests that when a kernel monitor throws an Error that it is propagated back to call in a KernelErrorsError.
     * </li><li>
     * Set all service monitors to throw a RuntimeException
     * </li><li>
     * Set kernel monitor to throw an Error
     * </li><li>
     * Fire all events and verify
     * </li></ul>
     */
    public void testKernelMonitorError() {
        throwable = new RuntimeException("notification exception");
        kernelThrowable = new Error("kernel error");
        fireAllEvents();
    }

    private void fireAllEvents() {
        for (int serviceId = 0; serviceId < serviceEvent.length; serviceId++) {
            for (int j = 0; j < NOTIFICATION_TYPES.length; j++) {
                int notificationType = NOTIFICATION_TYPES[j];
                initMonitors(serviceId, throwable);
                fireEvent(serviceId, notificationType);
                assertNotificationCorrect(serviceId, notificationType);
            }
        }
    }

    private void initMonitors(int serviceId, Throwable throwable) {
        if (throwable != null) {
            kernelMonitor.init(expectedMonitors[serviceId], serviceEvent[serviceId], throwable, kernelThrowable);
        } else {
            kernelMonitor.init(Collections.EMPTY_SET, null, null, null);

        }
        for (Iterator iterator = expectedMonitors[serviceId].iterator(); iterator.hasNext();) {
            ((MockServiceMonitor) iterator.next()).init(serviceEvent[serviceId], throwable);
        }
        for (Iterator iterator = unexpectedMonitors[serviceId].iterator(); iterator.hasNext();) {
            ((MockServiceMonitor) iterator.next()).init(null, throwable);
        }
    }

    private void fireEvent(int serviceId, int notificationType) {
        try {
            if (notificationType == SERVICE_REGISTERED) {
                serviceMonitorBroadcaster.serviceRegistered(serviceEvent[serviceId]);
            } else if (notificationType == SERVICE_STARTING) {
                serviceMonitorBroadcaster.serviceStarting(serviceEvent[serviceId]);
            } else if (notificationType == SERVICE_WAITING_TO_START) {
                serviceMonitorBroadcaster.serviceWaitingToStart(serviceEvent[serviceId]);
            } else if (notificationType == SERVICE_START_ERROR) {
                serviceMonitorBroadcaster.serviceStartError(serviceEvent[serviceId]);
            } else if (notificationType == SERVICE_RUNNING) {
                serviceMonitorBroadcaster.serviceRunning(serviceEvent[serviceId]);
            } else if (notificationType == SERVICE_STOPPING) {
                serviceMonitorBroadcaster.serviceStopping(serviceEvent[serviceId]);
            } else if (notificationType == SERVICE_WAITING_TO_STOP) {
                serviceMonitorBroadcaster.serviceWaitingToStop(serviceEvent[serviceId]);
            } else if (notificationType == SERVICE_STOP_ERROR) {
                serviceMonitorBroadcaster.serviceStopError(serviceEvent[serviceId]);
            } else if (notificationType == SERVICE_STOPPED) {
                serviceMonitorBroadcaster.serviceStopped(serviceEvent[serviceId]);
            } else if (notificationType == SERVICE_UNREGISTERED) {
                serviceMonitorBroadcaster.serviceUnregistered(serviceEvent[serviceId]);
            } else {
                fail("Unknown notification type " + notificationType);
            }
        } catch (KernelErrorsError e) {
            assertEquals(e.getErrors().size(), expectedMonitors[serviceId].size());
            for (Iterator iterator = e.getErrors().iterator(); iterator.hasNext();) {
                assertEquals(kernelThrowable, iterator.next());
            }
        }
    }

    private void assertNotificationCorrect(int serviceId, int notificationType) {
        for (Iterator iterator = expectedMonitors[serviceId].iterator(); iterator.hasNext();) {
            MockServiceMonitor mockServiceMonitor = (MockServiceMonitor) iterator.next();
            assertEquals(notificationType, mockServiceMonitor.getCalled());
        }
        for (Iterator iterator = unexpectedMonitors[serviceId].iterator(); iterator.hasNext();) {
            MockServiceMonitor mockServiceMonitor = (MockServiceMonitor) iterator.next();
            assertEquals(0, mockServiceMonitor.getCalled());
        }
        assertTrue("Unfired service errors " + kernelMonitor.getExpectedServiceMonitors(), kernelMonitor.getExpectedServiceMonitors().isEmpty());
    }

    protected void setUp() throws Exception {
        super.setUp();

        // serviceNames
        for (int i = 0; i < serviceName.length; i++) {
            serviceName[i] = new StringServiceName("service-" + i);
        }

        // serviceEvents
        for (int i = 0; i < serviceEvent.length; i++) {
            serviceEvent[i] = createEvent(serviceName[i]);
        }

        // all monitors
        Set allMonitors = new LinkedHashSet();

        // global
        globalMonitors = createMonitors(2, "global");
        allMonitors.addAll(Arrays.asList(globalMonitors));
        for (int i = 0; i < globalMonitors.length; i++) {
            MockServiceMonitor gloalMonitor = globalMonitors[i];
            serviceMonitorBroadcaster.addServiceMonitor(gloalMonitor, null);
        }

        // service specific
        serviceMonitors = new MockServiceMonitor[SERVICE_COUNT][];
        for (int i = 0; i < serviceMonitors.length; i++) {
            serviceMonitors[i] = createMonitors(i, "service" + i);
            allMonitors.addAll(Arrays.asList(serviceMonitors[i]));
            for (int j = 0; j < serviceMonitors[i].length; j++) {
                MockServiceMonitor serviceMonitor = serviceMonitors[i][j];
                serviceMonitorBroadcaster.addServiceMonitor(serviceMonitor, serviceName[i]);
            }
        }

        // even numbered services
        evenMonitor = new MockServiceMonitor("even");
        allMonitors.add(evenMonitor);
        for (int i = 0; i < serviceName.length; i++) {
            if (i % 2 == 0) {
                serviceMonitorBroadcaster.addServiceMonitor(evenMonitor, serviceName[i]);
            }

        }

        // odd numbered services
        oddMonitor = new MockServiceMonitor("odd");
        allMonitors.add(oddMonitor);
        for (int i = 0; i < serviceName.length; i++) {
            if (i % 2 != 0) {
                serviceMonitorBroadcaster.addServiceMonitor(oddMonitor, serviceName[i]);
            }

        }

        // expected monitors
        for (int i = 0; i < expectedMonitors.length; i++) {
            expectedMonitors[i] = new LinkedHashSet();
            expectedMonitors[i].addAll(Arrays.asList(globalMonitors));
            expectedMonitors[i].addAll(Arrays.asList(serviceMonitors[i]));
            if (i % 2 == 0) {
                expectedMonitors[i].add(evenMonitor);
            } else {
                expectedMonitors[i].add(oddMonitor);
            }
        }

        // unexpected monitors
        for (int i = 0; i < unexpectedMonitors.length; i++) {
            unexpectedMonitors[i] = new LinkedHashSet(allMonitors);
            unexpectedMonitors[i].removeAll(expectedMonitors[i]);
        }
    }

    private MockServiceMonitor[] createMonitors(int count, String name) {
        MockServiceMonitor[] gloalMonitors = new MockServiceMonitor[count];
        for (int i = 0; i < gloalMonitors.length; i++) {
            gloalMonitors[i] = new MockServiceMonitor(name + "-" + i);
        }
        return gloalMonitors;
    }

    private static ServiceEvent createEvent(ServiceName serviceName) {
        return new ServiceEvent(0,
                KERNEL,
                serviceName,
                SERVICE_FACTORY,
                SYSTEM_CLASS_LOADER,
                null,
                null,
                null);
    }

    private static class MockServiceMonitor implements ServiceMonitor {
        private final String name;
        private int called;
        private ServiceEvent expectedEvent;
        private Throwable throwable;

        private MockServiceMonitor(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }

        private void init(ServiceEvent expectedEvent, Throwable throwable) {
            this.expectedEvent = expectedEvent;
            this.throwable = throwable;
            called = 0;
        }

        public int getCalled() {
            return called;
        }

        public void serviceRegistered(ServiceEvent serviceEvent) {
            assertTrue("alread called", (called & SERVICE_REGISTERED) == 0);
            called |= SERVICE_REGISTERED;
            assertNotNull("call not expected", expectedEvent);
            assertSame("wrong event object", expectedEvent, serviceEvent);
            throwIfNecessary();
        }

        public void serviceStarting(ServiceEvent serviceEvent) {
            assertTrue("alread called", (called & SERVICE_STARTING) == 0);
            called |= SERVICE_STARTING;
            assertNotNull("call not expected", expectedEvent);
            assertSame("wrong event object", expectedEvent, serviceEvent);
            throwIfNecessary();
        }

        public void serviceWaitingToStart(ServiceEvent serviceEvent) {
            assertTrue("alread called", (called & SERVICE_WAITING_TO_START) == 0);
            called |= SERVICE_WAITING_TO_START;
            assertNotNull("call not expected", expectedEvent);
            assertSame("wrong event object", expectedEvent, serviceEvent);
            throwIfNecessary();
        }

        public void serviceStartError(ServiceEvent serviceEvent) {
            assertTrue("alread called", (called & SERVICE_START_ERROR) == 0);
            called |= SERVICE_START_ERROR;
            assertNotNull("call not expected", expectedEvent);
            assertSame("wrong event object", expectedEvent, serviceEvent);
            throwIfNecessary();
        }

        public void serviceRunning(ServiceEvent serviceEvent) {
            assertTrue("alread called", (called & SERVICE_RUNNING) == 0);
            called |= SERVICE_RUNNING;
            assertNotNull("call not expected", expectedEvent);
            assertSame("wrong event object", expectedEvent, serviceEvent);
            throwIfNecessary();
        }

        public void serviceStopping(ServiceEvent serviceEvent) {
            assertTrue("alread called", (called & SERVICE_STOPPING) == 0);
            called |= SERVICE_STOPPING;
            assertNotNull("call not expected", expectedEvent);
            assertSame("wrong event object", expectedEvent, serviceEvent);
            throwIfNecessary();
        }

        public void serviceWaitingToStop(ServiceEvent serviceEvent) {
            assertTrue("alread called", (called & SERVICE_WAITING_TO_STOP) == 0);
            called |= SERVICE_WAITING_TO_STOP;
            assertNotNull("call not expected", expectedEvent);
            assertSame("wrong event object", expectedEvent, serviceEvent);
            throwIfNecessary();
        }

        public void serviceStopError(ServiceEvent serviceEvent) {
            assertTrue("alread called", (called & SERVICE_STOP_ERROR) == 0);
            called |= SERVICE_STOP_ERROR;
            assertNotNull("call not expected", expectedEvent);
            assertSame("wrong event object", expectedEvent, serviceEvent);
            throwIfNecessary();
        }

        public void serviceStopped(ServiceEvent serviceEvent) {
            assertTrue("alread called", (called & SERVICE_STOPPED) == 0);
            called |= SERVICE_STOPPED;
            assertNotNull("call not expected", expectedEvent);
            assertSame("wrong event object", expectedEvent, serviceEvent);
            throwIfNecessary();
        }

        public void serviceUnregistered(ServiceEvent serviceEvent) {
            assertTrue("alread called", (called & SERVICE_UNREGISTERED) == 0);
            called |= SERVICE_UNREGISTERED;
            assertNotNull("call not expected", expectedEvent);
            assertSame("wrong event object", expectedEvent, serviceEvent);
            throwIfNecessary();
        }

        private void throwIfNecessary() {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            } else if (throwable instanceof Error) {
                throw (Error) throwable;
            } else {
                assertNull(throwable);
            }
        }
    }

    private static class MockKernelMonitor implements KernelMonitor {
        private Set expectedServiceMonitors;
        private ServiceEvent expectedEvent;
        private Throwable expectedThrowable;
        private Throwable kernelThrowable;

        private void init(Set expectedServiceMonitors, ServiceEvent expectedEvent, Throwable expectedThrowable, Throwable kernelThrowable) {
            this.expectedServiceMonitors = new LinkedHashSet(expectedServiceMonitors);
            this.expectedEvent = expectedEvent;
            this.expectedThrowable = expectedThrowable;
            this.kernelThrowable = kernelThrowable;
        }

        public Set getExpectedServiceMonitors() {
            return expectedServiceMonitors;
        }

        public void serviceNotificationError(ServiceMonitor serviceMonitor, ServiceEvent serviceEvent, Throwable throwable) {
            assertNotNull(expectedEvent);
            assertTrue(expectedServiceMonitors.contains(serviceMonitor));
            expectedServiceMonitors.remove(serviceMonitor);
            assertSame(expectedEvent, serviceEvent);
            assertSame(expectedThrowable, throwable);
            throwIfNecessary();
        }

        private void throwIfNecessary() {
            if (kernelThrowable instanceof RuntimeException) {
                throw (RuntimeException) kernelThrowable;
            } else if (kernelThrowable instanceof Error) {
                throw (Error) kernelThrowable;
            } else {
                assertNull(kernelThrowable);
            }
        }
    }
}
