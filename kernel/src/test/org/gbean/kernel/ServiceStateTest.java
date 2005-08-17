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

import java.rmi.MarshalledObject;

import junit.framework.TestCase;

/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class ServiceStateTest extends TestCase {
    public void testEquals() {
        assertTrue(ServiceState.STARTING.equals(ServiceState.STARTING));
        assertFalse(ServiceState.STARTING.equals(ServiceState.RUNNING));
        assertFalse(ServiceState.STARTING.equals(ServiceState.STOPPING));
        assertFalse(ServiceState.STARTING.equals(ServiceState.STOPPED));

        assertFalse(ServiceState.RUNNING.equals(ServiceState.STARTING));
        assertTrue(ServiceState.RUNNING.equals(ServiceState.RUNNING));
        assertFalse(ServiceState.RUNNING.equals(ServiceState.STOPPING));
        assertFalse(ServiceState.RUNNING.equals(ServiceState.STOPPED));

        assertFalse(ServiceState.STOPPING.equals(ServiceState.STARTING));
        assertFalse(ServiceState.STOPPING.equals(ServiceState.RUNNING));
        assertTrue(ServiceState.STOPPING.equals(ServiceState.STOPPING));
        assertFalse(ServiceState.STOPPING.equals(ServiceState.STOPPED));

        assertFalse(ServiceState.STOPPED.equals(ServiceState.STARTING));
        assertFalse(ServiceState.STOPPED.equals(ServiceState.RUNNING));
        assertFalse(ServiceState.STOPPED.equals(ServiceState.STOPPING));
        assertTrue(ServiceState.STOPPED.equals(ServiceState.STOPPED));
    }

    public void testHashCode() {
        assertTrue(ServiceState.STARTING.hashCode() == ServiceState.STARTING.hashCode());
        assertFalse(ServiceState.STARTING.hashCode() == ServiceState.RUNNING.hashCode());
        assertFalse(ServiceState.STARTING.hashCode() == ServiceState.STOPPING.hashCode());
        assertFalse(ServiceState.STARTING.hashCode() == ServiceState.STOPPED.hashCode());

        assertFalse(ServiceState.RUNNING.hashCode() == ServiceState.STARTING.hashCode());
        assertTrue(ServiceState.RUNNING.hashCode() == ServiceState.RUNNING.hashCode());
        assertFalse(ServiceState.RUNNING.hashCode() == ServiceState.STOPPING.hashCode());
        assertFalse(ServiceState.RUNNING.hashCode() == ServiceState.STOPPED.hashCode());

        assertFalse(ServiceState.STOPPING.hashCode() == ServiceState.STARTING.hashCode());
        assertFalse(ServiceState.STOPPING.hashCode() == ServiceState.RUNNING.hashCode());
        assertTrue(ServiceState.STOPPING.hashCode() == ServiceState.STOPPING.hashCode());
        assertFalse(ServiceState.STOPPING.hashCode() == ServiceState.STOPPED.hashCode());

        assertFalse(ServiceState.STOPPED.hashCode() == ServiceState.STARTING.hashCode());
        assertFalse(ServiceState.STOPPED.hashCode() == ServiceState.RUNNING.hashCode());
        assertFalse(ServiceState.STOPPED.hashCode() == ServiceState.STOPPING.hashCode());
        assertTrue(ServiceState.STOPPED.hashCode() == ServiceState.STOPPED.hashCode());
    }

    public void testGetServiceState() {
        assertSame(ServiceState.STARTING, ServiceState.getServiceState(ServiceState.STARTING.getIndex()));
        assertSame(ServiceState.RUNNING, ServiceState.getServiceState(ServiceState.RUNNING.getIndex()));
        assertSame(ServiceState.STOPPING, ServiceState.getServiceState(ServiceState.STOPPING.getIndex()));
        assertSame(ServiceState.STOPPED, ServiceState.getServiceState(ServiceState.STOPPED.getIndex()));

        try {
            ServiceState.getServiceState(ServiceState.STOPPED.getIndex() + 1);
            fail("ServiceState.getServiceState(ServiceState.STOPPED.getIndex() + 1) should have thrown and exception");
        } catch (IllegalArgumentException expected) {
            // expected
        }

        try {
            ServiceState.getServiceState(ServiceState.STARTING.getIndex() - 1);
            fail("ServiceState.getServiceState(ServiceState.STARTING.getIndex() - 1) should have thrown and exception");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    public void testFromString() {
        assertSame(ServiceState.STARTING, ServiceState.parseServiceState(ServiceState.STARTING.getName()));
        assertSame(ServiceState.RUNNING, ServiceState.parseServiceState(ServiceState.RUNNING.getName()));
        assertSame(ServiceState.STOPPING, ServiceState.parseServiceState(ServiceState.STOPPING.getName()));
        assertSame(ServiceState.STOPPED, ServiceState.parseServiceState(ServiceState.STOPPED.getName()));

        try {
            ServiceState.parseServiceState("donkey");
            fail("ServiceState.parseServiceState(\"donkey\") should have thrown and exception");
        } catch (IllegalArgumentException expected) {
            // expected
        }

        assertSame(ServiceState.STARTING, ServiceState.parseServiceState("StARting"));
        assertSame(ServiceState.RUNNING, ServiceState.parseServiceState("running"));
        assertSame(ServiceState.STOPPING, ServiceState.parseServiceState("stoPPing"));
        assertSame(ServiceState.STOPPED, ServiceState.parseServiceState("StoppeD"));
    }

    public void testSerialization() throws Exception {
        assertSame(ServiceState.STARTING, copyServiceState(ServiceState.STARTING));
        assertSame(ServiceState.RUNNING, copyServiceState(ServiceState.RUNNING));
        assertSame(ServiceState.STOPPING, copyServiceState(ServiceState.STOPPING));
        assertSame(ServiceState.STOPPED, copyServiceState(ServiceState.STOPPED));
    }

    private ServiceState copyServiceState(ServiceState original) throws Exception {
        MarshalledObject marshalledObject = new MarshalledObject(original);
        return (ServiceState) marshalledObject.get();
    }
}
