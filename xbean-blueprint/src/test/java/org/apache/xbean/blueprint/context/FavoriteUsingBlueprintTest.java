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
import java.util.Map;

import org.apache.xbean.blueprint.example.FavoriteService;
import org.apache.xbean.blueprint.example.GinService;
import org.apache.aries.blueprint.reflect.BeanMetadataImpl;

/**
 * @author James Strachan
 * @version $Id$
 * @since 1.0
 */
public class FavoriteUsingBlueprintTest extends BlueprintTestSupport {
    
    public void testFavs() throws Exception {
        BeanMetadataImpl meta = (BeanMetadataImpl)  reg.getComponentDefinition("favoriteService");
       //TODO blueprint understand maps
        Map favorites = null;//fs.getFavorites();
//        assertNotNull(favorites);
//        assertEquals(3, favorites.size());
//
//        assertEquals("Grey Goose", favorites.get("Dan"));
//        Object object = favorites.get("IndecisiveDan");
//        System.out.println(object.getClass());
//        assertTrue(object instanceof List);
//        List l = (List) object;
//        assertEquals(2, l.size());
//        object = l.get(0);
//        System.out.println(object.getClass());
//        assertTrue(object instanceof String);
//        object = l.get(1);
//        System.out.println(object.getClass());
//        assertTrue(object instanceof Integer);
//        object = favorites.get("WithInnerBean");
//        System.out.println(object.getClass());
//        assertTrue(object instanceof GinService);
    }

    protected String getPlan() {
        return "org/apache/xbean/blueprint/context/favorite-normal.xml";
    }
}
