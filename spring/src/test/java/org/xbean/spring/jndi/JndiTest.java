package org.xbean.spring.jndi;

import javax.naming.Context;
import javax.naming.InitialContext;

import junit.framework.TestCase;

public class JndiTest extends TestCase {

    protected void setUp() throws Exception {
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, SpringInitialContextFactory.class.getName());
        System.setProperty(Context.PROVIDER_URL, "classpath:org/xbean/spring/jndi/jndi.xml");
    }
    
    public void testJndi() throws Exception {
        InitialContext context = new InitialContext();
        assertNotNull(context);
        Object restaurant = context.lookup("restaurant");
        assertNotNull(restaurant);
    }
    
}
