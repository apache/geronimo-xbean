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

import junit.framework.TestCase;

import org.apache.xbean.blueprint.example.InnerBean;
import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.reflect.BeanMetadataImpl;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.CollectionMetadata;

public class ComponentTest extends TestCase {

    protected void setUp() throws Exception {
        InnerBean.INSTANCE = null;
    }
    
    public void test1() throws Exception {
        test("org/apache/xbean/blueprint/context/component-blueprint.xml");
    }

    public void test2() throws Exception {
        test("org/apache/xbean/blueprint/context/component-xbean.xml");
    }
    
    protected void test(String file) throws Exception {
        ComponentDefinitionRegistry f = BlueprintTestSupport.parse(file).getComponentDefinitionRegistry();
        BeanMetadataImpl meta = (BeanMetadataImpl) f.getComponentDefinition("container");
        assertNotNull(meta);
        CollectionMetadata list = (CollectionMetadata) BlueprintTestSupport.propertyByName("beans", meta).getValue();
        assertEquals(1, list.getValues().size());
        assertEquals(InnerBean.class.getName(), ((BeanMetadata)list.getValues().get(0)).getClassName());
    }

}
