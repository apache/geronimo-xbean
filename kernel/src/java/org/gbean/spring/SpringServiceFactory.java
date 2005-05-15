/**
 *
 * Copyright 2005 GBean.org
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.gbean.service.ConfigurableServiceFactory;
import org.gbean.service.ServiceContext;
import org.gbean.geronimo.GeronimoUtil;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;

/**
 * @version $Revision$ $Date$
 */
public class SpringServiceFactory implements ConfigurableServiceFactory {
    private final Map dependencies = new HashMap();
    private RootBeanDefinition beanDefinition;
    private GenericApplicationContext applicationContext;
    private boolean enabled = true;
    private static final String ROOT_BEAN_DEFINITION = "RootBeanDefinition";

    public SpringServiceFactory(RootBeanDefinition beanDefinition) {
        // add the dependencies
        String[] dependsOn = beanDefinition.getDependsOn();
        for (int i = 0; i < dependsOn.length; i++) {
            String dependencyString = dependsOn[i];
            Map map = GeronimoUtil.stringToDependency(dependencyString);
            Map.Entry entry = ((Map.Entry) map.entrySet().iterator().next());
            String dependencyName = (String) entry.getKey();
            Set patterns = (Set) entry.getValue();
            dependencies.put(dependencyName, patterns);
        }
    }

    public RootBeanDefinition getBeanDefinition() {
        return beanDefinition;
    }

    public void setBeanDefinition(RootBeanDefinition beanDefinition) {
        this.beanDefinition = beanDefinition;
    }

    public Map getDependencies() {
        return dependencies;
    }

    public Object createService(ServiceContext serviceContext) throws Exception {
        Object service = null;
        try {
            ServiceContext oldServiceContext = ServiceContextThreadLocal.get();
            try {
                ServiceContextThreadLocal.set(serviceContext);

                applicationContext = new GenericApplicationContext();
                RootBeanDefinition beanDefinition = new RootBeanDefinition(this.beanDefinition);
                // todo remove the depends on usage stuff
                // clear the depends on flag since it is used to signal geronimo dependencies and not spring dependencies
                beanDefinition.setDependsOn(new String[0]);
                applicationContext.registerBeanDefinition(serviceContext.getObjectName(), beanDefinition);
                service = applicationContext.getBean(serviceContext.getObjectName());

            } finally {
                ServiceContextThreadLocal.set(oldServiceContext);
            }

        } catch (Throwable t) {
            t.printStackTrace();

            applicationContext.close();
            applicationContext.destroy();
            applicationContext = null;

            if (t instanceof Exception) {
                throw (Exception) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            } else {
                throw new Error(t);
            }
        }

        return service;
    }

    public void destroyService(ServiceContext serviceContext, Object service) {
        applicationContext.close();
        applicationContext.destroy();
        applicationContext = null;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Set getPropertyNames() {
        return Collections.singleton(ROOT_BEAN_DEFINITION);
    }

    public Object getProperty(String propertyName) {
        if (ROOT_BEAN_DEFINITION.equals(propertyName)) {
            return beanDefinition;
        }

        throw new IllegalArgumentException("Unknown property: " + propertyName);
    }

    public void setProperty(String propertyName, Object persistentValue) {
        if (ROOT_BEAN_DEFINITION.equals(propertyName)) {
            beanDefinition = (RootBeanDefinition) persistentValue;
        }

        throw new IllegalArgumentException("Unknown property: " + propertyName);
    }
}
