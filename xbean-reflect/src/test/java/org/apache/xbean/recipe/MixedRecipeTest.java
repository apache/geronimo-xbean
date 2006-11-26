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
import org.apache.xbean.propertyeditor.PropertyEditors;

import java.net.URL;

public class MixedRecipeTest extends TestCase {

    protected void setUp() throws Exception {
        PropertyEditors.class.getName();
    }

    public void testAll() throws Exception {
        Widget expected = new Widget("uno", 2, true, "quatro", Object.class, "seis");

        ObjectRecipe objectRecipe = new ObjectRecipe(Widget.class, new String[]{"one", "two"}, new Class[]{String.class, int.class});
        objectRecipe.allow(Option.PRIVATE_PROPERTIES);
        objectRecipe.allow(Option.FIELD_INJECTION);
        objectRecipe.setProperty("one", "uno");
        objectRecipe.setProperty("two", "2");
        objectRecipe.setProperty("three", "true");
        objectRecipe.setProperty("four", "quatro");
        objectRecipe.setProperty("five", "java.lang.Object");
        objectRecipe.setProperty("six", "seis");

        Widget actual = (Widget) objectRecipe.create(Widget.class.getClassLoader());
        assertEquals("widget", expected, actual);
    }

}
