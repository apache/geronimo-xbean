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

    protected static Method coerceMethod;
    protected static Method createMethod;

    public static void coerceNamespaceAwarePropertyValues(AbstractBeanDefinition beanDefinition, Element element,
            PropertyDescriptor[] descriptors, int index) {
        QNameReflectionParams params = new QNameReflectionParams(beanDefinition, element, descriptors, index);
        if (coerceMethod == null) {
            coerceMethod = findMethod("coerceQNamePropertyValues");
        }
        if (coerceMethod != null) {
            try {
                coerceMethod.invoke(null, new Object[] { params });
            }
            catch (Exception e) {
                throw new BeanDefinitionStoreException("Failed to invoke method: " + coerceMethod + " via reflection: " + e,
                        e);
            }
        }
    }
    
    public static Object createQName(Element element, String text) {
        if (createMethod == null) {
            createMethod = findMethod("createQName");
        }
        if (createMethod != null) {
            try {
                return createMethod.invoke(null, new Object[] { element, text });
            }
            catch (Exception e) {
                throw new BeanDefinitionStoreException("Failed to invoke method: " + createMethod + " via reflection: " + e,
                        e);
            }
        }
        return null;
    }

    protected static Method findMethod(String name) {
        try {
            Class type = PropertyEditorHelper.loadClass("org.apache.xbean.spring.context.impl.QNameHelper");
            if (type != null) {
                Method[] methods = type.getMethods();
                for (int i = 0; i < methods.length; i++) {
                    Method method = methods[i];
                    if (method.getName().equals(name)) {
                        return method;
                    }
                }
            }
        } catch (Throwable t) {
            // Ignore, this is usually because QName method is not in the classpath 
        }
        return null;
    }

}
