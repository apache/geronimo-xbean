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
package org.apache.xbean.spring.context;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.w3c.dom.Document;

/**
 * SpringXmlPreprocessor preprocesses the xml Document before it is passed to Spring for processing.
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public interface SpringXmlPreprocessor {
    /**
     * Preprocesses the xml document which is being loaded by the specified application context and is being read by the
     * specified xml reader.
     * @param applicationContext the application context which is being loaded
     * @param reader the xml reader that read the document
     * @param document the xml document to read
     */
    public void preprocess(SpringApplicationContext applicationContext, XmlBeanDefinitionReader reader, Document document);
}
