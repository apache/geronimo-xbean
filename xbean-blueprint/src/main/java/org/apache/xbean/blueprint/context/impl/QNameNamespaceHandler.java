/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.xbean.blueprint.context.impl;

import java.net.URL;
import java.util.Collections;
import java.util.Set;

import javax.xml.namespace.QName;
import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.ParserContext;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @version $Rev$ $Date$
 */
public class QNameNamespaceHandler implements NamespaceHandler {

    private static final Set<Class> MANAGED_CLASS = Collections.<Class>singleton(QName.class);
    private final QNameHelper qNameHelper = new QNameHelper();

    public URL getSchemaLocation(String namespace) {
        return null;
    }

    public Set<Class> getManagedClasses() {
        return MANAGED_CLASS;
    }

    public Metadata parse(Element element, ParserContext context) {
        if ("qname".equals(element.getLocalName())) {
            return QNameHelper.createQNameMetadata(element, getElementText(element), context);
        }
        return null;
    }

    public ComponentMetadata decorate(Node node, ComponentMetadata component, ParserContext context) {
        return component;
    }

    private String getElementText(Element element) {
        StringBuilder buffer = new StringBuilder();
        NodeList nodeList = element.getChildNodes();
        for (int i = 0, size = nodeList.getLength(); i < size; i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
                buffer.append(node.getNodeValue());
            }
        }
        return buffer.toString();
    }

}
