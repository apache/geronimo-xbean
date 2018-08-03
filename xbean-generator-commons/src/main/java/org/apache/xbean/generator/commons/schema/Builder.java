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
package org.apache.xbean.generator.commons.schema;

import org.apache.ws.commons.schema.*;
import org.apache.xbean.generator.commons.dom.VirtualNodeList;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Helper type for handling XML Schema related operations.
 */
public class Builder {

    private final Document document;

    public Builder() throws ParserConfigurationException {
        this(createDocument());
    }

    public Builder(Document document) {
        this.document = document;
    }

    public XmlSchema createSchema(String namespace) {
        XmlSchemaCollection schemaCollection = new XmlSchemaCollection();
        XmlSchema schema = new XmlSchema(namespace, schemaCollection);
        schema.setSchemaNamespacePrefix("xs");
        schema.setElementFormDefault(XmlSchemaForm.QUALIFIED);
        return schema;
    }

    public void createAnnotation(XmlSchemaAnnotated annotated, String description) {
        Optional.ofNullable(description)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(this::createCDATA)
            .map(this::annotation)
            .ifPresent(annotated::setAnnotation);
    }

    public XmlSchemaSequence sequence() {
        return new XmlSchemaSequence();
    }

    public XmlSchemaChoice choice(long min, long max) {
        XmlSchemaChoice choice = new XmlSchemaChoice();
        choice.setMinOccurs(min);
        choice.setMaxOccurs(max);
        return choice;
    }

    public XmlSchemaChoice choice() {
        return new XmlSchemaChoice();
    }

    public XmlSchemaAny unboundedAny() {
        XmlSchemaAny any = new XmlSchemaAny();
        any.setNamespace("##other");
        any.setMinOccurs(0);
        any.setMaxOccurs(Long.MAX_VALUE);
        return any;
    }

    public XmlSchemaAny any() {
        XmlSchemaAny any = new XmlSchemaAny();
        any.setNamespace("##other");
        return any;
    }

    public void createAnnotation(XmlSchemaAnnotated annotated, Supplier<String> description) {
        createAnnotation(annotated, description.get());
    }

    private XmlSchemaAnnotation annotation(Node node) {
        XmlSchemaAnnotation annotation = new XmlSchemaAnnotation();
        XmlSchemaDocumentation documentation = new XmlSchemaDocumentation();
        documentation.setMarkup(new VirtualNodeList(node));
        annotation.getItems().add(documentation);
        return  annotation;
    }

    private CDATASection createCDATA(String text) {
        return this.document.createCDATASection(text);
    }

    // create an empty DOM document
    private static Document createDocument() throws ParserConfigurationException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);

        return documentBuilderFactory.newDocumentBuilder().newDocument();
    }

    public XmlSchemaAnyAttribute anyAttributeLax() {
        XmlSchemaAnyAttribute anyAttribute = new XmlSchemaAnyAttribute();
        anyAttribute.setNamespace("##other");
        anyAttribute.setProcessContent(XmlSchemaContentProcessing.LAX);
        return anyAttribute;
    }
}
