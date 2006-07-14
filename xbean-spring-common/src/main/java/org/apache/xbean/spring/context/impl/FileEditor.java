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

import java.beans.PropertyEditorSupport;
import java.io.File;

/**
 * Editor for <code>java.io.File</code>
 */
public class FileEditor extends PropertyEditorSupport {

    public void setAsText(String text) throws IllegalArgumentException {
        setValue(new File(text));
    }

    public String getAsText() {
        File value = (File) getValue();
        return (value != null ? value.toString() : "");
    }

}
