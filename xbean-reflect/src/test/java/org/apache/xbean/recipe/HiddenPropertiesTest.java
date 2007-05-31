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

import java.net.URL;

/**
 * @version $Rev$ $Date$
 */
public class HiddenPropertiesTest extends TestCase {

    public void test() throws Exception {
        ObjectRecipe recipe = new ObjectRecipe(Child.class);
        recipe.setProperty("age", 10);
        recipe.setProperty("website", "http://foo.com");
        recipe.setProperty(Child.class.getName()+"/name", "TheChild");
        recipe.setProperty(Parent.class.getName()+"/name", "TheParent");
        recipe.setProperty(Child.class.getName()+"/color", "Red");
        recipe.setProperty(Parent.class.getName()+"/color", "Blue");

        recipe.allow(Option.FIELD_INJECTION);
        recipe.allow(Option.PRIVATE_PROPERTIES);

        Child child = (Child) recipe.create();

        assertEquals("Child.getChildName()", "TheChild", child.getChildName());
        assertEquals("Child.getParentName()", "TheParent", child.getParentName());
        assertEquals("Child.getChildColor()", "Red", child.getChildColor());
        assertEquals("Child.getParentColor()", "Blue", child.getParentColor());
        assertEquals("Child.getWebsite()", new URL("http://foo.com"), child.getWebsite());
        assertEquals("Child.getAge()", 10, child.getAge());
    }
}
