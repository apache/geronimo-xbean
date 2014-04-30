/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.xbean.blueprint.cm;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.ext.impl.ExtNamespaceHandler;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutableCollectionMetadata;
import org.apache.aries.blueprint.mutable.MutableComponentMetadata;
import org.apache.aries.blueprint.mutable.MutableIdRefMetadata;
import org.apache.aries.blueprint.mutable.MutableMapMetadata;
import org.apache.aries.blueprint.mutable.MutableRefMetadata;
import org.apache.aries.blueprint.mutable.MutableReferenceMetadata;
import org.apache.aries.blueprint.mutable.MutableValueMetadata;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.IdRefMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.osgi.service.blueprint.reflect.ValueMetadata;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Modified from aries CmNamespaceHandler
 *
 * @version $Rev$ $Date$
 */
public class CmNamespaceHandler implements NamespaceHandler {

    public static final String BLUEPRINT_NAMESPACE = "http://www.osgi.org/xmlns/blueprint/v1.0.0";
    public static final String XBEAN_CM_NAMESPACE = "http://xbean.apache.org/blueprint/xmlns/xbean-cm/v1.0.0";

    public static final String PROPERTY_PLACEHOLDER_ELEMENT = "property-placeholder";
//    public static final String MANAGED_PROPERTIES_ELEMENT = "managed-properties";
//    public static final String MANAGED_SERVICE_FACTORY_ELEMENT = "managed-service-factory";
    public static final String CM_PROPERTIES_ELEMENT = "cm-properties";
    public static final String DEFAULT_PROPERTIES_ELEMENT = "default-properties";
    public static final String PROPERTY_ELEMENT = "property";
    public static final String INTERFACES_ELEMENT = "interfaces";
    public static final String VALUE_ELEMENT = "value";
    public static final String MANAGED_COMPONENT_ELEMENT = "managed-component";

    public static final String ID_ATTRIBUTE = "id";
    public static final String PERSISTENT_ID_ATTRIBUTE = "persistent-id";
    public static final String PLACEHOLDER_PREFIX_ATTRIBUTE = "placeholder-prefix";
    public static final String PLACEHOLDER_SUFFIX_ATTRIBUTE = "placeholder-suffix";
    public static final String DEFAULTS_REF_ATTRIBUTE = "defaults-ref";
    public static final String UPDATE_STRATEGY_ATTRIBUTE = "update-strategy";
    public static final String UPDATE_METHOD_ATTRIBUTE = "update-method";
    public static final String FACTORY_PID_ATTRIBUTE = "factory-pid";
    public static final String AUTO_EXPORT_ATTRIBUTE = "auto-export";
    public static final String RANKING_ATTRIBUTE = "ranking";
    public static final String INTERFACE_ATTRIBUTE = "interface";
    public static final String UPDATE_ATTRIBUTE = "update";

    public static final String AUTO_EXPORT_DISABLED = "disabled";
    public static final String AUTO_EXPORT_INTERFACES = "interfaces";
    public static final String AUTO_EXPORT_CLASS_HIERARCHY = "class-hierarchy";
    public static final String AUTO_EXPORT_ALL = "all-classes";
    public static final String AUTO_EXPORT_DEFAULT = AUTO_EXPORT_DISABLED;
    public static final String RANKING_DEFAULT = "0";

    private static final String MANAGED_OBJECT_MANAGER_NAME = "org.apache.aries.managedObjectManager";

    private static final Logger LOGGER = LoggerFactory.getLogger(CmNamespaceHandler.class);

//    private final ConfigurationAdmin configAdmin;

    private int idCounter;

//    public CmNamespaceHandler(ConfigurationAdmin configAdmin) {
//        this.configAdmin = configAdmin;
//    }

    public URL getSchemaLocation(String namespace) {
        return getClass().getResource("xbean-cm.xsd");
    }

    public Set<Class> getManagedClasses() {
        return new HashSet<Class>(Arrays.asList(
                JexlPropertyPlaceholder.class
        ));
    }

    public Metadata parse(Element element, ParserContext context) {
        LOGGER.debug("Parsing element {{}}{}", element.getNamespaceURI(), element.getLocalName());
        ComponentDefinitionRegistry registry = context.getComponentDefinitionRegistry();
//        registerManagedObjectManager(context, registry);
        if (nodeNameEquals(element, PROPERTY_PLACEHOLDER_ELEMENT)) {
            return parsePropertyPlaceholder(context, element);
//        } else if (nodeNameEquals(element, MANAGED_SERVICE_FACTORY_ELEMENT)) {
//            return parseManagedServiceFactory(context, element);
        } else {
            throw new ComponentDefinitionException("Unsupported element: " + element.getNodeName());
        }
    }

    public ComponentMetadata decorate(Node node, ComponentMetadata component, ParserContext context) {
        LOGGER.debug("Decorating node {{}}{}", node.getNamespaceURI(), node.getLocalName());
        ComponentDefinitionRegistry registry = context.getComponentDefinitionRegistry();
//        registerManagedObjectManager(context, registry);
        if (node instanceof Element) {
//            if (nodeNameEquals(node, MANAGED_PROPERTIES_ELEMENT)) {
//                return decorateManagedProperties(context, (Element) node, component);
//            } else
//            if (nodeNameEquals(node, CM_PROPERTIES_ELEMENT)) {
//                return decorateCmProperties(context, (Element) node, component);
//            } else {
            throw new ComponentDefinitionException("Unsupported element: " + node.getNodeName());
//            }
        } else {
            throw new ComponentDefinitionException("Illegal use of blueprint cm namespace");
        }
    }

    private ComponentMetadata parsePropertyPlaceholder(ParserContext context, Element element) {
        MutableBeanMetadata metadata = context.createMetadata(MutableBeanMetadata.class);
        metadata.setProcessor(true);
        metadata.setId(getId(context, element));
        metadata.setScope(BeanMetadata.SCOPE_SINGLETON);
        metadata.setRuntimeClass(JexlPropertyPlaceholder.class);
        metadata.addProperty("blueprintContainer", createRef(context, "blueprintContainer"));
//        metadata.addProperty("configAdmin", createConfigAdminProxy(context));
        metadata.addProperty("configAdmin", createReference(context, ConfigurationAdmin.class.getName()));
        metadata.addProperty("persistentId", createValue(context, element.getAttribute(PERSISTENT_ID_ATTRIBUTE)));
//        metadata.addArgument(createRef(context, "blueprintContainer"), BlueprintContainer.class.getName(), 0);
//        metadata.addArgument(createReference(context, ConfigurationAdmin.class.getName()), ConfigurationAdmin.class.getName(), 1);
//        metadata.addArgument(createValue(context, element.getAttribute(PERSISTENT_ID_ATTRIBUTE)), String.class.getName(), 2);
        String prefix = element.hasAttribute(PLACEHOLDER_PREFIX_ATTRIBUTE)
                ? element.getAttribute(PLACEHOLDER_PREFIX_ATTRIBUTE)
                : "${";
        metadata.addProperty("placeholderPrefix", createValue(context, prefix));
        String suffix = element.hasAttribute(PLACEHOLDER_SUFFIX_ATTRIBUTE)
                ? element.getAttribute(PLACEHOLDER_SUFFIX_ATTRIBUTE)
                : "}";
        metadata.addProperty("placeholderSuffix", createValue(context, suffix));
        String defaultsRef = element.hasAttribute(DEFAULTS_REF_ATTRIBUTE) ? element.getAttribute(DEFAULTS_REF_ATTRIBUTE) : null;
        if (defaultsRef != null) {
            metadata.addProperty("defaultProperties", createRef(context, defaultsRef));
        }
        String ignoreMissingLocations = element.hasAttributeNS(ExtNamespaceHandler.BLUEPRINT_EXT_NAMESPACE_V1_0, ExtNamespaceHandler.IGNORE_MISSING_LOCATIONS_ATTRIBUTE)
                ? element.getAttributeNS(ExtNamespaceHandler.BLUEPRINT_EXT_NAMESPACE_V1_0, ExtNamespaceHandler.IGNORE_MISSING_LOCATIONS_ATTRIBUTE) : null;
        if (ignoreMissingLocations != null) {
            metadata.addProperty("ignoreMissingLocations", createValue(context, ignoreMissingLocations));
        }
        String systemProperties = element.hasAttributeNS(ExtNamespaceHandler.BLUEPRINT_EXT_NAMESPACE_V1_0, ExtNamespaceHandler.SYSTEM_PROPERTIES_ATTRIBUTE)
                ? element.getAttributeNS(ExtNamespaceHandler.BLUEPRINT_EXT_NAMESPACE_V1_0, ExtNamespaceHandler.SYSTEM_PROPERTIES_ATTRIBUTE) : null;
        if (systemProperties == null) {
            systemProperties = ExtNamespaceHandler.SYSTEM_PROPERTIES_NEVER;
        }
        metadata.addProperty("systemProperties", createValue(context, systemProperties));
        // Parse elements
        List<String> locations = new ArrayList<String>();
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                if (XBEAN_CM_NAMESPACE.equals(e.getNamespaceURI())) {
                    if (nodeNameEquals(e, DEFAULT_PROPERTIES_ELEMENT)) {
                        if (defaultsRef != null) {
                            throw new ComponentDefinitionException("Only one of " + DEFAULTS_REF_ATTRIBUTE + " attribute or " + DEFAULT_PROPERTIES_ELEMENT + " element is allowed");
                        }
                        Metadata props = parseDefaultProperties(context, metadata, e);
                        metadata.addProperty("defaultProperties", props);
                    }
                } else if (ExtNamespaceHandler.BLUEPRINT_EXT_NAMESPACE_V1_0.equals(e.getNamespaceURI())) {
                    if (nodeNameEquals(e, ExtNamespaceHandler.LOCATION_ELEMENT)) {
                        locations.add(getTextValue(e));
                    }
                }
            }
        }
        if (!locations.isEmpty()) {
            metadata.addProperty("locations", createList(context, locations));
        }

//        PlaceholdersUtils.validatePlaceholder(metadata, context.getComponentDefinitionRegistry());

        return metadata;
    }

    private Metadata parseDefaultProperties(ParserContext context, MutableBeanMetadata enclosingComponent, Element element) {
        MutableMapMetadata props = context.createMetadata(MutableMapMetadata.class);
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                if (XBEAN_CM_NAMESPACE.equals(e.getNamespaceURI())) {
                    if (nodeNameEquals(e, PROPERTY_ELEMENT)) {
                        BeanProperty prop = context.parseElement(BeanProperty.class, enclosingComponent, e);
                        props.addEntry(createValue(context, prop.getName(), String.class.getName()), prop.getValue());
                    }
                }
            }
        }
        return props;
    }

//    private ComponentMetadata parseManagedServiceFactory(ParserContext context, Element element) {
//        String id = getId(context, element);
//
//        MutableBeanMetadata factoryMetadata = context.createMetadata(MutableBeanMetadata.class);
//        generateIdIfNeeded(context, factoryMetadata);
//        factoryMetadata.addProperty("id", createValue(context, factoryMetadata.getId()));
//        factoryMetadata.setScope(BeanMetadata.SCOPE_SINGLETON);
//        factoryMetadata.setRuntimeClass(CmManagedServiceFactory.class);
//        factoryMetadata.setInitMethod("init");
//        factoryMetadata.setDestroyMethod("destroy");
//        factoryMetadata.addProperty("configAdmin", createConfigAdminProxy(context));
//        factoryMetadata.addProperty("blueprintContainer", createRef(context, "blueprintContainer"));
//        factoryMetadata.addProperty("factoryPid", createValue(context, element.getAttribute(FACTORY_PID_ATTRIBUTE)));
//        String autoExport = element.hasAttribute(AUTO_EXPORT_ATTRIBUTE) ? element.getAttribute(AUTO_EXPORT_ATTRIBUTE) : AUTO_EXPORT_DEFAULT;
//        if (AUTO_EXPORT_DISABLED.equals(autoExport)) {
//            autoExport = Integer.toString(ServiceMetadata.AUTO_EXPORT_DISABLED);
//        } else if (AUTO_EXPORT_INTERFACES.equals(autoExport)) {
//            autoExport = Integer.toString(ServiceMetadata.AUTO_EXPORT_INTERFACES);
//        } else if (AUTO_EXPORT_CLASS_HIERARCHY.equals(autoExport)) {
//            autoExport = Integer.toString(ServiceMetadata.AUTO_EXPORT_CLASS_HIERARCHY);
//        } else if (AUTO_EXPORT_ALL.equals(autoExport)) {
//            autoExport = Integer.toString(ServiceMetadata.AUTO_EXPORT_ALL_CLASSES);
//        } else {
//            throw new ComponentDefinitionException("Illegal value (" + autoExport + ") for " + AUTO_EXPORT_ATTRIBUTE + " attribute");
//        }
//        factoryMetadata.addProperty("autoExport", createValue(context, autoExport));
//        String ranking = element.hasAttribute(RANKING_ATTRIBUTE) ? element.getAttribute(RANKING_ATTRIBUTE) : RANKING_DEFAULT;
//        factoryMetadata.addProperty("ranking", createValue(context, ranking));
//
//        List<String> interfaces = null;
//        if (element.hasAttribute(INTERFACE_ATTRIBUTE)) {
//            interfaces = Collections.singletonList(element.getAttribute(INTERFACE_ATTRIBUTE));
//            factoryMetadata.addProperty("interfaces", createList(context, interfaces));
//        }
//
//        Parser parser = getParser(context);
//
//        // Parse elements
//        List<RegistrationListener> listeners = new ArrayList<RegistrationListener>();
//        NodeList nl = element.getChildNodes();
//        for (int i = 0; i < nl.getLength(); i++) {
//            Node node = nl.item(i);
//            if (node instanceof Element) {
//                Element e = (Element) node;
//                if (isBlueprintNamespace(e.getNamespaceURI())) {
//                    if (nodeNameEquals(e, INTERFACES_ELEMENT)) {
//                        if (interfaces != null) {
//                            throw new ComponentDefinitionException("Only one of " + Parser.INTERFACE_ATTRIBUTE + " attribute or " + INTERFACES_ELEMENT + " element must be used");
//                        }
//                        interfaces = parseInterfaceNames(e);
//                        factoryMetadata.addProperty("interfaces", createList(context, interfaces));
//                    } else if (nodeNameEquals(e, Parser.SERVICE_PROPERTIES_ELEMENT)) {
//                        MapMetadata map = parser.parseServiceProperties(e, factoryMetadata);
//                        factoryMetadata.addProperty("serviceProperties", map);
//                    } else if (nodeNameEquals(e, Parser.REGISTRATION_LISTENER_ELEMENT)) {
//                        listeners.add(parser.parseRegistrationListener(e, factoryMetadata));
//                    }
//                } else if (XBEAN_CM_NAMESPACE.equals(e.getNamespaceURI())) {
//                    if (nodeNameEquals(e, MANAGED_COMPONENT_ELEMENT)) {
//                        MutableBeanMetadata managedComponent = context.parseElement(MutableBeanMetadata.class, null, e);
//                        generateIdIfNeeded(context, managedComponent);
//                        managedComponent.setScope(BeanMetadata.SCOPE_PROTOTYPE);
//                        // destroy-method on managed-component has different signature than on regular beans
//                        // so we'll handle it differently
//                        String destroyMethod = managedComponent.getDestroyMethod();
//                        if (destroyMethod != null) {
//                            factoryMetadata.addProperty("componentDestroyMethod", createValue(context, destroyMethod));
//                            managedComponent.setDestroyMethod(null);
//                        }
//                        context.getComponentDefinitionRegistry().registerComponentDefinition(managedComponent);
//                        factoryMetadata.addProperty("managedComponentName", createIdRef(context, managedComponent.getId()));
//                    }
//                }
//            }
//        }
//
//        MutableCollectionMetadata listenerCollection = context.createMetadata(MutableCollectionMetadata.class);
//        listenerCollection.setCollectionClass(List.class);
//        for (RegistrationListener listener : listeners) {
//            MutableBeanMetadata bean = context.createMetadata(MutableBeanMetadata.class);
//            bean.setRuntimeClass(ServiceListener.class);
//            bean.addProperty("listener", listener.getListenerComponent());
//            bean.addProperty("registerMethod", createValue(context, listener.getRegistrationMethod()));
//            bean.addProperty("unregisterMethod", createValue(context, listener.getUnregistrationMethod()));
//            listenerCollection.addValue(bean);
//        }
//        factoryMetadata.addProperty("listeners", listenerCollection);
//
//        context.getComponentDefinitionRegistry().registerComponentDefinition(factoryMetadata);
//
//        MutableBeanMetadata mapMetadata = context.createMetadata(MutableBeanMetadata.class);
//        mapMetadata.setScope(BeanMetadata.SCOPE_SINGLETON);
//        mapMetadata.setId(id);
//        mapMetadata.setFactoryComponent(createRef(context, factoryMetadata.getId()));
//        mapMetadata.setFactoryMethod("getServiceMap");
//        return mapMetadata;
//    }

//    private ComponentMetadata decorateCmProperties(ParserContext context, Element element, ComponentMetadata component) {
//        generateIdIfNeeded(context, ((MutableComponentMetadata) component));
//        MutableBeanMetadata metadata = context.createMetadata(MutableBeanMetadata.class);
//        metadata.setProcessor(true);
//        metadata.setId(getId(context, element));
//        metadata.setRuntimeClass(CmProperties.class);
//        String persistentId = element.getAttribute(PERSISTENT_ID_ATTRIBUTE);
//        // if persistentId is "" the cm-properties element in nested in managed-service-factory
//        // and the configuration object will come from the factory. So we only really need to register
//        // ManagedService if the persistentId is not an empty string.
//        if (persistentId.length() > 0) {
//            metadata.setInitMethod("init");
//            metadata.setDestroyMethod("destroy");
//        }
//        metadata.addProperty("blueprintContainer", createRef(context, "blueprintContainer"));
//        metadata.addProperty("configAdmin", createConfigAdminProxy(context));
//        metadata.addProperty("managedObjectManager", createRef(context, MANAGED_OBJECT_MANAGER_NAME));
//        metadata.addProperty("persistentId", createValue(context, persistentId));
//        if (element.hasAttribute(UPDATE_ATTRIBUTE)) {
//            metadata.addProperty("update", createValue(context, element.getAttribute(UPDATE_ATTRIBUTE)));
//        }
//        metadata.addProperty("serviceId", createIdRef(context, component.getId()));
//        context.getComponentDefinitionRegistry().registerComponentDefinition(metadata);
//        return component;
//    }

//    private ComponentMetadata decorateManagedProperties(ParserContext context, Element element, ComponentMetadata component) {
//        if (!(component instanceof MutableBeanMetadata)) {
//            throw new ComponentDefinitionException("Element " + MANAGED_PROPERTIES_ELEMENT + " must be used inside a <bp:bean> element");
//        }
//        generateIdIfNeeded(context, ((MutableBeanMetadata) component));
//        MutableBeanMetadata metadata = context.createMetadata(MutableBeanMetadata.class);
//        metadata.setProcessor(true);
//        metadata.setId(getId(context, element));
//        metadata.setRuntimeClass(CmManagedProperties.class);
//        String persistentId = element.getAttribute(PERSISTENT_ID_ATTRIBUTE);
//        // if persistentId is "" the managed properties element in nested in managed-service-factory
//        // and the configuration object will come from the factory. So we only really need to register
//        // ManagedService if the persistentId is not an empty string.
//        if (persistentId.length() > 0) {
//            metadata.setInitMethod("init");
//            metadata.setDestroyMethod("destroy");
//        }
//        metadata.addProperty("blueprintContainer", createRef(context, "blueprintContainer"));
//        metadata.addProperty("configAdmin", createConfigAdminProxy(context));
//        metadata.addProperty("managedObjectManager", createRef(context, MANAGED_OBJECT_MANAGER_NAME));
//        metadata.addProperty("persistentId", createValue(context, persistentId));
//        String updateStrategy = element.getAttribute(UPDATE_STRATEGY_ATTRIBUTE);
//        if (updateStrategy != null) {
//            metadata.addProperty("updateStrategy", createValue(context, updateStrategy));
//        }
//        if (element.hasAttribute(UPDATE_METHOD_ATTRIBUTE)) {
//            metadata.addProperty("updateMethod", createValue(context, element.getAttribute(UPDATE_METHOD_ATTRIBUTE)));
//        } else if ("component-managed".equals(updateStrategy)) {
//            throw new ComponentDefinitionException(UPDATE_METHOD_ATTRIBUTE + " attribute must be set when " + UPDATE_STRATEGY_ATTRIBUTE + " is set to 'component-managed'");
//        }
//        metadata.addProperty("beanName", createIdRef(context, component.getId()));
//        context.getComponentDefinitionRegistry().registerComponentDefinition(metadata);
//        return component;
//    }

    /**
     * Create a reference to the ConfigurationAdmin service if not already done
     * and add it to the registry.
     *
     * @param context the parser context
     * @return a metadata pointing to the config admin
     */
    private Metadata createConfigAdminProxy(ParserContext context) {
        MutableBeanMetadata bean = context.createMetadata(MutableBeanMetadata.class);
        bean.setRuntimeClass(CmNamespaceHandler.class);
        bean.setFactoryMethod("getConfigAdmin");
        bean.setActivation(MutableBeanMetadata.ACTIVATION_LAZY);
        bean.setScope(MutableBeanMetadata.SCOPE_PROTOTYPE);
        return bean;
    }

//    private void registerManagedObjectManager(ParserContext context, ComponentDefinitionRegistry registry) {
//        if (registry.getComponentDefinition(MANAGED_OBJECT_MANAGER_NAME) == null) {
//            MutableBeanMetadata beanMetadata = context.createMetadata(MutableBeanMetadata.class);
//            beanMetadata.setScope(BeanMetadata.SCOPE_SINGLETON);
//            beanMetadata.setId(MANAGED_OBJECT_MANAGER_NAME);
//            beanMetadata.setRuntimeClass(ManagedObjectManager.class);
//            registry.registerComponentDefinition(beanMetadata);
//        }
//    }

    private static ValueMetadata createValue(ParserContext context, String value) {
        return createValue(context, value, null);
    }

    private static ValueMetadata createValue(ParserContext context, String value, String type) {
        MutableValueMetadata m = context.createMetadata(MutableValueMetadata.class);
        m.setStringValue(value);
        m.setType(type);
        return m;
    }

    private static RefMetadata createRef(ParserContext context, String value) {
        MutableRefMetadata m = context.createMetadata(MutableRefMetadata.class);
        m.setComponentId(value);
        return m;
    }
    private static ReferenceMetadata createReference(ParserContext context, String interfaceName) {
        MutableReferenceMetadata m = context.createMetadata(MutableReferenceMetadata.class);
        m.setInterface(interfaceName);
        return m;
    }

    private static IdRefMetadata createIdRef(ParserContext context, String value) {
        MutableIdRefMetadata m = context.createMetadata(MutableIdRefMetadata.class);
        m.setComponentId(value);
        return m;
    }

    private static CollectionMetadata createList(ParserContext context, List<String> list) {
        MutableCollectionMetadata m = context.createMetadata(MutableCollectionMetadata.class);
        m.setCollectionClass(List.class);
        m.setValueType(String.class.getName());
        for (String v : list) {
            m.addValue(createValue(context, v, String.class.getName()));
        }
        return m;
    }

    private static String getTextValue(Element element) {
        StringBuffer value = new StringBuffer();
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node item = nl.item(i);
            if ((item instanceof CharacterData && !(item instanceof Comment)) || item instanceof EntityReference) {
                value.append(item.getNodeValue());
            }
        }
        return value.toString();
    }

    private static boolean nodeNameEquals(Node node, String name) {
        return (name.equals(node.getNodeName()) || name.equals(node.getLocalName()));
    }

    public static boolean isBlueprintNamespace(String ns) {
        return BLUEPRINT_NAMESPACE.equals(ns);
    }

    public String getId(ParserContext context, Element element) {
        if (element.hasAttribute(ID_ATTRIBUTE)) {
            return element.getAttribute(ID_ATTRIBUTE);
        } else {
            return generateId(context);
        }
    }

    public void generateIdIfNeeded(ParserContext context, MutableComponentMetadata metadata) {
        if (metadata.getId() == null) {
            metadata.setId(generateId(context));
        }
    }

    private String generateId(ParserContext context) {
        String id;
        do {
            id = ".cm-" + ++idCounter;
        } while (context.getComponentDefinitionRegistry().containsComponentDefinition(id));
        return id;
    }

//    private Parser getParser(ParserContext ctx) {
//        if (ctx instanceof ParserContextImpl) {
//            return ((ParserContextImpl) ctx).getParser();
//        }
//        throw new RuntimeException("Unable to get parser");
//    }

    public List<String> parseInterfaceNames(Element element) {
        List<String> interfaceNames = new ArrayList<String>();
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                if (nodeNameEquals(e, VALUE_ELEMENT)) {
                    String v = getTextValue(e).trim();
                    if (interfaceNames.contains(v)) {
                        throw new ComponentDefinitionException("The element " + INTERFACES_ELEMENT + " should not contain the same interface twice");
                    }
                    interfaceNames.add(getTextValue(e));
                } else {
                    throw new ComponentDefinitionException("Unsupported element " + e.getNodeName() + " inside an " + INTERFACES_ELEMENT + " element");
                }
            }
        }
        return interfaceNames;
    }

}
