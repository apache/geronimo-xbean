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
public class MatchBytypeTest extends TestCase {

    public void testMatch() throws Exception {
        ObjectRecipe recipe = new ObjectRecipe(Child.class);
        recipe.setAutoMatchProperty("java.net.URL", new URL("http://foo.com"));

        recipe.allow(Option.FIELD_INJECTION);
        recipe.allow(Option.PRIVATE_PROPERTIES);

        Child child = (Child) recipe.create();

        assertEquals("Child.getWebsite()", new URL("http://foo.com"), child.getWebsite());
    }

    public void testNoMatch() throws Exception {
        ObjectRecipe recipe = new ObjectRecipe(Child.class);
        recipe.setAutoMatchProperty("java.lang.String", "some string value");

        recipe.allow(Option.FIELD_INJECTION);
        recipe.allow(Option.PRIVATE_PROPERTIES);

        try {
            recipe.create();
            fail("Expected MissingAccessorException");
        } catch (MissingAccessorException expected) {
        }
    }
}