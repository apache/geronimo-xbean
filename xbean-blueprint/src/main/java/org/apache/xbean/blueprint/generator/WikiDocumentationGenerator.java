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
package org.apache.xbean.blueprint.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Hiram Chirino
 * @version $Id$
 * @since 1.0
 */
public class WikiDocumentationGenerator implements GeneratorPlugin {
    private final File destFile;
    private LogFacade log;

    public WikiDocumentationGenerator(File destFile) {
        this.destFile = destFile;
    }

    public void generate(NamespaceMapping namespaceMapping) throws IOException {
        String namespace = namespaceMapping.getNamespace();
        File file = new File(destFile.getParentFile(), destFile.getName() + ".wiki");
        log.log("Generating WIKI documentation file: " + file + " for namespace: " + namespace);
        PrintWriter out = new PrintWriter(new FileWriter(file));
        try {
            generateDocumentation(out, namespaceMapping);
        } finally {
            out.close();
        }
    }

    private void generateDocumentation(PrintWriter out, NamespaceMapping namespaceMapping) {
        HashMap refercencedTypes = new HashMap();
    	
        // Build of map of types that are referenced by element types. 
        for (Iterator iter = namespaceMapping.getElements().iterator(); iter.hasNext();) {
            ElementMapping element = (ElementMapping) iter.next();
            for (Iterator iterator = element.getAttributes().iterator(); iterator.hasNext();) {
                AttributeMapping attribute = (AttributeMapping) iterator.next();
                Type type = getNestedType( attribute.getType() );
				
                if( namespaceMapping.isSimpleType( type) )
                    continue;
				
                if( !refercencedTypes.containsKey(type.getName()) )
                    refercencedTypes.put(type.getName(), new ArrayList());
            }
        }
        
        // Add all the elements that implement those types.
        for (Iterator iter = refercencedTypes.entrySet().iterator(); iter.hasNext();) {
        	
            Map.Entry entry = (Map.Entry) iter.next();
            String type = (String) entry.getKey();
            ArrayList implementations = (ArrayList) entry.getValue();

            for (Iterator iterator = namespaceMapping.getElements().iterator(); iterator.hasNext();) {
                ElementMapping element = (ElementMapping) iterator.next();
	            
                // Check to see if the class is matches
                boolean matched=false;
                if (type.equals(element.getClassName())) {
                    implementations.add(element);
                    matched=true;
                }
	            
                // Perhaps a super class matches.
                if(!matched) {
                    for (Iterator j = element.getSuperClasses().iterator(); j.hasNext();) {
                        String t = (String) j.next();
                        if( type.equals(t) ) {
                            implementations.add(element);
                            matched=true;
                            break;
                        }
                    }
                }
	            
                // Or it might be an interface.
                if(!matched) {
                    for (Iterator j = element.getInterfaces().iterator(); j.hasNext();) {
                        String t = (String) j.next();
                        if( type.equals(t) ) {
                            implementations.add(element);
                            matched=true;
                            break;
                        }
                    }
                }
            }
        }
        
        // Remove any entries that did not have associated elements
        for (Iterator iter = refercencedTypes.values().iterator(); iter.hasNext();) {        	
            ArrayList implementations = (ArrayList) iter.next();
            if( implementations.isEmpty() )
                iter.remove();
        }        

        generateElementsByType(out, namespaceMapping, refercencedTypes);
        generateElementsDetail(out, namespaceMapping, refercencedTypes);
        generateElementsIndex(out, namespaceMapping, refercencedTypes);
    }

    private Type getNestedType(Type type) {
        if( type.isCollection() ) {
            return getNestedType(type.getNestedType());
        } else {
            return type;
        }
    }
    
    private void generateElementsByType(PrintWriter out, NamespaceMapping namespaceMapping, HashMap refercencedTypes) {
        out.println("h3. Elements By Type");
        for (Iterator iter = refercencedTypes.entrySet().iterator(); iter.hasNext();) {
            Entry entry = (Entry) iter.next();
            String className = (String) entry.getKey();
            Collection elements = (Collection) entry.getValue();

            out.println("{anchor:"+className+"-types}");
            out.println("h4. The _["+className+"|#"+className+"-types]_ Type Implementations");

            for (Iterator iterator = elements.iterator(); iterator.hasNext();) {
                ElementMapping element = (ElementMapping) iterator.next();
                out.println("    | _[<"+element.getElementName() +">|#"+element.getElementName() +"-element]_ | {html}"+element.getDescription()+"{html} |");
            }
            out.println();        	
        }
        out.println();
    }

	private void generateElementsIndex(PrintWriter out, NamespaceMapping namespaceMapping, HashMap refercencedTypes) {
    	
        out.println("h3. Element Index");
        for (Iterator iter = namespaceMapping.getElements().iterator(); iter.hasNext();) {
            ElementMapping element = (ElementMapping) iter.next();
        	out.println("    | _[<"+element.getElementName() +">|#"+element.getElementName() +"-element]_ | {html}"+element.getDescription()+"{html} |");
        }
        out.println();
    }

    private void generateElementsDetail(PrintWriter out, NamespaceMapping namespaceMapping, HashMap refercencedTypes) {
        for (Iterator iter = namespaceMapping.getElements().iterator(); iter.hasNext();) {
            ElementMapping element = (ElementMapping) iter.next();
            generateElementDetail(out, namespaceMapping, element, refercencedTypes);
        }
    }

    private void generateElementDetail(PrintWriter out, NamespaceMapping namespaceMapping, ElementMapping element, HashMap refercencedTypes) {    

        out.println("{anchor:" + element.getElementName() + "-element}");
        out.println("h3. The _[<" + element.getElementName() + ">|#" + element.getElementName() + "-element]_ Element");

        out.println("    {html}"+element.getDescription()+"{html}");

        if( element.getAttributes().size() > 0 ) {
            out.println("h4. Properties");
            out.println("    || Property Name || Type || Description ||");

            for ( Iterator iterator = element.getAttributes().iterator(); iterator.hasNext(); ) {
                AttributeMapping attribute = (AttributeMapping) iterator.next();
                Type type = attribute.getPropertyEditor() != null ? Type.newSimpleType(String.class.getName()): attribute.getType();
                out.println("    | " + attribute.getAttributeName() + " | "+getTypeLink(type, refercencedTypes)+" | {html}"+attribute.getDescription()+"{html} |");	
	          }
        }
        out.println();
    }

    private String getTypeLink(Type type, HashMap refercencedTypes) {
        if (type.isCollection()) {
            return "(" + getTypeLink(type.getNestedType(), refercencedTypes) +  ")\\*";
        } else {
        	  if( refercencedTypes.containsKey(type.getName()) ) {
        		    return "_["+type.getName()+"|#"+type.getName()+"-types]_";
        	  } else {
                return "_"+type.getName()+"_";
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

