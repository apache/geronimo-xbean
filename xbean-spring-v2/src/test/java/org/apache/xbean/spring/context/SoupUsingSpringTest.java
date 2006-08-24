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
import org.apache.xbean.spring.example.SoupService;

/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class SoupUsingSpringTest extends SpringTestSupport {
    private static final long time = System.currentTimeMillis();

    public void testSoup() throws Exception {
        SoupService soup = (SoupService) getBean("soupService");
        SoupService nestedBean = (SoupService) getBean("nestedBean");
        SoupService nestedValue = (SoupService) getBean("nestedValue");

        asssertValidSoup(soup);
        asssertValidSoup(nestedBean);
        asssertValidSoup(nestedValue);

        context.close();
        assertFalse(soup.exists());
        assertFalse(nestedBean.exists());
        assertFalse(nestedValue.exists());
    }

    private void asssertValidSoup(SoupService soup) {
        assertEquals("type", "French Onion", soup.getSoupType());
        assertTrue(soup.getCreateTime() >= time);
        assertTrue(soup.exists());
    }

    protected AbstractXmlApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("org/apache/xbean/spring/context/soup-normal.xml");
    }
}
