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
package org.apache.xbean.sca;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *
 * @version $Revision$
 */
public class PropertyTest extends SpringTestSupport {

    public void testProperties() throws Exception {
        MandatoryPropertyPojo pojo = (MandatoryPropertyPojo) getBean("pojo");
        assertEquals("getCheese()", "Edam", pojo.getCheese());
        assertEquals("isBeer()",true, pojo.isBeer());
    }
    
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("org/apache/xbean/sca/property.xml");
    }

}
