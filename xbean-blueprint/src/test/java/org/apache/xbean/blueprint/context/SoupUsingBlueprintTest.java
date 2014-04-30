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

import org.apache.aries.blueprint.reflect.BeanMetadataImpl;

/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class SoupUsingBlueprintTest extends BlueprintTestSupport {
    private static final long time = System.currentTimeMillis();

    public void testSoup() throws Exception {
        BeanMetadataImpl soup = (BeanMetadataImpl) reg.getComponentDefinition("soupService");
        BeanMetadataImpl nestedBean = (BeanMetadataImpl) reg.getComponentDefinition("nestedBean");
        BeanMetadataImpl nestedValue = (BeanMetadataImpl) reg.getComponentDefinition("nestedValue");

        asssertValidSoup(soup);
        asssertValidSoup(nestedBean);
        asssertValidSoup(nestedValue);

//        reg.;
//        assertFalse(soup.exists());
//        assertFalse(nestedBean.exists());
//        assertFalse(nestedValue.exists());
    }

    private void asssertValidSoup(BeanMetadataImpl soup) {
        checkArgumentValue(0, "French Onion", soup, true);
//        assertTrue(soup.getCreateTime() >= time);
        assertNotNull(soup.getInitMethod());

    }

    protected String getPlan() {
        return "org/apache/xbean/blueprint/context/soup-normal.xml";
    }
}
