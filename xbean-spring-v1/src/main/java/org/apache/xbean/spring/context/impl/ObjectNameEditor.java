/**
 * 
 * Copyright 2005-2006 The Apache Software Foundation or its licensors,  as applicable.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 * 
 **/
package org.apache.xbean.spring.context.impl;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import java.beans.PropertyEditorSupport;

/**
 * Editor for <code>java.net.URI</code>
 */
public class ObjectNameEditor extends PropertyEditorSupport {

    public void setAsText(String text) throws IllegalArgumentException {
        try {
            setValue(new ObjectName(text));
        }
        catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException("Could not convert ObjectName for " + text + ": " + e.getMessage());
        }
    }

    public String getAsText() {
        ObjectName value = (ObjectName) getValue();
        return (value != null ? value.toString() : "");
    }

}
