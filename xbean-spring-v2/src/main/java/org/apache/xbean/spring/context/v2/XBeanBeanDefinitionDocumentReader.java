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

import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

public class XBeanBeanDefinitionDocumentReader extends DefaultBeanDefinitionDocumentReader {

    protected BeanDefinitionParserDelegate createHelper(XmlReaderContext readerContext, Element root) {
        BeanDefinitionParserDelegate delegate = XBeanV2Helper.createParser(readerContext);
        delegate.initDefaults(root);
        return delegate;
    }

    protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
        String namespaceUri = root.getNamespaceURI();
        if (!DomUtils.nodeNameEquals(root, "beans") && 
            !delegate.isDefaultNamespace(namespaceUri)) {
            delegate.parseCustomElement(root, false);
        } else {
            super.parseBeanDefinitions(root, delegate);
        }
    }
    
}
