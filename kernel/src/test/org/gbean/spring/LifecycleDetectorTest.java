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

import junit.framework.TestCase;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * @version $Revision$ $Date$
 */
public class LifecycleDetectorTest extends TestCase {
    private DefaultListableBeanFactory factory;

    public void testLifecycleDetection() {
        String beanName = "hal";
        assertTrue(factory.containsBeanDefinition(beanName));
        RootBeanDefinition beanDefinition = (RootBeanDefinition) factory.getBeanDefinition(beanName);
        assertNotNull(beanDefinition);

        LifecycleDetector lifecycleDetector = new LifecycleDetector();
        lifecycleDetector.addLifecycleInterface(MyLifecycle.class, "begin", "end");
        lifecycleDetector.postProcessBeanFactory(factory);

        assertEquals("begin", beanDefinition.getInitMethodName());
        assertEquals("end", beanDefinition.getDestroyMethodName());
    }

    public void testObjectNameCreation() {
        String beanName = "foobar";
        assertTrue(factory.containsBeanDefinition(beanName));
        RootBeanDefinition beanDefinition = (RootBeanDefinition) factory.getBeanDefinition(beanName);
        assertNotNull(beanDefinition);

        LifecycleDetector lifecycleDetector = new LifecycleDetector();
        lifecycleDetector.addLifecycleInterface(MyLifecycle.class, "begin", "end");
        lifecycleDetector.postProcessBeanFactory(factory);

        assertEquals("foo", beanDefinition.getInitMethodName());
        assertEquals("bar", beanDefinition.getDestroyMethodName());
    }

    protected void setUp() throws Exception {
        factory = new DefaultListableBeanFactory();
        Resource resource = new FileSystemResource(new File("src/test-data/PostProcessorTest.xml"));
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
        reader.loadBeanDefinitions(resource);
    }
}
