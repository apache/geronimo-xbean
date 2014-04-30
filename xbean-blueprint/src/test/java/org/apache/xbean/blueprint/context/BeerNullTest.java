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

import org.apache.xbean.blueprint.example.BeerService;
import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.reflect.BeanMetadataImpl;
import org.osgi.service.blueprint.reflect.NullMetadata;

/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.6
 */
public class BeerNullTest extends BlueprintTestSupport {

    public void testBeer() throws Exception {
        BeanMetadataImpl meta = (BeanMetadataImpl) reg.getComponentDefinition("beerService");

        checkPropertyValue("name", "Stella", meta);
        checkPropertyValue("id", "123", meta);
        //no property set since this is the default
//        checkPropertyValue("source", "tap", meta);

        BeanMetadataImpl meta2 = (BeanMetadataImpl) reg.getComponentDefinition("beerService2");

        checkPropertyValue("name", "Blue Moon", meta2);
        checkPropertyValue("id", "123", meta2);
        assertTrue(propertyByName("source", meta2).getValue() instanceof NullMetadata);
    }

    protected String getPlan() {
        return "org/apache/xbean/blueprint/context/beer-xbean-null.xml";
    }
}
