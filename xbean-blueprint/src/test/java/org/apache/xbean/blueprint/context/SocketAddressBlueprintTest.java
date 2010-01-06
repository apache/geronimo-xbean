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

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.List;

import org.apache.xbean.blueprint.example.SocketService;
import org.apache.aries.blueprint.reflect.BeanMetadataImpl;
import org.osgi.service.blueprint.reflect.CollectionMetadata;

/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class SocketAddressBlueprintTest extends BlueprintTestSupport {

    public void testSocketService() throws Exception {
        BeanMetadataImpl socketService = (BeanMetadataImpl) reg.getComponentDefinition("socketService");

//        System.out.println();
//        System.out.println("===========================");
//        System.out.println(socketService.getAddresses());
//        System.out.println("===========================");
//        System.out.println();

//        List<InetSocketAddress> expected = Arrays.asList(new InetSocketAddress("localhost", 42), new InetSocketAddress("localhost", 42));

        assertEquals(1, socketService.getProperties().size());
        CollectionMetadata collection = (CollectionMetadata) socketService.getProperties().get(0).getValue();
        assertEquals(2, collection.getValues().size());

    }

    public void testSocketAddress() throws Exception {
        BeanMetadataImpl socketAddress = (BeanMetadataImpl) reg.getComponentDefinition("socketAddress");

//        System.out.println();
//        System.out.println("===========================");
//        System.out.println(socketAddress);
//        System.out.println("===========================");
//        System.out.println();

        assertNotNull(socketAddress);
    }

    protected String getPlan() {
        return "org/apache/xbean/blueprint/context/socket-address-normal.xml";
    }
}