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
package org.apache.xbean.sca;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *
 * @version $Revision$
 */
public class LifecycleTest extends SpringTestSupport {

    private static int initCounter;
    private static int destroyCounter;

    public void testLifecycle() throws Exception {
        getBean("lifecycle");
        
        assertTrue("Should have invoked the initCounter: " + initCounter, initCounter > 0);
    }
    
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("org/apache/xbean/sca/lifecycle.xml");
    }

    protected void setUp() throws Exception {
        initCounter = destroyCounter = 0;
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        
        assertTrue("Should have invoked the destroyCounter: " + destroyCounter, destroyCounter > 0);
    }

    public static void onInitCalled() {
        System.out.println("@Init called");
        ++initCounter;
    }
    
    public static void onDestroyCalled() {
        System.out.println("@Destroy called");
        ++destroyCounter;
    }
}
