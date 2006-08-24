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
package org.apache.xbean.spring.context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.apache.xbean.spring.example.RestaurantService;
import org.apache.xbean.spring.example.PizzaService;
import org.apache.xbean.spring.context.impl.XBeanXmlBeanFactory;

import javax.xml.namespace.QName;

import java.net.URI;
import java.util.List;

/**
 * @author James Strachan
 * @version $Id$
 * @since 2.0
 */
public class RestaurantUsingXBeanTest extends SpringTestSupport {

    private static final Log log = LogFactory.getLog(RestaurantUsingXBeanTest.class);

    public void testPizza() throws Exception {
        RestaurantService restaurant = (RestaurantService) getBean("restaurant");

        QName name = restaurant.getServiceName();
        assertNotNull("Name is null", name);
        assertEquals("Namespace URI", "http://acme.com", name.getNamespaceURI());
        assertEquals("localName", "xyz", name.getLocalPart());
        assertEquals("prefix", "foo", name.getPrefix());
        
        // dinners (1-many using list)
        List dinners = restaurant.getDinnerMenu();
        assertNotNull("dinners is null!", dinners);
        assertEquals("dinners size: " + dinners, 2, dinners.size());

        PizzaService pizza = (PizzaService) dinners.get(0);
        assertEquals("topping", "Ham", pizza.getTopping());
        assertEquals("cheese", "Mozzarella", pizza.getCheese());
        assertEquals("size", 15, pizza.getSize());
        
         pizza = (PizzaService) dinners.get(1);
        assertEquals("topping", "Eggs", pizza.getTopping());
        assertEquals("cheese", "Mozzarella", pizza.getCheese());
        assertEquals("size", 16, pizza.getSize());

        // lunches (1-many using array)
        PizzaService[] lunches = restaurant.getLunchMenu();
        assertNotNull("lunches is null!", lunches);
        assertEquals("lunches size: " + lunches, 1, lunches.length);

        pizza = lunches[0];
        assertEquals("topping", "Chicken", pizza.getTopping());
        assertEquals("cheese", "Brie", pizza.getCheese());
        assertEquals("size", 17, pizza.getSize());


        // favourite (1-1)
        pizza = restaurant.getFavourite();
        assertNotNull("Pizza is null!", pizza);
        pizza.makePizza();

        assertEquals("topping", "Salami", pizza.getTopping());
        assertEquals("cheese", "Edam", pizza.getCheese());
        assertEquals("size", 17, pizza.getSize());

        URI uri = restaurant.getUri();
        assertNotNull("URI is null", uri);
        assertEquals("URI", new URI("http://cheese.com"), uri);
    }

    protected AbstractXmlApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("org/apache/xbean/spring/context/restaurant-xbean.xml");
    }
    
     public void testCreateXBeanXmlBeanFactory() throws Exception {
        XBeanXmlBeanFactory factory = new XBeanXmlBeanFactory(new ClassPathResource("org/apache/xbean/spring/context/restaurant-xbean.xml"));
        assertNotNull(factory);
    }
 
}
