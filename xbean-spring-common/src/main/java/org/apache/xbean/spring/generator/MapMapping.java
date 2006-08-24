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
package org.apache.xbean.spring.generator;

public class MapMapping {
    private String entryName;
    private String keyName;

    public MapMapping(String entryName, String keyName) {
        this.entryName = entryName;
        this.keyName = keyName;
    }

    public String getEntryName() {
        return entryName;
    }

    public String getKeyName() {
        return keyName;
    }
}
