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
package org.apache.xbean.jmx;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import junit.framework.TestCase;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.beans.factory.BeanCreationException;


/**
 * @version $Revision: $ $Date: $
 */
public class JMXTest extends TestCase {

    public void testSimple() throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("org/apache/xbean/jmx/jmx-simple.xml");
        try {
            Object jmxService = context.getBean("jmxService");
            assertNotNull(jmxService);

            Object server = context.getBean("mbeanServer");
            assertNotNull(server);

            MBeanExporter jmxExporter = (MBeanExporter) context.getBean("jmxExporter");
            assertNotNull(jmxExporter);

            MBeanServer mbeanServer = jmxExporter.getMbeanServer();
            assertNotNull(mbeanServer);
            assertSame(server, mbeanServer);
            assertEquals("org.apache.xbean", mbeanServer.getDefaultDomain());

            ObjectName objectName = ObjectName.getInstance(":type=JMXService");
            assertTrue(mbeanServer.isRegistered(objectName));

            assertEquals("BAR", mbeanServer.getAttribute(objectName, "readOnly"));
            try {
                mbeanServer.setAttribute(objectName, new Attribute("readOnly", "CDR"));
                fail("Should have thrown an AttributeNotFoundException");
            } catch (AttributeNotFoundException doNothing) {
            }

            // iniital value should have been set by spring
            assertEquals(new Integer(32), mbeanServer.getAttribute(objectName, "intAttr"));
            mbeanServer.setAttribute(objectName, new Attribute("intAttr", new Integer(5)));
            assertEquals(new Integer(5), mbeanServer.getAttribute(objectName, "intAttr"));

            // iniital value should have been set by spring
            assertEquals(new Integer(64), mbeanServer.getAttribute(objectName, "integerAttr"));
            mbeanServer.setAttribute(objectName, new Attribute("integerAttr", new Integer(7)));
            assertEquals(new Integer(7), mbeanServer.getAttribute(objectName, "integerAttr"));

            mbeanServer.setAttribute(objectName, new Attribute("booleanAttr", Boolean.FALSE));
            assertEquals(Boolean.FALSE, mbeanServer.getAttribute(objectName, "booleanAttr"));
            mbeanServer.setAttribute(objectName, new Attribute("booleanAttr", Boolean.TRUE));
            assertEquals(Boolean.TRUE, mbeanServer.getAttribute(objectName, "booleanAttr"));

            mbeanServer.setAttribute(objectName, new Attribute("writeOnly", "FOO"));
            try {
                mbeanServer.getAttribute(objectName, "writeOnly");
                fail("Should have thrown an AttributeNotFoundException");
            } catch (AttributeNotFoundException doNothing) {
            }

        } finally {
            context.destroy();
        }
    }

    public void testSimpleMultiple() throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("org/apache/xbean/jmx/jmx-simple-multiple.xml");
        try {
            Object jmxService1 = context.getBean("jmxService1");
            assertNotNull(jmxService1);

            Object jmxService2 = context.getBean("jmxService2");
            assertNotNull(jmxService2);

            MBeanExporter jmxExporter = (MBeanExporter) context.getBean("jmxExporter");
            assertNotNull(jmxExporter);

            MBeanServer mbeanServer = jmxExporter.getMbeanServer();
            assertNotNull(mbeanServer);

            ObjectName objectName11 = ObjectName.getInstance(":type=JMXService1,id=1");
            ObjectName objectName12 = ObjectName.getInstance(":type=JMXService1,id=2");
            ObjectName objectName21 = ObjectName.getInstance(":type=JMXService2,id=1");
            assertTrue(mbeanServer.isRegistered(objectName11));
            assertTrue(mbeanServer.isRegistered(objectName12));
            assertTrue(mbeanServer.isRegistered(objectName21));
        }
        finally {
            context.destroy();
        }
    }

    public void testDuplicate() {
        try {
            new ClassPathXmlApplicationContext("org/apache/xbean/jmx/jmx-duplicate.xml");
            fail("Should have thrown an IllegalStateException");
        } catch (BeanCreationException bce) {
            assertTrue(bce.getCause().getClass().equals(IllegalStateException.class));
        }
    }

    public void testCustomDuplicate() {
        try {
            new ClassPathXmlApplicationContext("org/apache/xbean/jmx/jmx-custom-duplicate.xml");
            fail("Should have thrown an IllegalStateException");
        } catch (BeanCreationException bce) {
            assertTrue(bce.getCause().getClass().equals(IllegalStateException.class));
        }
    }

    public void testDuplicateWrapper() {
        try {
            new ClassPathXmlApplicationContext("org/apache/xbean/jmx/jmx-duplicate-wrapper.xml");
            fail("Should have thrown an IllegalStateException");
        } catch (BeanCreationException bce) {
            assertTrue(bce.getCause().getClass().equals(IllegalStateException.class));
        }
    }

    public void testCustomWrapper() throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("org/apache/xbean/jmx/jmx-custom-wrapper.xml");
        try {
            Object jmxService = context.getBean("jmxService");
            assertNotNull(jmxService);

            MBeanExporter jmxExporter = (MBeanExporter) context.getBean("jmxExporter");
            assertNotNull(jmxExporter);

            MBeanServer mbeanServer = jmxExporter.getMbeanServer();
            assertNotNull(mbeanServer);

            ObjectName objectName = ObjectName.getInstance(":type=JMXService");
            assertTrue(mbeanServer.isRegistered(objectName));
            javax.management.MBeanInfo mbeanInfo = mbeanServer.getMBeanInfo(objectName);
            assertEquals("Dummy Description", mbeanInfo.getDescription());
        }
        finally {
            context.destroy();
        }
    }

    public void testListener() throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("org/apache/xbean/jmx/jmx-listener.xml");
        try {
            JMXService jmxService = (JMXService) context.getBean("jmxService");
            assertNotNull(jmxService);

            assertEquals(1, jmxService.getPropertyChangeListeners().size());
            MBeanExporter jmxExporter = (MBeanExporter) context.getBean("jmxExporter");
            assertNotNull(jmxExporter);

            MBeanServer mbeanServer = jmxExporter.getMbeanServer();
            assertNotNull(mbeanServer);

            ObjectName objectName = ObjectName.getInstance(":type=JMXService");
            assertTrue(mbeanServer.isRegistered(objectName));

            final Reference notified = new Reference(Boolean.FALSE);
            mbeanServer.addNotificationListener(objectName, new NotificationListener() {
                public void handleNotification(Notification notification, Object handback) {
                    notified.set(Boolean.TRUE);
                }
            }, null, null);

            jmxService.firePropertyChange();

            assertEquals(Boolean.TRUE, notified.get());
        }
        finally {
            context.destroy();
        }
    }

    public void testListenerMultiple() throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("org/apache/xbean/jmx/jmx-listener-multiple.xml");
        try {
            JMXService jmxService = (JMXService) context.getBean("jmxService");
            assertNotNull(jmxService);

            assertEquals(2, jmxService.getPropertyChangeListeners().size());
            MBeanExporter jmxExporter = (MBeanExporter) context.getBean("jmxExporter");
            assertNotNull(jmxExporter);

            MBeanServer mbeanServer = jmxExporter.getMbeanServer();
            assertNotNull(mbeanServer);

            ObjectName objectName = ObjectName.getInstance(":type=JMXService");
            assertTrue(mbeanServer.isRegistered(objectName));

            final Reference notified = new Reference(new Integer(0));
            mbeanServer.addNotificationListener(objectName, new NotificationListener() {
                public void handleNotification(Notification notification, Object handback) {
                    Integer oldValue = (Integer) notified.get();
                    notified.set(new Integer(oldValue.intValue() + 1));
                }
            }, null, null);

            jmxService.firePropertyChange();

            assertEquals(2, ((Integer) notified.get()).intValue());
        }
        finally {
            context.destroy();
        }
    }

    private static class Reference {
        private Object reference;

        public Reference(Object reference) {
            this.reference = reference;
        }

        public Object get() {
            return reference;
        }

        public void set(Object reference) {
            this.reference = reference;
        }
    }
}
