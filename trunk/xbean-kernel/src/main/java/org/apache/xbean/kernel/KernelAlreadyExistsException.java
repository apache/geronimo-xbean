/**
 *
 * Copyright 2005-2006 The Apache Software Foundation or its licensors, as applicable.
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
package org.apache.xbean.kernel;

/**
 * Indicates that a kernel is already registerd with the KernelFactory under the specified name.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class KernelAlreadyExistsException extends RuntimeException {
    private final String name;

    /**
     * Creates a KernelAlreadyExistsException using the specified name.
     *
     * @param name the name of the kernel that was alredy registered
     */
    public KernelAlreadyExistsException(String name) {
        super("A kernel is already registered with the name " + name);
        if (name == null) throw new NullPointerException("name is null");
        this.name = name;
    }

    /**
     * Gets the name of the kernel that already existed.
     *
     * @return the name of the kernel that already existed.
     */
    public String getName() {
        return name;
    }
}
