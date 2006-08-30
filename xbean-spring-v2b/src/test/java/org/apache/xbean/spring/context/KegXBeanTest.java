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

import org.apache.xbean.spring.example.KegService;
import org.springframework.context.support.AbstractXmlApplicationContext;

/**
 * Used to verify that per propety Property Editors work correctly.
 * 
 * @author chirino
 * @version $Id$
 * @since 2.2
 */
public class KegXBeanTest extends SpringTestSupport {

    public void testBeer() throws Exception {
    	 
        KegService ml1000 = (KegService) getBean("ml1000");
        KegService empty = (KegService) getBean("empty");        
        KegService pints5 = (KegService) getBean("pints5");
        KegService liter20 = (KegService) getBean("liter20");
        
        assertEquals(1000, ml1000.getRemaining());
        assertEquals(0, empty.getRemaining());
        assertEquals(8750, pints5.getRemaining());        
        assertEquals(20000, liter20.getRemaining());
        
    }


    protected AbstractXmlApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("org/apache/xbean/spring/context/keg-xbean.xml");
    }

}
