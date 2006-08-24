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
package org.apache.xbean.jmx;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.ObjectName;


/**
 * Exports services to an MBeanServer for management.
 *
 * @version $Revision: $ $Date: $
 * @org.apache.xbean.XBean element="export"
 */
public class MBeanExporter {
    private MBeanServer mbeanServer;
    private List wrapStrategies;
    private List mbeans;
    private List connectors;
    private Map registeredMBeans = new HashMap();
    private Map configuredClasses = new HashMap();

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
     * @org.apache.xbean.Property alias="wrapStrategies"
     */
    public void setWrapStrategies(List wrapStrategies) {
        this.wrapStrategies = wrapStrategies;
    }

    public List getWrapStrategies() {
        return wrapStrategies;
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

        List strategies = getWrapStrategies();

        if (strategies != null) {
            for (int i = 0; i < strategies.size(); i++) {
                MBeanWrap wrap = (MBeanWrap) strategies.get(i);

                if (configuredClasses.containsKey(wrap.getBeanClass()))
                    throw new IllegalStateException(wrap.getBeanClass() + "configured multiple times in <wrapStrategies/>");

                if (wrap.getConfig() == null) wrap.setConfig(new Properties());

                configuredClasses.put(wrap.getBeanClass(), wrap);
            }
        }

        try {
            MBeanServer server = findMBeanServer();
            List mbeanElements = getMbeans();
            for (int i = 0; i < mbeanElements.size(); i++) {
                MBeanHolder mbean = (MBeanHolder) mbeanElements.get(i);
                MBeanWrap wrap = getWrapConfig(mbean);

                ObjectName objectName = mbean.createObjectName();
                JMXWrappingStrategy strategy = JMXStrategyFinder.newInstance(wrap.getStrategy());
                Object mbeanAdapter = strategy.wrapObject(mbean.getBean(), wrap.getConfig());

                server.registerMBean(mbeanAdapter, objectName);

                mbean.bindListeners(mbeanAdapter);

                registeredMBeans.put(objectName, mbean);
            }
        } catch (JMException x) {
            throw new JMXException(x);
        } catch (JMXServiceException e) {
            throw new JMXException(e);
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

    protected MBeanWrap getWrapConfig(MBeanHolder mbean) {
        MBeanWrap result;

        String className = mbean.getBean().getClass().getName();
        String strategyName = mbean.getWrapStrategy();

        result = (MBeanWrap) configuredClasses.get(className);
        if (result != null) {
            if (strategyName != null && !result.getStrategy().equals(strategyName))
                throw new IllegalStateException(className + "configured with " + strategyName + " and " + result.getStrategy());
        } else {
            result = new MBeanWrap();

            result.setBeanClass(className);
            result.setStrategy(strategyName);
            result.setConfig(new Properties());

            configuredClasses.put(className, result);
        }

        return result;
    }
}
