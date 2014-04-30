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
package org.apache.xbean.blueprint.context;

import org.apache.xbean.blueprint.example.VodkaService;
import org.apache.aries.blueprint.reflect.BeanMetadataImpl;

/**
 * @author Dan Diephouse
 * @version $Id: VodkaUsingSpringTest.java 434369 2006-08-24 10:24:21Z gnodet $
 * @since 1.0
 */
public class VodkaUsingBlueprintTest extends BlueprintTestSupport {
    
    public void testWine() throws Exception {
        BeanMetadataImpl meta = (BeanMetadataImpl) reg.getComponentDefinition("vodkaService");

        checkPropertyValue("name", "Grey Goose", meta);
        checkPropertyValue("id", "vodkaService", meta);
        
        // Test more complex classes
        checkPropertyValue("vodkaClass", VodkaService.class.getName(), meta);
    }

    protected String getPlan() {
        return "org/apache/xbean/blueprint/context/vodka-normal.xml";
    }
}
