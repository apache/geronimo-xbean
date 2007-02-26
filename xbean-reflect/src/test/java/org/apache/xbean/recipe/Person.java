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
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;

/**
 * @version $Rev$ $Date$
 */
public class Person {
    private String name;
    private int age;
    private URL homePage;
    private Map<String,Object> unsetMap;
    private Properties unsetProperties;

    public Person() {
    }

    public Person(String name, int age, URL homePage) {
        this.name = name;
        this.age = age;
        this.homePage = homePage;
    }

    public static Person newInstance(){
        return new Person();
    }

    public static Person newInstance(String name, int age, URL homePage){
        return new Person(name, age, homePage);        
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

    public String toString() {
        return super.toString() + " (name=\"" + name + "\", age=\"" + age + "\", homePage=\"" + homePage + "\")";
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Person person = (Person) o;

        if (age != person.age) return false;
        if (homePage != null ? !homePage.equals(person.homePage) : person.homePage != null) return false;
        if (name != null ? !name.equals(person.name) : person.name != null) return false;
        if (unsetMap != null ? !unsetMap.equals(person.unsetMap) : person.unsetMap != null) return false;
        if (unsetProperties != null ? !unsetProperties.equals(person.unsetProperties) : person.unsetProperties != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (name != null ? name.hashCode() : 0);
        result = 31 * result + age;
        result = 31 * result + (homePage != null ? homePage.hashCode() : 0);
        result = 31 * result + (unsetMap != null ? unsetMap.hashCode() : 0);
        result = 31 * result + (unsetProperties != null ? unsetProperties.hashCode() : 0);
        return result;
    }
}
