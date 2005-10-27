package org.xbean.spring.generator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jam.JAnnotatedElement;
import org.codehaus.jam.JAnnotation;
import org.codehaus.jam.JAnnotationValue;
import org.codehaus.jam.JClass;
import org.codehaus.jam.JComment;
import org.codehaus.jam.JConstructor;
import org.codehaus.jam.JMethod;
import org.codehaus.jam.JParameter;
import org.codehaus.jam.JProperty;
import org.codehaus.jam.JamService;
import org.codehaus.jam.JamServiceFactory;
import org.codehaus.jam.JamServiceParams;

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
 * An Ant task for executing Gram scripts, which are Groovy scripts executed on
 * the JAM context.
 *
 * @version $Revision$
 */
public class JamMappingLoader implements MappingLoader {
    private static final Log log = LogFactory.getLog(JamMappingLoader.class);
    private String defaultNamespace;
    private File[] toolClasspath;
    private File[] classpath;
    private File[] srcDirs;
    private String includes = "**/*.java";

    public JamMappingLoader() {
    }

    public JamMappingLoader(String defaultNamespace, File[] toolClasspath, File[] classpath, File[] srcDirs, String includes) {
        this.defaultNamespace = defaultNamespace;
        this.toolClasspath = toolClasspath;
        this.classpath = classpath;
        this.srcDirs = srcDirs;
        this.includes = includes;
    }

    public String getDefaultNamespace() {
        return defaultNamespace;
    }

    public void setDefaultNamespace(String defaultNamespace) {
        this.defaultNamespace = defaultNamespace;
    }

    public File[] getToolClasspath() {
        return toolClasspath;
    }

    public void setToolClasspath(File[] toolClasspath) {
        this.toolClasspath = toolClasspath;
    }

    public File[] getClasspath() {
        return classpath;
    }

    public void setClasspath(File[] classpath) {
        this.classpath = classpath;
    }

    public File[] getSrcDirs() {
        return srcDirs;
    }

    public void setSrcDirs(File[] srcDirs) {
        this.srcDirs = srcDirs;
    }

    public String getIncludes() {
        return includes;
    }

    public void setIncludes(String includes) {
        this.includes = includes;
    }

    public Set loadNamespaces() throws IOException {
        if (defaultNamespace == null) {
            throw new IllegalArgumentException("defaultNamespace must be specified");
        }
        if (srcDirs == null || srcDirs.length == 0) {
            throw new IllegalArgumentException("srcDirs must be specified");
        }

        JamServiceFactory jamServiceFactory = JamServiceFactory.getInstance();
        JamServiceParams serviceParams = jamServiceFactory.createServiceParams();
        if (toolClasspath != null) {
            for (int i = 0; i < toolClasspath.length; i++) {
                File file = toolClasspath[i];
                serviceParams.addToolClasspath(file);
            }
        }
        if (classpath != null) {
            for (int i = 0; i < classpath.length; i++) {
                File file = classpath[i];
                serviceParams.addClasspath(file);
            }
        }

        serviceParams.includeSourcePattern(srcDirs, includes);
        JamService jam = jamServiceFactory.createService(serviceParams);
        JClass[] classes = jam.getAllClasses();
        Set namespaces = loadNamespaces(classes, defaultNamespace);
        return namespaces;
    }

    private Set loadNamespaces(JClass[] classes, String defaultNamespace) {
        Map namespaceElements = new HashMap();
        Map namespaceRoots = new HashMap();
        for (int i = 0; i < classes.length; i++) {
            JClass type = classes[i];
            JAnnotation annotation = type.getAnnotation(Utils.XBEAN_ANNOTATION);
            if (annotation != null) {
                String localName = getStringValue(annotation, "element", getElementName(type));
                String namespace = getStringValue(annotation, "namespace", defaultNamespace);

                ElementMapping element = loadElement(type, localName, namespace);
                Set elements = (Set) namespaceElements.get(namespace);
                if (elements == null) {
                    elements = new HashSet();
                    namespaceElements.put(namespace, elements);
                }
                elements.add(element);
                if (element.isRootElement()) {
                    if (namespaceRoots.containsKey(namespace)) {
                        log.warn("Multiple root elements found for namespace " + namespace);
                    }
                    namespaceRoots.put(namespace, element);
                }
            } else {
                log.debug("No XML annotation found for type: " + type.getQualifiedName());
            }
        }

        Set namespaces = new TreeSet();
        for (Iterator iterator = namespaceElements.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String namespace = (String) entry.getKey();
            Set elements = (Set) entry.getValue();
            ElementMapping rootElement = (ElementMapping) namespaceRoots.get(namespace);
            NamespaceMapping namespaceMapping = new NamespaceMapping(namespace, elements, rootElement);
            namespaces.add(namespaceMapping);
        }
        return Collections.unmodifiableSet(namespaces);
    }

    public ElementMapping loadElement(JClass type, String elementName, String namespace) {
        String className = type.getQualifiedName();
        String description = getDescription(type);

        JAnnotation annotation = type.getAnnotation(Utils.XBEAN_ANNOTATION);
        String contentProperty = getStringValue(annotation, "contentProperty");
        boolean root = getBooleanValue(annotation, "rootElement");

        Set attributes = new HashSet();
        Map attributesByPropertyName = new HashMap();
        JProperty[] properties = type.getProperties();
        for (int i = 0; i < properties.length; i++) {
            JProperty property = properties[i];
            if (isVisibleProperty(property)) {
                String attributeName = getAttributeName(property);
                String propertyName = Utils.decapitalise(property.getSimpleName());
                String attributeType = property.getType().getQualifiedName();
                boolean list = false;
                String value = null;
                boolean fixed = false;
                boolean required = false;
                boolean primitive = property.getType().isPrimitiveType();
                boolean array = property.getType().isArrayType();
                String arrayType = null;
                if (array) {
                    arrayType = property.getType().getArrayComponentType().getQualifiedName();
                }
                String attributeDescription = getDescription(property);
                AttributeMapping attributeMapping = new AttributeMapping(attributeName,
                        propertyName,
                        attributeDescription,
                        attributeType,
                        primitive,
                        array,
                        arrayType,
                        list,
                        value,
                        fixed,
                        required);
                attributes.add(attributeMapping);
                attributesByPropertyName.put(propertyName, attributeMapping);
            }
        }

        JConstructor[] constructors = type.getConstructors();
        List constructorArgs = new ArrayList(constructors.length);
        for (int i = 0; i < constructors.length; i++) {
            JConstructor constructor = constructors[i];
            JParameter[] parameters = constructor.getParameters();
            if (parameters.length > 0) {
                List args = new ArrayList(parameters.length);
                for (int j = 0; j < parameters.length; j++) {
                    JParameter parameter = parameters[j];
                    String parameterName = Utils.decapitalise(parameter.getSimpleName());
                    String parameterType = parameter.getType().getQualifiedName();
                    AttributeMapping attributeMapping = (AttributeMapping) attributesByPropertyName.get(parameterName);
                    if (attributeMapping != null && parameterType.equals(attributeMapping.getType())) {
                        // todo this is a bad bean... what should we do?
                    }
                    if (attributeMapping == null) {
                        String attributeName = parameterName;
                        String propertyName = parameterName;
                        String attributeType = parameter.getType().getQualifiedName();
                        boolean list = false;
                        String value = null;
                        boolean fixed = false;
                        boolean required = false;
                        boolean primitive = parameter.getType().isPrimitiveType();
                        boolean array = parameter.getType().isArrayType();
                        String arrayType = null;
                        if (array) {
                            arrayType = parameter.getType().getArrayComponentType().getQualifiedName();
                        }
                        String attributeDescription = null;
                        attributeMapping = new AttributeMapping(attributeName,
                                propertyName,
                                attributeDescription,
                                attributeType,
                                primitive,
                                array,
                                arrayType,
                                list,
                                value,
                                fixed,
                                required);
                        attributes.add(attributeMapping);
                        attributesByPropertyName.put(propertyName, attributeMapping);
                    }
                    args.add(attributeMapping);
                }
                constructorArgs.add(Collections.unmodifiableList(args));
            }
        }
        List constructorList = Collections.unmodifiableList(constructorArgs);

        String initMethod = null;
        String destroyMethod = null;
        String factoryMethod = null;

        return new ElementMapping(namespace,
                elementName,
                className,
                description,
                root,
                initMethod,
                destroyMethod,
                factoryMethod,
                contentProperty,
                attributes,
                constructorList);
    }

    private static String getStringValue(JAnnotation annotation, String name) {
        return getStringValue(annotation, name, null);
    }

    private static String getStringValue(JAnnotation annotation, String name, String defaultValue) {
        if (annotation != null) {
            JAnnotationValue value = annotation.getValue(name);
            if (value != null) {
                return value.asString();
            }
        }
        return defaultValue;
    }

    private static boolean getBooleanValue(JAnnotation annotation, String name) {
        if (annotation != null) {
            JAnnotationValue value = annotation.getValue(name);
            if (value != null) {
                return value.asBoolean();
            }
        }
        return false;
    }

    private static String getElementName(JClass element) {
        String answer = element.getSimpleName();
        if (answer.length() > 0) {
            answer = Utils.decapitalise(answer);
        }

        // lets strip off the trailing Bean for *FactoryBean types by default
        if (element instanceof JClass && answer.endsWith("FactoryBean")) {
            answer = answer.substring(0, answer.length() - 4);
        }
        return answer;
    }

    private static String getDescription(JClass type) {
        return getCommentText(type);
    }

    private static String getDescription(JProperty property) {
        JMethod setter = property.getSetter();
        if (setter != null) {
            return getCommentText(setter);
        }
        return "";
    }

    private static String getCommentText(JAnnotatedElement element) {
        JAnnotation annotation = element.getAnnotation(Utils.XBEAN_ANNOTATION);
        if (annotation != null) {
            JAnnotationValue value = annotation.getValue("description");
            if (value != null) {
                return value.asString();
            }
        }
        JComment comment = element.getComment();
        if (comment != null) {
            return comment.getText();
        }
        return "";
    }

    private static boolean isVisibleProperty(JProperty property) {
        if (!property.getSimpleName().equals("Class")) {
            JMethod setter = property.getSetter();
            if (setter == null) {
                return false;
            }
            JAnnotation annotation = setter.getAnnotation(Utils.XBEAN_ANNOTATION);
            if (annotation != null) {
                JAnnotationValue value = annotation.getValue("hide");
                if (value != null) {
                    return !value.asBoolean();
                }
            }
            return true;
        }
        return false;
    }

    private static String getAttributeName(JProperty property) {
        String attributeName = getStringValue(property.getAnnotation(Utils.PROPERTY_ANNOTATION), "alias", property.getSimpleName());
        return Utils.decapitalise(attributeName);
    }
}
