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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.geronimo.gbean.GAttributeInfo;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GReferenceInfo;
import org.gbean.kernel.ClassLoading;
import org.gbean.kernel.ConstructorSignature;
import org.gbean.metadata.ClassMetadata;
import org.gbean.metadata.ConstructorMetadata;
import org.gbean.metadata.MetadataProvider;
import org.gbean.metadata.ParameterMetadata;

/**
 * @version $Revision$ $Date$
 */
public class GeronimoMetadataProvider implements MetadataProvider {
    public void addClassMetadata(ClassMetadata classMetadata) {
        Class type = classMetadata.getType();
        GBeanInfo gbeanInfo = getGBeanInfo(type);
        if (gbeanInfo == null) {
            return;
        }

        classMetadata.put("j2eeType", gbeanInfo.getJ2eeType());

        Set attributes = gbeanInfo.getAttributes();
        Set references = gbeanInfo.getReferences();
        Map attributeTypes = new HashMap(attributes.size() + references.size());
        Set persistentProperties = new HashSet();
        for (Iterator iterator = attributes.iterator(); iterator.hasNext();) {
            GAttributeInfo attributeInfo = (GAttributeInfo) iterator.next();
            Class attributeType = null;
            try {
                attributeType = ClassLoading.loadClass(attributeInfo.getType(), type.getClassLoader());
            } catch (ClassNotFoundException ignored) {
                // couldn't load the type.. just proceed and we'll skip the constructor declaration
            }
            attributeTypes.put(attributeInfo.getName(), attributeType);

            if (attributeInfo.isPersistent()) {
                persistentProperties.add(fixPropertyName(attributeInfo.getName()));
            }
        }

        for (Iterator iterator = references.iterator(); iterator.hasNext();) {
            GReferenceInfo referenceInfo = (GReferenceInfo) iterator.next();
            Class attributeType = null;
            try {
                attributeType = ClassLoading.loadClass(referenceInfo.getProxyType(), type.getClassLoader());
            } catch (ClassNotFoundException ignored) {
                // couldn't load the type.. just proceed and we'll skip the constructor declaration
            }
            attributeTypes.put(referenceInfo.getName(), attributeType);
            persistentProperties.add(fixPropertyName(referenceInfo.getName()));
        }


        List constructorArgNames = gbeanInfo.getConstructor().getAttributeNames();
        List constructorArgTypes = new ArrayList(constructorArgNames.size());
        for (Iterator iterator = constructorArgNames.iterator(); iterator.hasNext();) {
            String constructorArgName = (String) iterator.next();
            constructorArgTypes.add(attributeTypes.get(constructorArgName));
        }

        if (!constructorArgTypes.contains(null)) {
            ConstructorMetadata constructor = classMetadata.getConstructor(new ConstructorSignature(constructorArgTypes));
            if (constructor != null) {
                constructor.put("always-use", "true");
                for (ListIterator iterator = constructor.getParameters().listIterator(); iterator.hasNext();) {
                    ParameterMetadata parameter = (ParameterMetadata) iterator.next();
                    String name = (String) constructorArgNames.get(iterator.previousIndex());
                    parameter.put("name", fixPropertyName(name));
                }
            }
        }

        if (!persistentProperties.isEmpty()) {
            classMetadata.put("persistentProperties", persistentProperties);
        }
    }

    private static GBeanInfo getGBeanInfo(Class type) {
        try {
            Method method = type.getDeclaredMethod("getGBeanInfo", new Class[]{});
            return (GBeanInfo) method.invoke(type, new Object[]{});
        } catch (Exception ignored) {
        }

        try {
            Class gbeanClass = ClassLoading.loadClass(type.getName() + "GBean", type.getClassLoader());
            Method method = gbeanClass.getDeclaredMethod("getGBeanInfo", new Class[]{});
            return (GBeanInfo) method.invoke(gbeanClass, new Object[]{});
        } catch (Exception ignored) {
        }

        return null;
    }

    private static String fixPropertyName(String propertyName) {
        if (Character.isUpperCase(propertyName.charAt(0))) {
            return Character.toLowerCase(propertyName.charAt(0)) + propertyName.substring(1);
        }
        return propertyName;
    }
}
