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

import org.apache.xbean.blueprint.example.BeerService;

/**
 * @author James Strachan
 * @version $Id$
 * @since 1.0
 */
public class BeerUsingBlueprintTest extends BlueprintTestSupport {
    
    public void testBeer() throws Exception {
        BeerService o = (BeerService)container.getComponentInstance("beerService");
        assertNotNull(o);
        assertEquals("name", "Stella", o.getName());
        assertEquals("id", "123", o.getId());
    }

    protected String getPlan() {
        return "org/apache/xbean/blueprint/context/beer-normal.xml";
    }

}
