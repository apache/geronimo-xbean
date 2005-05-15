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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.management.ObjectName;

import org.gbean.kernel.NoSuchAttributeException;
import org.gbean.kernel.NoSuchOperationException;
import org.gbean.kernel.ServiceAlreadyExistsException;
import org.gbean.kernel.ServiceName;
import org.gbean.kernel.ServiceNotFoundException;
import org.gbean.kernel.simple.SimpleKernel;
import org.gbean.proxy.ProxyManager;
import org.gbean.reflect.ServiceInvoker;
import org.gbean.reflect.ServiceInvokerManager;
import org.gbean.service.AbstractServiceFactory;
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
    private final ObjectName serviceInvokerManagerName;
    private final ObjectName proxyManagerName;
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

        ServiceInvoker serviceInvoker = getServiceInvoker(objectName);
        try {
            Object value = serviceInvoker.getAttribute(attributeName);
            return value;
        } catch (NoSuchAttributeException e) {
            throw new org.apache.geronimo.kernel.NoSuchAttributeException(e);
        }
    }

    public void setAttribute(ObjectName objectName, String attributeName, Object attributeValue)
            throws org.apache.geronimo.kernel.GBeanNotFoundException,
            org.apache.geronimo.kernel.NoSuchAttributeException,
            Exception {

        ServiceInvoker serviceInvoker = getServiceInvoker(objectName);
        try {
            serviceInvoker.setAttribute(attributeName, attributeValue);
        } catch (NoSuchAttributeException e) {
            throw new org.apache.geronimo.kernel.NoSuchAttributeException(e);
        }
    }

    public Object invoke(ObjectName objectName, String methodName)
            throws org.apache.geronimo.kernel.GBeanNotFoundException,
            org.apache.geronimo.kernel.NoSuchOperationException,
            Exception {

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
        try {
            ServiceFactory serviceFactory = kernel.getServiceFactory(name);
            if (serviceFactory instanceof GeronimoServiceFactory) {
                GeronimoServiceFactory geronimoServiceFactory = (GeronimoServiceFactory) serviceFactory;
                return GeronimoUtil.createGBeanInfo(geronimoServiceFactory.getGBeanDefinition());
            }
        } catch (ServiceNotFoundException e) {
            throw new org.apache.geronimo.kernel.GBeanNotFoundException(e);
        }

        try {
            Object service = kernel.getService(name);
            return createGBeanInfo(service.getClass());
        } catch (ServiceNotFoundException e) {
            throw new org.apache.geronimo.kernel.GBeanNotFoundException(e);
        }
    }

    public org.apache.geronimo.gbean.GBeanData getGBeanData(ObjectName name) throws org.apache.geronimo.kernel.GBeanNotFoundException{
        GeronimoServiceFactory geronimoServiceFactory = null;
        try {
            ServiceFactory serviceFactory = kernel.getServiceFactory(name);
            if (!(serviceFactory instanceof GeronimoServiceFactory)) {
                throw new org.apache.geronimo.kernel.GBeanNotFoundException("This service does not use a GeronimoServiceFactory");
            }
            geronimoServiceFactory = (GeronimoServiceFactory) serviceFactory;
        } catch (ServiceNotFoundException e) {
            throw new org.apache.geronimo.kernel.GBeanNotFoundException(e);
        }

        return GeronimoUtil.createGBeanData(geronimoServiceFactory.getGBeanDefinition());
    }

    public void loadGBean(org.apache.geronimo.gbean.GBeanData gbeanData, ClassLoader classLoader) throws org.apache.geronimo.kernel.GBeanAlreadyExistsException {
        ObjectName objectName = gbeanData.getName();

        GBeanDefinition geronimoBeanDefinition = GeronimoUtil.createGeronimoBeanDefinition(gbeanData, classLoader);
        GeronimoServiceFactory geronimoServiceFactory = new GeronimoServiceFactory(this, proxyManager, geronimoBeanDefinition);
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
        ServiceInvoker serviceInvoker = null;
        try {
            serviceInvoker = serviceInvokerManager.getServiceInvoker(objectName);
        } catch (ServiceNotFoundException e) {
            throw new org.apache.geronimo.kernel.GBeanNotFoundException(e);
        }
        return serviceInvoker;
    }

    private org.apache.geronimo.gbean.GBeanInfo createGBeanInfo(Class type) {
        // attributes
        Method[] methods = type.getMethods();

        // map the getters
        Map getterMap = new HashMap(methods.length);
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            String methodName = method.getName();
            if (Modifier.isPublic(method.getModifiers()) &&
                    !Modifier.isStatic(method.getModifiers()) &&
                    method.getParameterTypes().length == 0 &&
                    method.getReturnType() != Void.TYPE) {
                if (methodName.length() > 3 && methodName.startsWith("get") && !methodName.equals("getClass")) {
                    String attributeName = fixAttributeName(methodName.substring(3));

                    // if this attribute also has an "is" accessor make sure the return type is boolean
                    Method isAccessor = (Method) getterMap.get(attributeName);
                    if (isAccessor != null && method.getReturnType() != Boolean.TYPE) {
                        throw new IllegalArgumentException("Getter has both a get<name> and is<name> accessor but the getter return type is not boolean:" +
                                " class=" + type.getName() +
                                ", attribute=" + attributeName +
                                ", getAccessorType=" + method.getReturnType().getName());
                    }

                    // add it
                    getterMap.put(attributeName, method);
                } else if (methodName.length() > 2 && methodName.startsWith("is")) {
                    String attributeName = fixAttributeName(methodName.substring(2));

                    // an is accessor must return boolean
                    if (method.getReturnType() != Boolean.TYPE) {
                        throw new IllegalArgumentException("An is<name> accessor must return boolean:" +
                                " class=" + type.getName() +
                                ", attribute=" + attributeName +
                                ", attributeType=" + method.getReturnType().getName());
                    }

                    // if this attribute also has a "get" accessor make sure the getter return type is boolean
                    Method getAccessor = (Method) getterMap.get(attributeName);
                    if (getAccessor != null && method.getReturnType() != Boolean.TYPE) {
                        throw new IllegalArgumentException("Getter has both a get<name> and is<name> accessor but the getter return type is not boolean:" +
                                " class=" + type.getName() +
                                ", attribute=" + attributeName +
                                ", getAccessorType=" + getAccessor.getReturnType().getName());
                    }

                    // add it
                    getterMap.put(attributeName, method);
                }
            }
        }

        // map the setters
        Map setterMap = new HashMap(methods.length);
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            String methodName = method.getName();
            if (Modifier.isPublic(method.getModifiers()) &&
                    !Modifier.isStatic(method.getModifiers()) &&
                    method.getParameterTypes().length == 1 &&
                    method.getReturnType() == Void.TYPE &&
                    methodName.length() > 3 &&
                    methodName.startsWith("set")) {

                String attributeName = fixAttributeName(methodName.substring(3));
                if (setterMap.containsKey(attributeName)) {
                    // this bean attributte has multiple setters with different types, so treat only as operations
                    setterMap.put(attributeName, null);
                } else {
                    setterMap.put(attributeName, method);
                }

                // the getter and setter types must match
                Method getterMethod = (Method) getterMap.get(attributeName);
                if (getterMethod != null && !getterMethod.getReturnType().equals(method.getParameterTypes()[0])) {
                    throw new IllegalArgumentException("Getter and setter types do not match:" +
                            " class=" + type.getName() +
                            ", attribute=" + attributeName +
                            ", getAccessorType=" + getterMethod.getReturnType().getName() +
                            ", setAccessorType=" + method.getParameterTypes()[0].getName());
                }
            }
        }
        // remove any setter with a null method (these setters have multiple methods with different types)
        for (Iterator iterator = setterMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            Method setter = (Method) entry.getValue();
            if (setter == null) {
                iterator.remove();
            }
        }

        TreeSet propertyNames = new TreeSet();
        propertyNames.addAll(getterMap.keySet());
        propertyNames.addAll(setterMap.keySet());

        Set attributeInfos = new HashSet();
        for (Iterator iterator = propertyNames.iterator(); iterator.hasNext();) {
            String propertyName = (String) iterator.next();
            Class propertyType = null;

            Method getter = (Method) getterMap.get(propertyName);
            String getterName = null;
            if (getter != null) {
                propertyType = getter.getReturnType();
                getterName = getter.getName();
            }

            Method setter = (Method) setterMap.get(propertyName);
            String setterName = null;
            if (setter != null) {
                propertyType = setter.getParameterTypes()[0];
                setterName = setter.getName();
            }

            attributeInfos.add(new org.apache.geronimo.gbean.GAttributeInfo(propertyName,
                    propertyType.getName(),
                    false,
                    getterName,
                    setterName));
        }

//        // operations
//        Set operations = new HashSet(methods.length);
//        for (int i = 0; i < methods.length; i++) {
//            Method method = methods[i];
//            if (Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())) {
//                operationIndex.put(new GOperationSignature(method), new Integer(operationList.size()));
//                operationList.add(new ServiceInvoker.OperationInvokerImpl(method));
//            }
//        }
//        operations = (ServiceInvoker.OperationInvokerImpl[]) operationList.toArray(new ServiceInvoker.OperationInvokerImpl[operationList.size()]);
        return new org.apache.geronimo.gbean.GBeanInfo(type.getName(),
                "GBean",
                attributeInfos,
                new org.apache.geronimo.gbean.GConstructorInfo(new String[] {}),
                Collections.EMPTY_SET,
                Collections.EMPTY_SET);
    }

    private static String fixAttributeName(String attributeName) {
        if (Character.isUpperCase(attributeName.charAt(0))) {
            return Character.toLowerCase(attributeName.charAt(0)) + attributeName.substring(1);
        }
        return attributeName;
    }

    private static class StaticServiceFactory implements ServiceFactory {
        private final Object service;

        public StaticServiceFactory(Object service) {
            this.service = service;
        }

        public Map getDependencies() {
            return Collections.EMPTY_MAP;
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

