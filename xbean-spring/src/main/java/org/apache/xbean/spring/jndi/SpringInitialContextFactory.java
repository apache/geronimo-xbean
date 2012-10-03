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
package org.apache.xbean.spring.jndi;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.apache.xbean.spring.context.impl.XBeanXmlBeanFactory;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * A simple JNDI initial context which loads the JNDI namespace from a spring.xml configuration file.
 * The spring.xml configuration file can be specified by the {@link Context#PROVIDER_URL} property
 * which can be any spring resource string (classpath://foo.xml, or file://foo/bar.xml or a URL)
 * otherwise the jndi.xml file is found on the classpath.
 *
 * @version $Revision: 657 $
 */
public class SpringInitialContextFactory implements InitialContextFactory {
    private static final transient Log log = LogFactory.getLog(SpringInitialContextFactory.class);

    private static Map cache = new HashMap();

    private static Context singleton;

    /**
     * A factory method which can be used to initialise a singleton JNDI context from inside a Spring.xml
     * such that future calls to new InitialContext() will reuse it
     */
    public static Context makeInitialContext() {
        singleton = new DefaultContext();
        return singleton;
    }

    public Context getInitialContext(Hashtable environment) throws NamingException {
        if (singleton != null) {
            return singleton;
        }
        Resource resource = null;
        Object value = environment.get(Context.PROVIDER_URL);
        String key = "jndi.xml";
        if (value == null) {
            resource = new ClassPathResource(key);
        }
        else {
            if (value instanceof Resource) {
                resource = (Resource) value;
            }
            else {
                ResourceEditor editor = new ResourceEditor();
                key = value.toString();
                editor.setAsText(key);
                resource = (Resource) editor.getValue();
            }
        }
        BeanFactory context = loadContext(resource, key);
        Context answer = (Context) context.getBean("jndi");
        if (answer == null) {
            log.warn("No JNDI context available in JNDI resource: " + resource);
            answer = new DefaultContext(environment, new ConcurrentHashMap());
        }
        return answer;
    }

    protected BeanFactory loadContext(Resource resource, String key) {
        synchronized (cache) {
            BeanFactory answer = (BeanFactory) cache.get(key);
            if (answer == null) {
                answer =  createContext(resource);
                cache.put(key, answer);
            }
            return answer;
        }
    }

    protected BeanFactory createContext(Resource resource) {
        log.info("Loading JNDI context from: " + resource);
        return new XBeanXmlBeanFactory(resource);
    }
}
