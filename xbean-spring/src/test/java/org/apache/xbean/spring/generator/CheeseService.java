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

/**
 * Cheezy goodness
 *
 * @org.apache.xbean.XBean element="cheese"
 *
 * @author Dain Sundstrom
 * @version $Id$
 */
public class CheeseService {
    private String id;
    private String name;

    private long volume;
    
    public CheeseService(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
    
    public long getVolumeWithPropertyEditor() {
    	return volume;
    }
    
    /**
    * @org.apache.xbean.Property propertyEditor="org.apache.xbean.spring.example.MilliLittersPropertyEditor"
    */
    public void setVolumeWithPropertyEditor(long volume) {
    	this.volume = volume;
    }
}
