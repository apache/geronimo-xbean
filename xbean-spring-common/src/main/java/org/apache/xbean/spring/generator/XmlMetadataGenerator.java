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
package org.apache.xbean.spring.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.xbean.spring.context.impl.NamespaceHelper;


/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class XmlMetadataGenerator implements GeneratorPlugin {
    private final String metaInfDir;
    private LogFacade log;
    private final File schema;

    public static final String NAMESPACE_HANDLER = "org.apache.xbean.spring.context.v2.XBeanNamespaceHandler";

    public XmlMetadataGenerator(String metaInfDir, File schema) {
        this.metaInfDir = metaInfDir;
        this.schema = schema;
    }

    public void generate(NamespaceMapping namespaceMapping) throws IOException {
        String namespace = namespaceMapping.getNamespace();
        if (namespace == null) {
            return;
        }

        File file = new File(metaInfDir, NamespaceHelper.createDiscoveryPathName(namespace));
        file.getParentFile().mkdirs();
        log.log("Generating META-INF properties file: " + file + " for namespace: " + namespace);
        PrintWriter out = new PrintWriter(new FileWriter(file));
        try {
            generatePropertiesFile(out, namespaceMapping.getElements());
        } finally {
            out.close();
        }
        
        // Generate spring 2.0 mapping
        file = new File(metaInfDir, "META-INF/spring.handlers");
        log.log("Generating Spring 2.0 handler mapping: " + file + " for namespace: " + namespace);
        out = new PrintWriter(new FileWriter(file));
        try {
            out.println(namespace.replaceAll(":", "\\\\:") + "=" + NAMESPACE_HANDLER);
        } finally {
            out.close();
        }

        if (schema != null) {
            String cp = new File(metaInfDir).toURI().relativize(schema.toURI()).toString();
            file = new File(metaInfDir, "META-INF/spring.schemas");
            log.log("Generating Spring 2.0 schema mapping: " + file + " for namespace: " + namespace);
            out = new PrintWriter(new FileWriter(file));
            try {
                out.println(namespace.replaceAll(":", "\\\\:") + "=" + cp);
            } finally {
                out.close();
            }
        }
    }

    private void generatePropertiesFile(PrintWriter out, Set elements) {
        out.println("# NOTE: this file is autogenerated by Apache XBean");
        out.println();
        out.println("# beans");

        for (Iterator iter = elements.iterator(); iter.hasNext();) {
            ElementMapping element = (ElementMapping) iter.next();
            out.println(element.getElementName() + " = " + element.getClassName());

            generatePropertiesFileContent(out, element);
            generatePropertiesFilePropertyAliases(out, element);
            generatePropertiesFileConstructors(out, element);
            out.println();
        }
    }

    private void generatePropertiesFileContent(PrintWriter out, ElementMapping element) {
        String contentProperty = element.getContentProperty();
        if (contentProperty != null) {
            out.println(element.getElementName() + ".contentProperty = " + contentProperty);
        }
        String initMethod = element.getInitMethod();
        if (initMethod != null) {
            out.println(element.getElementName() + ".initMethod = " + initMethod);
        }

        String destroyMethod = element.getDestroyMethod();
        if (destroyMethod != null) {
            out.println(element.getElementName() + ".destroyMethod = " + destroyMethod);
        }

        String factoryMethod = element.getFactoryMethod();
        if (factoryMethod != null) {
            out.println(element.getElementName() + ".factoryMethod = " + factoryMethod);
        }

        for (Iterator iter = element.getAttributes().iterator(); iter.hasNext();) {
            AttributeMapping attribute = (AttributeMapping) iter.next();
            if( attribute.getPropertyEditor() !=null ) {
                out.println(element.getElementName() + "."+attribute.getPropertyName()+ ".propertyEditor = " + attribute.getPropertyEditor());
            }
        }

        List flatProperties = element.getFlatProperties();
        for (Iterator itr = flatProperties.iterator(); itr.hasNext();) {
            out.println(element.getElementName() + "." + itr.next() + ".flat");
        }

        Map maps = element.getMapMappings();
        for (Iterator itr = maps.entrySet().iterator(); itr.hasNext();) {
            Map.Entry entry = (Map.Entry) itr.next();
            MapMapping mm = (MapMapping) entry.getValue();
            out.println(element.getElementName() + "." + entry.getKey() + ".map.entryName = " + mm.getEntryName());
            out.println(element.getElementName() + "." + entry.getKey() + ".map.keyName = " + mm.getKeyName());
        }

        Map flatCollections = element.getFlatCollections();
        for (Iterator itr = flatCollections.entrySet().iterator(); itr.hasNext();) {
            Map.Entry entry = (Map.Entry) itr.next();
            String child = (String) entry.getValue();
            out.println(element.getElementName() + "." + child + ".flatCollection = " + entry.getKey());
        }
    }

    private void generatePropertiesFileConstructors(PrintWriter out, ElementMapping element) {
        List constructors = element.getConstructors();
        for (Iterator iterator = constructors.iterator(); iterator.hasNext();) {
            List args = (List) iterator.next();
            generatePropertiesFileConstructor(out, element, args);
        }
    }

    private void generatePropertiesFileConstructor(PrintWriter out, ElementMapping element, List args) {
        out.print(element.getClassName());
        if (element.getFactoryMethod() != null) {
            out.print("." + element.getFactoryMethod());
        }
        out.print("(");
        for (Iterator iterator = args.iterator(); iterator.hasNext();) {
            ParameterMapping parameterMapping = (ParameterMapping) iterator.next();
            out.print(parameterMapping.getType().getName());
            if (iterator.hasNext()) {
                out.print(",");
            }
        }
        out.print(").parameterNames =");
        for (Iterator iterator = args.iterator(); iterator.hasNext();) {
            ParameterMapping parameterMapping = (ParameterMapping) iterator.next();
            out.print(" ");
            out.print(parameterMapping.getName());
        }
        out.println();
    }

    private void generatePropertiesFilePropertyAliases(PrintWriter out, ElementMapping element) {
        for (Iterator iterator = element.getAttributes().iterator(); iterator.hasNext();) {
            AttributeMapping attributeMapping = (AttributeMapping) iterator.next();
            String propertyName = attributeMapping.getPropertyName();
            String attributeName = attributeMapping.getAttributeName();
            if (!propertyName.equals(attributeName)) {
                if (List.class.getName().equals(attributeMapping.getType().getName())) {
                    out.println(element.getElementName() + ".list." + attributeName + " = " + propertyName);
                } else {
                    out.println(element.getElementName() + ".alias." + attributeName + " = " + propertyName);
                }
            }
        }
    }

    public LogFacade getLog() {
        return log;
    }

    public void setLog(LogFacade log) {
        this.log = log;
    }
}
