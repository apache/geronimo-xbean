/**
 * 
 * Copyright 2005 LogicBlaze, Inc. http://www.logicblaze.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 * 
 **/
package org.xbean.sca.impl;

import org.osoa.sca.ModuleContext;
import org.osoa.sca.annotations.Context;
import org.osoa.sca.model.Component;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.xbean.sca.ScaAdapter;

/**
 * A simple implementation of {@link ScaAdapter}
 * 
 * @version $Revision$
 */
public class DefaultScaAdapter implements ScaAdapter {
    private ModuleContext moduleContext;

    public Component getComponentMetaData(Object bean, String beanName) {
        throw new RuntimeException("TODO: Not Implemented yet");
    }

    public ModuleContext getComponentContext(Object bean, String beanName) {
        ModuleContext answer = getModuleContext();
        if (answer == null) {
            throw new BeanInitializationException("No moduleContext property is configured so cannot inject its value into bean: " + beanName);
        }
        return answer;

    }

    public Object getBeanSessionID(Object bean, String beanName) throws BeansException {
        throw new RuntimeException("TODO: Not Implemented yet");
    }

    // Properties
    // -------------------------------------------------------------------------
    public ModuleContext getModuleContext() {
        return moduleContext;
    }

    @Context
    public void setModuleContext(ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }
}
