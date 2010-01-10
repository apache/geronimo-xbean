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

import org.apache.xbean.blueprint.example.KegService;
import org.apache.aries.blueprint.reflect.BeanMetadataImpl;
import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.osgi.service.blueprint.reflect.MapEntry;
import org.osgi.service.blueprint.reflect.MapMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.ValueMetadata;

/**
 * @author gnodet
 */
public class FlatMapTest extends BlueprintTestSupport {

    public void testFlatMap() {
        BeanMetadataImpl meta = (BeanMetadataImpl) reg.getComponentDefinition("flat-map");
        MapMetadata c = (MapMetadata) propertyByName("services", meta).getValue();
        assertEquals(3, c.getEntries().size());
        MapEntry me = c.getEntries().get(0);
        assertEquals("key1", ((ValueMetadata) me.getKey()).getStringValue());
        CollectionMetadata l = (CollectionMetadata) me.getValue();
        assertEquals(2, l.getValues().size());
        checkEntry(l.getValues().get(0), "1000 ml");
        checkEntry(l.getValues().get(1), "5 pints");

        me = c.getEntries().get(1);
        assertEquals("key2", ((ValueMetadata) me.getKey()).getStringValue());
        l = (CollectionMetadata) me.getValue();
        assertEquals(1, l.getValues().size());
        checkEntry(l.getValues().get(0), "20 liter");

        me = c.getEntries().get(2);
        assertEquals("others", ((ValueMetadata) me.getKey()).getStringValue());
        l = (CollectionMetadata) me.getValue();
        assertEquals(1, l.getValues().size());
        checkEntry(l.getValues().get(0), "0");
    }

    private void checkEntry(Metadata me, String value) {
        BeanMetadataImpl beanMetadata = (BeanMetadataImpl) me;
        assertEquals(KegService.class.getName(), beanMetadata.getClassName());
        assertEquals(1, beanMetadata.getProperties().size());
        assertEquals(value, ((ValueMetadata) beanMetadata.getProperties().get(0).getValue()).getStringValue());
    }

    protected String getPlan() {
        return "org/apache/xbean/blueprint/context/flatmap-xbean.xml";
    }
}
