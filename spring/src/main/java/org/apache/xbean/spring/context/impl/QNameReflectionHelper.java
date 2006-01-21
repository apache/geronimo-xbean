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
package org.apache.xbean.spring.context.impl;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.w3c.dom.Element;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

/**
 * To avoid a runtime dependency on the QName class lets use reflection to
 * process QName instances.
 * 
 * @version $Revision: 1.1 $
 */
public class QNameReflectionHelper {

    protected static Method method;

    public static void coerceNamespaceAwarePropertyValues(AbstractBeanDefinition beanDefinition, Element element,
            PropertyDescriptor[] descriptors, int index) {
        QNameReflectionParams params = new QNameReflectionParams(beanDefinition, element, descriptors, index);
        if (method == null) {
            method = createMethod();
        }
        if (method != null) {
            try {
                method.invoke(null, new Object[] { params });
            }
            catch (Exception e) {
                throw new BeanDefinitionStoreException("Failed to invoke method: " + method + " via reflection: " + e,
                        e);
            }
        }
    }

    protected static Method createMethod() {
        Class type = PropertyEditorHelper.loadClass("org.apache.xbean.spring.context.impl.QNameHelper");
        if (type != null) {
            Method[] methods = type.getMethods();
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];
                if (method.getName().equals("coerceQNamePropertyValues")) {
                    return method;
                }
            }
        }
        return null;
    }

}
