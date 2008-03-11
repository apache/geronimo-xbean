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

/**
 * @version $Rev$ $Date$
 */
public class EnumTest extends TestCase {

    public void test() throws Exception {

        Canvas expected = new Canvas();
        expected.color = Color.RED;

        ObjectRecipe recipe = new ObjectRecipe(Canvas.class);
        recipe.setProperty("color", "RED");

        Canvas actual = (Canvas) recipe.create();

        assertEquals("Color", expected.getColor(), actual.getColor());

    }

    public static class Canvas {

        public Color color;

        public Color getColor() {
            return color;
        }
    }

    public static enum Color {
        RED, GREEN, BLUE;
    }
}
