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

package org.gbean.kernel.simple;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gbean.kernel.DependencyManager;
import org.gbean.kernel.Kernel;
import org.gbean.kernel.KernelRegistry;
import org.gbean.kernel.LifecycleListener;
import org.gbean.kernel.ServiceAlreadyExistsException;
import org.gbean.kernel.ServiceNotFoundException;
import org.gbean.kernel.runtime.ServiceInstance;
import org.gbean.service.AbstractServiceFactory;
import org.gbean.service.ServiceContext;
import org.gbean.service.ServiceFactory;


/**
 * @version $Rev: 154947 $ $Date: 2005-02-22 20:10:45 -0800 (Tue, 22 Feb 2005) $
 */
public class SimpleKernel implements Kernel {
    /**
     * Name of this kernel
     */
    private final String kernelName;

    /**
     * The log
     */
    private Log log;

    /**
     * Is this kernel running?
     */
    private boolean running;

    /**
     * The timestamp when the kernel was started
     */
    private Date bootTime;

    /**
     * The simple registry
     */
    private final SimpleRegistry registry;

    /**
     * Listeners for when the kernel shutdown
     */
    private final LinkedList shutdownHooks = new LinkedList();

    /**
     * This manager is used by the kernel to manage dependencies between services
     */
    private final DependencyManager dependencyManager;

    /**
     * Monitors the lifecycle of all services.
     */
    private final SimpleLifecycleMonitor lifecycleMonitor;

    /**
     * Construct a Kernel with the specified name.
     *
     * @param kernelName the name of the kernel
     */
    public SimpleKernel(String kernelName) {
        if (kernelName.indexOf(':') >= 0 || kernelName.indexOf('*') >= 0 || kernelName.indexOf('?') >= 0) {
            throw new IllegalArgumentException("Kernel name may not contain a ':', '*' or '?' character");
        }
        this.kernelName = kernelName;
        lifecycleMonitor = new SimpleLifecycleMonitor();
        this.registry = new SimpleRegistry(kernelName);
        dependencyManager = new SimpleDependencyManager(this);
    }

    public String getKernelName() {
        return kernelName;
    }

    /**
     * @deprecated Do not use.  This is only here for the geronimo bridge and will go away as soon as possible.
     */
    public DependencyManager getDependencyManager() {
        return dependencyManager;
    }

    public boolean isLoaded(ObjectName name) {
        return registry.isRegistered(name);
    }

    public void loadService(ObjectName objectName, ServiceFactory serviceFactory, ClassLoader classLoader) throws ServiceAlreadyExistsException {
        ServiceInstance serviceInstance = new ServiceInstance(objectName,
                serviceFactory,
                this,
                dependencyManager,
                lifecycleMonitor.createLifecycleBroadcaster(objectName),
                classLoader);
        registry.register(objectName, serviceInstance);
        serviceInstance.init();
    }

    public void startService(ObjectName name) throws ServiceNotFoundException, IllegalStateException {
        ServiceInstance serviceInstance = registry.getServiceInstance(name);
        serviceInstance.start();
    }

    public void startRecursiveService(ObjectName name) throws ServiceNotFoundException, IllegalStateException {
        ServiceInstance serviceInstance = registry.getServiceInstance(name);
        serviceInstance.startRecursive();
    }

    public void stopService(ObjectName name) throws ServiceNotFoundException, IllegalStateException {
        ServiceInstance serviceInstance = registry.getServiceInstance(name);
        serviceInstance.stop();
    }

    public void unloadService(ObjectName name) throws ServiceNotFoundException, IllegalStateException {
        ServiceInstance serviceInstance = registry.getServiceInstance(name);
        serviceInstance.destroy();
        registry.unregister(name);
    }

    public Object getService(ObjectName name) throws ServiceNotFoundException, IllegalStateException {
        ServiceInstance serviceInstance = registry.getServiceInstance(name);
        Object instance = serviceInstance.getInstance();
        if (instance == null) {
            throw new IllegalStateException("Service is not running: " + name);
        }
        return instance;
    }

    public ServiceFactory getServiceFactory(ObjectName name) throws ServiceNotFoundException {
        ServiceInstance serviceInstance = registry.getServiceInstance(name);
        return serviceInstance.getServiceFactory();
    }

    public int getServiceState(ObjectName name) throws ServiceNotFoundException {
        ServiceInstance serviceInstance = registry.getServiceInstance(name);
        return serviceInstance.getState();
    }

    public long getServiceStartTime(ObjectName name) throws ServiceNotFoundException {
        ServiceInstance serviceInstance = registry.getServiceInstance(name);
        return serviceInstance.getStartTime();
    }

    public boolean isServiceEnabled(ObjectName name) throws ServiceNotFoundException {
        ServiceInstance serviceInstance = registry.getServiceInstance(name);
        return serviceInstance.isEnabled();
    }

    public void setServiceEnabled(ObjectName name, boolean enabled) throws ServiceNotFoundException {
        ServiceInstance serviceInstance = registry.getServiceInstance(name);
        serviceInstance.setEnabled(enabled);
    }

    public Set listServices(ObjectName pattern) {
        return registry.listServices(pattern);
//        Set services = registry.listServices(pattern);
//        Set result = new HashSet(services.size());
//        for (Iterator i = services.iterator(); i.hasNext();) {
//            ServiceInstance instance = (ServiceInstance) i.next();
//            result.add(instance.getObjectName());
//        }
//        return result;
    }

    public Set listServices(Set patterns) {
        Set services = new HashSet();
        for (Iterator iterator = patterns.iterator(); iterator.hasNext();) {
            ObjectName pattern = (ObjectName) iterator.next();
            services.addAll(listServices(pattern));
        }
        return services;
    }

    public void addLifecycleListener(LifecycleListener lifecycleListener, ObjectName pattern) {
        addLifecycleListener(lifecycleListener, Collections.singleton(pattern));
    }

    public void addLifecycleListener(LifecycleListener lifecycleListener, Set patterns) {
        lifecycleMonitor.addLifecycleListener(lifecycleListener, patterns);
    }

    public void removeLifecycleListener(LifecycleListener lifecycleListener) {
        lifecycleMonitor.removeLifecycleListener(lifecycleListener);
    }

    /**
     * Boot this Kernel
     *
     * @throws Exception if the boot fails
     */
    public void boot() throws Exception {
        if (running) {
            return;
        }
        bootTime = new Date();
        log = LogFactory.getLog(SimpleKernel.class.getName());
        log.info("Starting boot");

        lifecycleMonitor.start();
        registry.start();
        dependencyManager.start();

        // mount the kernel into the kernel itself so it can be accessed just like
        // any other service in the system
        ServiceFactory kernelServiceFactory = new AbstractServiceFactory() {
            public Object createService(ServiceContext serviceContext) throws Exception {
                return SimpleKernel.this;
            }
        };
        loadService(KERNEL, kernelServiceFactory, getClass().getClassLoader());
        startService(KERNEL);

        running = true;
        log.info("Booted");

        KernelRegistry.registerKernel(this);
    }

    public Date getBootTime() {
        return bootTime;
    }

    public void registerShutdownHook(Runnable hook) {
        assert hook != null : "Shutdown hook was null";
        synchronized (shutdownHooks) {
            shutdownHooks.add(hook);
        }
    }

    public void unregisterShutdownHook(Runnable hook) {
        synchronized (shutdownHooks) {
            shutdownHooks.remove(hook);
        }
    }

    /**
     * Shutdown this kernel
     */
    public void shutdown() {
        if (!running) {
            return;
        }
        running = false;
        log.info("Starting kernel shutdown");

        notifyShutdownHooks();

        dependencyManager.stop();
        registry.stop();
        lifecycleMonitor.stop();

        synchronized (this) {
            notify();
        }

        KernelRegistry.unregisterKernel(this);

        log.info("Kernel shutdown complete");
    }

    private void notifyShutdownHooks() {
        while (!shutdownHooks.isEmpty()) {
            Runnable hook;
            synchronized (shutdownHooks) {
                hook = (Runnable) shutdownHooks.removeLast();
            }
            try {
                hook.run();
            } catch (Throwable e) {
                log.warn("Error from kernel shutdown hook", e);
            }
        }
    }

    public boolean isRunning() {
        return running;
    }

    public ClassLoader getClassLoaderFor(ObjectName name) throws ServiceNotFoundException {
        ServiceInstance serviceInstance = registry.getServiceInstance(name);
        return serviceInstance.getClassLoader();
    }
}
