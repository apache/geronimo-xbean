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

import java.util.Set;
import java.util.TreeSet;

import junit.framework.TestCase;

/**
 * Tests the StaticServiceFactory.
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class StaticServiceFactoryTest extends TestCase {
    private static final Object SERVICE = new TreeSet();
    private static final ServiceContext SERVICE_CONTEXT = new MockServiceContext();
    private static final ServiceCondition START_CONDITION = new MockStartCondition();
    private static final ServiceCondition STOP_CONDITION = new MockStopCondition();

    /**
     * Tests that the constructor works when called with an Object and fails when called with null.
     */
    public void testConstructor() {
        new StaticServiceFactory(SERVICE);
        try {
            new StaticServiceFactory(null);
            fail("new StaticServiceFactory(null) should have thrown a NullPointerException");
        } catch (NullPointerException expected) {
        }
    }

    /**
     * Tests that create service returns the same object that was passed to the constructor.
     */
    public void testCreateService() {
        assertSame(SERVICE, new StaticServiceFactory(SERVICE).createService(SERVICE_CONTEXT));
    }

    /**
     * Tests that the service factory is not restartable.
     */
    public void testIsRestartable() {
        assertEquals(false, new StaticServiceFactory(SERVICE).isRestartable());
    }

    /**
     * Tests that geting and setting the enalbe flag.
     */
    public void testEnabled() {
        StaticServiceFactory serviceFactory = new StaticServiceFactory(SERVICE);
        assertEquals(true, serviceFactory.isEnabled());
        serviceFactory.setEnabled(false);
        assertEquals(false, serviceFactory.isEnabled());
        serviceFactory.setEnabled(true);
        assertEquals(true, serviceFactory.isEnabled());
    }

    /**
     * Tests getting and setting start and stop conditions.
     */
    public void testConditions() {
        StaticServiceFactory serviceFactory = new StaticServiceFactory(SERVICE);

        // get the dependency set
        Set dependencies = serviceFactory.getStartConditions();
        assertNotNull(dependencies);
        // it should be initially empty
        assertTrue(dependencies.isEmpty());

        serviceFactory.addStartCondition(START_CONDITION);
        // old dependency set should still be empty... it is a snapshot
        assertTrue(dependencies.isEmpty());

        // get a new dependency set
        dependencies = serviceFactory.getStartConditions();
        assertNotNull(dependencies);
        // should have our dependency in it
        assertEquals(1, dependencies.size());
        assertTrue(dependencies.contains(START_CONDITION));

        try {
            dependencies.clear();
            fail("dependencies.clear() should have thrown an Exception");
        } catch (Exception expected) {
        }

        // get the dependency set
        dependencies = serviceFactory.getStopConditions();
        assertNotNull(dependencies);
        // it should be initially empty
        assertTrue(dependencies.isEmpty());

        serviceFactory.addStopCondition(STOP_CONDITION);
        // old dependency set should still be empty... it is a snapshot
        assertTrue(dependencies.isEmpty());

        // get a new dependency set
        dependencies = serviceFactory.getStopConditions();
        assertNotNull(dependencies);
        // should have our dependency in it
        assertEquals(1, dependencies.size());
        assertTrue(dependencies.contains(STOP_CONDITION));

        try {
            dependencies.clear();
            fail("dependencies.clear() should have thrown an Exception");
        } catch (Exception expected) {
        }
    }

    /**
     * Tests that getTypes returns an array containing a single class which is the class of the service passed to the constuctor.
     */
    public void testGetTypes() {
        StaticServiceFactory serviceFactory = new StaticServiceFactory(SERVICE);
        Class[] types = serviceFactory.getTypes();
        assertNotNull(types);
        assertEquals(1, types.length);
        assertSame(SERVICE.getClass(), types[0]);
    }

    private static class MockServiceContext implements ServiceContext {
        public Kernel getKernel() {
            throw new UnsupportedOperationException();
        }

        public ServiceName getServiceName() {
            throw new UnsupportedOperationException();
        }

        public ClassLoader getClassLoader() {
            throw new UnsupportedOperationException();
        }
    }

    private static class MockStartCondition implements ServiceCondition {
        public void initialize(ServiceConditionContext context) {
            throw new UnsupportedOperationException();
        }

        public boolean isSatisfied() {
            throw new UnsupportedOperationException();
        }

        public void destroy() {
            throw new UnsupportedOperationException();
        }
    }

    private static class MockStopCondition implements ServiceCondition {
        public void initialize(ServiceConditionContext context) {
            throw new UnsupportedOperationException();
        }

        public boolean isSatisfied() {
            throw new UnsupportedOperationException();
        }

        public void destroy() {
            throw new UnsupportedOperationException();
        }
    }
}
