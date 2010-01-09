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

import org.apache.aries.blueprint.reflect.BeanMetadataImpl;
import org.apache.aries.blueprint.reflect.RefMetadataImpl;
import org.osgi.service.blueprint.reflect.BeanProperty;

/**
 * @version $Revision$
 */
public class PizzaUsingXBeanWinBeanRefTest extends PizzaUsingBlueprintTest {

    public void testPizza() throws Exception {
        BeanMetadataImpl meta = (BeanMetadataImpl) reg.getComponentDefinition("pizzaService");

        BeanProperty p = propertyByName("topping", meta);
        assertTrue(p.getValue() instanceof RefMetadataImpl);
        assertEquals("topping", ((RefMetadataImpl)p.getValue()).getComponentId());
        checkPropertyValue("cheese", "#Edam", meta);
        checkPropertyValue("size", "17", meta);
    }

    protected String getPlan() {
        return "org/apache/xbean/blueprint/context/pizza-xbean-bean-ref.xml";
    }

}
