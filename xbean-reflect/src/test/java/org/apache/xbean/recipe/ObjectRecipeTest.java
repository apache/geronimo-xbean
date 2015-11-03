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

import junit.framework.TestCase;
import org.apache.xbean.propertyeditor.PropertyEditors;
import static org.apache.xbean.recipe.Person.ConstructionCalled.CONSTRUCTOR;
import static org.apache.xbean.recipe.Person.ConstructionCalled.CONSTRUCTOR_4_ARG;
import static org.apache.xbean.recipe.Person.ConstructionCalled.NEW_INSTANCE;
import static org.apache.xbean.recipe.Person.ConstructionCalled.NEW_INSTANCE_4_ARG;
import static org.apache.xbean.recipe.Person.ConstructionCalled.PERSON_FACTORY;
import static org.apache.xbean.recipe.Person.ConstructionCalled.PERSON_FACTORY_4_ARG;

import java.net.URL;

public class ObjectRecipeTest extends TestCase {

    protected void setUp() throws Exception {
        PropertyEditors.class.getName();
    }

    public void testSetters() throws Exception {

        ObjectRecipe objectRecipe = new ObjectRecipe(Person.class);
        doTest(objectRecipe, CONSTRUCTOR);
    }

    public void testConstructor() throws Exception {

        ObjectRecipe objectRecipe = new ObjectRecipe(Person.class, new String[]{"name", "age", "homePage", "car"}, new Class[]{String.class, Integer.TYPE, URL.class, Car.class});
        doTest(objectRecipe, CONSTRUCTOR_4_ARG);
    }

    public void testConstructorWithImpliedTypes() throws Exception {

        ObjectRecipe objectRecipe = new ObjectRecipe(Person.class, new String[]{"name", "age", "homePage", "car"}, null);
        doTest(objectRecipe, CONSTRUCTOR_4_ARG);
    }

    public void testConstructorWithNamedParameters() throws Exception {

        ObjectRecipe objectRecipe = new ObjectRecipe(Person.class);
        objectRecipe.allow(Option.NAMED_PARAMETERS);
        doTest(objectRecipe, CONSTRUCTOR_4_ARG);
    }

    public void testStaticFactoryMethodAndSetters() throws Exception {

        ObjectRecipe objectRecipe = new ObjectRecipe(Person.class, "newInstance");
        doTest(objectRecipe, NEW_INSTANCE);
    }

    public void testStaticFactoryMethodWithParams() throws Exception {

        ObjectRecipe objectRecipe = new ObjectRecipe(Person.class, "newInstance", new String[]{"name", "age", "homePage", "car"}, new Class[]{String.class, Integer.TYPE, URL.class, Car.class});
        doTest(objectRecipe, NEW_INSTANCE_4_ARG);
    }

    public void testStaticFactoryMethodWithImpliedTypes() throws Exception {

        ObjectRecipe objectRecipe = new ObjectRecipe(Person.class, "newInstance", new String[]{"name", "age", "homePage", "car"});
        doTest(objectRecipe, NEW_INSTANCE_4_ARG);
    }

    public void testStaticFactoryMethodWithNamedParameters() throws Exception {

        ObjectRecipe objectRecipe = new ObjectRecipe(Person.class, "newInstance");
        objectRecipe.allow(Option.NAMED_PARAMETERS);
        doTest(objectRecipe, NEW_INSTANCE_4_ARG);
    }

    public void testInstanceFactorySetters() throws Exception {

        ObjectRecipe objectRecipe = new ObjectRecipe(PersonFactory.class, "create");
        doTest(objectRecipe, PERSON_FACTORY);
    }

    public void testInstanceFactoryConstructor() throws Exception {

        ObjectRecipe objectRecipe = new ObjectRecipe(PersonFactory.class, "create", new String[]{"name", "age", "homePage", "car"}, new Class[]{String.class, Integer.TYPE, URL.class, Car.class});
        doTest(objectRecipe, PERSON_FACTORY_4_ARG);
    }

    public void testInstanceFactoryConstructorWithImpliedTypes() throws Exception {

        ObjectRecipe objectRecipe = new ObjectRecipe(PersonFactory.class, "create", new String[]{"name", "age", "homePage", "car"});
        doTest(objectRecipe, PERSON_FACTORY_4_ARG);
    }

    public void testInstanceFactoryConstructorWithNamedParameters() throws Exception {

        ObjectRecipe objectRecipe = new ObjectRecipe(PersonFactory.class, "create");
        objectRecipe.allow(Option.NAMED_PARAMETERS);
        doTest(objectRecipe, PERSON_FACTORY_4_ARG);
    }

    public void testWhitespaceInjection() throws Exception {
        String name = " Foo Bar ";
        char ch = ' ';
        
        ObjectRecipe objectRecipe = new ObjectRecipe(Value.class);
        objectRecipe.setProperty("name", name);
        objectRecipe.setProperty("type", " ");
        Value c = (Value) objectRecipe.create();
        
        assertEquals(name, c.name);
        assertEquals(ch, c.type);
    }
    
    public static class Value {
        public String name;
        public char type;
    }
    
    private void doTest(ObjectRecipe objectRecipe, Person.ConstructionCalled expectedConstruction) throws Exception {
        Person expected = new Person("Joe", 21, new URL("http://www.acme.org"), new Car("Mini", "Cooper", 2008));

        objectRecipe.setProperty("name", "Joe");
        objectRecipe.setProperty("age", "21");
        objectRecipe.setProperty("homePage", "http://www.acme.org");

        ObjectRecipe carRecipe = new ObjectRecipe(Car.class, new String[]{"make", "model", "year"});
        carRecipe.setProperty("make", "Mini");
        carRecipe.setProperty("model", "Cooper");
        carRecipe.setProperty("year", "2008");
        objectRecipe.setProperty("car", carRecipe);

        Person actual = (Person) objectRecipe.create(Person.class.getClassLoader());
        assertEquals("person", expected, actual);

        assertEquals(expectedConstruction, actual.getConstructionCalled());
    }

    public void testCompoundProperties() throws Exception {
        ObjectRecipe objectRecipe = new ObjectRecipe(Component.class);
        objectRecipe.setCompoundProperty("name", "Adam");
        objectRecipe.setCompoundProperty("box.height", 5);
        objectRecipe.setCompoundProperty("box.width", 10);
        
        objectRecipe.setCompoundProperty("component.name", "Eva");
        objectRecipe.setCompoundProperty("component.box.height", 10);
        objectRecipe.setCompoundProperty("component.box.width", 20);
                
        Component component = (Component) objectRecipe.create(Component.class.getClassLoader());
        
        assertEquals("Adam", component.getName());
        assertEquals(5, component.getBox().getHeight());
        assertEquals(10, component.getBox().getWidth());
        
        assertEquals("Eva", component.getComponent().getName());
        assertEquals(10, component.getComponent().getBox().getHeight());
        assertEquals(20, component.getComponent().getBox().getWidth());
    }

    public void testStringCharArray() {
        {
            final ObjectRecipe recipe = new ObjectRecipe(StringCharArray.class);
            recipe.setProperty("chars", "v1");
            recipe.setProperty("string", "v2");
            final StringCharArray v = StringCharArray.class.cast(recipe.create());
            assertEquals("v1", new String(v.chars));
            assertEquals("v2", v.string);
        }
        {
            final ObjectRecipe recipe = new ObjectRecipe(StringCharArray.class);
            recipe.setProperty("chars", "v1".toCharArray());
            recipe.setProperty("string", "v2".toCharArray());
            final StringCharArray v = StringCharArray.class.cast(recipe.create());
            assertEquals("v1", new String(v.chars));
            assertEquals("v2", v.string);
        }
    }
    
    public static class Component {
                
        String name;
        Box box;
        Component component;
        
        public Box getBox() {
            if (box == null) {
                box = new Box();
            }
            return box;
        }
        
        public Component getComponent() {
            if (component == null) {
                component = new Component();
            }
            return component;
        }
        
        public Component getNullComponent() {
            return null;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }                
            
    }

    public static class StringCharArray {
        private char[] chars;
        private String string;

        public void setChars(final char[] chars) {
            this.chars = chars;
        }

        public void setString(final String string) {
            this.string = string;
        }
    }
}
