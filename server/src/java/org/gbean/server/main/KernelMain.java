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
package org.gbean.server.main;

import java.util.Map;
import java.util.Collections;
import java.util.Iterator;

import org.gbean.kernel.Kernel;
import org.gbean.kernel.KernelFactory;
import org.gbean.kernel.ServiceName;
import org.gbean.kernel.StringServiceName;
import org.gbean.kernel.StaticServiceFactory;

/**
 * KernelMain is the standard entry point class used for a server.  It will initalize a kernel with a set of services
 * and can optional hold the thread of execution until the kernel or virtual machine is destroyed.
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class KernelMain implements Main {
    private static final String DEFAULT_KERNEL_NAME = "xbean";

    private Kernel kernel;
    private ClassLoader classLoader;
    private Map services = Collections.EMPTY_MAP;
    private boolean daemon = true;
    private Main next;

    /**
     * Gets the kernel that will be initialized in the main method.  If the kernel is null, a new kernel will be created
     * and initialized in the main method.
     * @return the kernel that will be initialized in the main method
     */
    public Kernel getKernel() {
        return kernel;
    }

    /**
     * Sets the kernel to be initialized in the main method.
     * @param kernel the kernel to initialize in the main method
     */
    public void setKernel(Kernel kernel) {
        this.kernel = kernel;
    }

    /**
     * Gets the class loader which is used as the thread context class loader during the main method.
     * @return the class loader which is used as the thread context class loader during the main method
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Sets the class loader to use as the thread context class loader during the main method.
     * @param classLoader the class loader to use as the thread context class loader during the main method
     */
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Gets the services to be registered with the kernel during the main method.
     * @return the services to be mounted added to the kernel during the main method
     */
    public Map getServices() {
        return services;
    }

    /**
     * Sets the services to be registered with the kernel during the main method.
     * @param services the services to be registered with the kernel during the main method
     */
    public void setServices(Map services) {
        this.services = services;
    }

    /**
     * Determines if the main method should hold the thread until the kernel is destroyed.
     * @return true if the main method should hold the thread until the kernel is destroyed; false otherwise
     */
    public boolean isDaemon() {
        return daemon;
    }

    /**
     * Sets the main method to hold the thread until the kernel is destroyed.
     * @param daemon true if the main method should hold the thread until the kernel is destroyed
     */
    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    /**
     * Gets the next main to call after the kernel has been initialized, but before destroying the kernel.
     * @return the next main to call after the kernel has been initialized
     */
    public Main getNext() {
        return next;
    }

    /**
     * Sets the next main to call after the kernel has been initialized.
     * @param next the next main to call after the kernel has been initialized
     */
    public void setNext(Main next) {
        this.next = next;
    }

    /**
     * Registers the services with the kernel, calls the next main, optionally holds the thread until the kernel is
     * destroyed, and then destroys the kernel.
     * @param args the arguments passed the next main
     */
    public void main(String[] args) {
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            // create a default kernel if necessary
            if (kernel == null) {
                kernel = KernelFactory.newInstance().createKernel(DEFAULT_KERNEL_NAME);
            }

            boolean failed = false;
            try {
                // bind the bootstrap services
                for (Iterator iterator = services.entrySet().iterator(); iterator.hasNext();) {
                    Map.Entry entry = (Map.Entry) iterator.next();
                    String name = (String) entry.getKey();
                    Object service = entry.getValue();

                    try {
                        ServiceName serviceName = new StringServiceName(name);
                        kernel.registerService(serviceName, new StaticServiceFactory(service), classLoader);
                        kernel.startService(serviceName);
                    } catch (Exception e) {
                        throw new FatalStartupError("Unable to bind bootstrap service '" + name + "' into the kernel", e);
                    }
                }

                // if we have a child main class call it
                if (next != null) {
                    next.main(args);
                }

                // if we are a daemon we wait here until the server stops
                if (daemon) {
                    // add our shutdown hook
                    Runtime.getRuntime().addShutdownHook(new DestroyKernelThread(kernel));

                    // wait for the kernel to be destroyed
                    kernel.waitForDestruction();
                }
            } catch (RuntimeException e) {
                failed = true;
                throw e;
            } catch (Error e) {
                failed = true;
                throw e;
            } finally {
                try {
                    kernel.destroy();
                } catch (Exception e) {
                    // if we are not alredy throwing an exception, throw a new exception
                    if (!failed) {
                        throw new FatalStartupError("Exception while shutting down kernel", e);
                    }
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private static class DestroyKernelThread extends Thread {
        private final Kernel kernel;

        private DestroyKernelThread(Kernel kernel) {
            super("Destroy Kernel Shutdown Hook");
            this.kernel = kernel;
        }

        public void run() {
            kernel.destroy();
        }
    }
}
