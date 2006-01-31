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
