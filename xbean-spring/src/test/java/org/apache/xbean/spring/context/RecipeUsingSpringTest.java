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
package org.apache.xbean.spring.context;

import java.util.List;

import org.apache.xbean.test.support.example.Recipe;
import org.apache.xbean.test.support.example.RecipeService;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class RecipeUsingSpringTest extends SpringTestSupport {

    public void testRecipes() throws Exception {
        RecipeService svc = (RecipeService) getBean("recipeService");

        List list = svc.getRecipes();
        assertNotNull(list);
        assertEquals(2, list.size());
        Recipe r = (Recipe) list.get(0);
        assertEquals("Food", r.getIngredients());
        assertEquals("Mash together", r.getInstructions());
        
        r = (Recipe) list.get(1);
        assertEquals("Food", r.getIngredients());
        assertEquals("Mash together", r.getInstructions());
        
        assertNotNull(svc.getTopRecipe());
        assertEquals("Food", svc.getTopRecipe().getIngredients());
    }

    protected AbstractXmlApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("org/apache/xbean/spring/context/recipe-normal.xml");
    }

}
