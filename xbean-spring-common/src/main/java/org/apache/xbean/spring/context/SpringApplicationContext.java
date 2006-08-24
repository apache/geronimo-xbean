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
package org.apache.xbean.spring.context;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;

import java.util.List;

/**
 * SpringApplicationContext is an interface that defines the actual interface exposed by the application contexts
 * provided by Spring.  This interface should be in Spring and the Spring application contexts should implement this
 * interface.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public interface SpringApplicationContext extends ConfigurableApplicationContext, DisposableBean, ResourceLoader{
    /**
     * Set a friendly name for this context.
     * Typically done during initialization of concrete context implementations.
     * @param displayName the display name for the context
     */
    void setDisplayName(String displayName);

    /**
     * Gets the list of BeanPostProcessors that will get applied
     * to beans created with this factory.
     * @return the list of BeanPostProcessors that will get applied
     * to beans created with this factory
     */
    List getBeanFactoryPostProcessors();

    /**
     * Specify the ClassLoader to load class path resources with,
     * or <code>null</code> if using the thread context class loader on actual access
     * (applying to the thread that does ClassPathResource calls).
     * <p>The default is that ClassLoader access will happen via the thread
     * context class loader on actual access (applying to the thread that
     * does ClassPathResource calls).
     * @param classLoader the ClassLoader to load class path resources
     */
    void setClassLoader(ClassLoader classLoader);

    /**
     * Return the ClassLoader to load class path resources with,
     * or <code>null</code> if using the thread context class loader on actual access
     * (applying to the thread that does ClassPathResource calls).
     * <p>Will get passed to ClassPathResource's constructor for all
     * ClassPathResource objects created by this resource loader.
     *
     * @return the ClassLoader to load class path resources
     * @see ClassPathResource
     */
    ClassLoader getClassLoader();
}
