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
package org.apache.xbean.spring.context.v2c;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

import org.apache.xbean.spring.context.impl.PropertyEditorHelper;
import org.apache.xbean.spring.context.impl.QNameReflectionHelper;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.w3c.dom.Element;

public class XBeanQNameHelper {

    private XmlReaderContext readerContext;
    
    private boolean qnameIsOnClassPath;

    private boolean initQNameOnClassPath;
    
    public XBeanQNameHelper(XmlReaderContext readerContext) {
        this.readerContext = readerContext;
    }
    
    /**
     * Any namespace aware property values (such as QNames) need to be coerced
     * while we still have access to the XML Element from which its value comes -
     * so lets do that now before we trash the DOM and just have the bean
     * definition.
     */
    public void coerceNamespaceAwarePropertyValues(BeanDefinition definition, Element element) {
        if (definition instanceof AbstractBeanDefinition && isQnameIsOnClassPath()) {
            AbstractBeanDefinition bd = (AbstractBeanDefinition) definition;
            // lets check for any QName types
            BeanInfo beanInfo = getBeanInfo(bd.getBeanClassName());
            if (beanInfo != null) {
                PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();
                for (int i = 0; i < descriptors.length; i++) {
                    QNameReflectionHelper.coerceNamespaceAwarePropertyValues(bd, element, descriptors, i);
                }
            }
        }
    }

    public BeanInfo getBeanInfo(String className) throws BeanDefinitionStoreException {
        if (className == null) {
            return null;
        }

        BeanInfo info = null;
        Class type = null;
        try {
            type = loadClass(className);
        }
        catch (ClassNotFoundException e) {
            throw new BeanDefinitionStoreException("Failed to load type: " + className + ". Reason: " + e, e);
        }
        try {
            info = Introspector.getBeanInfo(type);
        }
        catch (IntrospectionException e) {
            throw new BeanDefinitionStoreException("Failed to introspect type: " + className + ". Reason: " + e, e);
        }
        return info;
    }

    /**
     * Attempts to load the class on the current thread context class loader or
     * the class loader which loaded us
     */
    protected Class loadClass(String name) throws ClassNotFoundException {
        ClassLoader beanClassLoader = readerContext.getReader().getBeanClassLoader();
        if (beanClassLoader != null) {
            try {
                return beanClassLoader.loadClass(name);
            }
            catch (ClassNotFoundException e) {
            }
        }
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            try {
                return contextClassLoader.loadClass(name);
            }
            catch (ClassNotFoundException e) {
            }
        }
        return getClass().getClassLoader().loadClass(name);
    }

    protected boolean isQnameIsOnClassPath() {
        if (initQNameOnClassPath == false) {
            qnameIsOnClassPath = PropertyEditorHelper.loadClass("javax.xml.namespace.QName") != null;
            initQNameOnClassPath = true;
        }
        return qnameIsOnClassPath;
    }
    
}
