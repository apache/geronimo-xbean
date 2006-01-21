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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.xbean.jmx.assembler.JavaBeanMBeanInfoGenerator;


/**
 * Exports services to an MBeanServer for management.
 *
 * @version $Revision: $ $Date: $
 * @org.apache.xbean.XBean element="export"
 */
public class MBeanExporter {
    private MBeanServer mbeanServer;
    private List mbeanInfos;
    private List mbeans;
    private List connectors;
    private Map registeredMBeans = new HashMap();

    /**
     * @org.apache.xbean.Property alias="mbeanserver"
     */
    public void setMbeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }

    public MBeanServer getMbeanServer() {
        return mbeanServer;
    }

    /**
     * @org.apache.xbean.Property alias="mbeaninfos"
     */
    public void setMbeanInfos(List mbeanInfos) {
        this.mbeanInfos = mbeanInfos;
    }

    public List getMbeanInfos() {
        return mbeanInfos;
    }

    /**
     * @org.apache.xbean.Property alias="mbeans"
     */
    public void setMbeans(List mbeans) {
        this.mbeans = mbeans;
    }

    public List getMbeans() {
        return mbeans;
    }

    public void setConnectors(List connectors) {
        this.connectors = connectors;
    }

    public List getConnectors() {
        return connectors;
    }

    /**
     * @org.apache.xbean.InitMethod
     */
    public void start() {
        try {
            MBeanServer server = findMBeanServer();
            List mbeanElements = getMbeans();
            for (int i = 0; i < mbeanElements.size(); i++) {
                MBeanHolder mbean = (MBeanHolder) mbeanElements.get(i);

                ObjectName objectName = mbean.createObjectName();
                MBeanInfo metadata = createMBeanInfo(mbean, objectName);
                Object mbeanAdapter = mbean.createMBeanAdapter(metadata, objectName);
                server.registerMBean(mbeanAdapter, objectName);
                mbean.bindListeners(mbeanAdapter);
                registeredMBeans.put(objectName, mbean);
            }
        }
        catch (JMException x) {
            throw new JMXException(x);
        }
    }

    /**
     * @org.apache.xbean.DestroyMethod
     */
    public void stop() {
        MBeanServer server = findMBeanServer();
        for (Iterator iterator = registeredMBeans.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            try {
                server.unregisterMBean((ObjectName) entry.getKey());
                ((MBeanHolder) entry.getValue()).unbindListeners();
            }
            catch (InstanceNotFoundException ignored) {
            }
            catch (MBeanRegistrationException x) {
                throw new JMXException(x);
            }
        }
    }

    protected MBeanServer findMBeanServer() {
        MBeanServer server = getMbeanServer();
        if (server != null) return server;

        // TODO: handle JDK 5 Platform MBeanServer if data member is null
        throw new JMXException("Cannot find MBeanServer");
    }

    private MBeanInfo createMBeanInfo(MBeanHolder mbean, ObjectName objectName) {
        MBeanInfo metadata = null;
        Class beanClass = mbean.getBean().getClass();
        List infos = getMbeanInfos();
        if (infos != null) {
            while (metadata == null && beanClass != Object.class) {
                for (int j = 0; j < infos.size(); j++) {
                    BeanPairing beanPairing = (BeanPairing) infos.get(j);
                    if (beanPairing.getBeanClass().isAssignableFrom(beanClass)) {
                        metadata = beanPairing.createMBeanInfo(mbean.getBean(), objectName);
                        return metadata;
                    }
                }
                beanClass = beanClass.getSuperclass();
            }
        }
        // No specialized assembler found, use a default one
        return new JavaBeanMBeanInfoGenerator().createMBeanInfo(mbean.getBean(), objectName);
    }
}
