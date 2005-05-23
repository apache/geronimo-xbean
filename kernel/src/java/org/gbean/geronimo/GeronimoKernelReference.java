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

import java.io.Serializable;

import org.apache.geronimo.kernel.Kernel;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.gbean.service.ServiceContext;
import org.gbean.spring.ServiceContextThreadLocal;
import org.gbean.kernel.ServiceNotFoundException;

/**
 * @version $Rev: 71492 $ $Date: 2004-11-14 21:31:50 -0800 (Sun, 14 Nov 2004) $
 */
public class GeronimoKernelReference implements FactoryBean, Serializable {
    public static BeanDefinitionHolder createBeanDefinition() {
        RootBeanDefinition beanDefinition = new RootBeanDefinition(GeronimoKernelReference.class, 0);
        return new BeanDefinitionHolder(beanDefinition, GeronimoKernelReference.class.getName());
    }

    public final Class getObjectType() {
        return Kernel.class;
    }

    public synchronized final Object getObject() {
        ServiceContext serviceContext = ServiceContextThreadLocal.get();
        if (serviceContext == null) {
            throw new IllegalStateException("Service context has not been set");
        }
        try {
            return (Kernel) serviceContext.getKernel().getService(Kernel.KERNEL);
        } catch (ServiceNotFoundException e) {
            throw new IllegalStateException("A Geronimo kernel has not been loaded");
        }
    }

    public boolean isSingleton() {
        return true;
    }
}
