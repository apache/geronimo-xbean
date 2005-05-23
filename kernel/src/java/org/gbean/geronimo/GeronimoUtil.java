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

import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.HashSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;

import org.apache.geronimo.gbean.GBeanData;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GAttributeInfo;
import org.apache.geronimo.gbean.DynamicGAttributeInfo;
import org.apache.geronimo.gbean.GReferenceInfo;
import org.apache.geronimo.gbean.GConstructorInfo;
import org.apache.geronimo.gbean.InvalidConfigurationException;
import org.apache.geronimo.gbean.GBeanLifecycleController;
import org.apache.geronimo.gbean.GBeanLifecycle;
import org.apache.geronimo.kernel.Kernel;
import org.gbean.spring.NamedValueHolder;
import org.gbean.spring.ObjectNameStringReference;
import org.gbean.spring.ObjectNameReference;
import org.gbean.spring.ClassLoaderReference;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues;

/**
 * @version $Revision$ $Date$
 */
public class GeronimoUtil {
    private static final Map DEFAULT_VALUE;
    static {
        Map temp = new HashMap();
        temp.put("boolean", Boolean.FALSE);
        temp.put("byte", new Byte((byte) 0));
        temp.put("char", new Character((char) 0));
        temp.put("short", new Short((short) 0));
        temp.put("int", new Integer(0));
        temp.put("long", new Long(0));
        temp.put("float", new Float(0));
        temp.put("double", new Double(0));

        DEFAULT_VALUE = Collections.unmodifiableMap(temp);
    }

    /**
     * Converts dependecy to the following:
     * $name $pattern1 $pattern2 $patternN
     * Within each item a space character is excaped as '# ' and a '#' is escaped as '##'
     * This should result in almost no escaping since space and '#' are rarely used in
     * names or patterns.
     */
    public static String dependencyToString(String name, Set patterns) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(escape(name)).append(" ");
        for (Iterator iterator = patterns.iterator(); iterator.hasNext();) {
            ObjectName pattern = (ObjectName) iterator.next();
            buffer.append(escape(pattern.getCanonicalName()));
            if (iterator.hasNext()) {
                buffer.append(" ");
            }
        }
        return buffer.toString();
    }

    public static Map stringToDependency(String dependencyString) throws IllegalArgumentException {
        if (dependencyString == null || dependencyString.length() == 0) {
            throw new IllegalArgumentException("Dependency string is empty or null");
        }
        StringTokenizer tokenizer = new StringTokenizer(dependencyString, " ");
        if (!tokenizer.hasMoreTokens()) {
            throw new IllegalArgumentException("Dependency string does not contain a name");
        }
        String name = unescape(tokenizer.nextToken());
        Set patterns = new HashSet();
        while (tokenizer.hasMoreTokens()) {
            String patternString = unescape(tokenizer.nextToken());
            try {
                patterns.add(new ObjectName(patternString));
            } catch (MalformedObjectNameException e) {
                throw new IllegalArgumentException("Invalid pattern " + patternString);
            }
        }
        return Collections.singletonMap(name, patterns);
    }

    //todo change the replaceAll to cache the patterns directly
    private static String escape(String string) {
        string = string.replaceAll("#", "##");
        string = string.replaceAll(" ", "# ");
        return string;
    }

    private static String unescape(String string) {
        string = string.replaceAll("##", "#");
        string = string.replaceAll("# ", " ");
        return string;
    }

    /**
     * Converts the GBeanDefinition into a geronimo GBeanData
     *
     * @return the gbean data
     */
    public static GBeanData createGBeanData(GBeanDefinition gBeanDefinition) {
        GBeanData gbeanData = new GBeanData(gBeanDefinition.getObjectName(), createGBeanInfo(gBeanDefinition));
        gbeanData.setAttribute("gbeanEnabled", Boolean.valueOf(gBeanDefinition.isEnabled()));

        // add the normal properties
        PropertyValue[] properties = gBeanDefinition.getPropertyValues().getPropertyValues();
        for (int i = 0; i < properties.length; i++) {
            PropertyValue propertyValue = properties[i];
            gbeanData.setAttribute(propertyValue.getName(), propertyValue.getValue());
        }

        // add the dynamic properties
        PropertyValue[] dynamicProperties = gBeanDefinition.getDynamicPropertyValues().getPropertyValues();
        for (int i = 0; i < dynamicProperties.length; i++) {
            PropertyValue dynamicPropertyValue = dynamicProperties[i];
            gbeanData.setAttribute(dynamicPropertyValue.getName(), dynamicPropertyValue.getValue());
        }

        // add the constructor arguments
        for (Iterator iterator = gBeanDefinition.getConstructorArgumentValues().getIndexedArgumentValues().entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            ConstructorArgumentValues.ValueHolder valueHolder = (ConstructorArgumentValues.ValueHolder) entry.getValue();

            String constructorArgName;
            if (valueHolder instanceof NamedValueHolder) {
                NamedValueHolder namedValueHolder = (NamedValueHolder) valueHolder;
                constructorArgName = namedValueHolder.getName();
            } else {
                int index = ((Integer) entry.getKey()).intValue();
                constructorArgName = "constructor-argument-" + index;
            }
            gbeanData.setAttribute(constructorArgName, valueHolder.getValue());
        }

        // add the dependencies
        String[] dependsOn = gBeanDefinition.getDependsOn();
        for (int i = 0; i < dependsOn.length; i++) {
            String dependencyString = dependsOn[i];
            Map map = stringToDependency(dependencyString);
            Map.Entry entry = ((Map.Entry) map.entrySet().iterator().next());
            String dependencyName = (String) entry.getKey();
            Set patterns = (Set) entry.getValue();
            gbeanData.setReferencePatterns(dependencyName, patterns);
        }

        return gbeanData;
    }

    /**
     * Converts the GBeanDefinition into a geronimo GBeanInfo
     *
     * @return the GBeanInfo used to build this gbean
     */
    public static GBeanInfo createGBeanInfo(GBeanDefinition gBeanDefinition) {
        // add the normal properties
        Set attributeInfos = new HashSet();
        PropertyValue[] properties = gBeanDefinition.getPropertyValues().getPropertyValues();
        for (int i = 0; i < properties.length; i++) {
            PropertyValue propertyValue = properties[i];
            attributeInfos.add(new GAttributeInfo(propertyValue.getName(),
                    "java.lang.Object",
                    true,
                    null,
                    null));
        }

        // add the dynamic properties
        PropertyValue[] dynamicProperties = gBeanDefinition.getDynamicPropertyValues().getPropertyValues();
        for (int i = 0; i < dynamicProperties.length; i++) {
            PropertyValue dynamicPropertyValue = dynamicProperties[i];
            attributeInfos.add(new DynamicGAttributeInfo(dynamicPropertyValue.getName(),
                    "java.lang.Object",
                    true,
                    true,
                    true));
        }

        // add the constructor arguments
        int maxIndex = -1;
        for (Iterator iterator = gBeanDefinition.getConstructorArgumentValues().getIndexedArgumentValues().keySet().iterator(); iterator.hasNext();) {
            int index = ((Integer) iterator.next()).intValue();
            if (index > maxIndex) maxIndex = index;
        }
        String[] constructorArgs = new String[maxIndex + 1];
        for (Iterator iterator = gBeanDefinition.getConstructorArgumentValues().getIndexedArgumentValues().entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            int index = ((Integer) entry.getKey()).intValue();
            ConstructorArgumentValues.ValueHolder valueHolder = (ConstructorArgumentValues.ValueHolder) entry.getValue();

            String constructorArgName;
            if (valueHolder instanceof NamedValueHolder) {
                NamedValueHolder namedValueHolder = (NamedValueHolder) valueHolder;
                constructorArgName = namedValueHolder.getName();
            } else {
                constructorArgName = "constructor-argument-" + index;
            }
            attributeInfos.add(new GAttributeInfo(constructorArgName,
                    valueHolder.getType(),
                    true,
                    null,
                    null));

            constructorArgs[index] = constructorArgName;
        }

        // add the dependencies
        Set referenceInfos = new HashSet();
        String[] dependsOn = gBeanDefinition.getDependsOn();
        for (int i = 0; i < dependsOn.length; i++) {
            Map map = stringToDependency(dependsOn[i]);
            String dependencyName = (String) map.keySet().iterator().next();
            referenceInfos.add(new GReferenceInfo("@" + dependencyName,
                    DependencyOnly.class.getName(),
                    DependencyOnly.class.getName(),
                    null,
                    null));
        }

        return new GBeanInfo(gBeanDefinition.getBeanClassName(),
                "GBean",
                attributeInfos,
                new GConstructorInfo(constructorArgs),
                Collections.EMPTY_SET,
                referenceInfos);
    }

    public static GBeanDefinition createGeronimoBeanDefinition(GBeanData gbeanData, ClassLoader classLoader) throws InvalidConfigurationException {
        ObjectName objectName = gbeanData.getName();
        String beanClassName = gbeanData.getGBeanInfo().getClassName();

        // dependencies: map from dependency name to encoded dependency string
        Map dependencies = new HashMap();
        for (Iterator iterator = gbeanData.getGBeanInfo().getReferences().iterator(); iterator.hasNext();) {
            GReferenceInfo referenceInfo = (GReferenceInfo) iterator.next();
            String dependencyName = referenceInfo.getName();

            // get the patterns
            Set patterns = null;
            if (dependencyName.startsWith("@")) {
                dependencyName = dependencyName.substring(1);
                // the LocalConfig store sets a reference pattern on a gbean data which is supposed to
                // overwrite the existing value in the configuration object
                //
                // first we need to check if there is a patters with an upper case name, if so use that,
                // otherwise use the @name version
                patterns = gbeanData.getReferencePatterns(Character.toUpperCase(dependencyName.charAt(0)) + dependencyName.substring(1));
            }
            if (patterns == null) {
                // didn't find the pattern use the @name one
                patterns = gbeanData.getReferencePatterns(dependencyName);
            }

            if (patterns != null && !referenceInfo.getProxyType().equals(Collection.class.getName())) {
                // Remove all nulls from the patterns... there is bad code out there
                patterns = new HashSet(patterns);
                for (Iterator patternIterator = patterns.iterator(); patternIterator.hasNext();) {
                    Object pattern = patternIterator.next();
                    if (pattern == null) {
                        patternIterator.remove();
                    }
                }

                if (!patterns.isEmpty()) {
                    dependencyName = fixPropertyName(dependencyName);
                    String dependencyString = dependencyToString(dependencyName, patterns);
                    dependencies.put(dependencyName, dependencyString);
                }
            }
        }
        // add the missing dependencies... see LocalConfigStore
        for (Iterator iterator = gbeanData.getAttributes().entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String dependencyName = (String) entry.getKey();
            Object propertyValue = entry.getValue();
            if (!dependencies.containsKey(dependencyName) && propertyValue instanceof SingletonReference) {
                Set patterns = ((SingletonReference) propertyValue).getPatterns();
                String dependencyString = dependencyToString(dependencyName, patterns);
                dependencies.put(dependencyName, dependencyString);
            }
        }

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
            String propertyNames = fixPropertyName((String) iterator.next());
            if (lowerCasePropertyNameMap.containsKey(propertyNames.toLowerCase())) {
                propertyNames = (String) lowerCasePropertyNameMap.get(propertyNames.toLowerCase());
            }
            constructorArgs.add(propertyNames);
            lowerCasePropertyNameMap.put(propertyNames.toLowerCase(), propertyNames);
        }

        // determine the types of all properties... these needed for constructor args
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
        // all the references
        for (Iterator iterator = gbeanData.getGBeanInfo().getReferences().iterator(); iterator.hasNext();) {
            GReferenceInfo referenceInfo = (GReferenceInfo) iterator.next();
            String propertyName = fixPropertyName(referenceInfo.getName());
            if (!propertyName.startsWith("@")) {
                if (lowerCasePropertyNameMap.containsKey(propertyName.toLowerCase())) {
                    propertyName = (String) lowerCasePropertyNameMap.get(propertyName.toLowerCase());
                }
                persistentTypes.put(propertyName, referenceInfo.getProxyType());
            }
        }

        // determine the dynamic properties
        Set dynamicPropertyNames = new HashSet();
        for (Iterator iterator = gbeanData.getGBeanInfo().getAttributes().iterator(); iterator.hasNext();) {
            GAttributeInfo attributeInfo = (GAttributeInfo) iterator.next();
            if (attributeInfo instanceof DynamicGAttributeInfo) {
                dynamicPropertyNames.add(attributeInfo.getName());
            }
        }

        // initialize constructor args
        ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();
        for (ListIterator iterator = gbeanData.getGBeanInfo().getConstructor().getAttributeNames().listIterator(); iterator.hasNext();) {
            String propertyName = (String) iterator.next();
            String propertyType = (String) persistentTypes.get(propertyName);
            NamedValueHolder namedValueHolder = new NamedValueHolder(propertyName,
                    DEFAULT_VALUE.get(propertyType),
                    propertyType);
            constructorArgumentValues.getIndexedArgumentValues().put(new Integer(iterator.previousIndex()), namedValueHolder);
        }

        // values from the properties
        MutablePropertyValues propertyValues = new MutablePropertyValues();
        MutablePropertyValues dynamicPropertyValues = new MutablePropertyValues();
        for (Iterator iterator = gbeanData.getGBeanInfo().getAttributes().iterator(); iterator.hasNext();) {
            GAttributeInfo attributeInfo = (GAttributeInfo) iterator.next();
            String propertyName = attributeInfo.getName();
            if (propertyName.equals("gbeanEnabled")) {
                continue;
            }
            if (!dynamicPropertyNames.contains(propertyName)) {
                propertyName = fixPropertyName(propertyName);
                if (lowerCasePropertyNameMap.containsKey(propertyName.toLowerCase())) {
                    propertyName = (String) lowerCasePropertyNameMap.get(propertyName.toLowerCase());
                }
            }

            String propertyType = (String) persistentTypes.get(propertyName);
            Object propertyValue = gbeanData.getAttribute(attributeInfo.getName());

            // magic attributes
            if (attributeInfo.isWritable() || constructorArgs.contains(propertyName)) {
                if (propertyName.equals("objectName") && propertyType.equals(String.class.getName())) {
                    propertyValue = ObjectNameStringReference.createBeanDefinition();
                } else if (propertyName.equals("objectName") && propertyType.equals(ObjectName.class.getName())) {
                    propertyValue = ObjectNameReference.createBeanDefinition();
                } else if (propertyName.equals("classLoader") && propertyType.equals(ClassLoader.class.getName())) {
                    propertyValue = ClassLoaderReference.createBeanDefinition();
                } else if (propertyName.equals("gbeanLifecycleController") && propertyType.equals(GBeanLifecycleController.class.getName())) {
                    propertyValue = GeronimoLifecycleControllerReference.createBeanDefinition();
                } else if (propertyName.equals("kernel") && propertyType.equals(Kernel.class.getName())) {
                    propertyValue = GeronimoKernelReference.createBeanDefinition();
                }
            }

            if (propertyValue != null || gbeanData.getAttributes().containsKey(attributeInfo.getName())) {
                int index = constructorArgs.indexOf(propertyName);
                if (index >= 0) {
                    NamedValueHolder namedValueHolder = new NamedValueHolder(propertyName,
                            propertyValue,
                            propertyType);
                    constructorArgumentValues.getIndexedArgumentValues().put(new Integer(index), namedValueHolder);
                } else if (dynamicPropertyNames.contains(propertyName)) {
                    dynamicPropertyValues.addPropertyValue(propertyName, propertyValue);
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

            // get the patterns
            Set patterns = null;
            if (propertyName.startsWith("@")) {
                propertyName = propertyName.substring(1);
                // the LocalConfig store sets a reference pattern on a gbean data which is supposed to
                // overwrite the existing value in the configuration object
                //
                // we must only check if there is a patters with an upper case name, because an @name version
                // will already have it's property set in the attributes above
                patterns = gbeanData.getReferencePatterns(Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1));
            } else {
                patterns = gbeanData.getReferencePatterns(propertyName);
            }

            // now that we have the patterns adjust the name
            if (!dynamicPropertyNames.contains(propertyName)) {
                propertyName = fixPropertyName(propertyName);
                if (lowerCasePropertyNameMap.containsKey(propertyName.toLowerCase())) {
                    propertyName = (String) lowerCasePropertyNameMap.get(propertyName.toLowerCase());
                }
            }

            if (patterns != null && !patterns.isEmpty()) {
                // Remove all nulls from the patterns... there is bad code out there
                patterns = new HashSet(patterns);
                for (Iterator patternIterator = patterns.iterator(); patternIterator.hasNext();) {
                    Object pattern = patternIterator.next();
                    if (pattern == null) {
                        patternIterator.remove();
                    }
                }

                if (!patterns.isEmpty()) {
                    Object propertyValue;
                    if (referenceInfo.getProxyType().equals(Collection.class.getName())) {
                        propertyValue = CollectionReference.createBeanDefinition(propertyName, patterns, referenceInfo.getReferenceType());
                    } else {
                        propertyValue = SingletonReference.createBeanDefinition(propertyName, patterns, referenceInfo.getReferenceType());
                    }
                    int index = constructorArgs.indexOf(propertyName);
                    if (index >= 0) {
                        NamedValueHolder namedValueHolder = new NamedValueHolder(propertyName,
                                propertyValue,
                                (String) persistentTypes.get(propertyName));
                        constructorArgumentValues.getIndexedArgumentValues().put(new Integer(index), namedValueHolder);
                    } else if (dynamicPropertyNames.contains(propertyName)) {
                        dynamicPropertyValues.addPropertyValue(propertyName, propertyValue);
                    } else {
                        propertyValues.addPropertyValue(propertyName, propertyValue);
                    }
                }
            }
        }

        boolean enabled = true;
        if (gbeanData.getAttributes().containsKey("gbeanEnabled")) {
            enabled = ((Boolean) gbeanData.getAttribute("gbeanEnabled")).booleanValue();
        }

        String[] dependsOn = (String[]) dependencies.values().toArray(new String[dependencies.size()]);
        GBeanDefinition gBeanDefinition = new GBeanDefinition(objectName,
                beanClass,
                enabled,
                constructorArgumentValues,
                propertyValues,
                dynamicPropertyValues,
                dependsOn);

        if (GBeanLifecycle.class.isAssignableFrom(beanClass)) {
            gBeanDefinition.setInitMethodName("doStart");
            gBeanDefinition.setDestroyMethodName("doStop");
        }

        return gBeanDefinition;
    }

    private static String fixPropertyName(String propertyName) {
        if (Character.isUpperCase(propertyName.charAt(0))) {
            return Character.toLowerCase(propertyName.charAt(0)) + propertyName.substring(1);
        }
        return propertyName;
    }
}
