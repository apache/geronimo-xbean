package org.apache.xbean.spring.context.impl;

import java.lang.reflect.Constructor;
import java.util.List;

import org.apache.xbean.spring.context.SpringApplicationContext;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.SpringVersion;

public class XBeanHelper {

    public static XmlBeanDefinitionReader createBeanDefinitionReader(
                    SpringApplicationContext applicationContext,
                    BeanDefinitionRegistry registry,
                    List xmlPreprocessors) {
        String version = SpringVersion.getVersion();
        String className = "org.apache.xbean.spring.context.v" + version.charAt(0) + ".XBeanXmlBeanDefinitionReader";
        try {
            Class cl = Class.forName(className);
            Constructor cstr = cl.getConstructor(new Class[] { SpringApplicationContext.class, BeanDefinitionRegistry.class, List.class });
            return (XmlBeanDefinitionReader) cstr.newInstance(new Object[] { applicationContext, registry, xmlPreprocessors });
        } catch (Exception e) {
            throw (IllegalStateException) new IllegalStateException("Could not find valid implementation for: " + version).initCause(e);
        }
    }
}
