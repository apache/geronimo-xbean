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
package org.xbean.spring.generator;

import junit.framework.TestCase;

import java.io.File;
import java.util.Iterator;
import java.util.Set;
import java.util.List;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.Collections;
import java.beans.PropertyEditorManager;

/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class ModelTest extends TestCase {
    private static final String DEFAULT_NAMESPACE = "http://xbean.org/test";

    public void testQdox() throws Exception{
        QdoxMappingLoader mappingLoader = new QdoxMappingLoader(DEFAULT_NAMESPACE, new File[] { new File("src/test")});
        Set namespaces = mappingLoader.loadNamespaces();
        validateModel(namespaces);
    }

    private void validateModel(Set namespaces) {
        assertFalse(namespaces.isEmpty());

        NamespaceMapping defaultNamespace = null;
        for (Iterator iterator = namespaces.iterator(); iterator.hasNext();) {
            NamespaceMapping namespaceMapping = (NamespaceMapping) iterator.next();
            if (namespaceMapping.getNamespace().equals(DEFAULT_NAMESPACE)) {
                defaultNamespace = namespaceMapping;
                break;
            }
        }
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
    }

    public void testPropertyEditor() {
        List editorSearchPath = new LinkedList(Arrays.asList(PropertyEditorManager.getEditorSearchPath()));
        editorSearchPath.add("org.xbean.spring.context.impl");
        PropertyEditorManager.setEditorSearchPath((String[]) editorSearchPath.toArray(new String[editorSearchPath.size()]));
        assertTrue(Utils.isSimpleType(Type.newSimpleType("java.net.URI")));
    }
}
