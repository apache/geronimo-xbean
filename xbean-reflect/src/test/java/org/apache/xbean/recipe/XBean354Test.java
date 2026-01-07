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

import org.junit.Test;

import java.net.URL;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeFalse;


public class XBean354Test {
    @Test
    public void testRecipeBootstrapClass() {
        // broken on java 1.8 because openjdk seems to have been compiled without parameter names for constructors
        assumeFalse("JDK not 1.8", System.getProperty("java.version").startsWith("1.8"));

        ObjectRecipe recipe = new ObjectRecipe(URL.class);
        recipe.setConstructorArgNames(Collections.singletonList("spec"));
        recipe.setAllProperties(Collections.singletonMap("spec", "https://apache.org"));

        URL created = (URL) recipe.create();

        assertNotNull(created);
        assertEquals("https://apache.org", created.toString());
    }
}
