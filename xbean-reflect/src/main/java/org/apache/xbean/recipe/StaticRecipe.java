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
package org.apache.xbean.recipe;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Collections;

/**
 * @deprecated The functionality of StaticRecipe is built into ObjectRecipe as of xbean-reflect 3.4
 * @version $Rev$ $Date$
 */
public final class StaticRecipe implements Recipe {

    private final Object object;

    public StaticRecipe(Object object) {
        this.object = object;
    }

    public boolean canCreate(Type type) {
        return RecipeHelper.isInstance(type, object);
    }

    public Object create() throws ConstructionException {
        return object;
    }

    public Object create(ClassLoader classLoader) throws ConstructionException {
        return create();
    }

    public Object create(Type expectedType, boolean lazyRefAllowed) throws ConstructionException {
        return create();
    }

    public List<Recipe> getNestedRecipes() {
        return Collections.emptyList();
    }

    public List<Recipe> getConstructorRecipes() {
        return Collections.emptyList();
    }

    public String getName() {
        return object.getClass().getName();
    }

    public float getPriority() {
        return 0;
    }
}
