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
package org.apache.xbean.recipe;
/**
 * @version $Rev$ $Date$
 */

import junit.framework.TestCase;
import org.apache.xbean.propertyeditor.PropertyEditors;

import java.net.URL;
import java.net.MalformedURLException;

public class ObjectRecipeTest extends TestCase {

    protected void setUp() throws Exception {
        PropertyEditors.class.getName();
    }

    public void testSetters() throws Exception {

        ObjectRecipe objectRecipe = new ObjectRecipe(Person.class);
        doTest(objectRecipe);
    }

    public void testConstructor() throws Exception {

        ObjectRecipe objectRecipe = new ObjectRecipe(Person.class, new String[]{"name", "age", "homePage"}, new Class[]{String.class, Integer.TYPE, URL.class});
        doTest(objectRecipe);
    }

    public void testConstructorWithImpliedTypes() throws Exception {

        ObjectRecipe objectRecipe = new ObjectRecipe(Person.class, new String[]{"name", "age", "homePage"}, null);
        doTest(objectRecipe);
    }

    public void testFactoryMethodAndSetters() throws Exception {

        ObjectRecipe objectRecipe = new ObjectRecipe(Person.class, "newInstance");
        doTest(objectRecipe);
    }

    public void testFactoryMethodWithParams() throws Exception {

        ObjectRecipe objectRecipe = new ObjectRecipe(Person.class, "newInstance", new String[]{"name", "age", "homePage"}, new Class[]{String.class, Integer.TYPE, URL.class});
        doTest(objectRecipe);
    }

    public void testFactoryMethodWithImpliedTypes() throws Exception {

        ObjectRecipe objectRecipe = new ObjectRecipe(Person.class, "newInstance", new String[]{"name", "age", "homePage"}, null);
        doTest(objectRecipe);
    }

    private void doTest(ObjectRecipe objectRecipe) throws Exception {
        Person expected = new Person("Joe", 21, new URL("http://www.acme.org"));

        objectRecipe.setProperty("name", "Joe");
        objectRecipe.setProperty("age", "21");
        objectRecipe.setProperty("homePage", "http://www.acme.org");

        Person actual = (Person) objectRecipe.create(Person.class.getClassLoader());
        assertEquals("person", expected, actual);
    }

}
