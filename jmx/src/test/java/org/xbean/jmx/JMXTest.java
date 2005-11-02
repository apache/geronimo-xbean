package org.xbean.jmx;

import junit.framework.TestCase;
import org.xbean.spring.context.ClassPathXmlApplicationContext;

import java.util.List;

/**
 * $Rev$
 */
public class JMXTest extends TestCase {
    public void testSimple() throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("org/xbean/jmx/jmx-simple.xml");
        try {
            Object jmxService = context.getBean("jmxService");
            assertNotNull(jmxService);

            Object jmxExporter = context.getBean("jmxExporter");
            assertNotNull(jmxExporter);
            assertTrue(jmxExporter instanceof MBeanExporter);
            List mbeans = ((MBeanExporter) jmxExporter).getMBeans();
            assertNotNull(mbeans);
            assertEquals(2, mbeans.size());
            assertSame(jmxService, mbeans.get(0));
            assertSame(jmxService, mbeans.get(1));
        } finally {
            context.destroy();
        }
    }
}
