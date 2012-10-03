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

import java.lang.reflect.Constructor;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.w3c.dom.Element;

public class XBeanV2Helper {
    
    public static String version = null;

    public static BeanDefinitionParserDelegate createParser(XmlReaderContext readerContext) {
        try {
            String className = "org.apache.xbean.spring.context." + getVersion() + ".XBeanBeanDefinitionParserDelegate";
            Class clParser = Class.forName(className);
            Constructor cns = clParser.getConstructor(new Class[] { XmlReaderContext.class });
            return (BeanDefinitionParserDelegate) cns.newInstance(new Object[] { readerContext });
        } catch (Throwable e) {
            throw (IllegalStateException) new IllegalStateException("Unable to create namespace handler for: " + version).initCause(e);
        }
    }
    
    public static NamespaceHandler createNamespaceHandler() {
        try {
            String className = "org.apache.xbean.spring.context." + getVersion() + ".XBeanNamespaceHandler";
            Class clHandler = Class.forName(className);
            return (NamespaceHandler) clHandler.newInstance();
        } catch (Throwable e) {
            throw (IllegalStateException) new IllegalStateException("Unable to create namespace handler for: " + version).initCause(e);
        }
    }
    
    private static String getVersion() {
        if (version == null) {
            try {
                try {
                    Class.forName("org.springframework.beans.factory.parsing.BeanComponentDefinition");
                    version = "v2c";
                } catch (ClassNotFoundException e) {
                    Class cl = Class.forName("org.springframework.beans.factory.xml.BeanDefinitionParserDelegate");
                    try {
                        cl.getMethod("parsePropertyElements", new Class[] { Element.class, BeanDefinition.class });
                        version = "v2b";
                    } catch (NoSuchMethodException e2) {
                        version = "v2a";
                    }
                }
            } catch (Throwable e) {
                throw (IllegalStateException) new IllegalStateException("Could not create namespace handler for: " + version).initCause(e);
            }
        }
        return version;
    }
}
