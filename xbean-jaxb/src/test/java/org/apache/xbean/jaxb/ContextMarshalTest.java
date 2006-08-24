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
package org.apache.xbean.jaxb;

import org.apache.xbean.jaxb.example.Address;
import org.apache.xbean.jaxb.example.BusinessCard;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import java.io.File;
import java.io.FileOutputStream;

import junit.framework.TestCase;

/**
 * 
 * @version $Revision: $
 */
public class ContextMarshalTest extends TestCase {

    protected JAXBContext context;

    public void testMarshalThenUnmarshal() throws Exception {
        File dir = new File("target/jaxb-xml");
        dir.mkdirs();
        File file = new File(dir, "xbean.xml");

        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        ContextImpl oldValue = getBeans();
        System.out.println("OldValue: " + oldValue);

        m.marshal(oldValue, new FileOutputStream(file));
        unmarshallContext(file);
    }

    public void testUnmarshalExample1() throws Exception {
        unmarshallContext(new File("src/test/resources/org/apache/xbean/jaxb/example1.xml"));
    }
    
    protected void unmarshallContext(File file) throws JAXBException {
        Unmarshaller um = context.createUnmarshaller();
        // um.setSchema(getSchema("schema1.xsd"));
        ContextImpl newValue = (ContextImpl) um.unmarshal(file);
        System.out.println("new Value: " + newValue);

        Object foo = newValue.get("foo");
        System.out.println("Foo: " + foo);
        assertNotNull("entry for foo should not be null!", foo);
        System.out.println("Foo is of type: " + foo.getClass());
    }

    @Override
    protected void setUp() throws Exception {
         context = JAXBContext.newInstance("org.apache.xbean.jaxb:org.apache.xbean.jaxb.example");
    }

    protected ContextImpl getBeans() {
        ContextImpl beans = new ContextImpl();
        beans.put("foo", new BusinessCard("John Doe", "Sr. Widget Designer", "Acme, Inc.", new Address(null,
                "123 Widget Way", "Anytown", "MA", (short) 12345), "123.456.7890", null, "123.456.7891",
                "John.Doe@Acme.ORG"));
        return beans;
    }
}
