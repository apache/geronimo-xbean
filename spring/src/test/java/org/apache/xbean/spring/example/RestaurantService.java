/**
 * 
 * Copyright 2005-2006 The Apache Software Foundation or its licensors,  as applicable.
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
package org.apache.xbean.spring.example;

import javax.xml.namespace.QName;

import java.net.URI;
import java.util.List;

/**
 * An owner POJO used for testing out nested properties
 * 
 * @org.apache.xbean.XBean namespace="http://xbean.apache.org/schemas/pizza" element="restaurant"
 *   description="A Restaurant thingy"
 *
 * @author James Strachan
 * @version $Id$
 * @since 2.0
 */
public class RestaurantService {

    private PizzaService favourite;
    private List dinnerMenu;
    private PizzaService[] lunchMenu;
    private QName serviceName;
    private URI uri;

    /**
     * @org.apache.xbean.Property nestedType="org.apache.xbean.spring.example.PizzaService"
     */
    public List getDinnerMenu() {
        return dinnerMenu;
    }

    public void setDinnerMenu(List dinnerMenu) {
        this.dinnerMenu = dinnerMenu;
    }

    public PizzaService[] getLunchMenu() {
        return lunchMenu;
    }

    public void setLunchMenu(PizzaService[] lunchMenu) {
        this.lunchMenu = lunchMenu;
    }

    public PizzaService getFavourite() {
        return favourite;
    }

    public void setFavourite(PizzaService favourite) {
        this.favourite = favourite;
    }

    public QName getServiceName() {
        return serviceName;
    }

    public void setServiceName(QName name) {
        this.serviceName = name;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

}
