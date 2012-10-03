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

import org.springframework.context.support.AbstractXmlApplicationContext;
import org.apache.xbean.spring.example.BeerService;

/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.6
 */
public class BeerNullTest extends SpringTestSupport {

    public void testBeer() throws Exception {
        BeerService beer = (BeerService) getBean("beerService");

        assertEquals("name", "Stella", beer.getName());
        assertEquals("id", "123", beer.getId());
        assertEquals("source", "tap", beer.getSource());

        BeerService beer2 = (BeerService) getBean("beerService2");

        assertEquals("name", "Blue Moon", beer2.getName());
        assertEquals("id", "123", beer2.getId());
        assertNull("source", beer2.getSource());
    }

    protected AbstractXmlApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("org/apache/xbean/spring/context/beer-xbean-null.xml");
    }

}
