/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.xbean.propertyeditor;

import java.beans.PropertyEditor;
import java.util.Map;
import java.util.Collection;

/**
 * @version $Rev$ $Date$
 */
public class GenericMapConverter extends AbstractMapConverter {
    public GenericMapConverter(Class type, PropertyEditor keyEditor, PropertyEditor valueEditor) {
        super(type, keyEditor, valueEditor);
    }

    protected Map createMap(Map data) {
        try {
            Map map = (Map) getType().newInstance();
            map.putAll(data);
            return map;
        } catch (Exception e) {
            throw new PropertyEditorException(e);
        }
    }
}
