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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * A helper class which understands how to map an XML namespaced element to
 * Spring bean configurations
 *
 * @author James Strachan
 * @version $Id$
 * @since 2.0
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
    }

    /**
     * Returns the Java class name for the given XML element name
     */
    public String getClassName(String localName) {
        String className = properties.getProperty(localName);
        if (className == null && packageName != null) {
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
        return properties.getProperty(elementName + ".alias." + attributeName, attributeName);
    }

    /**
     * Returns a valid property name if the childElementName maps to a nested list property
     * 
     * @param elementName the owner element
     * @param childElementName is the child element name which maps to the nested list property 
     * @return the property name if available or null if it is not applicable
     */
    public String getNestedListProperty(String elementName, String childElementName) {
        return properties.getProperty(elementName + ".list." + childElementName);
    }
    
    /**
     * Returns a valid property name if the childElementName maps to a nested bean property
     * 
     * @param elementName the owner element
     * @param childElementName is the child element name which maps to the nested bean property 
     * @return the property name if available or null if it is not applicable
     */
    public String getNestedProperty(String elementName, String childElementName) {
        return properties.getProperty(elementName + ".alias." + childElementName);
    }

    public boolean isDefaultConstructor(Constructor constructor) {
        String property = properties.getProperty(constructorToPropertyName(constructor) + ".default");
        if (property != null) {
            return Boolean.valueOf(property).booleanValue();
        }
        return false;
    }

    public boolean isDefaultFactoryMethod(Class beanClass, Method factoryMethod) {
        String property = properties.getProperty(methodToPropertyName(beanClass, factoryMethod) + ".default");
        if (property != null) {
            return Boolean.valueOf(property).booleanValue();
        }
        return false;
    }

    public String[] getParameterNames(Constructor constructor) {
        String property = properties.getProperty(constructorToPropertyName(constructor) + ".parameterNames");
        if (property != null) {
            ArrayList names = Collections.list(new StringTokenizer(property, ", "));
            return (String[]) names.toArray(new String[0]);
        }
        return null;
    }

    public String[] getParameterNames(Class beanClass, Method factoryMethod) {
        String property = properties.getProperty(methodToPropertyName(beanClass, factoryMethod) + ".parameterNames");
        if (property != null) {
            ArrayList names = Collections.list(new StringTokenizer(property, ", "));
            return (String[]) names.toArray(new String[0]);
        }
        return null;
    }

    public static String constructorToPropertyName(Constructor constructor) {
        StringBuffer buf = new StringBuffer();
        buf.append(constructor.getName()).append("(");
        Class[] parameterTypes = constructor.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            Class parameterType = parameterTypes[i];
            buf.append(parameterType.getName());
            if (i < parameterTypes.length - 1) {
                buf.append(",");
            }
        }
        buf.append(")");
        return buf.toString();
    }

    public static String methodToPropertyName(Class beanClass, Method method) {
        StringBuffer buf = new StringBuffer();
        buf.append(beanClass.getName()).append(".");
        buf.append(method.getName()).append("(");
        Class[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            Class parameterType = parameterTypes[i];
            buf.append(parameterType.getName());
            if (i < parameterTypes.length - 1) {
                buf.append(",");
            }
        }
        buf.append(")");
        return buf.toString();
    }

    public String getInitMethodName(String elementName) {
        return properties.getProperty(elementName + ".initMethod");
    }

    public String getDestroyMethodName(String elementName) {
        return properties.getProperty(elementName + ".destroyMethod");
    }

    public String getFactoryMethodName(String elementName) {
        return properties.getProperty(elementName + ".factoryMethod");
    }

    public String getContentProperty(String elementName) {
        return properties.getProperty(elementName + ".contentProperty");
    }
    
    public String getMapEntryName(String elementName, String property) {
        return properties.getProperty(elementName + "." + property + ".map.entryName");
    }

    public String getMapKeyName(String elementName, String property) {
        return properties.getProperty(elementName + "." + property + ".map.keyName");
    }

    public boolean isFlatMap(String elementName, String property) {
        return properties.getProperty(elementName + "." + property + ".map.flat") != null;
    }
    
    public String getMapDupsMode(String elementName, String property) {
        return properties.getProperty(elementName + "." + property + ".map.dups");
    }
    
    public String getMapDefaultKey(String elementName, String property) {
        return properties.getProperty(elementName + "." + property + ".map.defaultKey");
    }
    
    public String getFlatCollectionProperty(String elementName, String property)
    {
        return properties.getProperty(elementName + "." + property + ".flatCollection");
    }
    
    public boolean isFlatProperty(String elementName, String property)  {
        return properties.getProperty(elementName + "." + property + ".flat") != null;
    }
    
    public String getPropertyEditor(String elementName, String property)
    {
        return properties.getProperty(elementName + "." + property + ".propertyEditor");
    }

}
