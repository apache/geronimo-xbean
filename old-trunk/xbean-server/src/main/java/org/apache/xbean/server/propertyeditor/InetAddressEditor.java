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
package org.apache.xbean.server.propertyeditor;

import java.beans.PropertyEditorSupport;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * InetAddressEditor is a java beans property editor that can convert an InetAddreass to and from a String.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class InetAddressEditor extends PropertyEditorSupport {
    /**
     * Converts the specified string value into an InetAddress and stores the value in this instance.
     * @param value the string to convert into an InetAddress
     * @throws IllegalArgumentException if the specified string value is not a valid InetAddress
     */
    public void setAsText(String value) throws IllegalArgumentException {
        try {
            setValue(InetAddress.getByName(value));
        } catch (UnknownHostException e) {
            throw (IllegalArgumentException) new IllegalArgumentException().initCause(e);
        }
    }

    /**
     * Converts the stored InetAddress value into a String.
     * @return the string form of the current InetAddress value
     * @throws NullPointerException if the current InetAddress is null
     */
    public String getAsText() throws NullPointerException {
        InetAddress inetAddress = (InetAddress) getValue();
        if (inetAddress == null) {
            throw new NullPointerException("Current InetAddress value is null");
        }
        String text = inetAddress.toString();
        return text;
    }
}