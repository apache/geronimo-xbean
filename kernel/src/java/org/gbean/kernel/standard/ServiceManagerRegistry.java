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
package org.gbean.kernel.standard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.Callable;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutionException;
import edu.emory.mathcs.backport.java.util.concurrent.Future;
import edu.emory.mathcs.backport.java.util.concurrent.FutureTask;
import org.gbean.kernel.IllegalServiceStateException;
import org.gbean.kernel.KernelErrorsError;
import org.gbean.kernel.KernelOperationInterruptedException;
import org.gbean.kernel.ServiceAlreadyExistsException;
import org.gbean.kernel.ServiceFactory;
import org.gbean.kernel.ServiceName;
import org.gbean.kernel.ServiceNotFoundException;
import org.gbean.kernel.ServiceRegistrationException;
import org.gbean.kernel.StopStrategies;
import org.gbean.kernel.StopStrategy;
import org.gbean.kernel.UnsatisfiedConditionsException;

/**
 * The StandardServiceRegistry manages the registration of ServiceManagers for the kernel.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class ServiceManagerRegistry {
    /**
     * The factory used to create service managers.
     */
    private final ServiceManagerFactory serviceManagerFactory;

    /**
     * The registered service managers.
     */
    private final Map serviceManagers = new HashMap();

    /**
     * Creates a ServiceManagerRegistry that uses the specified service manager factory to create new service managers.
     *
     * @param serviceManagerFactory the factory for new service managers
     */
    public ServiceManagerRegistry(ServiceManagerFactory serviceManagerFactory) {
        this.serviceManagerFactory = serviceManagerFactory;
    }

    /**
     * Stops and destroys all services service managers.  This method will FORCE stop the services if necessary.
     *
     * @throws KernelErrorsError if any errors occur while stopping or destroying the service managers
     */
    public void destroy() throws KernelErrorsError {
        // we gather all errors that occur during shutdown and throw them as on huge exception
        List errors = new ArrayList();

        List managerFutures;
        synchronized (serviceManagers) {
            managerFutures = new ArrayList(serviceManagers.values());
            serviceManagers.clear();

        }

        List managers = new ArrayList(managerFutures.size());
        for (Iterator iterator = managerFutures.iterator(); iterator.hasNext();) {
            Future future = (Future) iterator.next();
            try {
                managers.add(future.get());
            } catch (InterruptedException e) {
                // ignore -- this should not happen
                errors.add(new AssertionError(e));
            } catch (ExecutionException e) {
                // good -- one less manager to deal with
            }
        }

        // Be nice and try to stop asynchronously
        errors.addAll(stopAll(managers, StopStrategies.ASYNCHRONOUS));

        // Be really nice and try to stop asynchronously again
        errors.addAll(stopAll(managers, StopStrategies.ASYNCHRONOUS));

        // We have been nice enough now nuke them
        errors.addAll(stopAll(managers, StopStrategies.FORCE));

        // All managers are gaurenteed to be destroyed now
        for (Iterator iterator = managers.iterator(); iterator.hasNext();) {
            ServiceManager serviceManager = (ServiceManager) iterator.next();
            try {
                serviceManager.destroy(StopStrategies.FORCE);
            } catch (UnsatisfiedConditionsException e) {
                // this should not happen, because we force stopped
                errors.add(new AssertionError(e));
            } catch (IllegalServiceStateException e) {
                // this should not happen, because we force stopped
                errors.add(new AssertionError(e));
            } catch (RuntimeException e) {
                errors.add(new AssertionError(e));
            } catch (Error e) {
                errors.add(new AssertionError(e));
            }
        }

        if (!errors.isEmpty()) {
            throw new KernelErrorsError(errors);
        }
    }

    private List stopAll(List managers, StopStrategy stopStrategy) {
        List errors = new ArrayList();
        for (Iterator iterator = managers.iterator(); iterator.hasNext();) {
            ServiceManager serviceManager = (ServiceManager) iterator.next();
            try {
                serviceManager.stop(stopStrategy);
            } catch (UnsatisfiedConditionsException e) {
                // this should not happen in with an asynchronous strategy
                errors.add(new AssertionError(e));
            } catch (RuntimeException e) {
                errors.add(new AssertionError(e));
            } catch (Error e) {
                errors.add(new AssertionError(e));
            }
        }
        return errors;
    }

    /**
     * Determines if there is a service registered under the specified name.
     *
     * @param serviceName the unique name of the service
     * @return true if there is a service registered with the specified name; false otherwise
     */
    public boolean isRegistered(ServiceName serviceName) {
        assert serviceName != null : "serviceName is null";

        Future serviceManagerFuture;
        synchronized (serviceManagers) {
            serviceManagerFuture = (Future) serviceManagers.get(serviceName);
        }
        try {
            // the service is registered if we have a non-null future value
            return serviceManagerFuture != null && serviceManagerFuture.get() != null;
        } catch (InterruptedException e) {
            throw new KernelOperationInterruptedException(e, serviceName, "isRegistered");
        } catch (ExecutionException e) {
            return false;
        }
    }

    /**
     * Gets the service manager registered under the specified name.
     *
     * @param serviceName the unique name of the service
     * @return the ServiceManager
     * @throws ServiceNotFoundException if there is no service registered under the specified name
     */
    public ServiceManager getServiceManager(ServiceName serviceName) throws ServiceNotFoundException {
        assert serviceName != null : "serviceName is null";

        Future serviceManagerFuture;
        synchronized (serviceManagers) {
            serviceManagerFuture = (Future) serviceManagers.get(serviceName);
        }

        // this service has no future
        if (serviceManagerFuture == null) {
            throw new ServiceNotFoundException(serviceName);
        }

        try {
            ServiceManager serviceManager = (ServiceManager) serviceManagerFuture.get();
            if (serviceManager == null) {
                throw new ServiceNotFoundException(serviceName);
            }
            return serviceManager;
        } catch (InterruptedException e) {
            throw new KernelOperationInterruptedException(e, serviceName, "getServiceManager");
        } catch (ExecutionException e) {
            // registration threw an exception which means it didn't register
            throw new ServiceNotFoundException(serviceName);
        }
    }

    /**
     * Creates a ServiceManager and registers it under the specified name.  If the service is restartable, it will
     * enter the server in the STOPPED state.  If a service is not restartable, the service manager will assure that all
     * dependencies are satisfied and service will immediately enter in the  RUNNING state.  If a
     * dependency for a non-restartable service is not immediately satisfiable, this method will throw a
     * ServiceRegistrationException.
     *
     * @param serviceName the unique name of the service
     * @param serviceFactory the factory used to create the service
     * @param classLoader the class loader to use for this service
     * @throws ServiceAlreadyExistsException if service is already registered with the specified name
     * @throws ServiceRegistrationException if the service is not restartable and an error occured while starting the service
     */
    public void registerService(ServiceName serviceName, ServiceFactory serviceFactory, ClassLoader classLoader) throws ServiceAlreadyExistsException, ServiceRegistrationException {
        assert serviceName != null : "serviceName is null";
        assert serviceFactory != null : "serviceFactory is null";
        assert classLoader != null : "classLoader is null";

        if (!serviceFactory.isEnabled()) {
            throw new ServiceRegistrationException(serviceName,
                    new IllegalServiceStateException("A disabled non-restartable service factory can not be registered", serviceName));
        }

        FutureTask registrationTask = null;

        //
        // This loop will continue until we put our registrationTask in the serviceManagers map.  If at any point,
        // we discover that there is already a service registered under the specified service name, we will throw
        // a ServiceAlreadyExistsException exiting this method.
        //
        while (registrationTask == null) {
            Future existingRegistration;
            synchronized (serviceManagers) {
                existingRegistration = (Future) serviceManagers.get(serviceName);

                // if we do not have an existing registration or the existing registration task is complete
                // we can create the new registration task; otherwise we need to wait for the existing registration to
                // finish out side of the synchronized lock on serviceManagers.
                if (existingRegistration == null || existingRegistration.isDone()) {
                    // if we have a valid existing registration, throw a ServiceAlreadyExistsException
                    if (existingRegistration != null) {
                        try {
                            boolean alreadyRegistered = (existingRegistration.get() != null);
                            if (alreadyRegistered) {
                                throw new ServiceAlreadyExistsException(serviceName);
                            }
                        } catch (InterruptedException e) {
                            throw new KernelOperationInterruptedException(e, serviceName, "registerService");
                        } catch (ExecutionException e) {
                            // the previous registration threw an exception.. we can continure as normal
                        }
                    }

                    // we are ready to register our serviceManager
                    existingRegistration = null;
                    registrationTask = new FutureTask(new RegisterServiceManager(serviceManagerFactory,
                            serviceName,
                            serviceFactory,
                            classLoader));
                    serviceManagers.put(serviceName, registrationTask);
                }
            }

            // If there is an unfinished exiting registration task, wait until it is done executing
            if (existingRegistration != null) {
                try {
                    existingRegistration.get();
                    // we don't throw an error here because we want to check in the synchronized block that this
                    // future is still registered in the serviceManagers map
                } catch (InterruptedException e) {
                    throw new KernelOperationInterruptedException(e, serviceName, "registerService");
                } catch (ExecutionException e) {
                    // good
                }
            }
        }

        // run our registration task and check the results
        registrationTask.run();
        try {
            // if initialization completed successfully, this method will not throw an exception
            registrationTask.get();
        } catch (InterruptedException e) {
            throw new KernelOperationInterruptedException(e, serviceName, "registerService");
        } catch (ExecutionException e) {
            // registration failed, remove our task
            synchronized (serviceManagers) {
                // make sure our task is still the registered one
                if (serviceManagers.get(serviceName) == registrationTask) {
                    serviceManagers.remove(serviceName);
                }
            }
            throw new ServiceRegistrationException(serviceName, e.getCause());
        }
    }

    /**
     * Stops and destorys the ServiceManager and then unregisters it.  The ServiceManagerRegistry will attempt to stop
     * the service using the specified stop strategy, but if the service can not  be stopped a
     * ServiceRegistrationException will be thrown containing either an UnsatisfiedConditionsException or an
     * IllegalServiceStateException.
     *
     * @param serviceName the unique name of the service
     * @param stopStrategy the strategy that determines how unsatisfied conditions are handled
     * @throws ServiceNotFoundException if there is no service registered under the specified name
     * @throws ServiceRegistrationException if the service could not be stopped
     */
    public void unregisterService(ServiceName serviceName, StopStrategy stopStrategy) throws ServiceNotFoundException, ServiceRegistrationException {
        assert serviceName != null : "serviceName is null";
        assert stopStrategy != null : "stopStrategy is null";

        FutureTask unregistrationTask = null;
        UnregisterServiceManager unregisterCallable = null;

        //
        // This loop will continue until we put our unregistrationTask in the serviceManagers map.  If at any point,
        // we discover that there actually is not a service registered under the specified service name, we will throw
        // a ServiceNotFoundException exiting this method.
        //
        while (unregistrationTask == null) {
            Future existingRegistration;
            synchronized (serviceManagers) {
                existingRegistration = (Future) serviceManagers.get(serviceName);
                if (existingRegistration == null) {
                    throw new ServiceNotFoundException(serviceName);
                }

                // if existing registration is done running, we can destroy it
                if (existingRegistration.isDone()) {
                    ServiceManager serviceManager = null;
                    try {
                        serviceManager = (ServiceManager) existingRegistration.get();
                    } catch (InterruptedException e) {
                        throw new KernelOperationInterruptedException(e, serviceName, "unregisterService");
                    } catch (ExecutionException e) {
                        // good
                    }

                    // if there isn't a registered manager that is an exception
                    if (serviceManager == null) {
                        throw new ServiceNotFoundException(serviceName);
                    }

                    // we are ready to register our serviceManager
                    existingRegistration = null;
                    unregisterCallable = new UnregisterServiceManager(serviceManager, stopStrategy);
                    unregistrationTask = new FutureTask(unregisterCallable);
                    serviceManagers.put(serviceName, unregistrationTask);
                }
            }


            // If there is an unfinished exiting registration task, wait until it is done executing
            if (existingRegistration != null) {
                try {
                    existingRegistration.get();
                    // we don't throw an error here because we want to check in the synchronized block that this
                    // future is still registered in the serviceManagers map
                } catch (InterruptedException e) {
                    throw new KernelOperationInterruptedException(e, serviceName, "unregisterService");
                } catch (ExecutionException e) {
                    // good
                }
            }
        }

        unregistrationTask.run();
        try {
            // if get returns any value other then null, the unregistration failed
            if (unregistrationTask.get() == null) {
                // unregistration was successful, remove the furuture object
                synchronized (serviceManagers) {
                    // make sure our task is still the registered one
                    if (serviceManagers.get(serviceName) == unregistrationTask) {
                        serviceManagers.remove(serviceName);
                    }
                }
            } else {
                synchronized (unregisterCallable) {
                    // the root exception is contained in the exception handle
                    throw new ServiceRegistrationException(serviceName, unregisterCallable.getThrowable());
                }
            }
        } catch (InterruptedException e) {
            throw new KernelOperationInterruptedException(e, serviceName, "unregisterService");
        } catch (ExecutionException e) {
            // this won't happen
            throw new AssertionError(e);
        }
    }

    private static class RegisterServiceManager implements Callable {
        private final ServiceManagerFactory serviceManagerFactory;
        private final ServiceName serviceName;
        private final ServiceFactory serviceFactory;
        private final ClassLoader classLoader;

        private RegisterServiceManager(ServiceManagerFactory serviceManagerFactory, ServiceName serviceName, ServiceFactory serviceFactory, ClassLoader classLoader) {
            this.serviceManagerFactory = serviceManagerFactory;
            this.serviceName = serviceName;
            this.serviceFactory = serviceFactory;
            this.classLoader = classLoader;
        }

        public Object call() throws Exception {
            ServiceManager serviceManager = serviceManagerFactory.createServiceManager(serviceName, serviceFactory, classLoader);
            serviceManager.initialize();
            return serviceManager;
        }
    }

    private static class UnregisterServiceManager implements Callable {
        private final ServiceManager serviceManager;
        private final StopStrategy stopStrategy;
        private Throwable throwable;

        private UnregisterServiceManager(ServiceManager serviceManager, StopStrategy stopStrategy) {
            this.serviceManager = serviceManager;
            this.stopStrategy = stopStrategy;
        }

        public Object call() {
            try {
                serviceManager.destroy(stopStrategy);
                return null;
            } catch (Throwable e) {
                // Destroy failed, save the exception so it can be rethrown from the unregister method
                synchronized (this) {
                    throwable = e;
                }
                // return the service manager so the service remains registered
                return serviceManager;
            }
        }

        private synchronized Throwable getThrowable() {
            return throwable;
        }
    }
}
