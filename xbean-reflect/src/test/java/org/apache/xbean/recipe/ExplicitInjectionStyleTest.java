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

public class ExplicitInjectionStyleTest extends TestCase {

    protected void setUp() throws Exception {
        PropertyEditors.class.getName();
    }

    public void testAll() throws Exception {
        Box expected = new Box(10, 20, 30);


        ObjectRecipe objectRecipe = new ObjectRecipe(Box.class);
        objectRecipe.allow(Option.PRIVATE_PROPERTIES);

        objectRecipe.setProperty("width", "10");
        objectRecipe.setMethodProperty("height", "20");
        objectRecipe.setFieldProperty("depth", "30");

        Box actual = (Box) objectRecipe.create(Box.class.getClassLoader());
        assertEquals("box", expected, actual);

        objectRecipe.setMethodProperty("depth", "15");

        actual = (Box) objectRecipe.create(Box.class.getClassLoader());
        assertEquals("box", expected, actual);
    }

}
