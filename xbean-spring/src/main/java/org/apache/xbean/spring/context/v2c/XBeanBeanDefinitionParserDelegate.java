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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.xbean.spring.context.impl.PropertyEditorHelper;
import org.apache.xbean.spring.context.impl.QNameReflectionHelper;
import org.apache.xbean.spring.context.v2.XBeanNamespaceHandler;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XBeanBeanDefinitionParserDelegate extends BeanDefinitionParserDelegate {

    public static final String QNAME_ELEMENT = "qname";
    
    private XBeanQNameHelper qnameHelper;
    
    public XBeanBeanDefinitionParserDelegate(XmlReaderContext readerContext) {
        super(readerContext);
        qnameHelper = new XBeanQNameHelper(readerContext);
    }

    public Object parsePropertySubElement(Element ele, BeanDefinition bd, String defaultTypeClassName) {
        if (!isDefaultNamespace(ele.getNamespaceURI())) {
            return internalParseNestedCustomElement(ele, bd);
        } 
        else if (ele.getTagName().equals(QNAME_ELEMENT)) {
            return parseQNameElement(ele);
        } 
        else {
            return super.parsePropertySubElement(ele, bd, defaultTypeClassName);
        }
    }

    public AbstractBeanDefinition parseBeanDefinitionElement(Element ele, String beanName, BeanDefinition containingBean) {
        AbstractBeanDefinition bd = super.parseBeanDefinitionElement(ele, beanName, containingBean);
        qnameHelper.coerceNamespaceAwarePropertyValues(bd, ele);
        return bd;
    }
    
    public boolean isDefaultNamespace(String namespaceUri) {
        return (!StringUtils.hasLength(namespaceUri) || 
               BEANS_NAMESPACE_URI.equals(namespaceUri)) ||
               XBeanNamespaceHandler.SPRING_SCHEMA.equals(namespaceUri) ||
               XBeanNamespaceHandler.SPRING_SCHEMA_COMPAT.equals(namespaceUri);
    }
    
    protected Object parseQNameElement(Element element) {
        return QNameReflectionHelper.createQName(element, getElementText(element));
    }

    protected String getElementText(Element element) {
        StringBuffer buffer = new StringBuffer();
        NodeList nodeList = element.getChildNodes();
        for (int i = 0, size = nodeList.getLength(); i < size; i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.TEXT_NODE) {
                buffer.append(node.getNodeValue());
            }
        }
        return buffer.toString();
    }
    
    private Object internalParseNestedCustomElement(Element candidateEle, BeanDefinition containingBeanDefinition) {
        try {
            Method mth = getClass().getSuperclass().getDeclaredMethod("parseNestedCustomElement", new Class[] { Element.class, BeanDefinition.class });
            mth.setAccessible(true);
            return mth.invoke(this, new Object[] { candidateEle, containingBeanDefinition });
        } catch (Exception e) {
            if (e instanceof InvocationTargetException && e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw (IllegalStateException) new IllegalStateException("Unable to invoke parseNestedCustomElement method").initCause(e);
        }
    }

}
