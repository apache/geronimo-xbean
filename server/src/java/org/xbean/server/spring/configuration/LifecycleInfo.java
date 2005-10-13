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
package org.xbean.server.spring.configuration;

/**
 * LifecycleInfo defines the init and destroy method names for a lifecycle interface.
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class LifecycleInfo {
    private Class type;
    private String initMethodName;
    private String destroyMethodName;

    /**
     * Creates a new empty lifecycle info.  Note, this instance is unusable until the type, inti method name and
     * destroy method name are set.
     */
    public LifecycleInfo() {
    }

    /**
     * Creates a new lifecycle info object for the specified interface and defining the init and destroy method names.
     * @param type the lifecycle interface
     * @param initMethodName the init method name
     * @param destroyMethodName the destroy method name
     */
    public LifecycleInfo(Class type, String initMethodName, String destroyMethodName) {
        this.type = type;
        this.initMethodName = initMethodName;
        this.destroyMethodName = destroyMethodName;
    }

    /**
     * Gets the lifecycle interface type.
     * @return the lifecycle interface type
     */
    public Class getType() {
        return type;
    }

    /**
     * Sets the lifecycle interface type.
     * @param type the lifecycle interface type
     */
    public void setType(Class type) {
        this.type = type;
    }

    /**
     * Gets the init method name.
     * @return the init method name
     */
    public String getInitMethodName() {
        return initMethodName;
    }

    /**
     * Sets the init method name.
     * @param initMethodName the init method name
     */
    public void setInitMethodName(String initMethodName) {
        this.initMethodName = initMethodName;
    }

    /**
     * Gets the destroy method name.
     * @return the destroy method name
     */
    public String getDestroyMethodName() {
        return destroyMethodName;
    }

    /**
     * Sets the destroy method name.
     * @param destroyMethodName the destroy method name
     */
    public void setDestroyMethodName(String destroyMethodName) {
        this.destroyMethodName = destroyMethodName;
    }
}
