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
package org.apache.xbean.spring.context.v2;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * An enhanced XML parser capable of handling custom XML schemas.
 *
 * @author James Strachan
 * @version $Id$
 * @since 2.0
 */
public class XBeanNamespaceHandler implements NamespaceHandler {

    public static final String SPRING_SCHEMA = "http://xbean.apache.org/schemas/spring/1.0";
    public static final String SPRING_SCHEMA_COMPAT = "http://xbean.org/schemas/spring/1.0";

    private final NamespaceHandler delegate;
    
    public XBeanNamespaceHandler() {
        delegate = XBeanV2Helper.createNamespaceHandler();
    }
    
    public void init() {
        delegate.init();
    }

    public BeanDefinition parse(Element element, ParserContext parserContext) {
        return delegate.parse(element, parserContext);
    }

    public BeanDefinitionHolder decorate(Node element, BeanDefinitionHolder definition, ParserContext parserContext) {
        return delegate.decorate(element, definition, parserContext);
    }
    
}
