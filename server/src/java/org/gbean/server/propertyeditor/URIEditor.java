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
package org.gbean.server.propertyeditor;

import java.beans.PropertyEditorSupport;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * URIEditor is a java beans property editor that can convert an URI to and from a String.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class URIEditor extends PropertyEditorSupport {
    /**
     * Converts the specified string value into an URI and stores the value in this instance.
     * @param value the string to convert into an URI
     * @throws IllegalArgumentException if the specified string value is not a valid URI
     */
    public void setAsText(String value) throws IllegalArgumentException {
        try {
            setValue(new URI(value));
        } catch (URISyntaxException e) {
            throw (IllegalArgumentException) new IllegalArgumentException().initCause(e);
        }
    }

    /**
     * Converts the stored URI value into a String.
     * @return the string form of the current URI value
     * @throws NullPointerException if the current URI is null
     */
    public String getAsText() {
        URI uri = (URI) getValue();
        if (uri == null) {
            throw new NullPointerException("Current URI value is null");
        }
        String text = uri.toString();
        return text;
    }
}