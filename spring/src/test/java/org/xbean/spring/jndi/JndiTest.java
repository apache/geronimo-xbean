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
package org.xbean.spring.jndi;

import org.springframework.beans.factory.BeanFactory;
import org.xbean.spring.context.ClassPathXmlApplicationContext;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import java.util.Hashtable;

import junit.framework.TestCase;

public class JndiTest extends TestCase {

    protected InitialContext createInitialContext() throws Exception {
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, SpringInitialContextFactory.class.getName());
        env.put(Context.PROVIDER_URL, "classpath:org/xbean/spring/jndi/jndi.xml");
        return new InitialContext(env);
    }
    
    public void testSpringJNDILookup() throws Exception {
        InitialContext context = createInitialContext();
        assertEntryExists(context, "test");
        assertEntryExists(context, "test/restaurant");
    }
    
    public void testConfigureJndiInsideSpringXml() throws Exception {
        // lets load a spring context

        BeanFactory factory = new ClassPathXmlApplicationContext("org/xbean/spring/jndi/spring.xml");
        Object test = factory.getBean("restaurant");
        assertNotNull("Should have found the test object", test);
        Object jndi = factory.getBean("jndi");
        assertNotNull("Should have found the jndi object", jndi);

        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, SpringInitialContextFactory.class.getName());
        InitialContext context = new InitialContext(env);
        assertEntryExists(context, "test");
        assertEntryExists(context, "test/restaurant");
        assertSame(test, context.lookup("test/restaurant"));
    }
    
    protected void assertEntryExists(InitialContext context, String name) throws NamingException {
        Object value = context.lookup(name);
        assertNotNull(name + " should not be null", value);
    }

}
