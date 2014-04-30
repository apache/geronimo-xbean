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

import static org.apache.xbean.recipe.Person.ConstructionCalled;
import static org.apache.xbean.recipe.Person.ConstructionCalled.*;
import junit.framework.Assert;

public class PersonFactory {
    private String name;
    private int age;
    private URL homePage;
    private Car car;
    private ConstructionCalled constructionCalled;

    public PersonFactory() {
        constructionCalled = PERSON_FACTORY;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public PersonFactory(String name, int age, URL homePage) {
        Assert.fail("3 arg instance factory constructor should never be called");
    }

    public PersonFactory(String name, int age, URL homePage, Car car) {
        this.name = name;
        this.age = age;
        this.homePage = homePage;
        this.car = car;
        constructionCalled = PERSON_FACTORY_4_ARG;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public PersonFactory(String name, int age, URL homePage, Car car, String unused) {
        Assert.fail("3 arg instance factory constructor should never be called");
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

    public Person create() {
        return new Person(name, age, homePage, car, constructionCalled);
    }
}
