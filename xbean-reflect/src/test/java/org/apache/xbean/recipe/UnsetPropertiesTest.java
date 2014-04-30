/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.xbean.recipe;

import junit.framework.TestCase;

import java.net.URL;
import java.util.Collections;
import java.util.Properties;

public class UnsetPropertiesTest extends TestCase {
    public void testSetters() throws Exception {
        ObjectRecipe objectRecipe = new ObjectRecipe(Person.class);
        doTest(objectRecipe);
    }

    private void doTest(ObjectRecipe objectRecipe) throws Exception {
        Person expected = new Person("Joe", 21, new URL("http://www.acme.org"), null);
        expected.setUnsetMap(Collections.<String,Object>singletonMap("Fake Property", "Fake Value"));
        Properties properties = new Properties();
        properties.setProperty("Fake Property", "Fake Value");
        expected.setUnsetProperties(properties);

        objectRecipe.setProperty("name", "Joe");
        objectRecipe.setProperty("age", "21");
        objectRecipe.setProperty("homePage", "http://www.acme.org");
        objectRecipe.setProperty("Fake Property", "Fake Value");
        objectRecipe.setProperty("unsetMap", new UnsetPropertiesRecipe());
        objectRecipe.setProperty("unsetProperties", new UnsetPropertiesRecipe());

        Person actual = (Person) objectRecipe.create(Person.class.getClassLoader());
        assertEquals("person.getUnsetProperties()", properties, actual.getUnsetProperties());
        assertEquals("person.getUnsetMap()", properties, actual.getUnsetMap());
        assertEquals("person", expected, actual);
    }
}
