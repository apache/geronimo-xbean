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

import javax.management.ObjectName;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.RootBeanDefinition;

public final class GBeanDefinition extends RootBeanDefinition {
    private ObjectName objectName;
    private boolean enabled = true;
    private MutablePropertyValues dynamicPropertyValue = new MutablePropertyValues();

    public GBeanDefinition(GBeanDefinition original) {
        super(original);
        setObjectName(original.getObjectName());
        setEnabled(original.isEnabled());
        setDynamicPropertyValues(new MutablePropertyValues(original.getDynamicPropertyValues()));
    }

    public GBeanDefinition(ObjectName objectName,
            Class beanClass,
            boolean enabled,
            ConstructorArgumentValues constructorArgs,
            MutablePropertyValues propertyValues,
            MutablePropertyValues dynamicPropertyValue,
            String[] dependencies) {
        super(beanClass, constructorArgs, propertyValues);
        setObjectName(objectName);
        setEnabled(enabled);
        setDynamicPropertyValues(new MutablePropertyValues(dynamicPropertyValue));
        setDependsOn(dependencies);
    }

    /**
     * Gets the name of this bean
     *
     * @return the name of this bean
     */
    public ObjectName getObjectName() {
        return objectName;
    }

    /**
     * Sets the name of this bean
     *
     * @param objectName the new bean name
     */
    public void setObjectName(ObjectName objectName) {
        this.objectName = objectName;
    }

    /**
     * Should this bean be allowed to start
     *
     * @return true if the bean is startable
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the startable status of this bean
     *
     * @param enabled the new enabled state of this bean
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setDynamicPropertyValues(MutablePropertyValues dynamicPropertyValue) {
        if (dynamicPropertyValue == null) {
            dynamicPropertyValue = new MutablePropertyValues();
        }
        this.dynamicPropertyValue = dynamicPropertyValue;
    }

    public MutablePropertyValues getDynamicPropertyValues() {
        return dynamicPropertyValue;
    }
}
