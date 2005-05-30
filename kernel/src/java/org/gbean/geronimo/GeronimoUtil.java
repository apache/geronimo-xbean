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
package org.gbean.geronimo;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.ObjectName;

import org.apache.geronimo.gbean.DynamicGAttributeInfo;
import org.apache.geronimo.gbean.GAttributeInfo;
import org.apache.geronimo.gbean.GBeanData;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GConstructorInfo;
import org.apache.geronimo.gbean.GReferenceInfo;
import org.apache.geronimo.gbean.InvalidConfigurationException;
import org.gbean.metadata.MetadataManager;
import org.gbean.proxy.ProxyManager;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * @version $Revision$ $Date$
 */
public class GeronimoUtil {
    /**
     * Converts the GeronimoServiceFactory into a geronimo GBeanData
     *
     * @return the gbean data
     */
    public static GBeanData createGBeanData(ObjectName objectName, GeronimoServiceFactory serviceFactory) {
        RootBeanDefinition beanDefinition = serviceFactory.getBeanDefinition();
        Set persistentProperties = serviceFactory.getPersistentProperties();
        GBeanData gbeanData = new GBeanData(objectName, createGBeanInfo(serviceFactory));
        gbeanData.setAttribute("gbeanEnabled", Boolean.valueOf(serviceFactory.isEnabled()));

        // add the normal properties
        PropertyValue[] properties = beanDefinition.getPropertyValues().getPropertyValues();
        for (int i = 0; i < properties.length; i++) {
            PropertyValue propertyValue = properties[i];
            if (persistentProperties.contains(propertyValue.getName())) {
                gbeanData.setAttribute(propertyValue.getName(), propertyValue.getValue());
            }
        }

        // add the dynamic properties
        for (Iterator iterator = serviceFactory.getDynamicProperties().entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String propertyName = (String) entry.getKey();
            Object propertyValue = entry.getValue();
            gbeanData.setAttribute(propertyName, propertyValue);
        }

        return gbeanData;
    }

    /**
     * Converts the GeronimoServiceFactory into a geronimo GBeanInfo
     *
     * @return the GBeanInfo for the service
     */
    public static GBeanInfo createGBeanInfo(GeronimoServiceFactory serviceFactory) {
        RootBeanDefinition beanDefinition = serviceFactory.getBeanDefinition();
        Set persistentProperties = serviceFactory.getPersistentProperties();

        // add the normal properties
        Set attributeInfos = new HashSet();
        PropertyValue[] properties = beanDefinition.getPropertyValues().getPropertyValues();
        for (int i = 0; i < properties.length; i++) {
            PropertyValue propertyValue = properties[i];
            String propertyName = propertyValue.getName();
            attributeInfos.add(new GAttributeInfo(propertyName,
                    "java.lang.Object",
                    persistentProperties.contains(propertyName),
                    null,
                    null));
        }

        // add the dynamic properties
        for (Iterator iterator = serviceFactory.getDynamicProperties().keySet().iterator(); iterator.hasNext();) {
            String propertyName = (String) iterator.next();
            attributeInfos.add(new DynamicGAttributeInfo(propertyName,
                    "java.lang.Object",
                    true,
                    true,
                    true));
        }

        return new GBeanInfo(beanDefinition.getBeanClassName(),
                "GBean",
                attributeInfos,
                new GConstructorInfo(serviceFactory.getConstructorArgNames()),
                Collections.EMPTY_SET,
                null);
    }

    public static GeronimoServiceFactory createGeronimoServiceFactory(GBeanData gbeanData, ClassLoader classLoader, MetadataManager metadataManager, ProxyManager proxyManager) throws Exception {
        ObjectName objectName = gbeanData.getName();
        String beanClassName = gbeanData.getGBeanInfo().getClassName();

        // get a list of all methods in the target class
        Class beanClass = null;
        try {
            beanClass = classLoader.loadClass(beanClassName);
        } catch (ClassNotFoundException e) {
            throw new InvalidConfigurationException("Could not load class for GBeanInstance: objectName=" + objectName + " className=" + beanClassName);
        }
        Method[] methods = beanClass.getMethods();

        // build a map of the property names in lower case to the real property name
        // this fixes problem where geronimo property names were loosly matched
        Map lowerCasePropertyNameMap = new HashMap(methods.length);
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            String methodName = method.getName();
            if (Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())) {
                if (method.getParameterTypes().length == 0 && method.getReturnType() != Void.TYPE) {
                    if (methodName.length() > 3 && methodName.startsWith("get") && !methodName.equals("getClass")) {
                        String propertyName = fixPropertyName(methodName.substring(3));
                        lowerCasePropertyNameMap.put(propertyName.toLowerCase(), propertyName);
                    } else if (methodName.length() > 2 && methodName.startsWith("is")) {
                        String propertyName = fixPropertyName(methodName.substring(2));
                        lowerCasePropertyNameMap.put(propertyName.toLowerCase(), propertyName);
                    }
                }
                if (method.getParameterTypes().length == 1 &&
                        method.getReturnType() == Void.TYPE &&
                        methodName.length() > 3 &&
                        methodName.startsWith("set")) {
                    String propertyName = fixPropertyName(methodName.substring(3));
                    lowerCasePropertyNameMap.put(propertyName.toLowerCase(), propertyName);
                }
            }
        }

        // build a list of constructor args... adjust any incoming names to match the property name as determined by the getters and setters
        List constructorArgs = new ArrayList();
        for (Iterator iterator = gbeanData.getGBeanInfo().getConstructor().getAttributeNames().iterator(); iterator.hasNext();) {
            // for constructor args we only use simple fixPropertyName method
            String argName = fixPropertyName((String) iterator.next());
            constructorArgs.add(argName);
            lowerCasePropertyNameMap.put(argName.toLowerCase(), argName);
        }

        // determine the types of all properties... these are needed for constructor args
        Map persistentTypes = new HashMap();
        for (Iterator iterator = gbeanData.getGBeanInfo().getAttributes().iterator(); iterator.hasNext();) {
            GAttributeInfo attributeInfo = (GAttributeInfo) iterator.next();
            String propertyName = attributeInfo.getName();
            if (!(attributeInfo instanceof DynamicGAttributeInfo)) {
                propertyName = fixPropertyName(attributeInfo.getName());
                if (lowerCasePropertyNameMap.containsKey(propertyName.toLowerCase())) {
                    propertyName = (String) lowerCasePropertyNameMap.get(propertyName.toLowerCase());
                }
            }
            persistentTypes.put(propertyName, attributeInfo.getType());
        }
        for (Iterator iterator = gbeanData.getGBeanInfo().getReferences().iterator(); iterator.hasNext();) {
            GReferenceInfo referenceInfo = (GReferenceInfo) iterator.next();
            String propertyName = fixPropertyName(referenceInfo.getName());
            if (lowerCasePropertyNameMap.containsKey(propertyName.toLowerCase())) {
                propertyName = (String) lowerCasePropertyNameMap.get(propertyName.toLowerCase());
            }
            persistentTypes.put(propertyName, referenceInfo.getProxyType());
        }

        // determine which properties are dynamic
        Map dynamicProperties = new HashMap();
        for (Iterator iterator = gbeanData.getGBeanInfo().getAttributes().iterator(); iterator.hasNext();) {
            GAttributeInfo attributeInfo = (GAttributeInfo) iterator.next();
            if (attributeInfo instanceof DynamicGAttributeInfo) {
                dynamicProperties.put(attributeInfo.getName(), null);
            }
        }

        // values from the properties
        MutablePropertyValues propertyValues = new MutablePropertyValues();
        for (Iterator iterator = gbeanData.getGBeanInfo().getAttributes().iterator(); iterator.hasNext();) {
            GAttributeInfo attributeInfo = (GAttributeInfo) iterator.next();
            String propertyName = attributeInfo.getName();

            // skip the gbeanEnabled property... it is handled below
            if (propertyName.equals("gbeanEnabled")) {
                continue;
            }

            // fix any non-dynamic property name so that it matches spring's rules
            if (!dynamicProperties.containsKey(propertyName)) {
                propertyName = fixPropertyName(propertyName);
                if (lowerCasePropertyNameMap.containsKey(propertyName.toLowerCase())) {
                    propertyName = (String) lowerCasePropertyNameMap.get(propertyName.toLowerCase());
                }
            }

//            String propertyType = (String) persistentTypes.get(propertyName);
            Object propertyValue = gbeanData.getAttribute(attributeInfo.getName());

            // magic attributes
//            if (attributeInfo.isWritable() || constructorArgs.contains(propertyName)) {
//                if (propertyName.equals("objectName") && propertyType.equals(String.class.getName())) {
//                    propertyValue = new ObjectNameStringReference();
//                } else if (propertyName.equals("objectName") && propertyType.equals(ObjectName.class.getName())) {
//                    propertyValue = new ObjectNameReference();
//                } else if (propertyName.equals("classLoader") && propertyType.equals(ClassLoader.class.getName())) {
//                    propertyValue = new ClassLoaderReference();
//                } else if (propertyName.equals("gbeanLifecycleController") && propertyType.equals(GBeanLifecycleController.class.getName())) {
//                    propertyValue = new GeronimoLifecycleControllerReference();
//                } else if (propertyName.equals("kernel") && propertyType.equals(Kernel.class.getName())) {
//                    propertyValue = new GeronimoKernelReference();
//                }
//            }

            // set the property... we only set properties that have a defined value
            if (propertyValue != null || gbeanData.getAttributes().containsKey(attributeInfo.getName())) {
                if (dynamicProperties.containsKey(propertyName)) {
                    dynamicProperties.put(propertyName, propertyValue);
                } else {
                    propertyValues.addPropertyValue(propertyName, propertyValue);
                }
            }
        }

        // values from the references
        //
        // NOTE: we need to add the reference values after the attribue values so that newly added
        // reference patterns will overwrite references stored in properties in a previous run
        for (Iterator iterator = gbeanData.getGBeanInfo().getReferences().iterator(); iterator.hasNext();) {
            GReferenceInfo referenceInfo = (GReferenceInfo) iterator.next();
            String propertyName = referenceInfo.getName();

            // get the patterns before we mess with the proptery name
            Set patterns = gbeanData.getReferencePatterns(propertyName);

            // fix any non-dynamic property name so that it matches spring's rules
            if (!dynamicProperties.containsKey(propertyName)) {
                propertyName = fixPropertyName(propertyName);
                if (lowerCasePropertyNameMap.containsKey(propertyName.toLowerCase())) {
                    propertyName = (String) lowerCasePropertyNameMap.get(propertyName.toLowerCase());
                }
            }

            // get the patterns
            if (patterns != null && !patterns.isEmpty()) {
                // Remove all nulls from the patterns... there is bad code out there
                patterns = new HashSet(patterns);
                for (Iterator patternIterator = patterns.iterator(); patternIterator.hasNext();) {
                    Object pattern = patternIterator.next();
                    if (pattern == null) {
                        patternIterator.remove();
                    }
                }
            }

            // set the property... we only set properties that have a defined value
            if (patterns != null && !patterns.isEmpty()) {
                Object propertyValue;
                if (referenceInfo.getProxyType().equals(Collection.class.getName())) {
                    propertyValue = new CollectionReference(propertyName, patterns, referenceInfo.getReferenceType());
                } else {
                    propertyValue = new SingletonReference(propertyName, patterns, referenceInfo.getReferenceType());
                }
                if (dynamicProperties.containsKey(propertyName)) {
                    dynamicProperties.put(propertyName, propertyValue);
                } else {
                    propertyValues.addPropertyValue(propertyName, propertyValue);
                }
            }
        }

        RootBeanDefinition beanDefinition = new RootBeanDefinition(beanClass, propertyValues);
        GeronimoServiceFactory geronimoServiceFactory = new GeronimoServiceFactory(beanDefinition, dynamicProperties, metadataManager, proxyManager);

        boolean enabled = true;
        if (gbeanData.getAttributes().containsKey("gbeanEnabled")) {
            enabled = ((Boolean) gbeanData.getAttribute("gbeanEnabled")).booleanValue();
        }
        geronimoServiceFactory.setEnabled(enabled);

        return geronimoServiceFactory;
    }

    private static String fixPropertyName(String propertyName) {
        if (Character.isUpperCase(propertyName.charAt(0))) {
            return Character.toLowerCase(propertyName.charAt(0)) + propertyName.substring(1);
        }
        return propertyName;
    }
}
