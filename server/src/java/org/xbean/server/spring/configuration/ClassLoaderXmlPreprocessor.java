/**
 *
 * Copyright 2005 the original author or authors.
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
package org.xbean.server.spring.configuration;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.xbean.server.classloader.MultiParentClassLoader;
import org.xbean.server.repository.Repository;
import org.xbean.spring.context.SpringXmlPreprocessor;
import org.xbean.spring.context.SpringApplicationContext;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * ClassLoaderXmlPreprocessor extracts a ClassLoader definition from the xml document, builds a class loader, assigns
 * the class loader to the application context and xml reader, and removes the classpath element from document.
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class ClassLoaderXmlPreprocessor implements SpringXmlPreprocessor {
    private final Repository repository;

    /**
     * Creates a ClassLoaderXmlPreprocessor that uses the specified repository to resolve the class path locations.
     * @param repository the repository used to resolve the class path locations
     */
    public ClassLoaderXmlPreprocessor(Repository repository) {
        this.repository = repository;
    }

    /**
     * Extracts a ClassLoader definition from the xml document, builds a class loader, assigns
     * the class loader to the application context and xml reader, and removes the classpath element from document.
     *
     * @param applicationContext the application context on which the class loader will be set
     * @param reader the xml reader on which the class loader will be set
     * @param document the xml document to inspect
     */
    public void preprocess(SpringApplicationContext applicationContext, XmlBeanDefinitionReader reader, Document document) {
        // determine the classLoader
        ClassLoader classLoader;
        NodeList classpathElements = document.getDocumentElement().getElementsByTagName("classpath");
        if (classpathElements.getLength() < 1) {
            classLoader = getClass().getClassLoader();
        } else if (classpathElements.getLength() > 1) {
            throw new FatalBeanException("Expected only classpath element but found " + classpathElements.getLength());
        } else {
            Element classpathElement = (Element) classpathElements.item(0);

            // build the classpath
            List classpath = new ArrayList();
            NodeList locations = classpathElement.getElementsByTagName("location");
            for (int i = 0; i < locations.getLength(); i++) {
                Element locationElement = (Element) locations.item(i);

                String location = ((Text) locationElement.getFirstChild()).getData().trim();
                classpath.add(location);
            }

            // convert the paths to URLS
            URL[] urls = new URL[classpath.size()];
            for (ListIterator iterator = classpath.listIterator(); iterator.hasNext();) {
                String location = (String) iterator.next();
                urls[iterator.previousIndex()] = repository.getResource(location);
            }

            // create the classloader
            classLoader = new MultiParentClassLoader(applicationContext.getDisplayName(), urls, getClass().getClassLoader());

            // remove the classpath element so Spring doesn't get confused
            document.getDocumentElement().removeChild(classpathElement);
        }

        // assign the class loader to the xml reader and the application context
        reader.setBeanClassLoader(classLoader);
        applicationContext.setClassLoader(classLoader);
    }
}
