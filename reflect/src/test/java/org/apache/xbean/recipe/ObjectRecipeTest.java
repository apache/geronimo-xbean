package org.apache.xbean.recipe;
/**
 * @version $Rev$ $Date$
 */

import junit.framework.TestCase;
import org.apache.xbean.propertyeditor.PropertyEditors;

import java.net.URL;

public class ObjectRecipeTest extends TestCase {
    ObjectRecipe objectRecipe;

    public void testSetters() throws Exception {
        PropertyEditors.class.getName();

        Person expected = new Person("Joe", 21, new URL("http://www.acme.org"));

        ObjectRecipe objectRecipe = new ObjectRecipe(Person.class);
        objectRecipe.setProperty("name", "Joe");
        objectRecipe.setProperty("age", "21");
        objectRecipe.setProperty("homePage", "http://www.acme.org");

        Person actual = (Person) objectRecipe.create(Person.class.getClassLoader());
        assertEquals("person", expected, actual);
    }

    public void testConstructor() throws Exception {
        PropertyEditors.class.getName();

        Person expected = new Person("Joe", 21, new URL("http://www.acme.org"));

        ObjectRecipe objectRecipe = new ObjectRecipe(Person.class,new String[]{"name","age","homePage"},new Class[]{String.class, Integer.TYPE, URL.class});
        objectRecipe.setProperty("name", "Joe");
        objectRecipe.setProperty("age", "21");
        objectRecipe.setProperty("homePage", "http://www.acme.org");

        Person actual = (Person) objectRecipe.create(Person.class.getClassLoader());
        assertEquals("person", expected, actual);
    }


}