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
package org.apache.xbean.blueprint.context.impl;

import java.beans.PropertyDescriptor;
import java.util.List;

import javax.xml.namespace.QName;
import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutableCollectionMetadata;
import org.apache.aries.blueprint.mutable.MutableValueMetadata;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.ValueMetadata;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * 
 * @version $Revision: 1.1 $
 */
public class QNameHelper {
    private static final Log log = LogFactory.getLog(QNameHelper.class);
    
    public static Metadata createQNameMetadata(Element element, String qualifiedName, ParserContext parserContext) {
        MutableBeanMetadata beanMetadata = parserContext.createMetadata(MutableBeanMetadata.class);
        beanMetadata.setClassName(QName.class.getName());
        int index = qualifiedName.indexOf(':');
        if (index >= 0) {
            String prefix = qualifiedName.substring(0, index);
            String localName = qualifiedName.substring(index + 1);
            String uri = recursiveGetAttributeValue(element, "xmlns:" + prefix);
            beanMetadata.addArgument(valueMetadata(uri, parserContext), String.class.getName(), 0);
            beanMetadata.addArgument(valueMetadata(localName, parserContext), String.class.getName(), 1);
            beanMetadata.addArgument(valueMetadata(prefix, parserContext), String.class.getName(), 2);
        }
        else {
            String uri = recursiveGetAttributeValue(element, "xmlns");
            if (uri != null) {
                beanMetadata.addArgument(valueMetadata(uri, parserContext), String.class.getName(), 0);
                beanMetadata.addArgument(valueMetadata(qualifiedName, parserContext), String.class.getName(), 1);
            } else {
                beanMetadata.addArgument(valueMetadata(qualifiedName, parserContext), String.class.getName(), 0);
            }
        }
        return beanMetadata;
    }

    private static Metadata valueMetadata(String stringValue, ParserContext parserContext) {
        MutableValueMetadata value = parserContext.createMetadata(MutableValueMetadata.class);
        value.setStringValue(stringValue);
        return value;
    }

    /**
     * Recursive method to find a given attribute value in an element or its parents (towards root of dom tree)
     * @param element element to start searching for attribute in
     * @param attributeName name of desired attribute
     * @return value of found attribute
     */
    public static String recursiveGetAttributeValue(Element element, String attributeName) {
        String answer = null;
        try {
            answer = element.getAttribute(attributeName);
        }
        catch (Exception e) {
            if (log.isTraceEnabled()) {
                log.trace("Caught exception looking up attribute: " + attributeName + " on element: " + element + ". Cause: " + e, e);
            }
        }
        if (answer == null || answer.length() == 0) {
            Node parentNode = element.getParentNode();
            if (parentNode instanceof Element) {
                return recursiveGetAttributeValue((Element) parentNode, attributeName);
            }
        }
        return answer;
    }
    
    public static void coerceNamespaceAwarePropertyValues(MutableBeanMetadata bd, Element element, PropertyDescriptor descriptor, ParserContext parserContext) {
        // When the property is an indexed property, the getPropertyType can return null.
        if (descriptor.getPropertyType() == null) {
            return;
        }
        if (descriptor.getPropertyType().isAssignableFrom(QName.class)) {
            String name = descriptor.getName();
            BeanProperty propertyValue = XBeanNamespaceHandler.propertyByName(name, bd);
            if (propertyValue != null) {
                Metadata value = propertyValue.getValue();
                if (value instanceof ValueMetadata) {
                    bd.removeProperty(propertyValue);
                    Metadata valueMetadata = createQNameMetadata(element, ((ValueMetadata)value).getStringValue(), parserContext);
                    bd.addProperty(name, valueMetadata);
                }
                //else??
            }
        } else if (descriptor.getPropertyType().isAssignableFrom(QName[].class)) {
            String name = descriptor.getName();
            BeanProperty propertyValue = XBeanNamespaceHandler.propertyByName(name, bd);
            if (propertyValue != null) {
                Object value = propertyValue.getValue();
                if (value instanceof CollectionMetadata) {
                    List<Metadata> values = ((CollectionMetadata) value).getValues();
                    MutableCollectionMetadata newValue = parserContext.createMetadata(MutableCollectionMetadata.class);

                    for (Metadata v : values) {
                        if (v instanceof ValueMetadata) {
                            newValue.addValue(createQNameMetadata(element, ((ValueMetadata)v).getStringValue(), parserContext));
                        } else {
                            newValue.addValue(v);
                        }
                    }
                    bd.removeProperty(propertyValue);
                    bd.addProperty(name, newValue);
                }
            }
        }
    }

}
