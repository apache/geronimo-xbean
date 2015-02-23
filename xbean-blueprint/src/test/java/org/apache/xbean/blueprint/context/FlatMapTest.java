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

import org.apache.xbean.blueprint.example.FlatMapService;
import org.apache.xbean.blueprint.example.KegService;

/**
 * @author gnodet
 */
public class FlatMapTest extends BlueprintTestSupport {

    public void testFlatMap() {
        FlatMapService map = (FlatMapService)container.getComponentInstance("flat-map");
        Map<?, ?> services = map.getServices();
        assertNotNull(services);
        assertEquals(3, services.size());
        for (Map.Entry<?, ?> ent : services.entrySet()) {
            String key = ent.getKey().toString();
            if (key.equals("key1")) {
                List<?> list = (List<?>)ent.getValue();
                assertEquals(2, list.size());
                KegService keg = (KegService)list.get(0);
                assertNotNull(keg);
                assertEquals(1000, keg.getRemaining());
                keg = (KegService)list.get(1);
                assertNotNull(keg);
                assertEquals(8750, keg.getRemaining());
            } else if (key.equals("key2")) {
                List<?> list = (List<?>)ent.getValue();
                assertEquals(1, list.size());
                KegService keg = (KegService)list.get(0);
                assertNotNull(keg);
                assertEquals(20000, keg.getRemaining());
            } else if (key.equals("others")) {
                List<?> list = (List<?>)ent.getValue();
                assertEquals(1, list.size());
                KegService keg = (KegService)list.get(0);
                assertNotNull(keg);
                assertEquals(0, keg.getRemaining());
            } else {
                fail("Unexpected key " + key);
            }
        }
    }

    protected String getPlan() {
        return "org/apache/xbean/blueprint/context/flatmap-xbean.xml";
    }
}
