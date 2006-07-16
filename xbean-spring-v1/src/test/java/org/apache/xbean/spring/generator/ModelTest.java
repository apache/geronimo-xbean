/**
 *
 * Copyright 2005-2006 The Apache Software Foundation or its licensors, as applicable.
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
package org.apache.xbean.spring.generator;

import java.beans.PropertyEditorManager;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;
import org.apache.xbean.spring.example.BeerService;

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
}
