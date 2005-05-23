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
package org.gbean.geronimo;

import java.util.Set;
import java.util.Collection;
import javax.management.ObjectName;

import org.gbean.kernel.DependencyManager;

/**
 * @version $Revision$ $Date$
 */
public class DependencyManagerBridge implements org.apache.geronimo.kernel.DependencyManager {
    private final DependencyManager dependencyManager;

    public DependencyManagerBridge(DependencyManager dependencyManager) {
        this.dependencyManager = dependencyManager;
    }

    public void close() {
    }

    public void addDependency(ObjectName child, ObjectName parent) {
        dependencyManager.addDependency(child, parent);
    }

    public void removeDependency(ObjectName child, ObjectName parent) {
        dependencyManager.removeDependency(child, parent);
    }

    public void removeAllDependencies(ObjectName child) {
        dependencyManager.removeAllDependencies(child);
    }

    public void addDependencies(ObjectName child, Set parents) {
        dependencyManager.addDependencies(child, parents);
    }

    public Set getParents(ObjectName child) {
        return dependencyManager.getParents(child);
    }

    public Set getChildren(ObjectName parent) {
        return dependencyManager.getChildren(parent);
    }

    public void addStartHolds(ObjectName objectName, Collection holds) {
        dependencyManager.addStartHolds(objectName, holds);
    }

    public void removeStartHolds(ObjectName objectName, Collection holds) {
        dependencyManager.removeStartHolds(objectName, holds);
    }

    public void removeAllStartHolds(ObjectName objectName) {
        dependencyManager.removeAllStartHolds(objectName);
    }

    public ObjectName checkBlocker(ObjectName objectName) {
        return dependencyManager.checkBlocker(objectName);
    }
}
