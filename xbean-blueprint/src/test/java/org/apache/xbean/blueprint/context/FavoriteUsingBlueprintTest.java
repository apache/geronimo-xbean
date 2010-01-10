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
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.osgi.service.blueprint.reflect.MapEntry;
import org.osgi.service.blueprint.reflect.MapMetadata;
import org.osgi.service.blueprint.reflect.ValueMetadata;

/**
 * @author James Strachan
 * @version $Id$
 * @since 1.0
 */
public class FavoriteUsingBlueprintTest extends BlueprintTestSupport {

    public void testFavs() throws Exception {
        BeanMetadataImpl meta = (BeanMetadataImpl) reg.getComponentDefinition("favoriteService");

        assertEquals(1, meta.getProperties().size());
        BeanProperty prop = propertyByName("favorites", meta);
        MapMetadata favorites = (MapMetadata) prop.getValue();
        assertEquals(3, favorites.getEntries().size());
        MapEntry me = favorites.getEntries().get(0);
        assertEquals("Dan", ((ValueMetadata) me.getKey()).getStringValue());
        assertEquals("Grey Goose", ((ValueMetadata) me.getValue()).getStringValue());

        me = favorites.getEntries().get(1);
        assertEquals("IndecisiveDan", ((ValueMetadata) me.getKey()).getStringValue());

        CollectionMetadata cm = (CollectionMetadata) me.getValue();
        assertEquals(2, cm.getValues().size());
        assertEquals("Malbec", ((ValueMetadata) cm.getValues().get(0)).getStringValue());
        assertEquals("0", ((ValueMetadata) ((BeanMetadata)cm.getValues().get(1)).getArguments().get(0).getValue()).getStringValue());


        me = favorites.getEntries().get(2);
        assertEquals("WithInnerBean", ((ValueMetadata) me.getKey()).getStringValue());
        BeanMetadata bm = (BeanMetadata) me.getValue();
        assertEquals(GinService.class.getName(), bm.getClassName());
        assertEquals("Bombay Sapphire", ((ValueMetadata)bm.getProperties().get(0).getValue()).getStringValue());
    }

    protected String getPlan() {
        return "org/apache/xbean/blueprint/context/favorite-normal.xml";
    }
}
