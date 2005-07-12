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
package org.gbean.kernel;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.management.ObjectName;

import org.gbean.kernel.simple.SimpleServiceFactory;
import org.gbean.loader.LoaderUtil;
import org.gbean.spring.FatalStartupError;

/**
 * @version $Revision$ $Date$
 */
public class KernelMain implements Main {
    private static final String DEFAULT_KERNEL_NAME = "gbean";

    private Kernel kernel;
    private ClassLoader classLoader;
    private Map services = Collections.EMPTY_MAP;
    private List locations = Collections.EMPTY_LIST;
    private boolean daemon = true;
    private Main next;

    public Kernel getKernel() {
        return kernel;
    }

    public void setKernel(Kernel kernel) {
        this.kernel = kernel;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public Map getServices() {
        return services;
    }

    public void setServices(Map services) {
        this.services = services;
    }

    public List getLocations() {
        return locations;
    }

    public void setLocations(List locations) {
        this.locations = locations;
    }

    public boolean isDaemon() {
        return daemon;
    }

    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    public Main getNext() {
        return next;
    }

    public void setNext(Main next) {
        this.next = next;
    }

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

            // boot the kernel
            try {
                kernel.boot();
            } catch (Exception e) {
                throw new FatalStartupError("Unable to boot the kernel", e);
            }

            // add our shutdown hook
            kernel.registerShutdownHook(new Runnable() {
                public void run() {
                    // bind the bootstrap services
                    for (Iterator iterator = services.keySet().iterator(); iterator.hasNext();) {
                        String name = (String) iterator.next();
                        try {
                            ObjectName objectName = new ObjectName(name);
                            kernel.stopService(objectName);
                            kernel.unloadService(objectName);
                        } catch (Exception e) {
                            // igore -- a real exception will be logged by the kernel
                        }
                    }
                }
            });

            boolean failed = false;
            try {
                // bind the bootstrap services
                for (Iterator iterator = services.entrySet().iterator(); iterator.hasNext();) {
                    Map.Entry entry = (Map.Entry) iterator.next();
                    String name = (String) entry.getKey();
                    Object service = entry.getValue();

                    try {
                        ObjectName objectName = new ObjectName(name);
                        kernel.loadService(objectName, new SimpleServiceFactory(service), classLoader);
                        kernel.startService(objectName);
                    } catch (Exception e) {
                        throw new FatalStartupError("Unable to bind bootstrap service '" + name + "' into the kernel", e);
                    }
                }

                // verify that all bootstrap services started successfully
                LoaderUtil.verifyAllServicesRunning(kernel);


                // load each location and verify that all services started successfully
                for (Iterator iterator = locations.iterator(); iterator.hasNext();) {
                    String location = (String) iterator.next();
                    LoaderUtil.load(kernel, location);
                    LoaderUtil.verifyAllServicesRunning(kernel);
                }

                // if we have a child main class call it
                if (next != null) {
                    next.main(args);
                }

                // if we are a daemon we wait here until the server stops
                if (daemon) {
                    // add our shutdown hook
                    Runtime.getRuntime().addShutdownHook(new Thread("Shutdown Thread") {
                        public void run() {
                            kernel.shutdown();
                        }
                    });

                    while (kernel.isRunning()) {
                        try {
                            // wait for the kernel to be ready to exit
                            synchronized (kernel) {
                                kernel.wait();
                            }
                        } catch (InterruptedException e) {
                            // ignore - we check the variable above
                        }
                    }
                }
            } catch (RuntimeException e) {
                failed = true;
                throw e;
            } catch (Error e) {
                failed = true;
                throw e;
            } finally {
                try {
                    kernel.shutdown();
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
}
