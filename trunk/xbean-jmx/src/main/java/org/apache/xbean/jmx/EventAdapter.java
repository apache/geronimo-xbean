/**
 *
 * Copyright 2005-2006 The Apache Software Foundation or its licensors, as applicable.
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
package org.apache.xbean.jmx;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import javax.management.Notification;


/**
 * @version $Revision: $ $Date: $
 * @org.apache.xbean.XBean element="listen"
 */
public class EventAdapter {
    private Object bean;
    private Class listenerClass;
    private String addMethodName;
    private String removeMethodName;
    private Object listener;

    public void setBean(Object bean) {
        this.bean = bean;
    }

    /**
     * @org.apache.xbean.Property alias="listener"
     */
    public void setListenerClass(Class listenerClass) {
        this.listenerClass = listenerClass;
    }

    public void setAddMethodName(String addMethodName) {
        this.addMethodName = addMethodName;
    }

    public void setRemoveMethodName(String removeMethodName) {
        this.removeMethodName = removeMethodName;
    }

    public void bindListener(Object mbeanAdapter) {
        listener = Proxy.newProxyInstance(listenerClass.getClassLoader(), new Class[]{listenerClass}, new EventInvocationHandler(mbeanAdapter));
        String methodName = addMethodName;
        if (methodName == null) methodName = "add" + getSimpleClassName(listenerClass);
        try {
            Method addMethod = bean.getClass().getMethod(methodName, new Class[]{listenerClass});
            addMethod.invoke(bean, new Object[]{listener});
        }
        catch (NoSuchMethodException x) {
            throw new JMXException(x);
        }
        catch (IllegalAccessException x) {
            throw new JMXException(x);
        }
        catch (InvocationTargetException x) {
            throw new JMXException(x.getCause());
        }
    }

    public void unbindListener() {
        String methodName = removeMethodName;
        if (methodName == null) methodName = "remove" + getSimpleClassName(listenerClass);
        try {
            Method addMethod = bean.getClass().getMethod(methodName, new Class[]{listenerClass});
            addMethod.invoke(bean, new Object[]{listener});
        }
        catch (NoSuchMethodException x) {
            throw new JMXException(x);
        }
        catch (IllegalAccessException x) {
            throw new JMXException(x);
        }
        catch (InvocationTargetException x) {
            throw new JMXException(x.getCause());
        }
    }

    private String getSimpleClassName(Class cls) {
        String name = cls.getName();
        return name.substring(name.lastIndexOf('.') + 1);
    }

    private static class EventInvocationHandler implements InvocationHandler {
        private final Object emitter;

        public EventInvocationHandler(Object emitter) {
            this.emitter = emitter;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) return method.invoke(this, args);

            Object event = args[0];
            Notification notification = new Notification(event.getClass().getName(), emitter, 0, System.currentTimeMillis());
            notification.setUserData(event);

            try {
                Method send = emitter.getClass().getMethod("sendNotification", new Class[]{Notification.class});
                send.invoke(emitter, new Object[]{notification});
                return null;
            }
            catch (NoSuchMethodException x) {
                throw new JMXException(x);
            }
            catch (IllegalAccessException x) {
                throw new JMXException(x);
            }
            catch (InvocationTargetException x) {
                throw new JMXException(x.getCause());
            }
        }
    }
}
