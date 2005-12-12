/**
 *
 * Copyright 2005 (C) The original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.xbean.jmx;

import javax.management.MBeanInfo;
import javax.management.ObjectName;


/**
 * @version $Revision: $ $Date: $
 * @org.xbean.XBean element="beanPairing"
 */
public class BeanPairing {
    private Class beanClass;
    private Class generatorClass;

    /**
     * @org.xbean.Property alias="beanClass"
     */
    public void setBeanClass(Class beanClass) {
        this.beanClass = beanClass;
    }

    public Class getBeanClass() {
        return beanClass;
    }

    /**
     * @org.xbean.Property alias="generatorClass"
     */
    public void setGeneratorClass(Class generatorClass) {
        this.generatorClass = generatorClass;
    }

    public Class getGeneratorClass() {
        return generatorClass;
    }

    public MBeanInfo createMBeanInfo(Object bean, ObjectName objectName) {
        try {
            MBeanInfoGenerator generator = (MBeanInfoGenerator) getGeneratorClass().newInstance();
            return generator.createMBeanInfo(bean, objectName);
        }
        catch (InstantiationException x) {
            throw new JMXException(x);
        }
        catch (IllegalAccessException x) {
            throw new JMXException(x);
        }
        catch (ClassCastException x) {
            throw new JMXException(x);
        }
    }
}
