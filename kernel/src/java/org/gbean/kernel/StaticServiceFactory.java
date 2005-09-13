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

/**
 * A basic service factory that always creates the supplied object.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class StaticServiceFactory implements ServiceFactory {
    private final Object service;
    private boolean enabled = true;
    private final Set startConditions = new HashSet();
    private final Set stopConditions = new HashSet();

    /**
     * Creates a non-restartable service factory which will simply returns the specified service from the createService
     * method.
     *
     * @param service the static to which this factory "creates"
     * @throws NullPointerException if service is null
     */
    public StaticServiceFactory(Object service) throws NullPointerException {
        if (service == null) throw new NullPointerException("service is null");
        this.service = service;
    }

    public Class[] getTypes() {
        return new Class[]{service.getClass()};
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRestartable() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized boolean isEnabled() {
        return enabled;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Set getStartConditions() {
        return Collections.unmodifiableSet(new HashSet(startConditions));
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void addStartCondition(ServiceCondition startCondition) throws NullPointerException {
        if (startCondition == null) throw new NullPointerException("startCondition is null");
        startConditions.add(startCondition);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void removeStartCondition(ServiceCondition startCondition) throws NullPointerException {
        if (startCondition == null) throw new NullPointerException("startCondition is null");
        startConditions.remove(startCondition);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Set getStopConditions() {
        return Collections.unmodifiableSet(new HashSet(stopConditions));
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void addStopCondition(ServiceCondition stopCondition) throws NullPointerException {
        if (stopCondition == null) throw new NullPointerException("stopCondition is null");
        stopConditions.add(stopCondition);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void removeStopCondition(ServiceCondition stopCondition) throws NullPointerException {
        if (stopCondition == null) throw new NullPointerException("stopCondition is null");
        stopConditions.remove(stopCondition);
    }

    /**
     * {@inheritDoc}
     */
    public Set getOwnedServices() {
        return Collections.EMPTY_SET;
    }

    /**
     * Returns the static service instance.
     *
     * @param serviceContext ignored
     * @return the static service instance
     */
    public Object createService(ServiceContext serviceContext) {
        return service;
    }

    /**
     * This method is a noop.
     *
     * @param serviceContext ignored
     */
    public void destroyService(ServiceContext serviceContext) {
    }
}
