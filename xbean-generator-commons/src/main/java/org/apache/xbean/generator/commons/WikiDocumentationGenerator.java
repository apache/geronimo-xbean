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
package org.apache.xbean.generator.commons;

import org.apache.xbean.generator.*;
import org.apache.xbean.generator.artifact.SimpleArtifact;
import org.apache.xbean.model.*;
import org.apache.xbean.model.mapping.AttributeMapping;
import org.apache.xbean.model.mapping.ElementMapping;
import org.apache.xbean.model.mapping.NamespaceMapping;
import org.apache.xbean.model.type.Types;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author Hiram Chirino
 * @version $Id$
 * @since 1.0
 */
public class WikiDocumentationGenerator implements GeneratorPlugin {

    private final File destFile;
    private final ArtifactSet artifactSet;
    private LogFacade log;

    public WikiDocumentationGenerator(File destFile, ArtifactSet artifactSet, LogFacade logFacade) {
        this.destFile = destFile;
        this.artifactSet = artifactSet;
        this.log = logFacade;
    }

    public void generate(NamespaceMapping namespaceMapping, Map<String, String> configuration) throws IOException {
        String namespace = namespaceMapping.getNamespace();
        String fileName = new NamespaceFileNameExtractor().apply(namespace);

        File file = new File(destFile.getParentFile(), fileName + ".wiki");
        log.log("Generating WIKI documentation file: " + file + " for namespace: " + namespace);
        PrintWriter out = new PrintWriter(new FileWriter(file));
        try {
            generateDocumentation(out, namespaceMapping);
            Map<String, String> meta = new HashMap<>();
            meta.put("type", "wiki");
            meta.put("classifier", "documentation");
            artifactSet.register(new SimpleArtifact(file, true, meta));
        } finally {
            out.close();
        }
    }

    private void generateDocumentation(PrintWriter out, NamespaceMapping namespaceMapping) {
        Map<String, List<ElementMapping>> referencedTypes = new HashMap<String, List<ElementMapping>>();

        // Build of map of types that are referenced by element types. 
        for (ElementMapping element : namespaceMapping.getElements()) {
            for (AttributeMapping attribute : element.getAttributes()) {
                Type type = getNestedType(attribute.getType());

                if (!type.isComplex())
                    continue;

                if (!referencedTypes.containsKey(type.getName()))
                    referencedTypes.put(type.getName(), new ArrayList<>());
            }
        }
        
        // Add all the elements that implement those types.
        for (Entry<String, List<ElementMapping>> stringListEntry : referencedTypes.entrySet()) {

            Entry<String, List<ElementMapping>> entry = stringListEntry;
            String type = entry.getKey();
            List<ElementMapping> implementations = entry.getValue();

            for (ElementMapping element : namespaceMapping.getElements()) {
                // Check to see if the class is matches
                boolean matched = false;
                if (type.equals(element.getClassName())) {
                    implementations.add(element);
                    matched = true;
                }

                // Perhaps a super class matches.
                if (!matched) {
                    for (String parent : element.getSuperClasses()) {
                        if (type.equals(parent)) {
                            implementations.add(element);
                            matched = true;
                            break;
                        }
                    }
                }

                // Or it might be an interface.
                if (!matched) {
                    for (String iface : element.getInterfaces()) {
                        if (type.equals(iface)) {
                            implementations.add(element);
                            matched = true;
                            break;
                        }
                    }
                }
            }
        }
        
        // Remove any entries that did not have associated elements
        for (Iterator<List<ElementMapping>> iter = referencedTypes.values().iterator(); iter.hasNext();) {
            List<ElementMapping> implementations = iter.next();
            if( implementations.isEmpty() )
                iter.remove();
        }        

        generateElementsByType(out, namespaceMapping, referencedTypes);
        generateElementsDetail(out, namespaceMapping, referencedTypes);
        generateElementsIndex(out, namespaceMapping, referencedTypes);
    }

    private Type getNestedType(Type type) {
        if (type instanceof NestingType) {
            return getNestedType(((NestingType) type).getNestedType());
        }
        return type;
    }
    
    private void generateElementsByType(PrintWriter out, NamespaceMapping namespaceMapping, Map<String, List<ElementMapping>> referencedTypes) {
        out.println("h3. Elements By SimpleType");
        for (Entry<String, List<ElementMapping>> entry : referencedTypes.entrySet()) {
            String className = entry.getKey();
            Collection<ElementMapping> elements = entry.getValue();

            out.println("{anchor:" + className + "-types}");
            out.println("h4. The _[" + className + "|#" + className + "-types]_ SimpleType Implementations");

            for (ElementMapping element : elements) {
                out.println("    | _[<" + element.getElementName() + ">|#" + element.getElementName() + "-element]_ | {html}" + element.getDescription() + "{html} |");
            }
            out.println();
        }
        out.println();
    }

    private void generateElementsIndex(PrintWriter out, NamespaceMapping namespaceMapping, Map<String, List<ElementMapping>> referencedTypes) {
        out.println("h3. Element Index");
        for (ElementMapping element : namespaceMapping.getElements()) {
            out.println("    | _[<" + element.getElementName() + ">|#" + element.getElementName() + "-element]_ | {html}" + element.getDescription() + "{html} |");
        }
        out.println();
    }

    private void generateElementsDetail(PrintWriter out, NamespaceMapping namespaceMapping, Map<String, List<ElementMapping>> referencedTypes) {
        for (Iterator iter = namespaceMapping.getElements().iterator(); iter.hasNext();) {
            ElementMapping element = (ElementMapping) iter.next();
            generateElementDetail(out, namespaceMapping, element, referencedTypes);
        }
    }

    private void generateElementDetail(PrintWriter out, NamespaceMapping namespaceMapping, ElementMapping element, Map<String, List<ElementMapping>> referencedTypes) {

        out.println("{anchor:" + element.getElementName() + "-element}");
        out.println("h3. The _[<" + element.getElementName() + ">|#" + element.getElementName() + "-element]_ Element");

        out.println("    {html}"+element.getDescription()+"{html}");

        if (element.getAttributes().size() > 0 ) {
            out.println("h4. Properties");
            out.println("    || Property Name || SimpleType || Description ||");

            for (AttributeMapping attribute : element.getAttributes()) {
                Type type = attribute.getPropertyEditor() != null ? Types.newSimpleType(String.class.getName()) : attribute.getType();
                out.println("    | " + attribute.getAttributeName() + " | " + getTypeLink(type, referencedTypes) + " | {html}" + attribute.getDescription() + "{html} |");
            }
        }
        out.println();
    }

    private String getTypeLink(Type type, Map<String, List<ElementMapping>> referencedTypes) {
        if (type instanceof NestingType) {
            return "(" + getTypeLink(((NestingType) type).getNestedType(), referencedTypes) +  ")\\*";
        } else {
            if (referencedTypes.containsKey(type.getName()) ) {
                return "_["+type.getName()+"|#"+type.getName()+"-types]_";
            }

            return "_"+type.getName()+"_";
        }
    }

    public LogFacade getLog() {
        return log;
    }

    public void setLog(LogFacade log) {
        this.log = log;
    }
}

