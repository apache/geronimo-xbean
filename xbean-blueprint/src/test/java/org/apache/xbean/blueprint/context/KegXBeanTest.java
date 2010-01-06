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

/**
 * Used to verify that per propety Property Editors work correctly.
 * 
 * @author chirino
 * @version $Id$
 * @since 2.2
 */
public class KegXBeanTest extends BlueprintTestSupport {

    public void testBeer() throws Exception {
        //TODO blueprint value conversion using units??
        BeanMetadataImpl ml1000 = (BeanMetadataImpl) reg.getComponentDefinition("ml1000");
        BeanMetadataImpl empty = (BeanMetadataImpl) reg.getComponentDefinition("empty");
        BeanMetadataImpl pints5 = (BeanMetadataImpl) reg.getComponentDefinition("pints5");
        BeanMetadataImpl liter20 = (BeanMetadataImpl) reg.getComponentDefinition("liter20");
        
        checkPropertyValue("remaining", "1000 ml", ml1000);
        checkPropertyValue("remaining", "0", empty);
        checkPropertyValue("remaining", "5 pints", pints5);
        checkPropertyValue("remaining", "20 liter", liter20);
//        checkPropertyValue("remaining", "1000", ml1000);
//        checkPropertyValue("remaining", "0", empty);
//        checkPropertyValue("remaining", "8750", pints5);
//        checkPropertyValue("remaining", "20000", liter20);

    }

    protected String getPlan() {
        return "org/apache/xbean/blueprint/context/keg-xbean.xml";
    }
}
