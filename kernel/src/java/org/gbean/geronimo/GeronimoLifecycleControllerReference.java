/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
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

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.apache.geronimo.gbean.GBeanLifecycleController;
import org.gbean.service.ServiceContext;
import org.gbean.spring.ServiceContextThreadLocal;

/**
 * @version $Rev: 71492 $ $Date: 2004-11-14 21:31:50 -0800 (Sun, 14 Nov 2004) $
 */
public class GeronimoLifecycleControllerReference implements FactoryBean, Serializable {
    public static BeanDefinitionHolder createBeanDefinition() {
        RootBeanDefinition beanDefinition = new RootBeanDefinition(GeronimoLifecycleControllerReference.class, 0);
        return new BeanDefinitionHolder(beanDefinition, GeronimoLifecycleControllerReference.class.getName());
    }

    public final Class getObjectType() {
        return GBeanLifecycleController.class;
    }

    public synchronized final Object getObject() {
        ServiceContext serviceContext = ServiceContextThreadLocal.get();
        if (serviceContext == null) {
            throw new IllegalStateException("Service context has not been set");
        }
        return new GBeanLifecycleControllerImpl(serviceContext);
    }

    public boolean isSingleton() {
        return true;
    }

    private static class GBeanLifecycleControllerImpl implements GBeanLifecycleController {
        private final ServiceContext serviceContext;

        public GBeanLifecycleControllerImpl(ServiceContext serviceContext) {
            this.serviceContext = serviceContext;
        }

        public int getState() {
            return serviceContext.getState();
        }

        public void stop() throws Exception {
            serviceContext.stop();
        }
    }
}
