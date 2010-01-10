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

import java.util.List;

import org.apache.aries.blueprint.reflect.BeanMetadataImpl;
import org.apache.aries.blueprint.reflect.ValueMetadataImpl;
import org.apache.aries.blueprint.reflect.CollectionMetadataImpl;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.ValueMetadata;

/**
 * @author James Strachan
 * @version $Id$
 * @since 2.0
 */
public class RestaurantUsingBlueprintTest extends BlueprintTestSupport {

    public void testPizza() throws Exception {
        BeanMetadataImpl restaurant = (BeanMetadataImpl) reg.getComponentDefinition("restaurant");

        BeanProperty prop = propertyByName("serviceName", restaurant);

        BeanMetadataImpl qname = (BeanMetadataImpl) prop.getValue();
        assertEquals(3, qname.getArguments().size());
        assertEquals("http://acme.com", ((ValueMetadataImpl) qname.getArguments().get(0).getValue()).getStringValue());
        assertEquals("xyz", ((ValueMetadataImpl) qname.getArguments().get(1).getValue()).getStringValue());
        assertEquals("foo", ((ValueMetadataImpl) qname.getArguments().get(2).getValue()).getStringValue());

        // dinners (1-many using list)
        BeanProperty dinnerProp = propertyByName("dinnerMenu", restaurant);
        List<Metadata> dinners = ((CollectionMetadata) dinnerProp.getValue()).getValues();
        assertNotNull("dinners is null!", dinners);
        assertEquals("dinners size: " + dinners, 2, dinners.size());

        BeanMetadataImpl pizza = (BeanMetadataImpl) dinners.get(0);
        checkPropertyValue("topping", "Ham", pizza);
        checkPropertyValue("cheese", "Mozzarella", pizza);
        //TODO blueprint int value
        checkPropertyValue("size", "15", pizza);

        pizza = (BeanMetadataImpl) dinners.get(1);
        checkPropertyValue("topping", "Eggs", pizza);
        checkPropertyValue("cheese", "Mozzarella", pizza);
        //TODO blueprint int value
        checkPropertyValue("size", "16", pizza);

        // dinners (1-many using Set)
        BeanProperty snackProp = propertyByName("snackMenu", restaurant);
        List<Metadata> snacks = ((CollectionMetadata) snackProp.getValue()).getValues();
        assertNotNull("dinners is null!", snacks);
        assertEquals("dinners size: " + snacks, 2, snacks.size());
        for (Metadata snackMeta : snacks) {
            BeanMetadataImpl snack = (BeanMetadataImpl) snackMeta;
            BeanProperty toppingProp = propertyByName("topping", snack);
            String topping = ((ValueMetadataImpl) toppingProp.getValue()).getStringValue();
            if ("Tofu".equals(topping)) {
                checkPropertyValue("cheese", "Parmesan", snack);
                checkPropertyValue("size", "6", snack);
            } else if ("Prosciutto".equals(topping)) {
                checkPropertyValue("cheese", "Blue", snack);
                checkPropertyValue("size", "8", snack);
            } else {
                fail("wrong topping: " + snackMeta);
            }
        }
        // lunches (1-many using array)
        CollectionMetadataImpl lunches = (CollectionMetadataImpl) propertyByName("lunchMenu", restaurant).getValue();
        assertNotNull("lunches is null!", lunches);
        assertEquals("lunches size: " + lunches, 1, lunches.getValues().size());

        pizza = (BeanMetadataImpl) lunches.getValues().get(0);
        checkPropertyValue("topping", "Chicken", pizza);
        checkPropertyValue("cheese", "Brie", pizza);
        checkPropertyValue("size", "17", pizza);

        // favourite (1-1)
        BeanProperty favourite = propertyByName("favourite", restaurant);
        pizza = (BeanMetadataImpl) favourite.getValue();
        assertNotNull("Pizza is null!", pizza);
//        pizza.makePizza();
//
        checkPropertyValue("topping", "Salami", pizza);
        checkPropertyValue("cheese", "Edam", pizza);
        checkPropertyValue("size", "17", pizza);

    }

    protected String getPlan() {
        return "org/apache/xbean/blueprint/context/restaurant-normal.xml";
    }
}
