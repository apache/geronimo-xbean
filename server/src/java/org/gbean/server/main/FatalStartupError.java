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
package org.gbean.server.main;

/**
 * Indicates that a fatal error occured while starting the server.
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class FatalStartupError extends Error {
    /**
     * The default exit code assigned to new exception if an exit code is not provided.
     */
    public static final int DEFAULT_EXIT_CODE = 3;

    private final int exitCode;

    /**
     * Creates a FatalStartupError containing the specified message and the DEFAULT_EXIT_CODE.
     * @param message a descrption of the cause of the error
     */
    public FatalStartupError(String message) {
        this(message, DEFAULT_EXIT_CODE);
    }

    /**
     * Creates a FatalStartupError containing the specified message and exit code.
     * @param message a descrption of the cause of the error
     * @param exitCode the exit code that should be passed to System.exit(int)
     */
    public FatalStartupError(String message, int exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    /**
     * Creates a FatalStartupError containing the specified message, cause by exception, and the DEFAULT_EXIT_CODE.
     * @param message a descrption of the cause of the error
     * @param cause the cause of this exception
     */
    public FatalStartupError(String message, Throwable cause) {
        this(message, DEFAULT_EXIT_CODE, cause);
    }

    /**
     * Creates a FatalStartupError containing the specified message, cause by exception, and the specified exit code.
     * @param message a descrption of the cause of the error
     * @param exitCode the exit code that should be passed to System.exit(int)
     * @param cause the cause of this exception
     */
    public FatalStartupError(String message, int exitCode, Throwable cause) {
        super(message, cause);
        this.exitCode = exitCode;
    }

    /**
     * Gets the number that should be passed to System.exit(int) when the virtual machine is halted.
     * @return the exit code that should be passed to System.exit(int)
     */
    public int getExitCode() {
        return exitCode;
    }
}
