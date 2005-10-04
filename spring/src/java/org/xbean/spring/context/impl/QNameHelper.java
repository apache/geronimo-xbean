/**
 * 
 * Copyright 2005 LogicBlaze, Inc. http://www.logicblaze.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 * 
 **/
package org.xbean.spring.context.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;

/**
 * 
 * @version $Revision: 1.1 $
 */
public class QNameHelper {
    private static final Log log = LogFactory.getLog(QNameHelper.class);

    public static QName createQName(Element element, String qualifiedName) {
        int index = qualifiedName.indexOf(':');
        if (index >= 0) {
            String prefix = qualifiedName.substring(0, index);
            String localName = qualifiedName.substring(index + 1);
            String uri = recursiveGetAttributeValue(element, "xmlns:" + prefix);
            return new QName(uri, localName, prefix);
        }
        else {
            String uri = recursiveGetAttributeValue(element, "xmlns");
            if (uri != null) {
                return new QName(uri, qualifiedName);
            }
            return new QName(qualifiedName);
        }
    }

    /**
     * Recursive method to find a given attribute value
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
}
