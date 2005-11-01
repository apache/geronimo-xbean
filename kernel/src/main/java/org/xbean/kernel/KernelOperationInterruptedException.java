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
 * Signifies that a thread carrying out a kernel operation was interrupted.  The kernel will always leave the
 * system in a stable state before returning to the caller.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class KernelOperationInterruptedException extends RuntimeException {
    private final ServiceName serviceName;
    private final String operationName;

    /**
     * Created a KernelOperationInterruptedException for the specified operation on the specified service.
     *
     * @param cause the {@link InterruptedException} that casused the kernel operation to be interrupted
     * @param serviceName the name of the service for which the operation was interrupted
     * @param operationName the name of the operation that was interrupted
     */
    public KernelOperationInterruptedException(InterruptedException cause, ServiceName serviceName, String operationName) {
        super(cause);
        if (serviceName == null) throw new NullPointerException("serviceName is null");
        if (operationName == null) throw new NullPointerException("operationName is null");
        this.serviceName = serviceName;
        this.operationName = operationName;
    }

    /**
     * Created a KernelOperationInterruptedException with a custom message.
     *
     * @param message a custom message for this exception
     * @param cause the {@link InterruptedException} that casused the kernel operation to be interrupted
     * @param serviceName the name of the service for which the operation was interrupted
     * @param operationName the name of the operation that was interrupted
     */
    public KernelOperationInterruptedException(String message, InterruptedException cause, ServiceName serviceName, String operationName) {
        super(message, cause);
        if (serviceName == null) throw new NullPointerException("serviceName is null");
        if (operationName == null) throw new NullPointerException("operationName is null");
        this.serviceName = serviceName;
        this.operationName = operationName;
    }

    /**
     * Gets the name of the service for which the operation was interrupted.
     *
     * @return the name of the service for which the operation was interrupted
     */
    public ServiceName getServiceName() {
        return serviceName;
    }

    /**
     * Gets the name of the operation that was interrupted.
     *
     * @return the name of the operation that was interrupted
     */
    public String getOperationName() {
        return operationName;
    }
}
