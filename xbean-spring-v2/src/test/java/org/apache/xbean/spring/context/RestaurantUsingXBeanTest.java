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
import org.apache.xbean.spring.example.RestaurantService;

import javax.xml.namespace.QName;

import java.net.URI;

/**
 * @author James Strachan
 * @version $Id$
 * @since 2.0
 */
public class RestaurantUsingXBeanTest extends RestaurantUsingSpringTest {
    private static final Log log = LogFactory.getLog(RestaurantUsingXBeanTest.class);

    public void testPizza() throws Exception {
        super.testPizza();

        RestaurantService restaurant = (RestaurantService) getBean("restaurant");
        QName name = restaurant.getServiceName();
        assertNotNull("Name is null", name);

        assertEquals("Namespace URI", "http://acme.com", name.getNamespaceURI());
        assertEquals("localName", "xyz", name.getLocalPart());
        assertEquals("prefix", "foo", name.getPrefix());

        log.info("Successfully converted the property to a QName: " + name);
        
        URI uri = restaurant.getUri();
        assertNotNull("URI is null", uri);
        assertEquals("URI", new URI("http://cheese.com"), uri);
        
        log.info("Successfully converted the property to a URI: " + uri);
    }

    protected AbstractXmlApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("org/apache/xbean/spring/context/restaurant-xbean.xml");
    }
}
