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
package org.apache.xbean.spring.context;

import java.util.List;

import javax.xml.namespace.QName;

import org.apache.xbean.spring.example.QNameService;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class QNameUsingSpringTest extends SpringTestSupport {

    public void testQName() throws Exception {
        QNameService svc = (QNameService) getBean("qnameService");

        QName[] services = svc.getServices();
        assertNotNull(services);
        assertEquals(2, services.length);
        assertEquals(new QName("urn:foo", "test"), services[0]);
        assertEquals(new QName("urn:foo", "bar"), services[1]);
        
        List list = svc.getList();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals(new QName("urn:foo", "list"), list.get(0));
    }

    protected AbstractXmlApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("org/apache/xbean/spring/context/qname-normal.xml");
    }

}
