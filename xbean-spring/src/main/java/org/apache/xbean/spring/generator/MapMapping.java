/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.xbean.spring.generator;

public class MapMapping {
    private String entryName;
    private String keyName;
    private boolean flat;
    private String dupsMode;
    private String defaultKey;

    public MapMapping(String entryName, 
                      String keyName, 
                      boolean flat, 
                      String dupsMode, 
                      String defaultKey) {
        this.entryName = entryName;
        this.keyName = keyName;
        this.flat = flat;
        this.dupsMode = dupsMode;
        this.defaultKey = defaultKey;
    }

    public String getEntryName() {
        return entryName;
    }

    public String getKeyName() {
        return keyName;
    }

    public boolean isFlat() {
        return flat;
    }

    public String getDupsMode() {
        return dupsMode;
    }

    public String getDefaultKey() {
        return defaultKey;
    }
}
