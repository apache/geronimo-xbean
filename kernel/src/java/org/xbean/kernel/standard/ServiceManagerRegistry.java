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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.emory.mathcs.backport.java.util.concurrent.ExecutionException;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicLong;
import org.xbean.kernel.IllegalServiceStateException;
import org.xbean.kernel.KernelErrorsError;
import org.xbean.kernel.KernelOperationInterruptedException;
import org.xbean.kernel.ServiceAlreadyExistsException;
import org.xbean.kernel.ServiceFactory;
import org.xbean.kernel.ServiceName;
import org.xbean.kernel.ServiceNotFoundException;
import org.xbean.kernel.ServiceRegistrationException;
import org.xbean.kernel.StopStrategies;
import org.xbean.kernel.StopStrategy;
import org.xbean.kernel.UnsatisfiedConditionsException;

/**
 * The StandardServiceRegistry manages the registration of ServiceManagers for the kernel.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class ServiceManagerRegistry {
    /**
     * The sequence used for the serviceId assigned to service managers.
     */
    private final AtomicLong serviceId = new AtomicLong(1);

    /**
     * The factory used to create service managers.
     */
    private final ServiceManagerFactory serviceManagerFactory;

    /**
     * The registered service managers.
     */
    private final Map serviceManagers = new HashMap();

    /**
     * The service managers indexed by the service type.  This map is populated when a service enters the running state.
     */
    private final Map serviceManagersByType = new HashMap();

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
            RegistryFutureTask registryFutureTask = (RegistryFutureTask) iterator.next();
            try {
                managers.add(registryFutureTask.get());
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
        if (serviceName == null) throw new NullPointerException("serviceName is null");

        RegistryFutureTask registryFutureTask;
        synchronized (serviceManagers) {
            registryFutureTask = (RegistryFutureTask) serviceManagers.get(serviceName);
        }
        try {
            // the service is registered if we have a non-null future value
            return registryFutureTask != null && registryFutureTask.get() != null;
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
        if (serviceName == null) throw new NullPointerException("serviceName is null");

        RegistryFutureTask registryFutureTask;
        synchronized (serviceManagers) {
            registryFutureTask = (RegistryFutureTask) serviceManagers.get(serviceName);
        }

        // this service has no future
        if (registryFutureTask == null) {
            throw new ServiceNotFoundException(serviceName);
        }

        try {
            ServiceManager serviceManager = (ServiceManager) registryFutureTask.get();
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
     * Gets the first registered service manager that creates an instance of the specified type, or null if no service
     * managers create an instance of the specified type.
     *
     * @param type the of the desired service
     * @return the first registered service manager that creates an instance of the specified type, or null if none found
     */
    public ServiceManager getServiceManager(Class type) {
        SortedSet serviceManagerFutures = getServiceManagerFutures(type);
        for (Iterator iterator = serviceManagerFutures.iterator(); iterator.hasNext();) {
            RegistryFutureTask registryFutureTask = (RegistryFutureTask) iterator.next();
            try {
                ServiceManager serviceManager = (ServiceManager) registryFutureTask.get();
                if (serviceManager != null) {
                    return serviceManager;
                }
            } catch (InterruptedException e) {
                throw new KernelOperationInterruptedException(e, registryFutureTask.getServiceName(), "getServiceManagers(java.lang.Class)");
            } catch (ExecutionException ignored) {
                // registration threw an exception which means it didn't register
            }
        }
        return null;
    }

    /**
     * Gets all service managers that create an instances of the specified type, or an empty list if no service
     * managers create an instance of the specified type.
     *
     * @param type the of the desired service managers
     * @return all service managers that create an instances of the specified type, or an empty list if none found
     */
    public List getServiceManagers(Class type) {
        SortedSet serviceManagerFutures = getServiceManagerFutures(type);
        List serviceManagers = new ArrayList(serviceManagerFutures.size());
        for (Iterator iterator = serviceManagerFutures.iterator(); iterator.hasNext();) {
            RegistryFutureTask registryFutureTask = (RegistryFutureTask) iterator.next();
            try {
                ServiceManager serviceManager = (ServiceManager) registryFutureTask.get();
                if (serviceManager != null) {
                    serviceManagers.add(serviceManager);
                }
            } catch (InterruptedException e) {
                throw new KernelOperationInterruptedException(e, registryFutureTask.getServiceName(), "getServiceManagers(java.lang.Class)");
            } catch (ExecutionException ignored) {
                // registration threw an exception which means it didn't register
            }
        }
        return serviceManagers;
    }

    /**
     * Gets the first registed and running service that is an instance of the specified type, or null if no instances
     * of the specified type are running.
     *
     * @param type the of the desired service
     * @return the first registed and running service that is an instance of the specified type or null if none found
     */
    public synchronized Object getService(Class type) {
        SortedSet serviceManagerFutures = getServiceManagerFutures(type);
        for (Iterator iterator = serviceManagerFutures.iterator(); iterator.hasNext();) {
            RegistryFutureTask registryFutureTask = (RegistryFutureTask) iterator.next();
            try {
                ServiceManager serviceManager = (ServiceManager) registryFutureTask.get();
                if (serviceManager != null) {
                    Object service = serviceManager.getService();
                    if (service != null) {
                        return service;
                    }
                }
            } catch (InterruptedException e) {
                throw new KernelOperationInterruptedException(e, registryFutureTask.getServiceName(), "getService(java.lang.Class)");
            } catch (ExecutionException ignored) {
                // registration threw an exception which means it didn't register
            }
        }
        return null;
    }

    /**
     * Gets the all of running service that are an instances of the specified type, or an empty list if no instances
     * of the specified type are running.
     *
     * @param type the of the desired service
     * @return the all of running service that are an instances of the specified type, or an empty list if none found
     */
    public synchronized List getServices(Class type) {
        List serviceManagers = getServiceManagers(type);
        List services = new ArrayList(serviceManagers.size());
        for (Iterator iterator = serviceManagers.iterator(); iterator.hasNext();) {
            ServiceManager serviceManager = (ServiceManager) iterator.next();
            if (serviceManager != null) {
                Object service = serviceManager.getService();
                if (service != null) {
                    services.add(service);
                }
            }
        }
        return services;
    }

    private SortedSet getServiceManagerFutures(Class type) {
        SortedSet serviceManagerFutures;
        synchronized (serviceManagers) {
            serviceManagerFutures = (SortedSet) serviceManagersByType.get(type);
            if (serviceManagerFutures != null) {
                serviceManagerFutures = new TreeSet(serviceManagerFutures);
            } else {
                serviceManagerFutures = new TreeSet();
            }
        }
        return serviceManagerFutures;
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
        if (serviceName == null) throw new NullPointerException("serviceName is null");
        if (serviceFactory == null) throw new NullPointerException("serviceFactory is null");
        if (classLoader == null) throw new NullPointerException("classLoader is null");

        if (!serviceFactory.isEnabled()) {
            throw new ServiceRegistrationException(serviceName,
                    new IllegalServiceStateException("A disabled non-restartable service factory can not be registered", serviceName));
        }

        RegistryFutureTask registrationTask = null;

        //
        // This loop will continue until we put our registrationTask in the serviceManagers map.  If at any point,
        // we discover that there is already a service registered under the specified service name, we will throw
        // a ServiceAlreadyExistsException exiting this method.
        //
        while (registrationTask == null) {
            RegistryFutureTask existingRegistration;
            synchronized (serviceManagers) {
                existingRegistration = (RegistryFutureTask) serviceManagers.get(serviceName);

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
                    ServiceManager serviceManager = serviceManagerFactory.createServiceManager(serviceId.getAndIncrement(),
                            serviceName,
                            serviceFactory,
                            classLoader);
                    registrationTask = RegistryFutureTask.createRegisterTask(serviceManager);
                    serviceManagers.put(serviceName, registrationTask);
                    addTypeIndex(serviceManager, registrationTask);
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
                    removeTypeIndex(registrationTask);
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
        if (serviceName == null) throw new NullPointerException("serviceName is null");
        if (stopStrategy == null) throw new NullPointerException("stopStrategy is null");

        RegistryFutureTask unregistrationTask = null;

        //
        // This loop will continue until we put our unregistrationTask in the serviceManagers map.  If at any point,
        // we discover that there actually is not a service registered under the specified service name, we will throw
        // a ServiceNotFoundException exiting this method.
        //
        while (unregistrationTask == null) {
            RegistryFutureTask existingRegistration;
            synchronized (serviceManagers) {
                existingRegistration = (RegistryFutureTask) serviceManagers.get(serviceName);
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
                    unregistrationTask = RegistryFutureTask.createUnregisterTask(serviceManager, stopStrategy);
                    serviceManagers.put(serviceName, unregistrationTask);
                    addTypeIndex(serviceManager, unregistrationTask);
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
                        removeTypeIndex(unregistrationTask);
                    }
                }
            } else {
                synchronized (unregistrationTask) {
                    // the root exception is contained in the exception handle
                    throw new ServiceRegistrationException(serviceName, unregistrationTask.getThrowable());
                }
            }
        } catch (InterruptedException e) {
            throw new KernelOperationInterruptedException(e, serviceName, "unregisterService");
        } catch (ExecutionException e) {
            // this won't happen
            throw new AssertionError(e);
        }
    }

    private void addTypeIndex(ServiceManager serviceManager, RegistryFutureTask registryFutureTask) {
        if (serviceManager == null) throw new NullPointerException("serviceManager is null");
        if (registryFutureTask == null) throw new NullPointerException("serviceManagerFuture is null");

        Set allTypes = new LinkedHashSet();
        for (Iterator iterator = serviceManager.getServiceTypes().iterator(); iterator.hasNext();) {
            Class serviceType = (Class) iterator.next();

            if (serviceType.isArray()) {
                throw new IllegalArgumentException("Service is an array: serviceName=" + serviceManager.getServiceName() +
                        ", serviceType=" + serviceManager.getServiceTypes());
            }

            allTypes.add(serviceType);
            allTypes.addAll(getAllSuperClasses(serviceType));
            allTypes.addAll(getAllInterfaces(serviceType));
        }

        synchronized (serviceManagers) {
            for (Iterator iterator = allTypes.iterator(); iterator.hasNext();) {
                Class type = (Class) iterator.next();
                Set futureServiceManagers = (Set) serviceManagersByType.get(type);
                if (futureServiceManagers == null) {
                    futureServiceManagers = new TreeSet();
                    serviceManagersByType.put(type, futureServiceManagers);
                }
                futureServiceManagers.add(registryFutureTask);
            }
        }
    }

    private void removeTypeIndex(RegistryFutureTask registryFutureTask) {
        if (registryFutureTask == null) throw new NullPointerException("serviceManagerFuture is null");
        synchronized (serviceManagers) {
            for (Iterator iterator = serviceManagersByType.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry entry = (Map.Entry) iterator.next();
                Set serviceManagers = (Set) entry.getValue();
                serviceManagers.remove(registryFutureTask);
                if (serviceManagers.isEmpty()) {
                    iterator.remove();
                }
            }
        }
    }

    private static Set getAllSuperClasses(Class clazz) {
        Set allSuperClasses = new LinkedHashSet();
        for (Class superClass = clazz.getSuperclass(); superClass != null; superClass = superClass.getSuperclass()) {
            allSuperClasses.add(superClass);
        }
        return allSuperClasses;
    }

    private static Set getAllInterfaces(Class clazz) {
        Set allInterfaces = new LinkedHashSet();
        LinkedList stack = new LinkedList();
        stack.addAll(Arrays.asList(clazz.getInterfaces()));
        while (!stack.isEmpty()) {
            Class intf = (Class) stack.removeFirst();
            if (!allInterfaces.contains(intf)) {
                allInterfaces.add(intf);
                stack.addAll(Arrays.asList(intf.getInterfaces()));
            }
        }
        return allInterfaces;
    }
}
