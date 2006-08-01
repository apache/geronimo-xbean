/**
 *
 * Copyright 2005-2006 The Apache Software Foundation or its licensors, as applicable.
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
package org.apache.xbean.spring.generator;

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
import java.util.LinkedHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.BeanProperty;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaParameter;
import com.thoughtworks.qdox.model.JavaSource;
import com.thoughtworks.qdox.model.Type;

/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class QdoxMappingLoader implements MappingLoader {
    public static final String XBEAN_ANNOTATION = "org.apache.xbean.XBean";
    public static final String PROPERTY_ANNOTATION = "org.apache.xbean.Property";
    public static final String INIT_METHOD_ANNOTATION = "org.apache.xbean.InitMethod";
    public static final String DESTROY_METHOD_ANNOTATION = "org.apache.xbean.DestroyMethod";
    public static final String FACTORY_METHOD_ANNOTATION = "org.apache.xbean.FactoryMethod";
    public static final String MAP_ANNOTATION = "org.apache.xbean.Map";
    public static final String FLAT_PROPERTY_ANNOTATION = "org.apache.xbean.Flat";
    public static final String FLAT_COLLECTION_ANNOTATION = "org.apache.xbean.FlatCollection";
    public static final String ELEMENT_ANNOTATION = "org.apache.xbean.Element";
    
    private static final Log log = LogFactory.getLog(QdoxMappingLoader.class);
    private final String defaultNamespace;
    private final File[] srcDirs;
    private final String[] excludedClasses;
    private Type listType;

    public QdoxMappingLoader(String defaultNamespace, File[] srcDirs, String[] excludedClasses) {
        this.defaultNamespace = defaultNamespace;
        this.srcDirs = srcDirs;
        this.excludedClasses = excludedClasses;
    }

    public String getDefaultNamespace() {
        return defaultNamespace;
    }

    public File[] getSrcDirs() {
        return srcDirs;
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

            Map sourceFiles = getSourceFiles(sourceDirectory, excludedClasses);
            for (Iterator iterator = sourceFiles.values().iterator(); iterator.hasNext();) {
                File file = (File) iterator.next();
                builder.addSource(file);
            }
        }

        listType = builder.getClassByName("java.util.List").asType();
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

            ElementMapping element = loadElement(javaClass);
            if (element != null && !javaClass.isAbstract()) {
                elements.add(element);
            } else {
                log.debug("No XML annotation found for type: " + javaClass.getFullyQualifiedName());
            }
        }
        return elements;
    }

    private ElementMapping loadElement(JavaClass javaClass) {
        DocletTag xbeanTag = javaClass.getTagByName(XBEAN_ANNOTATION);
        if (xbeanTag == null) {
            return null;
        }

        String element = getElementName(javaClass, xbeanTag);
        String description = getProperty(xbeanTag, "description");
        if (description == null) {
            description = javaClass.getComment();

        }
        String namespace = getProperty(xbeanTag, "namespace", defaultNamespace);
        boolean root = getBooleanProperty(xbeanTag, "rootElement");
        String contentProperty = getProperty(xbeanTag, "contentProperty");

        Map mapsByPropertyName = new HashMap();
        List flatProperties = new ArrayList();
        Map flatCollections = new HashMap();
        Set attributes = new HashSet();
        Map attributesByPropertyName = new HashMap();
        
        for (JavaClass jClass = javaClass; jClass != null; jClass = jClass.getSuperJavaClass()) {
            BeanProperty[] beanProperties = jClass.getBeanProperties();
            for (int i = 0; i < beanProperties.length; i++) {
                BeanProperty beanProperty = beanProperties[i];
    
                // we only care about properties with a setter
                if (beanProperty.getMutator() != null) {
                    AttributeMapping attributeMapping = loadAttribute(beanProperty, "");
                    if (attributeMapping != null) {
                        attributes.add(attributeMapping);
                        attributesByPropertyName.put(attributeMapping.getPropertyName(), attributeMapping);
                    }
                    JavaMethod acc = beanProperty.getAccessor();
                    if (acc != null) {
                        DocletTag mapTag = acc.getTagByName(MAP_ANNOTATION);
                        if (mapTag != null) {
                            MapMapping mm = new MapMapping(mapTag.getNamedParameter("entryName"), 
                                    mapTag.getNamedParameter("keyName"));
                            mapsByPropertyName.put(beanProperty.getName(), mm);
                        }
                        
                        DocletTag flatColTag = acc.getTagByName(FLAT_COLLECTION_ANNOTATION);
                        if (flatColTag != null) {
                            String childName = flatColTag.getNamedParameter("childElement");
                            if (childName == null)
                                throw new InvalidModelException("Flat collections must specify the childElement attribute.");
                            flatCollections.put(beanProperty.getName(), childName);
                        }
                        
                        DocletTag flatPropTag = acc.getTagByName(FLAT_PROPERTY_ANNOTATION);
                        if (flatPropTag != null) {
                            flatProperties.add(beanProperty.getName());
                        }
                    }
                }
            }
        }

        String initMethod = null;
        String destroyMethod = null;
        String factoryMethod = null;
        for (JavaClass jClass = javaClass; jClass != null; jClass = jClass.getSuperJavaClass()) {
            JavaMethod[] methods = javaClass.getMethods();
            for (int i = 0; i < methods.length; i++) {
                JavaMethod method = methods[i];
                if (method.isPublic() && !method.isConstructor()) {
                    if (initMethod == null && method.getTagByName(INIT_METHOD_ANNOTATION) != null) {
                        initMethod = method.getName();
                    }
                    if (destroyMethod == null && method.getTagByName(DESTROY_METHOD_ANNOTATION) != null) {
                        destroyMethod = method.getName();
                    }
                    if (factoryMethod == null && method.getTagByName(FACTORY_METHOD_ANNOTATION) != null) {
                        factoryMethod = method.getName();
                    }
                    
                }
            }
        }

        List constructorArgs = new ArrayList();
        JavaMethod[] methods = javaClass.getMethods();
        for (int i = 0; i < methods.length; i++) {
            JavaMethod method = methods[i];
            JavaParameter[] parameters = method.getParameters();
            if (isValidConstructor(factoryMethod, method, parameters)) {
                List args = new ArrayList(parameters.length);
                for (int j = 0; j < parameters.length; j++) {
                    JavaParameter parameter = parameters[j];
                    AttributeMapping attributeMapping = (AttributeMapping) attributesByPropertyName.get(parameter.getName());
                    if (attributeMapping == null) {
                        attributeMapping = loadParameter(parameter);

                        attributes.add(attributeMapping);
                        attributesByPropertyName.put(attributeMapping.getPropertyName(), attributeMapping);
                    }
                    args.add(new ParameterMapping(attributeMapping.getPropertyName(), toMappingType(parameter.getType(), null)));
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
                constructorArgs,
                flatProperties,
                mapsByPropertyName,
                flatCollections);
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

    private AttributeMapping loadAttribute(BeanProperty beanProperty, String defaultDescription) {
        DocletTag propertyTag = getPropertyTag(beanProperty);

        if (getBooleanProperty(propertyTag, "hidden")) {
            return null;
        }

        String attribute = getProperty(propertyTag, "alias", beanProperty.getName());
        String attributeDescription = getAttributeDescription(beanProperty, propertyTag, defaultDescription);
        String defaultValue = getProperty(propertyTag, "default");
        boolean fixed = getBooleanProperty(propertyTag, "fixed");
        boolean required = getBooleanProperty(propertyTag, "required");
        String nestedType = getProperty(propertyTag, "nestedType");

        return new AttributeMapping(attribute,
                beanProperty.getName(),
                attributeDescription,
                toMappingType(beanProperty.getType(), nestedType),
                defaultValue,
                fixed,
                required);
    }

    private static DocletTag getPropertyTag(BeanProperty beanProperty) {
        JavaMethod accessor = beanProperty.getAccessor();
        if (accessor != null) {
            DocletTag propertyTag = accessor.getTagByName(PROPERTY_ANNOTATION);
            if (propertyTag != null) {
                return propertyTag;
            }
        }
        JavaMethod mutator = beanProperty.getMutator();
        if (mutator != null) {
            DocletTag propertyTag = mutator.getTagByName(PROPERTY_ANNOTATION);
            if (propertyTag != null) {
                return propertyTag;
            }
        }
        return null;
    }

    private String getAttributeDescription(BeanProperty beanProperty, DocletTag propertyTag, String defaultDescription) {
        String description = getProperty(propertyTag, "description");
        if (description != null && description.trim().length() > 0) {
            return description.trim();
        }

        JavaMethod accessor = beanProperty.getAccessor();
        if (accessor != null) {
            description = accessor.getComment();
            if (description != null && description.trim().length() > 0) {
                return description.trim();
            }
        }

        JavaMethod mutator = beanProperty.getMutator();
        if (mutator != null) {
            description = mutator.getComment();
            if (description != null && description.trim().length() > 0) {
                return description.trim();
            }
        }
        return defaultDescription;
    }

    private AttributeMapping loadParameter(JavaParameter parameter) {
        String parameterName = parameter.getName();
        String parameterDescription = getParameterDescription(parameter);

        // first attempt to load the attribute from the java beans accessor methods
        JavaClass javaClass = parameter.getParentMethod().getParentClass();
        BeanProperty beanProperty = javaClass.getBeanProperty(parameterName);
        if (beanProperty != null) {
            AttributeMapping attributeMapping = loadAttribute(beanProperty, parameterDescription);
            // if the attribute mapping is null, the property was tagged as hidden and this is an error
            if (attributeMapping == null) {
                throw new InvalidModelException("Hidden property usage: " +
                        "The construction method " + toMethodLocator(parameter.getParentMethod()) +
                        " can not use a hidded property " + parameterName);
            }
            return attributeMapping;
        }

        // create an attribute solely based on the parameter information
        return new AttributeMapping(parameterName,
                parameterName,
                parameterDescription,
                toMappingType(parameter.getType(), null),
                null,
                false,
                false);
    }

    private String getParameterDescription(JavaParameter parameter) {
        String parameterName = parameter.getName();
        DocletTag[] tags = parameter.getParentMethod().getTagsByName("param");
        for (int k = 0; k < tags.length; k++) {
            DocletTag tag = tags[k];
            if (tag.getParameters()[0].equals(parameterName)) {
                String parameterDescription = tag.getValue().trim();
                if (parameterDescription.startsWith(parameterName)) {
                    parameterDescription = parameterDescription.substring(parameterName.length()).trim();
                }
                return parameterDescription;
            }
        }
        return null;
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

    private org.apache.xbean.spring.generator.Type toMappingType(Type type, String nestedType) {
        if (type.isArray()) {
            return org.apache.xbean.spring.generator.Type.newArrayType(type.getValue(), type.getDimensions());
        } else if (type.isA(listType)) {
            if (nestedType == null) nestedType = "java.lang.Object";
            return org.apache.xbean.spring.generator.Type.newCollectionType(type.getValue(),
                    org.apache.xbean.spring.generator.Type.newSimpleType(nestedType));
        } else {
            return org.apache.xbean.spring.generator.Type.newSimpleType(type.getValue());
        }
    }

    private static String toMethodLocator(JavaMethod method) {
        StringBuffer buf = new StringBuffer();
        buf.append(method.getParentClass().getFullyQualifiedName());
        if (!method.isConstructor()) {
            buf.append(".").append(method.getName());
        }
        buf.append("(");
        JavaParameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            JavaParameter parameter = parameters[i];
            if (i > 0) {
                buf.append(", ");
            }
            buf.append(parameter.getName());
        }
        buf.append(") : ").append(method.getLineNumber());
        return buf.toString();
    }

    private static Map getSourceFiles(File base, String[] excludedClasses) {
        return listAllFileNames(base, "", excludedClasses);
    }

    private static Map listAllFileNames(File base, String prefix, String[] excludedClasses) {
        if (!base.canRead() || !base.isDirectory()) {
            throw new IllegalArgumentException(base.getAbsolutePath());
        }
        Map map = new LinkedHashMap();
        File[] hits = base.listFiles();
        for (int i = 0; i < hits.length; i++) {
            File hit = hits[i];
            String name = prefix.equals("") ? hit.getName() : prefix + "/" + hit.getName();
            if (hit.canRead() && !isExcluded(name, excludedClasses)) {
                if (hit.isDirectory()) {
                    map.putAll(listAllFileNames(hit, name, excludedClasses));
                } else if (name.endsWith(".java")) {
                    map.put(name, hit);
                }
            }
        }
        return map;
    }

    private static boolean isExcluded(String sourceName, String[] excludedClasses) {
        if (excludedClasses == null) {
            return false;
        }

        String className = sourceName;
        if (sourceName.endsWith(".java")) {
            className = className.substring(0, className.length() - ".java".length());
        }
        className = className.replace("/", ".");
        for (int i = 0; i < excludedClasses.length; i++) {
            String excludedClass = excludedClasses[i];
            if (className.equals(excludedClass)) {
                return true;
            }
        }
        return false;
    }
}
