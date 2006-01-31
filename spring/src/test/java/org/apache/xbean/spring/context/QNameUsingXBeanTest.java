package org.apache.xbean.spring.context;

import org.springframework.context.support.AbstractXmlApplicationContext;

public class QNameUsingXBeanTest extends QNameUsingSpringTest {

    protected AbstractXmlApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("org/apache/xbean/spring/context/qname-xbean.xml");
    }

}
