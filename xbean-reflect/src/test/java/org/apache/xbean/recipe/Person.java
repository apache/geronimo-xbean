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

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import junit.framework.Assert;
import static org.apache.xbean.recipe.Person.ConstructionCalled.*;

/**
 * @version $Rev$ $Date$
 */
public class Person {
    public static enum ConstructionCalled {
        CONSTRUCTOR,
        CONSTRUCTOR_4_ARG,
        NEW_INSTANCE,
        NEW_INSTANCE_4_ARG,
        PERSON_FACTORY,
        PERSON_FACTORY_4_ARG,
    }

    private String name;
    private int age;
    private URL homePage;
    private Car car;
    private Map<String, Object> unsetMap;
    private Properties unsetProperties;
    private Map<String, Object> allMap;
    private Properties allProperties;

    private ConstructionCalled constructionCalled;

    public Person() {
        this(null, 0, null, null, CONSTRUCTOR);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public Person(String name, int age, URL homePage) {
        Assert.fail("3 arg constructor should never be called");
    }

    public Person(String name, int age, URL homePage, Car car) {
        this(name, age, homePage, car, CONSTRUCTOR_4_ARG);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public Person(String name, int age, URL homePage, Car car, String unused) {
        Assert.fail("5 arg constructor should never be called");
    }

    Person(String name, int age, URL homePage, Car car, ConstructionCalled constructionCalled) {
        this.name = name;
        this.age = age;
        this.homePage = homePage;
        this.car = car;
        this.constructionCalled = constructionCalled;
    }

    public static Person newInstance() {
        return new Person(null, 0, null, null, NEW_INSTANCE);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static Person newInstance(String name, int age, URL homePage) {
        Assert.fail("3 arg static factory should never be called");
        return null;
    }

    public static Person newInstance(String name, int age, URL homePage, Car car) {
        return new Person(name, age, homePage, car, NEW_INSTANCE_4_ARG);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static Person newInstance(String name, int age, URL homePage, Car car, String unused) {
        Assert.fail("5 arg static factory should never be called");
        return null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public URL getHomePage() {
        return homePage;
    }

    public void setHomePage(URL homePage) {
        this.homePage = homePage;
    }

    public Car getCar() {
        return car;
    }

    public void setCar(Car car) {
        this.car = car;
    }

    public Map<String, Object> getUnsetMap() {
        return unsetMap;
    }

    public void setUnsetMap(Map<String, Object> unsetMap) {
        this.unsetMap = new HashMap<String, Object>(unsetMap);
    }

    public Properties getUnsetProperties() {
        return unsetProperties;
    }

    public void setUnsetProperties(Properties unsetProperties) {
        this.unsetProperties = unsetProperties;
    }

    public Map<String, Object> getAllMap() {
        return allMap;
    }

    public void setAllMap(Map<String, Object> allMap) {
        this.allMap = allMap;
    }

    public Properties getAllProperties() {
        return allProperties;
    }

    public void setAllProperties(Properties allProperties) {
        this.allProperties = allProperties;
    }

    public ConstructionCalled getConstructionCalled() {
        return constructionCalled;
    }

    public String toString() {
        return "[Person: name=\"" + name + "\", age=\"" + age + "\", homePage=\"" + homePage +"\", car=\"" + car + "\"]";
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Person person = (Person) o;

        if (age != person.age) return false;
        if (homePage != null ? !homePage.equals(person.homePage) : person.homePage != null) return false;
        if (name != null ? !name.equals(person.name) : person.name != null) return false;
        if (car != null ? !car.equals(person.car) : person.car != null) return false;
        if (unsetMap != null ? !unsetMap.equals(person.unsetMap) : person.unsetMap != null) return false;
        if (unsetProperties != null ? !unsetProperties.equals(person.unsetProperties) : person.unsetProperties != null) return false;
        if (allMap != null ? !allMap.equals(person.allMap) : person.allMap != null) return false;
        if (allProperties != null ? !allProperties.equals(person.allProperties) : person.allProperties != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (name != null ? name.hashCode() : 0);
        result = 31 * result + age;
        result = 31 * result + (homePage != null ? homePage.hashCode() : 0);
        result = 31 * result + (car != null ? car.hashCode() : 0);
        result = 31 * result + (unsetMap != null ? unsetMap.hashCode() : 0);
        result = 31 * result + (unsetProperties != null ? unsetProperties.hashCode() : 0);
        result = 31 * result + (allMap != null ? allMap.hashCode() : 0);
        result = 31 * result + (allProperties != null ? allProperties.hashCode() : 0);
        return result;
    }
}
