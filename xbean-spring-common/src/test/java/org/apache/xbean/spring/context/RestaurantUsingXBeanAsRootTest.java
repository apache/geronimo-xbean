package org.apache.xbean.spring.context;

import org.springframework.context.support.AbstractXmlApplicationContext;

public class RestaurantUsingXBeanAsRootTest extends RestaurantUsingSpringTest {

    protected AbstractXmlApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("org/apache/xbean/spring/context/restaurant-xbean-root.xml");
    }
}
