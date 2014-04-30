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
package org.apache.xbean.spring.example;

/**
 * It comes from a potatoe, it must be good.
 * 
 * @org.apache.xbean.XBean element="vodka"
 * 
 * @author Dan Diephouse
 * @version $Id: VodkaService.java 434369 2006-08-24 10:24:21Z gnodet $
 * @since 2.0
 */

// START SNIPPET: bean
public class VodkaService {
    private String id;
    private String name;
    private Class vodkaClass;
    
    public VodkaService() {
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class getVodkaClass() {
        return vodkaClass;
    }

    public void setVodkaClass(Class vodkaClass) {
        this.vodkaClass = vodkaClass;
    }
}
// END SNIPPET: bean

