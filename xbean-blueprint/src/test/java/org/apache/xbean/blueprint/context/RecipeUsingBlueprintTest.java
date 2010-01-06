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
package org.apache.xbean.blueprint.context;

import java.util.List;

import org.apache.xbean.blueprint.example.Recipe;
import org.apache.xbean.blueprint.example.RecipeService;
import org.apache.aries.blueprint.reflect.BeanMetadataImpl;
import org.apache.aries.blueprint.reflect.CollectionMetadataImpl;
import org.osgi.service.blueprint.reflect.Metadata;

public class RecipeUsingBlueprintTest extends BlueprintTestSupport {

    public void testRecipes() throws Exception {
        BeanMetadataImpl svc = (BeanMetadataImpl) reg.getComponentDefinition("recipeService");

        List<Metadata> list = ((CollectionMetadataImpl)propertyByName("recipes", svc).getValue()).getValues();
        assertNotNull(list);
        assertEquals(2, list.size());
        BeanMetadataImpl r = (BeanMetadataImpl) list.get(0);
        checkPropertyValue("ingredients", "Food", r);
        checkPropertyValue("instructions", "Mash together", r);
        
        r = (BeanMetadataImpl) list.get(1);
        checkPropertyValue("ingredients", "Food", r);
        checkPropertyValue("instructions", "Mash together", r);

        BeanMetadataImpl topRecipe = (BeanMetadataImpl) propertyByName("topRecipe", svc).getValue();
        assertNotNull(topRecipe);
        checkPropertyValue("ingredients", "Food", topRecipe);
    }

    protected String getPlan() {
        return "org/apache/xbean/blueprint/context/recipe-normal.xml";
    }
}
