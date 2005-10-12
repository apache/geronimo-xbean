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
package org.xbean.kernel.standard;

import edu.emory.mathcs.backport.java.util.concurrent.Callable;
import edu.emory.mathcs.backport.java.util.concurrent.FutureTask;
import org.xbean.kernel.ServiceName;
import org.xbean.kernel.StopStrategy;

/**
 * RegistryFutureTask preforms service registration and unregistration in a FutureTask.
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
class RegistryFutureTask extends FutureTask implements Comparable {
    private final long serviceId;
    private final ServiceName serviceName;
    private final String taskType;
    private Throwable throwable;

    static RegistryFutureTask createRegisterTask(ServiceManager serviceManager) {
        RegisterCallable registerCallable = new RegisterCallable(serviceManager);
        RegistryFutureTask registryFutureTask = new RegistryFutureTask(serviceManager.getServiceId(),
                        serviceManager.getServiceName(),
                        "RegisterServiceManager",
                        registerCallable);
        return registryFutureTask;
    }

    static RegistryFutureTask createUnregisterTask(ServiceManager serviceManager, StopStrategy stopStrategy) {
        UnregisterCallable unregisterCallable = new UnregisterCallable(serviceManager, stopStrategy);
        RegistryFutureTask registryFutureTask = new RegistryFutureTask(serviceManager.getServiceId(),
                        serviceManager.getServiceName(),
                        "UnregisterServiceManager",
                        unregisterCallable);
        unregisterCallable.setRegistryFutureTask(registryFutureTask);
        return registryFutureTask;
    }

    private RegistryFutureTask(long serviceId, ServiceName serviceName, String taskType, Callable callable) {
        super(callable);
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.taskType = taskType;
    }

    public ServiceName getServiceName() {
        return serviceName;
    }

    public synchronized Throwable getThrowable() {
        return throwable;
    }

    private synchronized void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    public int hashCode() {
        return (int) (serviceId ^ (serviceId >>> 32));
    }

    public boolean equals(Object o) {
        if (o instanceof RegistryFutureTask) {
            return serviceId == ((RegistryFutureTask) o).serviceId;
        }
        return false;
    }

    public int compareTo(Object o) {
        RegistryFutureTask registryFutureTask = (RegistryFutureTask) o;

        if (serviceId < registryFutureTask.serviceId) {
            return -1;
        } else if (serviceId > registryFutureTask.serviceId) {
            return 1;
        } else {
            return 0;
        }
    }

    public String toString() {
        return "[RegistryFutureTask: task=" + taskType + ", serviceName=" + serviceName + "]";
    }


    private static class RegisterCallable implements Callable {
        private final ServiceManager serviceManager;

        private RegisterCallable(ServiceManager serviceManager) {
            this.serviceManager = serviceManager;
        }

        public Object call() throws Exception {
            serviceManager.initialize();
            return serviceManager;
        }
    }

    private static class UnregisterCallable implements Callable {
        private final ServiceManager serviceManager;
        private final StopStrategy stopStrategy;
        private RegistryFutureTask registryFutureTask;

        private UnregisterCallable(ServiceManager serviceManager, StopStrategy stopStrategy) {
            this.serviceManager = serviceManager;
            this.stopStrategy = stopStrategy;
        }

        public void setRegistryFutureTask(RegistryFutureTask registryFutureTask) {
            this.registryFutureTask = registryFutureTask;
        }

        public Object call() {
            try {
                serviceManager.destroy(stopStrategy);
                return null;
            } catch (Throwable e) {
                // Destroy failed, save the exception so it can be rethrown from the unregister method
                registryFutureTask.setThrowable(e);

                // return the service manager so the service remains registered
                return serviceManager;
            }
        }
    }
}
