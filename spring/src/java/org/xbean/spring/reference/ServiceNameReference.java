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

package org.xbean.spring.reference;

import java.io.Serializable;

import org.gbean.kernel.ServiceContext;
import org.gbean.kernel.ServiceContextThreadLocal;
import org.gbean.kernel.ServiceName;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * ServiceNameReference is a the ServiceName in the ServiceContextThreadLocal.
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class ServiceNameReference implements FactoryBean, Serializable {
    /**
     * Creates a bean definition for ServiceNameReference.
     * @return a bean definition for ServiceNameReference
     */
    public static BeanDefinitionHolder createBeanDefinition() {
        RootBeanDefinition beanDefinition = new RootBeanDefinition(ServiceNameReference.class, 0);
        return new BeanDefinitionHolder(beanDefinition, ServiceNameReference.class.getName());
    }

    public final Class getObjectType() {
        return ServiceName.class;
    }

    public synchronized final Object getObject() {
        ServiceContext serviceContext = ServiceContextThreadLocal.get();
        if (serviceContext == null) {
            throw new IllegalStateException("Service context has not been set");
        }
        return serviceContext.getServiceName();
    }

    public boolean isSingleton() {
        return true;
    }
}
