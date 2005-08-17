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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class ServiceDependencyRegistryTest extends TestCase {
    private final ServiceDependencyRegistry serviceDependencyRegistry = new ServiceDependencyRegistry();

    private final ServiceName parent1 = new StringServiceName("parent1");
    private final ServiceName parent2 = new StringServiceName("parent2");
    private final ServiceName parent3 = new StringServiceName("parent3");

    private final ServiceName child1 = new StringServiceName("child1");
    private final ServiceName child2 = new StringServiceName("child2");
    private final ServiceName child3 = new StringServiceName("child3");

    private final Set parent1Children = new HashSet();
    private final Set parent2Children = new HashSet();
    private final Set parent3Children = new HashSet();

    private final Set child1Parents = new HashSet();
    private final Set child2Parents = new HashSet();
    private final Set child3Parents = new HashSet();

    public void testDependencies() {
        assertEquals(child1Parents, serviceDependencyRegistry.getMyDependencies(child1));
        assertEquals(child2Parents, serviceDependencyRegistry.getMyDependencies(child2));
        assertEquals(child3Parents, serviceDependencyRegistry.getMyDependencies(child3));

        assertEquals(parent1Children, serviceDependencyRegistry.getDependenciesOnMe(parent1));
        assertEquals(parent2Children, serviceDependencyRegistry.getDependenciesOnMe(parent2));
        assertEquals(parent3Children, serviceDependencyRegistry.getDependenciesOnMe(parent3));
    }

    public void testRemoveDependencies() {
        serviceDependencyRegistry.unregisterDependencies(child1);

        assertEquals(Collections.EMPTY_SET, serviceDependencyRegistry.getMyDependencies(child1));
        assertEquals(child2Parents, serviceDependencyRegistry.getMyDependencies(child2));
        assertEquals(child3Parents, serviceDependencyRegistry.getMyDependencies(child3));

        parent1Children.remove(child1);
        assertEquals(parent1Children, serviceDependencyRegistry.getDependenciesOnMe(parent1));
        parent2Children.remove(child1);
        assertEquals(parent2Children, serviceDependencyRegistry.getDependenciesOnMe(parent2));
        parent3Children.remove(child1);
        assertEquals(parent3Children, serviceDependencyRegistry.getDependenciesOnMe(parent3));
    }

    protected void setUp() throws Exception {
        super.setUp();
        parent1Children.add(child1);
        parent1Children.add(child2);
        parent1Children.add(child3);

        parent2Children.add(child2);
        parent2Children.add(child3);

        parent3Children.add(child3);

        child1Parents.add(parent1);

        child2Parents.add(parent1);
        child2Parents.add(parent2);

        child3Parents.add(parent1);
        child3Parents.add(parent2);
        child3Parents.add(parent3);

        serviceDependencyRegistry.registerDependencies(child1, child1Parents);
        serviceDependencyRegistry.registerDependencies(child2, child2Parents);
        serviceDependencyRegistry.registerDependencies(child3, child3Parents);
    }
}
