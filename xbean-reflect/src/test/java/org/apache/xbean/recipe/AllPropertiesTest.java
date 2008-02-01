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

import java.net.URL;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

public class AllPropertiesTest extends TestCase {
    public void testSetters() throws Exception {
        ObjectRecipe objectRecipe = new ObjectRecipe(Person.class);
        doTest(objectRecipe);
    }

    private void doTest(ObjectRecipe objectRecipe) throws Exception {
        Person expected = new Person("Joe", 21, new URL("http://www.acme.org"), null);

        AllPropertiesRecipe allPropertiesRecipe = new AllPropertiesRecipe();

        Properties properties = new Properties();
        properties.setProperty("name", "Joe");
        properties.setProperty("age", "21");
        properties.setProperty("homePage", "http://www.acme.org");
        properties.setProperty("Fake Property", "Fake Value");
        properties.put("allMap", allPropertiesRecipe);
        properties.put("allProperties", allPropertiesRecipe);
        expected.setAllProperties(properties);
        expected.setAllMap((Map)properties);

        objectRecipe.setProperty("name", "Joe");
        objectRecipe.setProperty("age", "21");
        objectRecipe.setProperty("homePage", "http://www.acme.org");
        objectRecipe.setProperty("Fake Property", "Fake Value");
        objectRecipe.setProperty("allMap", allPropertiesRecipe);
        objectRecipe.setProperty("allProperties", allPropertiesRecipe);
        objectRecipe.allow(Option.IGNORE_MISSING_PROPERTIES);

        Person actual = (Person) objectRecipe.create(Person.class.getClassLoader());
        assertEquals(properties, actual.getAllProperties());
        assertEquals(properties, actual.getAllMap());
        assertEquals("person", expected, actual);
    }
}