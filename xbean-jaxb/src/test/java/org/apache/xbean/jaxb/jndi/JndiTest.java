/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.xbean.jaxb.jndi;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import java.util.Hashtable;

import junit.framework.TestCase;

/**
 * 
 * @version $Revision: $
 */
public class JndiTest extends TestCase {

    protected Context context;

    public void testContextFromJAXB() throws Exception {
        Object foo = context.lookup("foo");
        System.out.println("Foo: " + foo);
        assertNotNull("entry for foo should not be null!", foo);
        System.out.println("Foo is of type: " + foo.getClass());
    }

    @Override
    protected void setUp() throws Exception {
        context = createInitialContext();

        assertNotNull("Should have created a context!", context);
    }

    protected InitialContext createInitialContext() throws NamingException {
        Hashtable environment = createEnvironment();
        return new InitialContext(environment);
    }

    protected Hashtable createEnvironment() {
        Hashtable answer = new Hashtable();
        answer.put(Context.PROVIDER_URL, "file:src/test/resources/org/apache/xbean/jaxb/example1.xml");
        answer.put(Context.INITIAL_CONTEXT_FACTORY, JaxbInitialContextFactory.class.getName());
        answer.put(JaxbInitialContextFactory.JAXB_PACKAGES, "org.apache.xbean.jaxb:org.apache.xbean.jaxb.example");
        return answer;
    }
}
