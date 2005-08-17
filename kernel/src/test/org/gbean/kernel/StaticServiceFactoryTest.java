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

import junit.framework.TestCase;

/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class StaticServiceFactoryTest extends TestCase {
    private static final Object SERVICE = new Object();
    private static final ServiceContext SERVICE_CONTEXT = new MockServiceContext();
    private static final ServiceCondition START_CONDITION = new MockStartCondition();

    public void testConstructor() {
        new StaticServiceFactory(SERVICE);
        try {
            new StaticServiceFactory(null);
            fail("new StaticServiceFactory(null) should have thrown a NullPointerException");
        } catch (NullPointerException expected) {
        }
    }

    public void testCreateService() {
        assertSame(SERVICE, new StaticServiceFactory(SERVICE).createService(SERVICE_CONTEXT));
    }

    public void testIsRestartable() {
        assertEquals(false, new StaticServiceFactory(SERVICE).isRestartable());
    }

    public void testEnabled() {
        StaticServiceFactory serviceFactory = new StaticServiceFactory(SERVICE);
        assertEquals(true, serviceFactory.isEnabled());
        serviceFactory.setEnabled(false);
        assertEquals(false, serviceFactory.isEnabled());
        serviceFactory.setEnabled(true);
        assertEquals(true, serviceFactory.isEnabled());
    }

    public void testDependency() {
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
}
