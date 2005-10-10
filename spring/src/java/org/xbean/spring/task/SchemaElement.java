/**
 * 
 * Copyright 2005 LogicBlaze, Inc. http://www.logicblaze.com
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
package org.xbean.spring.task;

import org.codehaus.jam.JClass;

/**
 * Represents an element in the schema
 * 
 * @version $Revision: 1.1 $
 */
public class SchemaElement implements Comparable {

    private final JClass type;
    private final String localName;
    private final String namespace;

    public SchemaElement(JClass type, String localName, String namespace) {
        this.type = type;
        this.localName = localName;
        this.namespace = namespace;
    }

    public String getLocalName() {
        return localName;
    }

    public String getNamespace() {
        return namespace;
    }

    public JClass getType() {
        return type;
    }

    public int compareTo(Object that) {
        if (that instanceof SchemaElement) {
            SchemaElement thatElement = (SchemaElement) that;
            return localName.compareTo(thatElement.localName);
        }
        return getClass().getName().compareTo(that.getClass().getName());
    }
}
