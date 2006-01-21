/**
 *
 * Copyright 2005-2006 The Apache Software Foundation or its licensors, as applicable.
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
package org.apache.xbean.spring.context;

import org.springframework.context.support.AbstractXmlApplicationContext;
import org.apache.xbean.spring.example.SaladService;

/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class SaladUsingSpringTest extends SpringTestSupport {
    public void testSalad() throws Exception {
        SaladService salad = (SaladService) getBean("saladService");

        assertEquals("dressing", "Cesar", salad.getDressing());
        assertEquals("size", "Small", salad.getSize());
        assertEquals("crouton", true, salad.isCrouton());
    }

    protected AbstractXmlApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("org/apache/xbean/spring/context/salad-normal.xml");
    }
}
