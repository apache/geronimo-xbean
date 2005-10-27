/**
 *
 * Copyright 2005 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.xbean.spring.generator;

import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.BeanProperty;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaParameter;
import com.thoughtworks.qdox.model.JavaSource;
import com.thoughtworks.qdox.model.Type;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class QdoxMappingLoader implements MappingLoader {
    public static final String XBEAN_ANNOTATION = "org.xbean.XBean";
    public static final String PROPERTY_ANNOTATION = "org.xbean.Property";
    public static final String INIT_METHOD_ANNOTATION = "org.xbean.InitMethod";
    public static final String DESTROY_METHOD_ANNOTATION = "org.xbean.DestroyMethod";
    public static final String FACTORY_METHOD_ANNOTATION = "org.xbean.FactoryMethod";

    private static final Log log = LogFactory.getLog(QdoxMappingLoader.class);
    private String defaultNamespace;
    private File[] srcDirs;

    public QdoxMappingLoader() {
    }

    public QdoxMappingLoader(String defaultNamespace, File[] srcDirs) {
        this.defaultNamespace = defaultNamespace;
        this.srcDirs = srcDirs;
    }

    public String getDefaultNamespace() {
        return defaultNamespace;
    }

    public void setDefaultNamespace(String defaultNamespace) {
        this.defaultNamespace = defaultNamespace;
    }

    public File[] getSrcDirs() {
        return srcDirs;
    }

    public void setSrcDirs(File[] srcDirs) {
        this.srcDirs = srcDirs;
    }

    public Set loadNamespaces() throws IOException {
        JavaDocBuilder builder = new JavaDocBuilder();

        log.debug("Source directories: ");

        for (int it = 0; it < srcDirs.length; it++) {
            File sourceDirectory = srcDirs[it];

            if (!sourceDirectory.isDirectory()) {
                log.warn("Specified source directory isn't a directory: '" + sourceDirectory.getAbsolutePath() + "'.");
            }
            log.debug(" - " + sourceDirectory.getAbsolutePath());

            builder.addSourceTree(sourceDirectory);
        }

        Set namespaces = loadNamespaces(builder);
        return namespaces;
    }

    private Set loadNamespaces(JavaDocBuilder builder) {
        // load all of the elements
        List elements = loadElements(builder);


        // index the elements by namespace and find the root element of each namespace
        Map elementsByNamespace = new HashMap();
        Map namespaceRoots = new HashMap();
        for (Iterator iterator = elements.iterator(); iterator.hasNext();) {
            ElementMapping element = (ElementMapping) iterator.next();
            String namespace = element.getNamespace();
            Set namespaceElements = (Set) elementsByNamespace.get(namespace);
            if (namespaceElements == null) {
                namespaceElements = new HashSet();
                elementsByNamespace.put(namespace, namespaceElements);
            }
            namespaceElements.add(element);
            if (element.isRootElement()) {
                if (namespaceRoots.containsKey(namespace)) {
                    log.warn("Multiple root elements found for namespace " + namespace);
                }
                namespaceRoots.put(namespace, element);
            }
        }

        // build the NamespaceMapping objects
        Set namespaces = new TreeSet();
        for (Iterator iterator = elementsByNamespace.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String namespace = (String) entry.getKey();
            Set namespaceElements = (Set) entry.getValue();
            ElementMapping rootElement = (ElementMapping) namespaceRoots.get(namespace);
            NamespaceMapping namespaceMapping = new NamespaceMapping(namespace, namespaceElements, rootElement);
            namespaces.add(namespaceMapping);
        }
        return Collections.unmodifiableSet(namespaces);
    }

    private List loadElements(JavaDocBuilder builder) {
        JavaSource[] javaSources = builder.getSources();
        List elements = new ArrayList();
        for (int i = 0; i < javaSources.length; i++) {
            JavaClass javaClass = javaSources[i].getClasses()[0];

            ElementMapping element = loadElement(builder, javaClass);
            if (element != null && !javaClass.isAbstract()) {
                elements.add(element);
            } else {
                log.debug("No XML annotation found for type: " + javaClass.getFullyQualifiedName());
            }
        }
        return elements;
    }

    private ElementMapping loadElement(JavaDocBuilder builder, JavaClass javaClass) {
        DocletTag tag = javaClass.getTagByName(XBEAN_ANNOTATION);
        if (tag == null) {
            return null;
        }

        String element = getElementName(javaClass, tag);
        String description = getProperty(tag, "description");
        String namespace = getProperty(tag, "namespace", defaultNamespace);
        boolean root = getBooleanProperty(tag, "rootElement");
        String contentProperty = getProperty(tag, "contentProperty");

        Set attributes = new HashSet();
        Map attributesByPropertyName = new HashMap();
        BeanProperty[] beanProperties = javaClass.getBeanProperties();
        for (int i = 0; i < beanProperties.length; i++) {
            BeanProperty beanProperty = beanProperties[i];

            AttributeMapping attributeMapping = loadAttribute(builder, beanProperty);
            if (attributeMapping != null) {
                attributes.add(attributeMapping);
                attributesByPropertyName.put(attributeMapping.getPropertyName(), attributeMapping);
            }
        }

        String initMethod = null;
        String destroyMethod = null;
        String factoryMethod = null;
        JavaMethod[] methods = javaClass.getMethods();
        for (int i = 0; i < methods.length; i++) {
            JavaMethod method = methods[i];
            if (method.isPublic() && !method.isConstructor()) {
                if (method.getTagByName(INIT_METHOD_ANNOTATION) != null) {
                    initMethod = method.getName();
                }
                if (method.getTagByName(DESTROY_METHOD_ANNOTATION) != null) {
                    destroyMethod = method.getName();
                }
                if (method.getTagByName(FACTORY_METHOD_ANNOTATION) != null) {
                    factoryMethod = method.getName();
                }
            }
        }

        Type listType = builder.getClassByName("java.util.List").asType();
        List constructorArgs = new ArrayList();
        for (int i = 0; i < methods.length; i++) {
            JavaMethod method = methods[i];
            JavaParameter[] parameters = method.getParameters();
            if (isValidConstructor(factoryMethod, method, parameters)) {
                List args = new ArrayList(parameters.length);
                for (int j = 0; j < parameters.length; j++) {
                    JavaParameter parameter = parameters[j];
                    String parameterName = parameter.getName();
                    String parameterType = parameter.getType().toString();
                    AttributeMapping attributeMapping = (AttributeMapping) attributesByPropertyName.get(parameterName);
                    if (attributeMapping != null && parameterType.equals(attributeMapping.getType())) {
                        // todo this is a bad bean... what should we do?
                    }
                    if (attributeMapping == null) {
                        boolean list = parameter.getType().isA(listType);
                        attributeMapping = new AttributeMapping(parameterName,
                                parameterName,
                                null,
                                parameterType,
                                primitives.contains(parameterType),
                                parameter.getType().isArray(),
                                parameter.getType().getValue(),
                                list,
                                null,
                                false,
                                false);
                        attributes.add(attributeMapping);
                        attributesByPropertyName.put(parameterName, attributeMapping);
                    }
                    args.add(attributeMapping);
                }
                constructorArgs.add(Collections.unmodifiableList(args));
            }
        }

        return new ElementMapping(namespace,
                element,
                javaClass.getFullyQualifiedName(),
                description,
                root,
                initMethod,
                destroyMethod,
                factoryMethod,
                contentProperty,
                attributes,
                constructorArgs);
    }

    private boolean isValidConstructor(String factoryMethod, JavaMethod method, JavaParameter[] parameters) {
        if (!method.isPublic() || parameters.length == 0) {
            return false;
        }

        if (factoryMethod == null) {
            return method.isConstructor();
        } else {
            return method.getName().equals(factoryMethod);
        }
    }

    private String getElementName(JavaClass javaClass, DocletTag tag) {
        String elementName = getProperty(tag, "element");
        if (elementName == null) {
            String className = javaClass.getFullyQualifiedName();
            int index = className.lastIndexOf(".");
            if (index > 0) {
                className = className.substring(index + 1);
            }
            // strip off "Bean" from a spring factory bean
            if (className.endsWith("FactoryBean")) {
                className = className.substring(0, className.length() - 4);
            }
            elementName = Utils.decapitalise(className);
        }
        return elementName;
    }

    private AttributeMapping loadAttribute(JavaDocBuilder builder, BeanProperty beanProperty) {
        Type listType = builder.getClassByName("java.util.List").asType();
        AttributeMapping attributeMapping = null;
        JavaMethod accessor = beanProperty.getAccessor();
        DocletTag propertyTag = accessor.getTagByName(PROPERTY_ANNOTATION);
        boolean hidden = getBooleanProperty(propertyTag, "hidden");
        if (!hidden) {
            String attribute = getProperty(propertyTag, "alias", beanProperty.getName());
            String attributeDescription = getProperty(propertyTag, "description");
            String defaultValue = getProperty(propertyTag, "default");
            boolean fixed = getBooleanProperty(propertyTag, "fixed");
            boolean required = getBooleanProperty(propertyTag, "required");
            boolean list = beanProperty.getType().isA(listType);
            attributeMapping = new AttributeMapping(attribute,
                    beanProperty.getName(),
                    attributeDescription,
                    beanProperty.getType().toString(),
                    primitives.contains(beanProperty.getType().toString()),
                    beanProperty.getType().isArray(),
                    beanProperty.getType().getValue(),
                    list,
                    defaultValue,
                    fixed,
                    required);
        }
        return attributeMapping;
    }

    private static String getProperty(DocletTag propertyTag, String propertyName) {
        return getProperty(propertyTag, propertyName, null);
    }

    private static String getProperty(DocletTag propertyTag, String propertyName, String defaultValue) {
        String value = null;
        if (propertyTag != null) {
            value = propertyTag.getNamedParameter(propertyName);
        }
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    private boolean getBooleanProperty(DocletTag propertyTag, String propertyName) {
        return toBoolean(getProperty(propertyTag, propertyName));
    }

    private static boolean toBoolean(String value) {
        if (value != null) {
            return new Boolean(value).booleanValue();
        }
        return false;
    }

    private static final Set primitives;

    static {
        Set set = new HashSet();
        set.add("boolean");
        set.add("byte");
        set.add("char");
        set.add("short");
        set.add("int");
        set.add("long");
        set.add("float");
        set.add("double");
        primitives = Collections.unmodifiableSet(set);
    }
}
