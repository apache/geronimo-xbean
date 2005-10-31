package org.xbean.jmx;

import junit.framework.TestCase;
import org.xbean.spring.context.ClassPathXmlApplicationContext;

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
        } finally {
            context.destroy();
        }
    }
}
