/**
 *
 * Copyright 2005 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.gbean.server.annotation;

/**
 * A sample pizza service.
 * @org.gbean.server.annotation.Description ("pizza making service")
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class PizzaService {
    private String topping;
    private String cheese;
    private int size;
    private double price;

    /**
     * Gets the cheese type.
     * @return the cheese type
     *
     * @org.gbean.server.annotation.Description ("type of cheese")
     */
    public String getCheese() {
        return cheese;
    }

    /**
     * Sets the cheese type.
     * @param cheese the cheese type
     */
    public void setCheese(String cheese) {
        this.cheese = cheese;
    }

    /**
     * Gets the price.
     * @return the price
     *
     * @org.gbean.server.annotation.Description ("price in dollars")
     */
    public double getPrice() {
        return price;
    }

    /**
     * Sets the price.
     * @param price the price
     */
    public void setPrice(double price) {
        this.price = price;
    }

    /**
     * Gets the size.
     * @return the size
     *
     * @org.gbean.server.annotation.Description ("size of the pizza in inches")
     */
    public int getSize() {
        return size;
    }

    /**
     * Sets the size.
     * @param size the size
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * Gets the toppings.
     * @return the toppings
     *
     * @org.gbean.server.annotation.Description ("pizza topping")
     */
    public String getTopping() {
        return topping;
    }

    /**
     * Sets the toppings.
     * @param topping the toppings
     */
    public void setTopping(String topping) {
        this.topping = topping;
    }
}
