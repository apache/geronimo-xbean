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
package org.apache.xbean.generator.common;

import junit.framework.TestCase;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.xbean.generator.AttributeMapping;
import org.apache.xbean.generator.ElementMapping;
import org.apache.xbean.generator.LogFacade;
import org.apache.xbean.generator.NamespaceMapping;
import org.apache.xbean.generator.Type;
import org.apache.xbean.generator.Utils;
import org.apache.xbean.generator.artifact.HashedArtifactSet;
import org.apache.xbean.generator.commons.XsdGenerator;
import org.apache.xbean.test.support.example.BeerService;
import org.apache.xbean.generator.qdox.QdoxMappingLoader;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.beans.PropertyEditorManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
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

    private final LogFacade log = new LogFacade() {
        public void log(String message) {
            System.out.println("> " + message);
        }

        public void log(String message, int level) {
            System.out.println("> (" +  level + ") " + message);
        }
    };;

    public void testQdox() throws Exception{
        QdoxMappingLoader mappingLoader = new QdoxMappingLoader(DEFAULT_NAMESPACE, getSourceDirectory(), null);
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

        Map<String, String> flatCollections = recipeService.getFlatCollections();
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
        XsdGenerator generator = new XsdGenerator(true, new File("target/"), new HashedArtifactSet(), log) {};
        generator.generateSchema(new PrintWriter(System.out) {
            @Override
            public void println(String text) {
                System.out.println(text);
                if (text.contains("volumeWithPropertyEditor")) {
                    if (text.contains("xs:string")) {
                        gotExpected.set(true);
                    }
                }
            }

            @Override
            public void print(String s) {

            }
        }, defaultNamespace);

        assertTrue("xsd with string got genereated", gotExpected.get());
    }

    private File[] getSourceDirectory() {
        String basedir = System.getProperties().getProperty("basedir", ".");
        return new File[]{new File(basedir, "/../xbean-test-support/src/main/java")};
    }

    public void testQdoxExcludeClass() throws Exception{
        QdoxMappingLoader mappingLoader = new QdoxMappingLoader(DEFAULT_NAMESPACE,
                getSourceDirectory(),
                new String[] { BeerService.class.getName() } );

        NamespaceMapping defaultNamespace = getDefaultNamespace(mappingLoader);
        assertNotNull(defaultNamespace);

        ElementMapping element = defaultNamespace.getElement("pizza");
        assertNotNull(element);
        ElementMapping beer = defaultNamespace.getElement("beer");
        assertNull(beer);
    }

    public void testQdoxExcludePackage() throws Exception{
        QdoxMappingLoader mappingLoader = new QdoxMappingLoader(DEFAULT_NAMESPACE,
                getSourceDirectory(),
                new String[] { "org.apache.xbean.test.support.example" } );

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
        Set<NamespaceMapping> namespaces = mappingLoader.loadNamespaces();
        assertFalse(namespaces.isEmpty());

        NamespaceMapping defaultNamespace = null;
        for (NamespaceMapping namespaceMapping : namespaces) {
            if (namespaceMapping.getNamespace().equals(DEFAULT_NAMESPACE)) {
                defaultNamespace = namespaceMapping;
                break;
            }
        }
        return defaultNamespace;
    }

    public void xtestPropertyEditor() {
        List<String> editorSearchPath = new LinkedList<String>(Arrays.asList(PropertyEditorManager.getEditorSearchPath()));
        editorSearchPath.add("org.apache.xbean.propertyeditors");
        PropertyEditorManager.setEditorSearchPath(editorSearchPath.toArray(new String[editorSearchPath.size()]));
        assertTrue(Utils.isSimpleType(Type.newSimpleType("java.net.URI")));
    }

    public void xtestXSDValidation() throws Exception{
        InputStream xmlFile = ModelTest.class.getResourceAsStream("model-test-xsd-validation.xml");
        File xsd = generateXSD();
        validate(xmlFile, xsd);

    }

    private File generateXSD() throws IOException {
        String basedir = System.getProperties().getProperty("basedir", ".");
        final File targetXSD = new File(basedir, "target/test-data/model-test.xsd");
        targetXSD.getParentFile().mkdirs();
        QdoxMappingLoader mappingLoader = new QdoxMappingLoader(DEFAULT_NAMESPACE, getSourceDirectory(), null);
        NamespaceMapping namespaceMapping = getDefaultNamespace(mappingLoader);
        XsdGenerator generator = new XsdGenerator(true, targetXSD, new HashedArtifactSet(), log) {};
        generator.generate(namespaceMapping, Collections.emptyMap());
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
        //TODO blueprint what ??
        builder.setEntityResolver(new EntityResolver() {
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
//                PluggableSchemaResolver springResolver = new PluggableSchemaResolver(getClass().getClassLoader());
                InputSource source = null;//springResolver.resolveEntity(publicId, systemId);
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
            fail("Validation failed: " + error.get().getMessage());
        }
    }
}
