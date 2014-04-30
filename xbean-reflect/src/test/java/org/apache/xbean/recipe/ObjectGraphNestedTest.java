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

import java.util.Arrays;

public class ObjectGraphNestedTest extends AbstractObjectGraphTest {
    protected Repository createNewRepository() {
        Repository repository = new DefaultRepository();

        ObjectRecipe radiohead = new ObjectRecipe(Artist.class, new String[]{"name"});
        radiohead.setName("Radiohead");
        radiohead.setProperty("name", "Radiohead");
        repository.add("Radiohead", radiohead);

        ObjectRecipe highAndDry = new ObjectRecipe(Song.class, new String[]{"name", "composer"});
        highAndDry.setName("High and Dry");
        highAndDry.setProperty("name", "High and Dry");
        highAndDry.setProperty("composer", radiohead);
        repository.add("High and Dry", highAndDry);

        ObjectRecipe fakePlasticTrees = new ObjectRecipe(Song.class, new String[]{"name", "composer"});
        fakePlasticTrees.setName("Fake Plastic Trees");
        fakePlasticTrees.setProperty("name", "Fake Plastic Trees");
        fakePlasticTrees.setProperty("composer", radiohead);
        repository.add("Fake Plastic Trees", fakePlasticTrees);

        ObjectRecipe bends = new ObjectRecipe(Album.class, new String[]{"name", "artist"});
        bends.setName("Bends");
        bends.setProperty("name", "Bends");
        bends.setProperty("artist", radiohead);
        bends.setProperty("songs", new CollectionRecipe(Arrays.asList(highAndDry, fakePlasticTrees)));
        bends.setProperty("artist", radiohead);
        repository.add("Bends", bends);

        return repository;
    }

}