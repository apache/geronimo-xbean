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
 * A problem occured while creating or using the kernel factory.  This error indicates that the kernel factory is
 * misconfigured or there is a programming error in the use of the kernel factory.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class KernelFactoryError extends Error {
    /**
     * Creates a KernelFactoryError using the specified message.
     *
     * @param message information about the cause of this error
     */
    public KernelFactoryError(String message) {
        super(message);
    }

    /**
     * Creates a KernelFactoryError using the specified message and cause.
     *
     * @param message information about the cause of this error
     * @param cause the cause of this error
     */
    public KernelFactoryError(String message, Throwable cause) {
        super(message, cause);
    }
}
