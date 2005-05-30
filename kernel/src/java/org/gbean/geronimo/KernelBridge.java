/**
 *
 * Copyright 2005 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.ObjectName;

import org.gbean.kernel.Kernel;
import org.gbean.kernel.NoSuchAttributeException;
import org.gbean.kernel.NoSuchOperationException;
import org.gbean.kernel.ServiceAlreadyExistsException;
import org.gbean.kernel.ServiceName;
import org.gbean.kernel.ServiceNotFoundException;
import org.gbean.kernel.runtime.ServiceState;
import org.gbean.kernel.simple.SimpleKernel;
import org.gbean.metadata.MetadataManager;
import org.gbean.metadata.simple.PropertiesMetadataProvider;
import org.gbean.metadata.simple.SimpleMetadataManager;
import org.gbean.proxy.ProxyManager;
import org.gbean.reflect.PropertyInvoker;
import org.gbean.reflect.ServiceInvoker;
import org.gbean.reflect.ServiceInvokerManager;
import org.gbean.service.AbstractServiceFactory;
import org.gbean.service.ConfigurableServiceFactory;
import org.gbean.service.ServiceContext;
import org.gbean.service.ServiceFactory;


/**
 * @version $Rev: 154947 $ $Date: 2005-02-22 20:10:45 -0800 (Tue, 22 Feb 2005) $
 */
public class KernelBridge implements org.apache.geronimo.kernel.Kernel {
    /**
     * Helper objects for invoke and getAttribute
     */
    private static final Object[] NO_ARGS = new Object[0];
    private static final String[] NO_TYPES = new String[0];

    private final SimpleKernel kernel;
    private final DependencyManagerBridge dependencyManagerBridge;
    private final ObjectName metadataManagerName;
    private final ObjectName serviceInvokerManagerName;
    private final ObjectName proxyManagerName;
    private final MetadataManager metadataManager;
    private final ServiceInvokerManager serviceInvokerManager;
    private final ProxyManager proxyManager;
    private final ProxyManagerBridge proxyManagerBridge;
    private final LifecycleMonitorBridge lifecycleMonitorBridge;

    public KernelBridge(String kernelName) {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            // normally we would use the KernelFactory, but geronimo will only work with the simple kernel
            kernel = new SimpleKernel(kernelName);
            dependencyManagerBridge = new DependencyManagerBridge(kernel.getDependencyManager());

            metadataManagerName = ServiceName.createName(":j2eeType=MetadataManager");
            List metadataProviders = new ArrayList(2);
            metadataProviders.add(new GeronimoMetadataProvider());
            metadataProviders.add(new PropertiesMetadataProvider());
            metadataManager = new SimpleMetadataManager(metadataProviders);
            kernel.loadService(metadataManagerName, new StaticServiceFactory(metadataManager), classLoader);
            kernel.startService(metadataManagerName);

            serviceInvokerManagerName = ServiceName.createName(":j2eeType=ServiceInvokerManager,name=default");
            serviceInvokerManager = new ServiceInvokerManager(kernel);
            serviceInvokerManager.start();
            kernel.loadService(serviceInvokerManagerName, new StaticServiceFactory(serviceInvokerManager), classLoader);
            kernel.startService(serviceInvokerManagerName);

            proxyManagerName = ServiceName.createName(":j2eeType=ProxyManager,name=default");
            proxyManager = new ProxyManager(serviceInvokerManager);
            kernel.loadService(proxyManagerName, new StaticServiceFactory(proxyManager), classLoader);
            kernel.startService(proxyManagerName);

            lifecycleMonitorBridge = new LifecycleMonitorBridge(kernel);
            this.proxyManagerBridge = new ProxyManagerBridge(proxyManager);
        } catch (Exception e) {
            throw new org.apache.geronimo.kernel.InternalKernelException(e);
        }
    }

    public Kernel getKernel() {
        return kernel;
    }

    public String getKernelName() {
        return kernel.getKernelName();
    }

    public org.apache.geronimo.kernel.DependencyManager getDependencyManager() {
        return dependencyManagerBridge;
    }

    public org.apache.geronimo.kernel.lifecycle.LifecycleMonitor getLifecycleMonitor() {
        return lifecycleMonitorBridge;
    }

    public org.apache.geronimo.kernel.proxy.ProxyManager getProxyManager() {
        return proxyManagerBridge;
    }

    public Object getAttribute(ObjectName objectName, String attributeName)
            throws org.apache.geronimo.kernel.GBeanNotFoundException,
            org.apache.geronimo.kernel.NoSuchAttributeException,
            Exception {

        boolean running = isRunning(objectName);
        if (running) {
            ServiceInvoker serviceInvoker = getServiceInvoker(objectName);
            try {
                Object value = serviceInvoker.getAttribute(attributeName);
                return value;
            } catch (NoSuchAttributeException e) {
                throw new org.apache.geronimo.kernel.NoSuchAttributeException(e);
            }
        } else {
            ServiceFactory serviceFactory = getServiceFactory(objectName);
            if (!(serviceFactory instanceof ConfigurableServiceFactory)) {
                throw new NoSuchAttributeException("Service is stopped and the service factory not configurable: objectName=" + objectName + ", propertyName=" + attributeName);
            }
            ConfigurableServiceFactory configurableServiceFactory = (ConfigurableServiceFactory) serviceFactory;
            return configurableServiceFactory.getProperty(attributeName);
        }
    }

    public void setAttribute(ObjectName objectName, String attributeName, Object attributeValue)
            throws org.apache.geronimo.kernel.GBeanNotFoundException,
            org.apache.geronimo.kernel.NoSuchAttributeException,
            Exception {

        boolean running = isRunning(objectName);
        if (running) {
            ServiceInvoker serviceInvoker = getServiceInvoker(objectName);
            try {
                serviceInvoker.setAttribute(attributeName, attributeValue);
            } catch (NoSuchAttributeException e) {
                throw new org.apache.geronimo.kernel.NoSuchAttributeException(e);
            }
        } else {
            ServiceFactory serviceFactory = getServiceFactory(objectName);
            if (!(serviceFactory instanceof ConfigurableServiceFactory)) {
                throw new NoSuchAttributeException("Service is stopped and the service factory not configurable: objectName=" + objectName + ", propertyName=" + attributeName);
            }
            ConfigurableServiceFactory configurableServiceFactory = (ConfigurableServiceFactory) serviceFactory;
            configurableServiceFactory.setProperty(attributeName, attributeValue);
        }
    }

    public Object invoke(ObjectName objectName, String methodName)
            throws org.apache.geronimo.kernel.GBeanNotFoundException,
            org.apache.geronimo.kernel.NoSuchOperationException,
            Exception {

        boolean running = isRunning(objectName);
        if (running) {
            throw new IllegalStateException("Service is not running: name=" + objectName);
        }

        ServiceInvoker serviceInvoker = getServiceInvoker(objectName);
        try {
            Object value = serviceInvoker.invoke(methodName, NO_ARGS, NO_TYPES);
            return value;
        } catch (NoSuchOperationException e) {
            throw new org.apache.geronimo.kernel.NoSuchOperationException(e);
        }
    }

    public Object invoke(ObjectName objectName, String methodName, Object[] args, String[] types)             throws org.apache.geronimo.kernel.GBeanNotFoundException,
            org.apache.geronimo.kernel.NoSuchOperationException,
            Exception {

        ServiceInvoker serviceInvoker = getServiceInvoker(objectName);
        try {
            Object value = serviceInvoker.invoke(methodName, args, types);
            return value;
        } catch (NoSuchOperationException e) {
            throw new org.apache.geronimo.kernel.NoSuchOperationException(e);
        }
    }

    public boolean isLoaded(ObjectName name) {
        return kernel.isLoaded(name);
    }

    public org.apache.geronimo.gbean.GBeanInfo getGBeanInfo(ObjectName name) throws org.apache.geronimo.kernel.GBeanNotFoundException {
        ServiceFactory serviceFactory = getServiceFactory(name);
        if (serviceFactory instanceof GeronimoServiceFactory) {
            GeronimoServiceFactory geronimoServiceFactory = (GeronimoServiceFactory) serviceFactory;
            return GeronimoUtil.createGBeanInfo(geronimoServiceFactory);
        }

        return createGBeanInfo(name);
    }

    public org.apache.geronimo.gbean.GBeanData getGBeanData(ObjectName objectName) throws org.apache.geronimo.kernel.GBeanNotFoundException{
        ServiceFactory serviceFactory = getServiceFactory(objectName);
        if (serviceFactory instanceof GeronimoServiceFactory) {
            GeronimoServiceFactory geronimoServiceFactory = null;
            geronimoServiceFactory = (GeronimoServiceFactory) serviceFactory;
            return GeronimoUtil.createGBeanData(objectName, geronimoServiceFactory);
        }

        return createGBeanData(objectName);
    }

    public void loadGBean(org.apache.geronimo.gbean.GBeanData gbeanData, ClassLoader classLoader) throws org.apache.geronimo.kernel.GBeanAlreadyExistsException {
        ObjectName objectName = gbeanData.getName();

        GeronimoServiceFactory geronimoServiceFactory = null;
        try {
            geronimoServiceFactory =GeronimoUtil.createGeronimoServiceFactory(gbeanData, classLoader, metadataManager, proxyManager);
        } catch (Exception e) {
            throw new org.apache.geronimo.kernel.InternalKernelException(e);
        }
        try {
            kernel.loadService(objectName, geronimoServiceFactory, classLoader);
        } catch (ServiceAlreadyExistsException e) {
            throw new org.apache.geronimo.kernel.GBeanAlreadyExistsException(e);
        }
    }

    public void startGBean(ObjectName name) throws org.apache.geronimo.kernel.GBeanNotFoundException,  IllegalStateException {
        try {
            kernel.startService(name);
        } catch (ServiceNotFoundException e) {
            throw new org.apache.geronimo.kernel.GBeanNotFoundException(e);
        }
    }

    public void startRecursiveGBean(ObjectName name) throws org.apache.geronimo.kernel.GBeanNotFoundException, IllegalStateException {
        try {
            kernel.startRecursiveService(name);
        } catch (ServiceNotFoundException e) {
            throw new org.apache.geronimo.kernel.GBeanNotFoundException(e);
        }
    }

    public void stopGBean(ObjectName name) throws org.apache.geronimo.kernel.GBeanNotFoundException, IllegalStateException {
        try {
            kernel.stopService(name);
        } catch (ServiceNotFoundException e) {
            throw new org.apache.geronimo.kernel.GBeanNotFoundException(e);
        }
    }

    public void unloadGBean(ObjectName name) throws org.apache.geronimo.kernel.GBeanNotFoundException, IllegalStateException {
        try {
            kernel.unloadService(name);
        } catch (ServiceNotFoundException e) {
            throw new org.apache.geronimo.kernel.GBeanNotFoundException(e);
        }
    }
    
    public int getGBeanState(ObjectName name) throws org.apache.geronimo.kernel.GBeanNotFoundException {
        try {
            return kernel.getServiceState(name);
        } catch (ServiceNotFoundException e) {
            throw new org.apache.geronimo.kernel.GBeanNotFoundException(e);
        }
    }

    public long getGBeanStartTime(ObjectName name) throws org.apache.geronimo.kernel.GBeanNotFoundException {
        try {
            return kernel.getServiceStartTime(name);
        } catch (ServiceNotFoundException e) {
            throw new org.apache.geronimo.kernel.GBeanNotFoundException(e);
        }
    }

    public boolean isGBeanEnabled(ObjectName name) throws org.apache.geronimo.kernel.GBeanNotFoundException {
        try {
            return kernel.isServiceEnabled(name);
        } catch (ServiceNotFoundException e) {
            throw new org.apache.geronimo.kernel.GBeanNotFoundException(e);
        }
    }

    public void setGBeanEnabled(ObjectName name, boolean enabled) throws org.apache.geronimo.kernel.GBeanNotFoundException {
        try {
            kernel.setServiceEnabled(name, enabled);
        } catch (ServiceNotFoundException e) {
            throw new org.apache.geronimo.kernel.GBeanNotFoundException(e);
        }
    }

    public Set listGBeans(ObjectName pattern) {
        return kernel.listServices(pattern);
    }

    public Set listGBeans(Set patterns) {
        return kernel.listServices(patterns);
    }

    public void boot() throws Exception {
        kernel.boot();

        // mount the kernel into the kernel itself so it can be accessed just like
        // any other service in the system
        ServiceFactory kernelServiceFactory = new AbstractServiceFactory() {
            public Object createService(ServiceContext serviceContext) throws Exception {
                return KernelBridge.this;
            }
        };
        kernel.loadService(KERNEL, kernelServiceFactory, getClass().getClassLoader());
        kernel.startService(KERNEL);

        org.apache.geronimo.kernel.KernelRegistry.registerKernel(this);
    }

    public Date getBootTime() {
        return kernel.getBootTime();
    }

    public void registerShutdownHook(Runnable hook) {
        kernel.registerShutdownHook(hook);
    }

    public void unregisterShutdownHook(Runnable hook) {
        kernel.unregisterShutdownHook(hook);
    }

    public void shutdown() {
        try {
            kernel.stopService(proxyManagerName);
            kernel.unloadService(proxyManagerName);
        } catch (ServiceNotFoundException e) {
            // igore service has already been removed
        }
        try {
            kernel.stopService(serviceInvokerManagerName);
            kernel.unloadService(serviceInvokerManagerName);
            serviceInvokerManager.stop();
        } catch (ServiceNotFoundException e) {
            // igore service has already been removed
        }
        try {
            kernel.stopService(metadataManagerName);
            kernel.unloadService(metadataManagerName);
        } catch (ServiceNotFoundException e) {
            // igore service has already been removed
        }
        kernel.shutdown();

        synchronized (this) {
            notify();
        }

        org.apache.geronimo.kernel.KernelRegistry.unregisterKernel(this);
    }

    public boolean isRunning() {
        return kernel.isRunning();
    }

    public ClassLoader getClassLoaderFor(ObjectName name) throws org.apache.geronimo.kernel.GBeanNotFoundException {
        try {
            return kernel.getClassLoaderFor(name);
        } catch (ServiceNotFoundException e) {
            throw new org.apache.geronimo.kernel.GBeanNotFoundException(e);
        }
    }

    private ServiceInvoker getServiceInvoker(ObjectName objectName) throws org.apache.geronimo.kernel.GBeanNotFoundException {
        try {
            return serviceInvokerManager.getServiceInvoker(objectName);
        } catch (ServiceNotFoundException e) {
            throw new org.apache.geronimo.kernel.GBeanNotFoundException(e);
        }
    }

    private ServiceFactory getServiceFactory(ObjectName objectName) throws org.apache.geronimo.kernel.GBeanNotFoundException {
        try {
            return kernel.getServiceFactory(objectName);
        } catch (ServiceNotFoundException e) {
            throw new org.apache.geronimo.kernel.GBeanNotFoundException(e);
        }
    }
    private boolean isRunning(ObjectName objectName) {
        try {
            int serviceState = kernel.getServiceState(objectName);
            boolean running = serviceState == ServiceState.RUNNING_INDEX || serviceState == ServiceState.STOPPING_INDEX;
            return running;
        } catch (ServiceNotFoundException e) {
            return false;
        }
    }

    private org.apache.geronimo.gbean.GBeanInfo createGBeanInfo(ObjectName objectName) throws org.apache.geronimo.kernel.GBeanNotFoundException {
        boolean running = isRunning(objectName);
        if (running) {
            ServiceInvoker serviceInvoker = getServiceInvoker(objectName);
            return createGBeanInfo(serviceInvoker);
        } else {
            ServiceFactory serviceFactory = getServiceFactory(objectName);
            return createGBeanInfo(serviceFactory);
        }

    }

    private org.apache.geronimo.gbean.GBeanInfo createGBeanInfo(ServiceInvoker serviceInvoker) {
        Set attributeInfos = new HashSet();
        List propertyIndex = serviceInvoker.getPropertyIndex();
        for (Iterator iterator = propertyIndex.iterator(); iterator.hasNext();) {
            PropertyInvoker propertyInvoker = (PropertyInvoker) iterator.next();
            String getterName = null;
            if (propertyInvoker.isReadable()) {
                getterName = propertyInvoker.getGetterSignature().getName();
            }
            String setterName = null;
            if (propertyInvoker.isWritable()) {
                setterName = propertyInvoker.getSetterSignature().getName();
            }
            attributeInfos.add(new org.apache.geronimo.gbean.GAttributeInfo(propertyInvoker.getPropertyName(),
                    propertyInvoker.getType().getName(),
                    false,
                    getterName,
                    setterName));
        }
        return new org.apache.geronimo.gbean.GBeanInfo(serviceInvoker.getServiceType().getName(),
                "GBean",
                attributeInfos,
                new org.apache.geronimo.gbean.GConstructorInfo(new String[] {}),
                Collections.EMPTY_SET,
                Collections.EMPTY_SET);
    }

    private org.apache.geronimo.gbean.GBeanInfo createGBeanInfo(ServiceFactory serviceFactory) {
        String serviceType;
        Set attributeInfos = new HashSet();
        serviceType = Object.class.getName();
        if (serviceFactory instanceof ConfigurableServiceFactory) {
            ConfigurableServiceFactory configurableServiceFactory = (ConfigurableServiceFactory) serviceFactory;
            Set propertyNames = configurableServiceFactory.getPropertyNames();
            for (Iterator iterator = propertyNames.iterator(); iterator.hasNext();) {
                String propertyName = (String) iterator.next();
                String ucase = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
                String getterName = "get" + ucase;
                String setterName = "set" + ucase;
                attributeInfos.add(new org.apache.geronimo.gbean.GAttributeInfo(propertyName,
                        Object.class.getName(),
                        false,
                        getterName,
                        setterName));
            }
        }
        return new org.apache.geronimo.gbean.GBeanInfo(serviceType,
                "GBean",
                attributeInfos,
                new org.apache.geronimo.gbean.GConstructorInfo(new String[] {}),
                Collections.EMPTY_SET,
                Collections.EMPTY_SET);
    }

    private org.apache.geronimo.gbean.GBeanData createGBeanData(ObjectName objectName) throws org.apache.geronimo.kernel.GBeanNotFoundException {
        boolean running = isRunning(objectName);
        if (running) {
            ServiceInvoker serviceInvoker = getServiceInvoker(objectName);
            return createGBeanData(serviceInvoker);
        } else {
            ServiceFactory serviceFactory = getServiceFactory(objectName);
            return createGBeanData(objectName, serviceFactory);
        }

    }

    private org.apache.geronimo.gbean.GBeanData createGBeanData(ServiceInvoker serviceInvoker) {
        org.apache.geronimo.gbean.GBeanInfo gbeanInfo = createGBeanInfo(serviceInvoker);
        org.apache.geronimo.gbean.GBeanData gbeanData = new org.apache.geronimo.gbean.GBeanData(serviceInvoker.getServiceName(), gbeanInfo);
        for (Iterator iterator = gbeanInfo.getAttributes().iterator(); iterator.hasNext();) {
            org.apache.geronimo.gbean.GAttributeInfo attribute = (org.apache.geronimo.gbean.GAttributeInfo) iterator.next();
            if (attribute.isReadable()) {
                try {
                    String attributeName = attribute.getName();
                    Object attributeValue = serviceInvoker.getAttribute(attributeName);
                    gbeanData.setAttribute(attributeName, attributeValue);
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        return gbeanData;
    }

    private org.apache.geronimo.gbean.GBeanData createGBeanData(ObjectName objectName, ServiceFactory serviceFactory) {
        org.apache.geronimo.gbean.GBeanInfo gbeanInfo = createGBeanInfo(serviceFactory);
        org.apache.geronimo.gbean.GBeanData gbeanData = new org.apache.geronimo.gbean.GBeanData(objectName, gbeanInfo);
        if (serviceFactory instanceof ConfigurableServiceFactory) {
            ConfigurableServiceFactory configurableServiceFactory = (ConfigurableServiceFactory) serviceFactory;
            for (Iterator iterator = gbeanInfo.getAttributes().iterator(); iterator.hasNext();) {
                org.apache.geronimo.gbean.GAttributeInfo attribute = (org.apache.geronimo.gbean.GAttributeInfo) iterator.next();
                if (attribute.isReadable()) {
                    try {
                        String attributeName = attribute.getName();
                        Object attributeValue = configurableServiceFactory.getProperty(attributeName);
                        gbeanData.setAttribute(attributeName, attributeValue);
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        }
        return gbeanData;
    }

    private static class StaticServiceFactory implements ServiceFactory {
        private final Object service;

        public StaticServiceFactory(Object service) {
            this.service = service;
        }

        public Map getDependencies() {
            return Collections.EMPTY_MAP;
        }

        public void addDependency(String name, Set patterns) {
        }

        public Object createService(ServiceContext serviceContext) throws Exception {
            return service;
        }

        public void destroyService(ServiceContext serviceContext, Object service) {
            if (service != this.service) {
                throw new IllegalArgumentException("Wrong service instance");
            }
        }

        public boolean isEnabled() {
            return true;
        }

        public void setEnabled(boolean enabled) {
        }
    }

}

