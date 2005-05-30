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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.management.ObjectName;

import org.gbean.kernel.ServiceName;
import org.gbean.metadata.ClassMetadata;
import org.gbean.metadata.MetadataManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * @version $Revision$ $Date$
 */
public class ObjectNameBuilder implements BeanFactoryPostProcessor {
    private static final String OBJECT_NAME_PROPERTY = "gbean-objectName";
    private final MetadataManager metadataManager;
    private final String domainName;
    private final String serverName;
    private final String moduleName;
    private final Map objectNameMap = new HashMap();

    public ObjectNameBuilder(MetadataManager metadataManager, String domainName, String serverName, String moduleName) {
        this.metadataManager = metadataManager;
        this.domainName = domainName;
        this.serverName = serverName;
        this.moduleName = moduleName;
    }

    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        String[] beanNames = beanFactory.getBeanDefinitionNames();
        if (beanNames == null) {
            return;
        }
        for (int i = 0; i < beanNames.length; i++) {
            String beanName = beanNames[i];
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            if (beanDefinition instanceof RootBeanDefinition) {
                ObjectName objectName = buildObjectName(beanName, (RootBeanDefinition) beanDefinition);
                objectNameMap.put(beanName, objectName);
            }
        }
    }

    private ObjectName buildObjectName(String beanName, RootBeanDefinition beanDefinition) {
        MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();
        if (propertyValues.contains(OBJECT_NAME_PROPERTY)) {
            String objectNameString = (String) propertyValues.getPropertyValue(OBJECT_NAME_PROPERTY).getValue();
            propertyValues.removePropertyValue(OBJECT_NAME_PROPERTY);
            return ServiceName.createName(objectNameString);
        } else {
            ClassMetadata classMetadata = metadataManager.getClassMetadata(beanDefinition.getBeanClass());

            String type = (String) classMetadata.get("j2eeType");
            return createObjectName(beanName, type);
        }
    }

    public Map getObjectNameMap() {
        return objectNameMap;
    }

    public ObjectName getObjectName(String beanName) {
        return (ObjectName) objectNameMap.get(beanName);
    }

    private ObjectName createObjectName(String name, String type) {
        Properties props = new Properties();
        if (type != null) {
            props.put("j2eeType", type);
        }
        props.put("name", name);
        props.put("J2EEApplication", "null");
        props.put("J2EEModule", moduleName);
        props.put("J2EEServer", serverName);

        return ServiceName.createName(domainName, props);
    }

}
