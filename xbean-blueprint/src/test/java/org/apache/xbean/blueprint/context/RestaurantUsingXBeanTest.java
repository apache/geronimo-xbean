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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xbean.blueprint.example.RestaurantService;
import org.apache.aries.blueprint.reflect.BeanMetadataImpl;
import org.osgi.service.blueprint.reflect.ValueMetadata;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;

import javax.xml.namespace.QName;

import java.net.URI;

/**
 * @author James Strachan
 * @version $Id$
 * @since 2.0
 */
public class RestaurantUsingXBeanTest extends RestaurantUsingBlueprintTest {
    private static final Log log = LogFactory.getLog(RestaurantUsingXBeanTest.class);

    public void testPizza() throws Exception {
        super.testPizza();

        BeanMetadataImpl restaurant = (BeanMetadataImpl) reg.getComponentDefinition("restaurant");

        ValueMetadata uri = (ValueMetadata) propertyByName("uri", restaurant).getValue();
        assertNotNull("URI is null", uri);
        assertEquals("URI", "http://cheese.com", uri.getStringValue());

        log.info("Successfully converted the property to a URI: " + uri);
    }

    protected String getPlan() {
        return "org/apache/xbean/blueprint/context/restaurant-xbean.xml";
    }

}
