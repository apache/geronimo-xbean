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
package org.gbean.metadata.simple;

import java.util.LinkedHashMap;
import java.util.Map;

import org.gbean.metadata.ParameterMetadata;

/**
 * @version $Revision$ $Date$
 */
public class SimpleParameterMetadata implements ParameterMetadata {
    private final Map properties = new LinkedHashMap();
    private final int index;
    private final Class type;

    public SimpleParameterMetadata(int index, Class type) {
        this.index = index;
        this.type = type;
    }

    public int getIndex() {
        return index;
    }

    public Class getType() {
        return type;
    }

    public Map getProperties() {
        return properties;
    }

    public Object get(Object key) {
        return properties.get(key);
    }

    public Object put(Object key, Object value) {
        return properties.put(key, value);
    }

    public Object remove(Object key) {
        return properties.remove(key);
    }
}
