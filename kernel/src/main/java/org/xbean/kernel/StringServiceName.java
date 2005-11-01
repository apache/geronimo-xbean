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
package org.xbean.kernel;

/**
 * A simple service name containing a single String.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class StringServiceName implements ServiceName {
    /**
     * The strang name of the service.
     */
    private final String name;

    /**
     * Create a StringServiceName wrapping specified name.
     *
     * @param name the name of the service
     */
    public StringServiceName(String name) {
        if (name == null) throw new NullPointerException("name is null");
        if (name.length() == 0) throw new IllegalArgumentException("name must be atleast one character long");
        this.name = name;
    }

    public int hashCode() {
        return name.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj instanceof StringServiceName) {
            StringServiceName stringServiceName = (StringServiceName) obj;
            return name.equals(stringServiceName.name);
        }
        return false;
    }

    public String toString() {
        return name;
    }
}
