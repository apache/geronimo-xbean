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

import java.io.File;
import java.util.Properties;

import junit.framework.TestCase;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.gbean.metadata.simple.PropertiesMetadataProvider;
import org.gbean.metadata.ClassMetadata;
import org.gbean.metadata.ConstructorMetadata;
import org.gbean.kernel.ConstructorSignature;

/**
 * @version $Revision$ $Date$
 */
public class NamedConstructorArgsTest extends TestCase {
    private DefaultListableBeanFactory factory;

    public void testSimplePropertiesConversion() {
        // pre conditions
        String beanName = "hal";
        assertTrue(factory.containsBeanDefinition(beanName));
        RootBeanDefinition beanDefinition = (RootBeanDefinition) factory.getBeanDefinition(beanName);
        assertNotNull(beanDefinition);

        ConstructorArgumentValues constructorArgumentValues = beanDefinition.getConstructorArgumentValues();
        assertEquals(0, constructorArgumentValues.getArgumentCount());

        MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();
        assertTrue(propertyValues.contains("prefix"));
        assertTrue(propertyValues.contains("suffix"));

        // process factory
        PropertiesMetadataProvider metadataProvider = new PropertiesMetadataProvider();
        NamedConstructorArgs namedConstructorArgs = new NamedConstructorArgs(metadataProvider);
        namedConstructorArgs.postProcessBeanFactory(factory);

        // post conditions
        beanDefinition = (RootBeanDefinition) factory.getBeanDefinition(beanName);
        assertNotNull(beanDefinition);

        constructorArgumentValues = beanDefinition.getConstructorArgumentValues();
        assertEquals(2, constructorArgumentValues.getArgumentCount());
        assertTrue(constructorArgumentValues.getGenericArgumentValues().isEmpty());
        ConstructorArgumentValues.ValueHolder arg = constructorArgumentValues.getIndexedArgumentValue(0, String.class);
        assertNotNull(arg);
        assertEquals("I'm sorry ", arg.getValue());
        arg = constructorArgumentValues.getIndexedArgumentValue(1, String.class);
        assertNotNull(arg);
        assertEquals("; I'm afraid I can't do that.", arg.getValue());

        propertyValues = beanDefinition.getPropertyValues();
        assertFalse(propertyValues.contains("prefix"));
        assertFalse(propertyValues.contains("suffix"));
    }

    public void testForcedConstructor() {
        // pre conditions
        String beanName = "hal";
        assertTrue(factory.containsBeanDefinition(beanName));
        RootBeanDefinition beanDefinition = (RootBeanDefinition) factory.getBeanDefinition(beanName);
        assertNotNull(beanDefinition);

        ConstructorArgumentValues constructorArgumentValues = beanDefinition.getConstructorArgumentValues();
        assertEquals(0, constructorArgumentValues.getArgumentCount());

        MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();
        assertTrue(propertyValues.contains("prefix"));
        assertTrue(propertyValues.contains("suffix"));

        // process factory
        PropertiesMetadataProvider metadataProvider = new PropertiesMetadataProvider() {
            public ClassMetadata getClassMetadata(Class type) {
                ClassMetadata classMetadata = super.getClassMetadata(type);
                if (type.equals(HelloMessage.class)) {
                    ConstructorMetadata constructor = classMetadata.getConstructor(
                            new ConstructorSignature(new String[] {"java.lang.String", "java.lang.String", "java.util.Properties"}));
                    constructor.put("always-use", null);
                }
                return classMetadata;
            }
        };

        NamedConstructorArgs namedConstructorArgs = new NamedConstructorArgs(metadataProvider);
        namedConstructorArgs.postProcessBeanFactory(factory);

        // post conditions
        beanDefinition = (RootBeanDefinition) factory.getBeanDefinition(beanName);
        assertNotNull(beanDefinition);

        constructorArgumentValues = beanDefinition.getConstructorArgumentValues();
        assertEquals(3, constructorArgumentValues.getArgumentCount());
        assertTrue(constructorArgumentValues.getGenericArgumentValues().isEmpty());
        ConstructorArgumentValues.ValueHolder arg = constructorArgumentValues.getIndexedArgumentValue(0, String.class);
        assertNotNull(arg);
        assertEquals("I'm sorry ", arg.getValue());
        arg = constructorArgumentValues.getIndexedArgumentValue(1, String.class);
        assertNotNull(arg);
        assertEquals("; I'm afraid I can't do that.", arg.getValue());
        arg = constructorArgumentValues.getIndexedArgumentValue(2, Properties.class);
        assertNotNull(arg);
        assertNull(arg.getValue());

        propertyValues = beanDefinition.getPropertyValues();
        assertFalse(propertyValues.contains("prefix"));
        assertFalse(propertyValues.contains("suffix"));
    }

    protected void setUp() throws Exception {
        factory = new DefaultListableBeanFactory();
        Resource resource = new FileSystemResource(new File("src/test-data/NamedConstructorArgsTest.xml"));
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
        reader.loadBeanDefinitions(resource);
    }
}
