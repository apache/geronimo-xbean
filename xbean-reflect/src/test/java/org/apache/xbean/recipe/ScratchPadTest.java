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

import java.util.Properties;

/**
 * @version $Rev$ $Date$
 */
public class ScratchPadTest extends TestCase {

    /**
     * Plain static factory that consumes properties.  The same properties bucket is
     * also used to hidrate the instance the factory produces.
     * 
     * @throws Exception
     */
    public void testStaticFactory() throws Exception {
        ObjectRecipe recipe = new ObjectRecipe(ColorStaticFactory.class);
        recipe.setFactoryMethod("createColor");
        recipe.setConstructorArgNames(new String[]{"type"});
        recipe.setProperty("type", RGBColor.class.getName()); // using a string to show conversion

        recipe.setProperty("r", "255");

        RGBColor red = (RGBColor) recipe.create();

        assertEquals("Red.getR()", 255, red.getR());
        assertEquals("Red.getG()", 0, red.getG());
        assertEquals("Red.getB()", 0, red.getB());
    }

    /**
     * This technique is in pretty heavy use in Geronimo and OpenEJB, primarily
     * for injecting into things created by othe frameworks.
     *
     * Would be nice to be able to pass the instance in the ObjectRecipe
     * constructor instead of a class.  It's an enhancement that we've
     * always wanted but haven't had time for.
     * 
     * @throws Exception
     */
    public void testInjectIntoExistingInstance() throws Exception {

        RGBColor rgbColor = new RGBColor();

        ObjectRecipe recipe = new ObjectRecipe(PassthroughFactory.class);
        recipe.setFactoryMethod("create");
        recipe.setConstructorArgNames(new String[]{"instance"});
        recipe.setProperty("instance", rgbColor);

        recipe.setProperty("r", "255");

        RGBColor red = (RGBColor) recipe.create();

        assertSame(rgbColor, red);

        assertEquals("Red.getR()", 255, red.getR());
        assertEquals("Red.getG()", 0, red.getG());
        assertEquals("Red.getB()", 0, red.getB());

        /* Ideally the enhanced version would look like this */
        /*
        ObjectRecipe recipe = new ObjectRecipe(new RGBColor());

        recipe.setProperty("r", "255");

        RGBColor red = (RGBColor) recipe.create();
        */
    }

    public static class PassthroughFactory {
        public static Object create(Object instance) {
            return instance;
        }
    }


    // ---------------------------------------------------- //


    /**
     * Was surprised this doesn't work.  The arguments are assumed to be
     * constructor args.  Not critical if we can get the scenario after
     * this one working.
     *
     * @throws Exception
     */
    public void _testInstanceFactoryWithMethodArgs() throws Exception {

        ObjectRecipe recipe = new ObjectRecipe(ColorInstanceFactory.class);
        recipe.setFactoryMethod("createColor");
        recipe.setConstructorArgNames(new String[]{"type"});
        recipe.setProperty("type", RGBColor.class.getName());
        recipe.setProperty("foo", "bar");
        recipe.setProperty("r", "255");

        RGBColor red = (RGBColor) recipe.create();

        assertEquals("Red.getR()", 255, red.getR());
        assertEquals("Red.getG()", 0, red.getG());
        assertEquals("Red.getB()", 0, red.getB());

        /* Could be implemented with a new "setFactoryArgNames(...)" method
         * so that it is clear where the args should not be used for constructor
         * resolution
         */
        /*
        ObjectRecipe recipe = new ObjectRecipe(ColorInstanceFactory.class);
        recipe.setFactoryMethod("createColor");
        recipe.setFactoryArgNames(new String[]{"type"});
        recipe.setProperty("type", RGBColor.class.getName());
        recipe.setProperty("foo", "bar");
        recipe.setProperty("r", "255");

        RGBColor red = (RGBColor) recipe.create();
        */
    }


    // ---------------------------------------------------- //

    /**
     * Not really "doable" with current code even using tricks.
     *
     * We're going to need this for JSR-299 as well.
     *
     * @throws Exception
     */
    public void testReusableInstanceFactory() throws Exception {

        // This part is fine
        ObjectRecipe factoryRecipe = new ObjectRecipe(ColorInstanceFactory.class);
        factoryRecipe.setProperty("foo", "bar");

        ColorInstanceFactory colorInstanceFactory = (ColorInstanceFactory) factoryRecipe.create();

        /* Ideally this would leverage the related enhancement and would look like this */
        /*
        ObjectRecipe recipe = new ObjectRecipe(colorInstanceFactory);
        recipe.setFactoryMethod("createColor");
        recipe.setFactoryArgNames(new String[]{"type"});
        recipe.setProperty("type", RGBColor.class.getName());

        recipe.setProperty("r", "255");

        RGBColor red = (RGBColor) recipe.create();
        */
    }

    // ---------------------------------------------------- //

    /**
     * Another feature we'll need for JCDI (JSR-299)
     * 
     * @throws Exception
     */
    public void _testMultiParameterSetters() throws Exception {
        ObjectRecipe recipe = new ObjectRecipe(RGBColor2.class);
        recipe.setProperty("r", "255");
        recipe.setProperty("g", "0");
        recipe.setProperty("b", "0");

        // Not sure what additional meta-data we can exect
        // from 299, if any, that will make this example work.

        /* recipe.????(????); */

        // and finally create the object
        
        RGBColor2 red = (RGBColor2) recipe.create();

        assertEquals("Red.getR()", 255, red.getR());
        assertEquals("Red.getG()", 0, red.getG());
        assertEquals("Red.getB()", 0, red.getB());
    }

    public static class RGBColor2 extends Color {
        private int r;
        private int g;
        private int b;

        public void setRGB(int r, int g, int b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public int getR() {
            return r;
        }

        public int getG() {
            return g;
        }

        public int getB() {
            return b;
        }
    }


    // ---------------------------------------------------- //

    public static class Color {
    }

    public static class ColorStaticFactory {

        public static Color createColor(Class type) throws IllegalAccessException, InstantiationException {
            return (Color) type.newInstance();
        }

    }

    public static class ColorInstanceFactory {
        public void setFoo(String foo) {
        }

        public Color createColor(Class type) throws IllegalAccessException, InstantiationException {
            return (Color) type.newInstance();
        }
    }

    public static class RGBColor extends Color {
        private int r;
        private int g;
        private int b;

        public int getR() {
            return r;
        }

        public void setR(int r) {
            this.r = r;
        }

        public int getG() {
            return g;
        }

        public void setG(int g) {
            this.g = g;
        }

        public int getB() {
            return b;
        }

        public void setB(int b) {
            this.b = b;
        }
    }

    public static class YUVColor extends Color {
        private int y;
        private int u;
        private int v;

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getU() {
            return u;
        }

        public void setU(int u) {
            this.u = u;
        }

        public int getV() {
            return v;
        }

        public void setV(int v) {
            this.v = v;
        }
    }
}
