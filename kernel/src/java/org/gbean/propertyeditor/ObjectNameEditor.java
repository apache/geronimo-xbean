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
package org.gbean.propertyeditor;

import java.beans.PropertyEditorSupport;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * @version $Revision$ $Date$
 */
public class ObjectNameEditor extends PropertyEditorSupport {
    public void setAsText(String value) throws IllegalArgumentException {
        try {
            setValue(new ObjectName(value));
        } catch (MalformedObjectNameException e) {
            throw (IllegalArgumentException) new IllegalArgumentException().initCause(e);
        }
    }

    public String getAsText() {
        ObjectName objectName = ((ObjectName) getValue());
        String text = objectName.getCanonicalName();
        return text;
    }
}