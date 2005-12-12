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
package org.xbean.sca;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Useful base class for introspection of POJOs
 * 
 * @version $Revision$
 */
public abstract class IntrospectionSupport {

    protected static final Object[] EMPTY_ARGUMENTS = {};

    protected BeanInfo getBeanInfo(Object bean) {
        BeanInfo beanInfo;
        try {
            beanInfo = Introspector.getBeanInfo(bean.getClass());
        }
        catch (IntrospectionException e) {
            throw new BeanInitializationException("Failed to introspect: " + bean + ". Reason: " + e, e);
        }
        return beanInfo;
    }

    protected void invokeMethod(Object bean, Method method, Object[] arguments) {
        try {
            method.invoke(bean, arguments);
        }
        catch (Exception e) {
            throw new BeanInitializationException("Failed to invoke: " + method + ". Reason: " + e, e);
        }
    }

    protected void setField(Object bean, Field field, Object value) {
        try {
            if (! field.isAccessible()) {
                field.setAccessible(true);
            }
            field.set(bean, value);
        }
        catch (Exception e) {
            throw new BeanInitializationException("Failed to set field: " + field + " to value: " + value + ". Reason: " + e, e);
        }
    }

    protected List findMethodsWithAnnotation(Object bean, Class annotation) {
        List answer = new ArrayList();
        appendMethodsWithAnnotation(bean.getClass(), answer, annotation);
        return answer;
    }

    protected void appendMethodsWithAnnotation(Class type, List list, Class annotation) {
        Method[] methods = type.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (hasAnnotation(method, annotation)) {
                list.add(method);
            }
        }
        if (!type.equals(Object.class)) {
            appendMethodsWithAnnotation(type.getSuperclass(), list, annotation);
        }
    }

    /**
     * Returns true if the given method has the annotation
     */
    protected boolean hasAnnotation(Method method, Class annotation) {
        return method.getAnnotation(annotation) != null;
    }

    protected boolean hasAnnotation(Field field, Class annotation) {
        return field.getAnnotation(annotation) != null;
    }

    protected void invokeVoidMethods(Object bean, List methods) throws BeansException {
        for (Iterator iter = methods.iterator(); iter.hasNext();) {
            Method method = (Method) iter.next();
            Class[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 0) {
                try {
                    method.invoke(bean, EMPTY_ARGUMENTS);
                }
                catch (IllegalArgumentException e) {
                    throw new BeanInitializationException("Should never happen when calling no-parameter method. " + e, e);
                }
                catch (IllegalAccessException e) {
                    throw new BeanInitializationException(e.getMessage(), e);
                }
                catch (InvocationTargetException e) {
                    throw new BeanInitializationException(e.getMessage(), e);
                }
            }
        }
    }

}
