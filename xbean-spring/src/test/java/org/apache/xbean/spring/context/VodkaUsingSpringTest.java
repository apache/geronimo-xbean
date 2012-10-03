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
import org.apache.xbean.spring.example.VodkaService;

/**
 * @author Dan Diephouse
 * @version $Id: VodkaUsingSpringTest.java 434369 2006-08-24 10:24:21Z gnodet $
 * @since 1.0
 */
public class VodkaUsingSpringTest extends SpringTestSupport {
    
    public void testWine() throws Exception {
        VodkaService vodka = (VodkaService) getBean("vodkaService");

        assertEquals("name", "Grey Goose", vodka.getName());
        assertEquals("id", "vodkaService", vodka.getId());
        
        // Test more complex classes
        assertEquals("class", VodkaService.class, vodka.getClass());
    }

    protected AbstractXmlApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("org/apache/xbean/spring/context/vodka-normal.xml");
    }
}
