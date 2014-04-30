/**
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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.net.URI;

/**
 * @version $Rev$ $Date$
 */
public class GenericCollectionsTest extends TestCase {

    public void test() throws Exception {
        Something expected = new Something();

        expected.plainList = new ArrayList();
        expected.plainList.add("red");
        expected.plainList.add("green");
        expected.plainList.add("blue");

        expected.plainMap = new LinkedHashMap();
        expected.plainMap.put("red","rojo");
        expected.plainMap.put("green","verde");
        expected.plainMap.put("blue","azul");

        expected.listOfClass = new ArrayList<Class>();
        expected.listOfClass.add(Red.class);
        expected.listOfClass.add(Green.class);
        expected.listOfClass.add(Blue.class);

        expected.listOfURI = new ArrayList<URI>();
        expected.listOfURI.add(new URI("red://rojo"));
        expected.listOfURI.add(new URI("green://verde"));
        expected.listOfURI.add(new URI("blue://azul"));

        expected.listOfEnums = new ArrayList<Color>();
        expected.listOfEnums.add(Color.RED);
        expected.listOfEnums.add(Color.GREEN);
        expected.listOfEnums.add(Color.BLUE);

        expected.mapOfClass = new LinkedHashMap<String, Class>();
        expected.mapOfClass.put("Rojo", Red.class);
        expected.mapOfClass.put("Verde", Green.class);
        expected.mapOfClass.put("Azul", Blue.class);

        expected.mapOfURI = new LinkedHashMap<URI, String>();
        expected.mapOfURI.put(new URI("red://rojo"), "Rojo");
        expected.mapOfURI.put(new URI("green://verde"), "Verde");
        expected.mapOfURI.put(new URI("blue://azul"), "Azul");

        expected.mapOfEnums = new LinkedHashMap<String, Color>();
        expected.mapOfEnums.put("RED", Color.RED);
        expected.mapOfEnums.put("GREEN", Color.GREEN);
        expected.mapOfEnums.put("BLUE", Color.BLUE);

        expected.setOfClass = new LinkedHashSet<Class>();
        expected.setOfClass.add(Red.class);
        expected.setOfClass.add(Green.class);
        expected.setOfClass.add(Blue.class);

        expected.setOfURI = new LinkedHashSet<URI>();
        expected.setOfURI.add(new URI("red://rojo"));
        expected.setOfURI.add(new URI("green://verde"));
        expected.setOfURI.add(new URI("blue://azul"));

        expected.setOfEnums = new LinkedHashSet<Color>();
        expected.setOfEnums.add(Color.RED);
        expected.setOfEnums.add(Color.GREEN);
        expected.setOfEnums.add(Color.BLUE);


        ObjectRecipe recipe = new ObjectRecipe(Something.class);
        recipe.setProperty("plainList", toString(expected.plainList));
        recipe.setProperty("plainMap", toString(expected.plainMap));
        recipe.setProperty("listOfClass", toString(expected.listOfClass));
        recipe.setProperty("listOfURI", toString(expected.listOfURI));
        recipe.setProperty("listOfEnums", toString(expected.listOfEnums));
        recipe.setProperty("mapOfClass", toString(expected.mapOfClass));
        recipe.setProperty("mapOfURI", toString(expected.mapOfURI));
        recipe.setProperty("mapOfEnums", toString(expected.mapOfEnums));
        recipe.setProperty("setOfClass", toString(expected.setOfClass));
        recipe.setProperty("setOfURI", toString(expected.setOfURI));
        recipe.setProperty("setOfEnums", toString(expected.setOfEnums));

        Something actual = (Something) recipe.create();

        assertEquals("PlainList", expected.getPlainList(), actual.getPlainList());
        assertEquals("PlainMap", expected.getPlainMap(), actual.getPlainMap());
        assertEquals("ListOfClass", expected.getListOfClass(), actual.getListOfClass());
        assertEquals("ListOfURI", expected.getListOfURI(), actual.getListOfURI());
        assertEquals("MapOfClass", expected.getMapOfClass(), actual.getMapOfClass());
        assertEquals("MapOfURI", expected.getMapOfURI(), actual.getMapOfURI());
        assertEquals("SetOfClass", expected.getSetOfClass(), actual.getSetOfClass());
        assertEquals("SetOfURI", expected.getSetOfURI(), actual.getSetOfURI());
    }

    private String toString(Map m) {
        Map<Object,Object> map   = m;
        List<String> pairs = new ArrayList<String>();
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            String key = toString(entry.getKey());
            key = key.replaceAll(":","\\\\:");
            String value = toString(entry.getValue());
            pairs.add(key +"="+value);
        }
        return toString(pairs).replace(',', '\n');
    }

    private String toString(Collection collection) {
        StringBuilder s = new StringBuilder();
        for (Object obj : collection) {
            s.append(toString(obj));
            s.append(",");
        }

        s.deleteCharAt(s.length()-1);

        return s.toString();
    }

    private String toString(Object obj) {
        if (obj instanceof Class) {
            return ((Class) obj).getName();
        } else {
            return obj.toString();
        }
    }

    public static class Something {

        public List plainList;

        public Map plainMap;

        public List<Class> listOfClass;

        public List<URI> listOfURI;

        public List<Color> listOfEnums;

        public Map<String,Class> mapOfClass;

        public Map<URI, String> mapOfURI;

        public Map<String, Color> mapOfEnums;

        public Set<Class> setOfClass;

        public Set<URI> setOfURI;

        public Set<Color> setOfEnums;

        public List getPlainList() {
            return plainList;
        }

        public Map getPlainMap() {
            return plainMap;
        }

        public List<Class> getListOfClass() {
            return listOfClass;
        }

        public List<URI> getListOfURI() {
            return listOfURI;
        }

        public Map<String, Class> getMapOfClass() {
            return mapOfClass;
        }

        public Map<URI, String> getMapOfURI() {
            return mapOfURI;
        }

        public Set<Class> getSetOfClass() {
            return setOfClass;
        }

        public Set<URI> getSetOfURI() {
            return setOfURI;
        }

        public List<Color> getListOfEnums() {
            return listOfEnums;
        }

        public Map<String, Color> getMapOfEnums() {
            return mapOfEnums;
        }

        public Set<Color> getSetOfEnums() {
            return setOfEnums;
        }
    }

    public static class Red {}
    public static class Green {}
    public static class Blue {}


    public static enum Color {
        RED, GREEN, BLUE;
    }

}
