/**
 *
 * Copyright 2005-2006 The Apache Software Foundation or its licensors, as applicable.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.xbean.spring.context.v2;

import java.lang.reflect.Field;

import org.apache.xbean.spring.context.impl.PropertyEditorHelper;
import org.apache.xbean.spring.context.impl.QNameReflectionHelper;
import org.springframework.beans.factory.ParseState;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XBeanBeanDefinitionParserDelegate extends BeanDefinitionParserDelegate {

    public static final String QNAME_ELEMENT = "qname";
    
    private XBeanQNameHelper qnameHelper;
    
    static {
        PropertyEditorHelper.registerCustomEditors();
    }
    
    public XBeanBeanDefinitionParserDelegate(XmlReaderContext readerContext) {
        super(readerContext);
        qnameHelper = new XBeanQNameHelper(readerContext);
    }

    public Object parsePropertySubElement(Element ele, String defaultTypeClassName) {
        if (!isDefaultNamespace(ele.getNamespaceURI())) {
            return parseNestedCustomElement(ele);
        } 
        else if (ele.getTagName().equals(QNAME_ELEMENT)) {
            return parseQNameElement(ele);
        } 
        else {
            return super.parsePropertySubElement(ele, defaultTypeClassName);
        }
    }

    public BeanDefinition parseBeanDefinitionElement(Element ele, String beanName) {
        BeanDefinition bd = super.parseBeanDefinitionElement(ele, beanName);
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
    
    private Object parseNestedCustomElement(Element candidateEle) {
        BeanDefinition innerDefinition = parseCustomElement(candidateEle, true);
        if(innerDefinition == null) {
            error("Incorrect usage of element '" + candidateEle.getNodeName()
                            + "' in a nested manner. This tag cannot be used nested inside <property>.", candidateEle);
            return null;
        }
        return innerDefinition;
    }

    private void error(String message, Object source) {
        ParseState parseState = null;
        try {
            Field f = getClass().getField("parseState");
            f.setAccessible(true);
            ParseState ps = (ParseState) f.get(this);
            parseState = ps.snapshot();
        } catch (Exception e) {}
        getReaderContext().error(message, source, parseState);
    }

}
