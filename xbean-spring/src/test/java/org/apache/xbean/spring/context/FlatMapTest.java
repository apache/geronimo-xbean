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

import org.apache.xbean.spring.example.FlatMapService;
import org.apache.xbean.spring.example.KegService;
import org.springframework.context.support.AbstractXmlApplicationContext;

/**
 * @author gnodet
 */
public class FlatMapTest extends SpringTestSupport {

    public void testFlatMap() {
        FlatMapService fm = (FlatMapService) getBean("flat-map");
        assertEquals(3, fm.getServices().size());
        Object obj = fm.getServices().get("key1");
        assertTrue(obj instanceof List);
        List l = (List) obj;
        assertEquals(2, l.size());
        System.out.println(l.get(0).getClass());
        assertTrue(l.get(0) instanceof KegService);
        System.out.println(l.get(1).getClass());
        assertTrue(l.get(1) instanceof KegService);
        obj = fm.getServices().get("key2");
        assertTrue(obj instanceof List);
        l = (List) obj;
        assertEquals(1, l.size());
        System.out.println(l.get(0).getClass());
        assertTrue(l.get(0) instanceof KegService);
        obj = fm.getServices().get("others");
        assertTrue(obj instanceof List);
        l = (List) obj;
        assertEquals(1, l.size());
        System.out.println(l.get(0).getClass());
        assertTrue(l.get(0) instanceof KegService);
    }
    
    protected AbstractXmlApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("org/apache/xbean/spring/context/flatmap-xbean.xml");
    }

}
