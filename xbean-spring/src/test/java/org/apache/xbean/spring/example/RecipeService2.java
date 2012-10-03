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
package org.apache.xbean.spring.example;

import java.util.List;

/**
 * @org.apache.xbean.XBean element="recipe-service2"
 * @author Dan Diephouse
 */
public class RecipeService2
{
    private final List recipes;
    private final Recipe topRecipe;


    public RecipeService2(List recipes, Recipe topRecipe) {
        this.recipes = recipes;
        this.topRecipe = topRecipe;
    }

    /**
     * @org.apache.xbean.FlatCollection childElement="recipe"
     * @return
     */
    public List getRecipes()
    {
        return recipes;
    }

    /**
     * @org.apache.xbean.Flat
     * @return
     */
    public Recipe getTopRecipe() {
        return topRecipe;
    }
}
