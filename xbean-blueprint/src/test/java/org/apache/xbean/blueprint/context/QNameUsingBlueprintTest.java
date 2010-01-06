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

import javax.xml.namespace.QName;

import org.apache.xbean.blueprint.example.QNameService;
import org.apache.aries.blueprint.reflect.BeanMetadataImpl;
import org.apache.aries.blueprint.reflect.CollectionMetadataImpl;
import org.osgi.service.blueprint.reflect.Metadata;

public class QNameUsingBlueprintTest extends BlueprintTestSupport {

    public void testQName() throws Exception {
        BeanMetadataImpl svc = (BeanMetadataImpl) reg.getComponentDefinition("qnameService");

        List<Metadata> services = ((CollectionMetadataImpl)propertyByName("services", svc).getValue()).getValues();
        assertNotNull(services);
        assertEquals(2, services.size());
        checkQName("urn:foo", "test", services.get(0));
        checkQName("urn:foo", "bar", services.get(1));
        
        List<Metadata> list = ((CollectionMetadataImpl)propertyByName("list", svc).getValue()).getValues();
        assertNotNull(list);
        assertEquals(1, list.size());
        checkQName("urn:foo", "list", list.get(0));
    }

    protected void checkQName(String namespace, String local, Metadata meta) {
        checkArgumentValue(0, namespace, (BeanMetadataImpl) meta, false);
        checkArgumentValue(1, local, (BeanMetadataImpl) meta, false);
    }

    protected String getPlan() {
        return "org/apache/xbean/blueprint/context/qname-normal.xml";
    }
}
