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

import java.util.Properties;

/**
 * A helper class which understands how to map an XML namespaced element to
 * Spring bean configurations
 *
 * @author James Strachan
 * @version $Id$
 * @since 1.0
 */
public class MappingMetaData {
    private Properties properties;
    private String packageName;

    /**
     * Creates an empty MappingMetaData for the specified Java package.
     * @param packageName the Java package to map
     */
    public MappingMetaData(String packageName) {
        this.packageName = packageName;
        this.properties = new Properties();
    }

    /**
     * Creates MappingMetaData using the specified properties which contan the package name.
     * @param properties
     */
    public MappingMetaData(Properties properties) {
        this.properties = properties;
        this.packageName = properties.getProperty("package", "");
    }

    /**
     * Returns the Java class name for the given XML element name
     */
    public String getClassName(String localName) {
        String className = properties.getProperty(localName);
        if (className == null) {
            if (packageName.length() > 0) {
                className = packageName + "." + localName;
            }
            else {
                className = localName;
            }
        }
        return className;
    }

    /**
     * Returns the property name for the given element and attribute name
     * 
     * @param elementName the XML local name of the element
     * @param attributeName the XML local name of the attribute
     * @return the property name to use or null if the attribute is not a valid property
     */
    public String getPropertyName(String elementName, String attributeName) {
        return properties.getProperty(elementName + "." + attributeName, attributeName);
    }

    /**
     * Returns a valid property name if the childElementName maps to a nested list property
     * 
     * @param elementName the owner element
     * @param childElementName is the child element name which maps to the nested list property 
     * @return the property name if available or null if it is not applicable
     */
    public String getNestedListProperty(String elementName, String childElementName) {
        return properties.getProperty(elementName + "." + childElementName + ".list");
    }
    
    /**
     * Returns a valid property name if the childElementName maps to a nested bean property
     * 
     * @param elementName the owner element
     * @param childElementName is the child element name which maps to the nested bean property 
     * @return the property name if available or null if it is not applicable
     */
    public String getNestedProperty(String elementName, String childElementName) {
        return properties.getProperty(elementName + "." + childElementName);
    }
}
