/**
 * 
 * Copyright 2005 LogicBlaze, Inc. http://www.logicblaze.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 * 
 **/
package org.xbean.spring.context;

import org.springframework.context.support.AbstractXmlApplicationContext;
import org.xbean.spring.example.PizzaService;
import org.xbean.spring.example.RestaurantService;

import java.util.List;

/**
 * 
 * @version $Revision: 1.1 $
 */
public class RestaurantUsingSpringTest extends SpringTestSupport {

    public void testPizza() throws Exception {
        RestaurantService restaurant = (RestaurantService) getBean("restaurant");

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

    }

    protected AbstractXmlApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("org/xbean/spring/context/restaurant-normal.xml");
    }
}
