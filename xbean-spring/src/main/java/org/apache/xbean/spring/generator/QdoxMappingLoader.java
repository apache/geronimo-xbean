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
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
    private Type collectionType;

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

    public Set<NamespaceMapping> loadNamespaces() throws IOException {
        JavaDocBuilder builder = new JavaDocBuilder();

        log.debug("Source directories: ");

        for (File sourceDirectory : srcDirs) {
            if (!sourceDirectory.isDirectory() && !sourceDirectory.toString().endsWith(".jar")) {
                log.warn("Specified source directory isn't a directory or a jar file: '" + sourceDirectory.getAbsolutePath() + "'.");
            }
            log.debug(" - " + sourceDirectory.getAbsolutePath());

            getSourceFiles(sourceDirectory, excludedClasses, builder);
        }

        collectionType = builder.getClassByName("java.util.Collection").asType();
        return loadNamespaces(builder);
    }

    private Set<NamespaceMapping> loadNamespaces(JavaDocBuilder builder) {
        // load all of the elements
        List<ElementMapping> elements = loadElements(builder);

        // index the elements by namespace and find the root element of each namespace
        Map<String, Set<ElementMapping>> elementsByNamespace = new HashMap<String, Set<ElementMapping>>();
        Map<String, ElementMapping> namespaceRoots = new HashMap<String, ElementMapping>();
        for (ElementMapping element : elements) {
            String namespace = element.getNamespace();
            Set<ElementMapping> namespaceElements = elementsByNamespace.get(namespace);
            if (namespaceElements == null) {
                namespaceElements = new HashSet<ElementMapping>();
                elementsByNamespace.put(namespace, namespaceElements);
            }
            namespaceElements.add(element);
            if (element.isRootElement()) {
                if (namespaceRoots.containsKey(namespace)) {
                    log.info("Multiple root elements found for namespace " + namespace);
                }
                namespaceRoots.put(namespace, element);
            }
        }

        // build the NamespaceMapping objects
        Set<NamespaceMapping> namespaces = new TreeSet<NamespaceMapping>();
        for (Map.Entry<String, Set<ElementMapping>> entry : elementsByNamespace.entrySet()) {
            String namespace = entry.getKey();
            Set namespaceElements = entry.getValue();
            ElementMapping rootElement = namespaceRoots.get(namespace);
            NamespaceMapping namespaceMapping = new NamespaceMapping(namespace, namespaceElements, rootElement);
            namespaces.add(namespaceMapping);
        }
        return Collections.unmodifiableSet(namespaces);
    }

    private List<ElementMapping> loadElements(JavaDocBuilder builder) {
        JavaSource[] javaSources = builder.getSources();
        List<ElementMapping> elements = new ArrayList<ElementMapping>();
        for (JavaSource javaSource : javaSources) {
            if (javaSource.getClasses().length == 0) {
                log.info("No Java Classes defined in: " + javaSource.getURL());
            } else {
                JavaClass[] classes = javaSource.getClasses();
                for (JavaClass javaClass : classes) {
                    ElementMapping element = loadElement(builder, javaClass);
                    if (element != null && !javaClass.isAbstract()) {
                        elements.add(element);
                    } else {
                        log.debug("No XML annotation found for type: " + javaClass.getFullyQualifiedName());
                    }
                }
            }
        }
        return elements;
    }

    private ElementMapping loadElement(JavaDocBuilder builder, JavaClass javaClass) {
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
        String factoryClass = getProperty(xbeanTag, "factoryClass");

        Map<String, MapMapping> mapsByPropertyName = new HashMap<String, MapMapping>();
        List<String> flatProperties = new ArrayList<String>();
        Map<String, String> flatCollections = new HashMap<String, String>();
        Set<AttributeMapping> attributes = new HashSet<AttributeMapping>();
        Map<String, AttributeMapping> attributesByPropertyName = new HashMap<String, AttributeMapping>();

        for (JavaClass jClass = javaClass; jClass != null; jClass = jClass.getSuperJavaClass()) {
            BeanProperty[] beanProperties = jClass.getBeanProperties();
            for (BeanProperty beanProperty : beanProperties) {
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
                            MapMapping mm = new MapMapping(
                                    mapTag.getNamedParameter("entryName"),
                                    mapTag.getNamedParameter("keyName"),
                                    Boolean.valueOf(mapTag.getNamedParameter("flat")),
                                    mapTag.getNamedParameter("dups"),
                                    mapTag.getNamedParameter("defaultKey"));
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
            for (JavaMethod method : methods) {
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

        List<List<ParameterMapping>> constructorArgs = new ArrayList<List<ParameterMapping>>();
        JavaMethod[] methods = javaClass.getMethods();
        for (JavaMethod method : methods) {
            JavaParameter[] parameters = method.getParameters();
            if (isValidConstructor(factoryMethod, method, parameters)) {
                List<ParameterMapping> args = new ArrayList<ParameterMapping>(parameters.length);
                for (JavaParameter parameter : parameters) {
                    AttributeMapping attributeMapping = attributesByPropertyName.get(parameter.getName());
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

        HashSet<String> interfaces = new HashSet<String>();
        interfaces.addAll(getFullyQualifiedNames(javaClass.getImplementedInterfaces()));

        JavaClass actualClass = javaClass;
        if (factoryClass != null) {
            JavaClass clazz = builder.getClassByName(factoryClass);
            if (clazz != null) {
                log.info("Detected factory: using " + factoryClass + " instead of " + javaClass.getFullyQualifiedName());
                actualClass = clazz;
            } else {
                log.info("Could not load class built by factory: " + factoryClass);
            }
        }

        ArrayList<String> superClasses = new ArrayList<String>();
        JavaClass p = actualClass;
        if (actualClass != javaClass) {
            superClasses.add(actualClass.getFullyQualifiedName());
        }
        while (true) {
            JavaClass s = p.getSuperJavaClass();
            if (s == null || s.equals(p) || "java.lang.Object".equals(s.getFullyQualifiedName())) {
                break;
            }
            p = s;
            superClasses.add(p.getFullyQualifiedName());
            interfaces.addAll(getFullyQualifiedNames(p.getImplementedInterfaces()));
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
                flatCollections,
                superClasses,
                interfaces);
    }

    private List<String> getFullyQualifiedNames(JavaClass[] implementedInterfaces) {
        ArrayList<String> l = new ArrayList<String>();
        for (JavaClass implementedInterface : implementedInterfaces) {
            l.add(implementedInterface.getFullyQualifiedName());
        }
        return l;
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
        String propertyEditor = getProperty(propertyTag, "propertyEditor");

        return new AttributeMapping(attribute,
                beanProperty.getName(),
                attributeDescription,
                toMappingType(beanProperty.getType(), nestedType),
                defaultValue,
                fixed,
                required,
                propertyEditor);
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
                false,
                null);
    }

    private String getParameterDescription(JavaParameter parameter) {
        String parameterName = parameter.getName();
        DocletTag[] tags = parameter.getParentMethod().getTagsByName("param");
        for (DocletTag tag : tags) {
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
            return Boolean.valueOf(value);
        }
        return false;
    }

    private org.apache.xbean.spring.generator.Type toMappingType(Type type, String nestedType) {
        try {
            if (type.isArray()) {
                return org.apache.xbean.spring.generator.Type.newArrayType(type.getValue(), type.getDimensions());
            } else if (type.isA(collectionType)) {
                if (nestedType == null) nestedType = "java.lang.Object";
                return org.apache.xbean.spring.generator.Type.newCollectionType(type.getValue(),
                        org.apache.xbean.spring.generator.Type.newSimpleType(nestedType));
            }
        } catch (Throwable t) {
            log.debug("Could not load type mapping", t);
        }
        return org.apache.xbean.spring.generator.Type.newSimpleType(type.getValue());
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

    private static void getSourceFiles(File base, String[] excludedClasses, JavaDocBuilder builder) throws IOException {
        if (base.isDirectory()) {
            listAllFileNames(base, "", excludedClasses, builder);
        } else {
            listAllJarEntries(base, excludedClasses, builder);
        }
    }

    private static void listAllFileNames(File base, String prefix, String[] excludedClasses, JavaDocBuilder builder) throws IOException {
        if (!base.canRead() || !base.isDirectory()) {
            throw new IllegalArgumentException(base.getAbsolutePath());
        }
        File[] hits = base.listFiles();
        for (File hit : hits) {
            String name = prefix.equals("") ? hit.getName() : prefix + "/" + hit.getName();
            if (hit.canRead() && !isExcluded(name, excludedClasses)) {
                if (hit.isDirectory()) {
                    listAllFileNames(hit, name, excludedClasses, builder);
                } else if (name.endsWith(".java")) {
                    builder.addSource(hit);
                }
            }
        }
    }

    private static void listAllJarEntries(File base, String[] excludedClasses, JavaDocBuilder builder) throws IOException {
        JarFile jarFile = new JarFile(base);
        for (Enumeration entries = jarFile.entries(); entries.hasMoreElements(); ) {
            JarEntry entry = (JarEntry) entries.nextElement();
            String name = entry.getName();
            if (name.endsWith(".java") && !isExcluded(name, excludedClasses) && !name.endsWith("/package-info.java")) {
                builder.addSource(new URL("jar:" + base.toURI().toURL().toString() + "!/" + name));
            }
        }
    }

    private static boolean isExcluded(String sourceName, String[] excludedClasses) {
        if (excludedClasses == null) {
            return false;
        }

        String className = sourceName;
        if (sourceName.endsWith(".java")) {
            className = className.substring(0, className.length() - ".java".length());
        }
        className = className.replace('/', '.');
        for (String excludedClass : excludedClasses) {
            if (className.equals(excludedClass)) {
                return true;
            }
        }
        return false;
    }
}
