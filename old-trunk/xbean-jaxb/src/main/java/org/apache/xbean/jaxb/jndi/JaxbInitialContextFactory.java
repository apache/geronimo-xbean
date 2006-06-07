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
package org.apache.xbean.jaxb.jndi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xbean.jaxb.ContextImpl;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;

/**
 * A simple JNDI initial context which loads the JNDI namespace from an XML
 * configuration file using JAXB2. The configuration file can be specified by
 * the {@link Context#PROVIDER_URL} property which can be any spring resource
 * string (classpath://foo.xml, or file://foo/bar.xml or a URL) otherwise the
 * jndi.xml file is found on the classpath.
 * 
 * @version $Revision: 657 $
 */
public class JaxbInitialContextFactory implements InitialContextFactory {
    public static final String JAXB_PACKAGES = "jaxb.packages";
    public static final String DEFAULT_URL = "jndi.xml";

    private static final transient Log log = LogFactory.getLog(JaxbInitialContextFactory.class);

    private static Context singleton;

    /**
     * A factory method which can be used to initialise a singleton JNDI context
     * from inside a Spring.xml such that future calls to new InitialContext()
     * will reuse it
     */
    public static Context makeInitialContext() {
        singleton = new DefaultContext();
        return singleton;
    }

    public Context getInitialContext(Hashtable environment) throws NamingException {
        if (singleton != null) {
            return singleton;
        }
        Object value = environment.get(Context.PROVIDER_URL);
        if (value == null) {
            value = DEFAULT_URL;
        }

        InputStream in = null;
        ContextImpl answer = null;
        try {
            in = loadResource(value);
            if (in != null) {
                answer = ContextImpl.load(createJaxbContext(environment), in);
            }
        }
        catch (Exception e) {
            log.warn("Caught: " + e, e);
            throw new NamingException("Failed to parse resource: " + value + ". Reason: " + e);
        }
        if (in == null) {
            throw new NamingException("Could not find resource: " + value);
        }
        if (answer == null) {
            throw new NamingException("No context returned after parsing resource: " + value);
        }
        return answer.createJndiContext(environment);
    }

    /**
     * Loads the given resource. If the resource is not a URL then it is assumed
     * to be a classpath relative string
     */
    protected InputStream loadResource(Object value) throws IOException, NamingException {
        URL url = null;
        if (value instanceof URL) {
            url = (URL) value;
        }
        else if (value instanceof String) {
            String text = (String) value;
            if (text.indexOf(":") > 0) {
                url = new URL(text);
            }
            else {
                return loadResourceFromClassPath(text);
            }
        }
        if (url != null) {
            if ("classpath".equals(url.getProtocol())) {
                return loadResourceFromClassPath(url.getPath());
            }
            else {
                return url.openStream();
            }
        }
        return null;
    }

    protected InputStream loadResourceFromClassPath(String text) throws NamingException {
        InputStream answer = Thread.currentThread().getContextClassLoader().getResourceAsStream(text);
        if (answer == null) {
            answer = getClass().getResourceAsStream(text);
            if (answer == null) {
                throw new NamingException("Could not find resourcE: " + text + " on the classpath");
            }
        }
        return answer;
    }

    /**
     * Factory method to create the JAXBContext
     */
    protected JAXBContext createJaxbContext(Hashtable environment) throws JAXBException {
        // TODO dirty hack!!!
        String packages = (String) environment.get(JAXB_PACKAGES);
        if (packages == null) {
            packages = "org.apache.xbean.jaxb";
        }
        return JAXBContext.newInstance(packages);
    }
}
