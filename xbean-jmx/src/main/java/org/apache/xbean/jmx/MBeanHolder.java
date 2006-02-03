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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import javax.management.MBeanInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.livetribe.jmx.DynamicMBeanAdapter;


/**
 * @version $Revision: $ $Date: $
 * @org.apache.xbean.XBean element="mbean"
 */
public class MBeanHolder {
    private Object bean;
    private String objectName;
    private Class mbeanClass;
    private List eventAdapters;

    public void setBean(Object bean) {
        this.bean = bean;
    }

    public Object getBean() {
        return bean;
    }

    /**
     * @org.apache.xbean.Property alias="objectname"
     */
    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    private String getObjectName() {
        return objectName;
    }

    public void setMbeanClass(Class mbeanClass) {
        this.mbeanClass = mbeanClass;
    }

    private Class getMbeanClass() {
        return mbeanClass;
    }

    /**
     * @org.apache.xbean.Property alias="listeners"
     */
    public void setEventAdapters(List adapters) {
        this.eventAdapters = adapters;
    }

    private List getEventAdapters() {
        return eventAdapters;
    }

    public ObjectName createObjectName() {
        String name = getObjectName();
        try {
            return ObjectName.getInstance(name);
        }
        catch (MalformedObjectNameException x) {
            throw new JMXException(x);
        }
    }

    protected Object createMBeanAdapter(MBeanInfo mbeanInfo, ObjectName objectName) {
        try {
            Class adapterClass = getMbeanClass();
            if (adapterClass == null) adapterClass = DynamicMBeanAdapter.class;
            Constructor ctor = adapterClass.getConstructor(new Class[]{Object.class, MBeanInfo.class});
            return ctor.newInstance(new Object[]{getBean(), mbeanInfo});
        }
        catch (NoSuchMethodException x) {
            throw new JMXException(x);
        }
        catch (InstantiationException x) {
            throw new JMXException(x);
        }
        catch (IllegalAccessException x) {
            throw new JMXException(x);
        }
        catch (InvocationTargetException x) {
            throw new JMXException(x.getCause());
        }
    }

    public void bindListeners(Object mbeanAdapter) {
        List adapters = getEventAdapters();
        if (adapters != null) {
            for (int i = 0; i < adapters.size(); i++) {
                EventAdapter adapter = (EventAdapter) adapters.get(i);
                adapter.bindListener(mbeanAdapter);
            }
        }
    }

    public void unbindListeners() {
        List adapters = getEventAdapters();
        if (adapters != null) {
            for (int i = 0; i < adapters.size(); i++) {
                EventAdapter adapter = (EventAdapter) adapters.get(i);
                adapter.unbindListener();
            }
        }
    }
}
