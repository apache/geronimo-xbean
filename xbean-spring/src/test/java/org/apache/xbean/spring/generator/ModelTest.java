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
package org.apache.xbean.spring.generator;

import junit.framework.TestCase;
import org.apache.xbean.spring.example.BeerService;
import org.springframework.beans.factory.xml.PluggableSchemaResolver;
import org.xml.sax.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.beans.PropertyEditorManager;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class ModelTest extends TestCase {
    private static final String DEFAULT_NAMESPACE = "http://xbean.apache.org/test";

    public void testQdox() throws Exception{
        String basedir = System.getProperties().getProperty("basedir", ".");
        QdoxMappingLoader mappingLoader = new QdoxMappingLoader(DEFAULT_NAMESPACE, new File[] { new File(basedir, "/src/test/java")}, null);
        NamespaceMapping defaultNamespace = getDefaultNamespace(mappingLoader);
        assertNotNull(defaultNamespace);
        ElementMapping element = defaultNamespace.getElement("pizza");
        assertNotNull(element);
        AttributeMapping attribute = element.getAttribute("myTopping");
        assertNotNull(attribute);
        assertEquals("topping", attribute.getPropertyName());

        ElementMapping beer = defaultNamespace.getElement("beer");
        assertNotNull(beer);
        AttributeMapping beerId = beer.getAttribute("id");
        assertNotNull(beerId);
        AttributeMapping beerName = beer.getAttribute("name");
        assertNotNull(beerName);

        ElementMapping recipeService = defaultNamespace.getElement("recipe-service");
        assertNotNull(recipeService);

        Map flatCollections = recipeService.getFlatCollections();
        assertNotNull(flatCollections);
        assertEquals(1, flatCollections.size());
        
        ElementMapping cheese = defaultNamespace.getElement("cheese");
        assertNotNull(cheese);
        AttributeMapping volume = cheese.getAttribute("volumeWithPropertyEditor");
        assertNotNull(volume);
        assertNotNull(volume.getPropertyEditor());
        assertEquals(volume.getType().getName(), "long");
        
        // validate xsd has string for attribute VolumeWithPropertyEditor
        final AtomicBoolean gotExpected = new AtomicBoolean(false);
        XsdGenerator generator = new XsdGenerator(null);
        generator.generateSchema(new PrintWriter("dummy") {
            @Override
            public void println(String x) {
                if (x.indexOf("volumeWithPropertyEditor") != -1) {
                    if (x.indexOf("xs:string") != -1) {
                        gotExpected.set(true);
                    }
                }
            }
        }, defaultNamespace);

        assertTrue("xsd with string got genereated", gotExpected.get());
    }

    public void testQdoxExcludeClass() throws Exception{
        String basedir = System.getProperties().getProperty("basedir", ".");
        QdoxMappingLoader mappingLoader = new QdoxMappingLoader(DEFAULT_NAMESPACE,
                new File[] { new File(basedir, "/src/test/java")},
                new String[] { BeerService.class.getName() } );

        NamespaceMapping defaultNamespace = getDefaultNamespace(mappingLoader);
        assertNotNull(defaultNamespace);

        ElementMapping element = defaultNamespace.getElement("pizza");
        assertNotNull(element);
        ElementMapping beer = defaultNamespace.getElement("beer");
        assertNull(beer);
    }

    public void testQdoxExcludePackage() throws Exception{
        String basedir = System.getProperties().getProperty("basedir", ".");
        QdoxMappingLoader mappingLoader = new QdoxMappingLoader(DEFAULT_NAMESPACE,
                new File[] { new File(basedir, "/src/test/java")},
                new String[] { "org.apache.xbean.spring.example" } );

        NamespaceMapping defaultNamespace = getDefaultNamespace(mappingLoader);
        assertNotNull(defaultNamespace);

        ElementMapping element = defaultNamespace.getElement("pizza");
        assertNull(element);
        ElementMapping beer = defaultNamespace.getElement("beer");
        assertNull(beer);
        ElementMapping cheese = defaultNamespace.getElement("cheese");
        assertNotNull(cheese);
    }

    private NamespaceMapping getDefaultNamespace(QdoxMappingLoader mappingLoader) throws IOException {
        Set namespaces = mappingLoader.loadNamespaces();
        assertFalse(namespaces.isEmpty());

        NamespaceMapping defaultNamespace = null;
        for (Iterator iterator = namespaces.iterator(); iterator.hasNext();) {
            NamespaceMapping namespaceMapping = (NamespaceMapping) iterator.next();
            if (namespaceMapping.getNamespace().equals(DEFAULT_NAMESPACE)) {
                defaultNamespace = namespaceMapping;
                break;
            }
        }
        return defaultNamespace;
    }

    public void testPropertyEditor() {
        List editorSearchPath = new LinkedList(Arrays.asList(PropertyEditorManager.getEditorSearchPath()));
        editorSearchPath.add("org.apache.xbean.spring.context.impl");
        PropertyEditorManager.setEditorSearchPath((String[]) editorSearchPath.toArray(new String[editorSearchPath.size()]));
        assertTrue(Utils.isSimpleType(Type.newSimpleType("java.net.URI")));
    }

    public void testXSDValidation() throws Exception{

        InputStream xmlFile = ModelTest.class.getResourceAsStream("model-test-xsd-validation.xml");
        File xsd = generateXSD();
        validate(xmlFile, xsd);

    }

    private File generateXSD() throws IOException {
        String basedir = System.getProperties().getProperty("basedir", ".");
        final File targetXSD = new File(basedir, "target/test-data/model-test.xsd");
        targetXSD.getParentFile().mkdirs();
        QdoxMappingLoader mappingLoader = new QdoxMappingLoader(DEFAULT_NAMESPACE, new File[] { new File(basedir, "/src/test/java")}, null);
        NamespaceMapping namespaceMapping = getDefaultNamespace(mappingLoader);
        XsdGenerator generator = new XsdGenerator(targetXSD);
        generator.setLog(new LogFacade() {
            public void log(String message) {
            }
            public void log(String message, int level) {
            }
        });
        generator.generate(namespaceMapping);
        return targetXSD;
    }

    private void validate(InputStream xml, final File xsd) throws ParserConfigurationException, SAXException, IOException {
        assertNotNull(xml);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(true);
        factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");

        final AtomicReference<SAXParseException> error = new AtomicReference<SAXParseException>();

        DocumentBuilder builder = factory.newDocumentBuilder();

        builder.setErrorHandler(new ErrorHandler() {
            public void warning(SAXParseException exception) throws SAXException {
                error.set(exception);
            }

            public void error(SAXParseException exception) throws SAXException {
                error.set(exception);
            }

            public void fatalError(SAXParseException exception) throws SAXException {
                error.set(exception);
            }
        });
        builder.setEntityResolver(new EntityResolver() {
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                PluggableSchemaResolver springResolver = new PluggableSchemaResolver(getClass().getClassLoader());
                InputSource source = springResolver.resolveEntity(publicId, systemId);
                if (source == null && "http://xbean.apache.org/test.xsd".equals(systemId)) {
                    source = new InputSource(new FileInputStream(xsd));
                    source.setPublicId(publicId);
                    source.setSystemId(systemId);
                }

                return source;
            }
        });
        builder.parse(xml);
        if (error.get() != null) {
            error.get().printStackTrace();
            fail("Validateion failed: " + error.get().getMessage());
        }
    }
}
