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
package org.gbean.spring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import org.gbean.metadata.ClassMetadata;
import org.gbean.metadata.ConstructorMetadata;
import org.gbean.metadata.MetadataProvider;
import org.gbean.metadata.ParameterMetadata;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * @version $Revision$ $Date$
 */
public class NamedConstructorArgs implements BeanFactoryPostProcessor {
    private final MetadataProvider metadataProvider;

    public NamedConstructorArgs(MetadataProvider metadataProvider) {
        this.metadataProvider = metadataProvider;
    }

    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        SpringVisitor visitor = new AbstractSpringVisitor() {
            public void visitBeanDefinition(BeanDefinition beanDefinition) throws BeansException {
                super.visitBeanDefinition(beanDefinition);

                if (!(beanDefinition instanceof RootBeanDefinition)) {
                    return;
                }

                RootBeanDefinition rootBeanDefinition = ((RootBeanDefinition) beanDefinition);
                parametersToConstructorArgs(rootBeanDefinition);

            }
        };
        visitor.visitBeanFactory(beanFactory);
    }

    private void parametersToConstructorArgs(RootBeanDefinition rootBeanDefinition) {
        ConstructorArgumentValues constructorArgumentValues = rootBeanDefinition.getConstructorArgumentValues();

        // if this bean already has constructor arguments defined, don't mess with them
        if (constructorArgumentValues.getArgumentCount() > 0) {
            return;
        }

        // try to get a list of constructor arg names to use
        ConstructorMetadata constructorMetadata = getConstructor(rootBeanDefinition);
        if (constructorMetadata == null) {
            return;
        }

        // remove each named property and add an indexed constructor arg
        MutablePropertyValues propertyValues = rootBeanDefinition.getPropertyValues();
        List parameters = constructorMetadata.getParameters();
        for (ListIterator iterator = parameters.listIterator(); iterator.hasNext();) {
            ParameterMetadata parameterMetadata = (ParameterMetadata) iterator.next();
            String name = (String) parameterMetadata.get("name");

            Class parameterType = parameterMetadata.getType();
            PropertyValue propertyValue = propertyValues.getPropertyValue(name);
            if (propertyValue != null) {
                propertyValues.removePropertyValue(name);
                constructorArgumentValues.addIndexedArgumentValue(iterator.previousIndex(), propertyValue.getValue(), parameterType.getName());
            } else {
                Object defaultValue = DEFAULT_VALUE.get(parameterType);
                constructorArgumentValues.addIndexedArgumentValue(iterator.previousIndex(), defaultValue, parameterType.getName());
            }
        }
    }

    private ConstructorMetadata getConstructor(RootBeanDefinition rootBeanDefinition) {
        Class beanType = rootBeanDefinition.getBeanClass();

        // try to get the class metadata
        ClassMetadata classMetadata = metadataProvider.getClassMetadata(beanType);

        // if we don't have metadata we can't do anything
        if (classMetadata == null) {
            return null;
        }

        // get a set containing the names of the defined properties
        Set propertyNames = new HashSet();
        PropertyValue[] values = rootBeanDefinition.getPropertyValues().getPropertyValues();
        for (int i = 0; i < values.length; i++) {
            propertyNames.add(values[i].getName());
        }

        // get the constructors sorted by longest arg length first
        List constructors = new ArrayList(classMetadata.getConstructors());
        Collections.sort(constructors, new ArgLengthComparator());

        // try to find a constructor for which we have all of the properties defined
        for (Iterator iterator = constructors.iterator(); iterator.hasNext();) {
            ConstructorMetadata constructorMetadata = (ConstructorMetadata) iterator.next();
            if (constructorMetadata.getProperties().containsKey("always-use")) {
                return constructorMetadata;
            }
            List constructorArgNames = getConstructorArgNames(constructorMetadata);
            if (constructorArgNames != null && propertyNames.containsAll(constructorArgNames)) {
                return constructorMetadata;
            }
        }
        return null;
    }

    private List getConstructorArgNames(ConstructorMetadata constructor) {
        List constructorArgNames = new LinkedList();

        List parameterMetadata = constructor.getParameters();
        for (Iterator iterator = parameterMetadata.iterator(); iterator.hasNext();) {
            ParameterMetadata parameter = (ParameterMetadata) iterator.next();
            String name = (String) parameter.get("name");
            if (name == null) {
                return null;
            }
            constructorArgNames.add(name);
        }
        return constructorArgNames;
    }

    private static class ArgLengthComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            ConstructorMetadata constructor1 = (ConstructorMetadata) o1;
            ConstructorMetadata constructor2 = (ConstructorMetadata) o2;
            return constructor2.getParameters().size() - constructor1.getParameters().size();
        }
    }

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
}
