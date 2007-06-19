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

import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.xbean.spring.example.PizzaService;
import org.apache.xbean.spring.example.RestaurantService;
import org.springframework.context.support.AbstractXmlApplicationContext;

/**
 * @author James Strachan
 * @version $Id$
 * @since 2.0
 */
public class RestaurantUsingSpringTest extends SpringTestSupport {

    public void testPizza() throws Exception {
        RestaurantService restaurant = (RestaurantService) getBean("restaurant");

        QName service = restaurant.getServiceName();
        assertEquals(new QName("http://acme.com", "xyz"), service);

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

        // dinners (1-many using Set)
        Set<PizzaService> snacks = restaurant.getSnackMenu();
        assertNotNull("dinners is null!", snacks);
        assertEquals("dinners size: " + snacks, 2, snacks.size());
        for (PizzaService snack : snacks) {
            String topping = snack.getTopping();
            if ("Tofu".equals(topping)) {
                assertEquals("cheese", "Parmesan", snack.getCheese());
                assertEquals("size", 6, snack.getSize());
            } else if ("Prosciutto".equals(topping)) {
                assertEquals("cheese", "Blue", snack.getCheese());
                assertEquals("size", 8, snack.getSize());
            } else {
                fail("wrong topping: " + snack);
            }
        }
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

    }

    protected AbstractXmlApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("org/apache/xbean/spring/context/restaurant-normal.xml");
    }
}
