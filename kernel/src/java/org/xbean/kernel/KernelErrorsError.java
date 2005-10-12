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

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Groups a collection of errors from a set of work so they maybe be thrown together from the kernel.  This is used
 * when the kernel does aggregate work on somthing that shouldn't fail such as when notifying kernel monitors or
 * destroying the kernel.  This allows the kernel to preform all required work and then throw any errors as a single
 * exception object.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class KernelErrorsError extends Error {
    private final List errors;

    /**
     * Creates an Errors error containing the list of errors.
     *
     * @param errors the errors
     */
    public KernelErrorsError(List errors) {
        if (errors == null) throw new NullPointerException("errors is null");
        if (errors.isEmpty()) throw new IllegalArgumentException("errors is empty");
        for (ListIterator iterator = errors.listIterator(); iterator.hasNext();) {
            Object error = iterator.next();
            if (error == null) {
                throw new IllegalArgumentException("Errors element " + iterator.previousIndex() + " is null");
            }
            if (!(error instanceof Error)) {
                throw new IllegalArgumentException("Errors element " + iterator.previousIndex() +
                        " is not an instance of java.lang.Error " + error.getClass() + ": " + error);
            }
        }

        this.errors = Collections.unmodifiableList(errors);
    }

    /**
     * Gets the errors that casued this error.
     *
     * @return the errors that casued this error
     */
    public List getErrors() {
        return errors;
    }

    public String getMessage() {
        StringBuffer message = new StringBuffer();
        message.append(errors.size() + " Error(s) occured [");
        for (Iterator iterator = errors.iterator(); iterator.hasNext();) {
            Error error = (Error) iterator.next();
            message.append('\"').append(error.getMessage()).append('\"');
            if (iterator.hasNext()) {
                message.append(", ");
            }
        }
        return message.append("]").toString();
    }

    public String getLocalizedMessage() {
        StringBuffer message = new StringBuffer();
        message.append(errors.size() + " Error(s) occured [");
        for (Iterator iterator = errors.iterator(); iterator.hasNext();) {
            Error error = (Error) iterator.next();
            message.append('\"').append(error.getLocalizedMessage()).append('\"');
            if (iterator.hasNext()) {
                message.append(", ");
            }
        }
        return message.append("]").toString();
    }

    public void printStackTrace(PrintStream stream) {
        synchronized (stream) {
            stream.println(this);
            for (Iterator iterator = errors.iterator(); iterator.hasNext();) {
                Error error = (Error) iterator.next();
                error.printStackTrace(stream);
            }
        }
    }

    public void printStackTrace(PrintWriter writer) {
        synchronized (writer) {
            writer.println(this);
            for (Iterator iterator = errors.iterator(); iterator.hasNext();) {
                Error error = (Error) iterator.next();
                error.printStackTrace(writer);
            }
        }
    }
}
