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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;
import java.io.Serializable;

import edu.emory.mathcs.backport.java.util.concurrent.Callable;
import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutionException;
import edu.emory.mathcs.backport.java.util.concurrent.FutureTask;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import junit.framework.TestCase;
import org.xbean.kernel.IllegalServiceStateException;
import org.xbean.kernel.KernelErrorsError;
import org.xbean.kernel.ServiceAlreadyExistsException;
import org.xbean.kernel.ServiceFactory;
import org.xbean.kernel.ServiceName;
import org.xbean.kernel.ServiceNotFoundException;
import org.xbean.kernel.ServiceRegistrationException;
import org.xbean.kernel.StaticServiceFactory;
import org.xbean.kernel.StopStrategies;
import org.xbean.kernel.StopStrategy;
import org.xbean.kernel.StringServiceName;
import org.xbean.kernel.UnsatisfiedConditionsException;
import org.xbean.kernel.NullServiceMonitor;

/**
 * Test the ServiceManagerRegistry.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class ServiceManagerRegistryTest extends TestCase {
    private static final int TIMEOUT_DURATION = 5;
    private static final TimeUnit TIMEOUT_UNITS = TimeUnit.SECONDS;

    private static final StringServiceName SERVICE_NAME = new StringServiceName("Service");
    private static final StaticServiceFactory SERVICE_FACTORY = new StaticServiceFactory(new Object());
    private static final ClassLoader CLASS_LOADER = new URLClassLoader(new URL[0]);
    private static final Class[] EXPECTED_TYPES =  new Class[] {
       TreeSet.class,
       AbstractSet.class,
       AbstractCollection.class,
       Object.class,
       SortedSet.class,
       Set.class,
       Collection.class,
       Cloneable.class,
       Serializable.class,
       List.class
    };
    private final MockServiceManager serviceManager = new MockServiceManager();
    private final MockServiceManagerFactory serviceManagerFactory = new MockServiceManagerFactory();
    private final ServiceManagerRegistry registry = new ServiceManagerRegistry(serviceManagerFactory);

    /**
     * Tests the initial state of the registry.
     */
    public void testInitialState() {
        assertFalse(registry.isRegistered(SERVICE_NAME));
        try {
            assertNull(registry.getServiceManager(SERVICE_NAME));
            fail("should have thrown an exception");
        } catch (ServiceNotFoundException expected) {
            // expected
            assertEquals(SERVICE_NAME, expected.getServiceName());
        }
        assertFalse(serviceManager.isInitializeCalled());
        assertFalse(serviceManager.isDestroyCalled());
    }

    /**
     * Test the registration and unregistration.
     * Strategy:
     * <ul><li>
     * Register a service
     * </li><li>
     * Verify the service was registered and callbacks were made
     * <ul><li>
     * Unregister the service
     * </li><li>
     * Verify the service was unregistered and callbacks were made
     * </li></ul>
     *
     * @throws Exception if there is a failure
     */
    public void testRegisterUnregister() throws Exception {
        register();
        unregister();
    }

    /**
     * Tests the destroy method.
     * Strategy:
     * <ul><li>
     * Register a service
     * </li><li>
     * Verify the service was registered and callbacks were made
     * <ul><li>
     * Destroy the registry
     * </li><li>
     * Verify the service was stopped and callbacks were made
     * </li></ul>
     *
     * @throws Exception if there is a failure
     */
    public void testDestroy() throws Exception {
        register();
        destroy();
    }

    /**
     * Tests that an exception is thrown if an attempt is made to register.
     *
     * @throws Exception if there is a failure
     */
    public void testDoubleRegister() throws Exception {
        register();
        try {
            registry.registerService(SERVICE_NAME, SERVICE_FACTORY, CLASS_LOADER);
            fail("should have thrown an exception");
        } catch (ServiceAlreadyExistsException expected) {
            // expected
            assertEquals(SERVICE_NAME, expected.getServiceName());
        }
    }

    /**
     * Test that an attempt to unregister a service that is not registered throws an exception.
     *
     * @throws Exception if there is a failure
     */
    public void testUnregisterUnknown() throws Exception {
        try {
            registry.unregisterService(SERVICE_NAME, StopStrategies.SYNCHRONOUS);
        } catch (ServiceNotFoundException expected) {
            // expected
            assertEquals(SERVICE_NAME, expected.getServiceName());
        }
    }

    /**
     * Tests that when the initialize method throws an exception the service is not registered.
     *
     * @throws Exception if there is a failure
     */
    public void testRegisterException() throws Exception {
        register(new Exception("register exception"));
        register(new RuntimeException("register runtime exception"));
        register(new Error("register error"));
        register();
        unregister();
    }

    /**
     * Tests that when the destroy method throws an exception the service is not unregistered.
     *
     * @throws Exception if there is a failure
     */
    public void testUnregisterException() throws Exception {
        register();
        unregister(new UnsatisfiedConditionsException("destroy exception", SERVICE_NAME, Collections.EMPTY_SET));
        unregister(new IllegalServiceStateException("destroy exception", SERVICE_NAME));
        unregister(new RuntimeException("destroy exception"));
        unregister(new Error("destroy exception"));
        unregister();
    }

    /**
     * Tests that when the destroy and/or stop methods throw an exception during registry destroy, that destruction
     * continues and the exceptions are thrown in a single KernelErrorsError.
     *
     * @throws Exception if there is a failure
     */
    public void testDestroyException() throws Exception {
        register();
        destroy(new UnsatisfiedConditionsException("destroy exception", SERVICE_NAME, Collections.EMPTY_SET), null);
        register();
        destroy(new RuntimeException("destroy exception"), null);
        register();
        destroy(new Error("destroy exception"), null);
        register();
        destroy(new UnsatisfiedConditionsException("destroy exception", SERVICE_NAME, Collections.EMPTY_SET), new UnsatisfiedConditionsException("destroy exception", SERVICE_NAME, Collections.EMPTY_SET));
        register();
        destroy(new RuntimeException("destroy exception"), new UnsatisfiedConditionsException("destroy exception", SERVICE_NAME, Collections.EMPTY_SET));
        register();
        destroy(new Error("destroy exception"), new UnsatisfiedConditionsException("destroy exception", SERVICE_NAME, Collections.EMPTY_SET));
        register();
        destroy(null, new UnsatisfiedConditionsException("destroy exception", SERVICE_NAME, Collections.EMPTY_SET));
        register();
        destroy(new UnsatisfiedConditionsException("destroy exception", SERVICE_NAME, Collections.EMPTY_SET), new IllegalServiceStateException("destroy exception", SERVICE_NAME));
        register();
        destroy(new RuntimeException("destroy exception"), new IllegalServiceStateException("destroy exception", SERVICE_NAME));
        register();
        destroy(new Error("destroy exception"), new IllegalServiceStateException("destroy exception", SERVICE_NAME));
        register();
        destroy(null, new IllegalServiceStateException("destroy exception", SERVICE_NAME));
        register();
        destroy(new UnsatisfiedConditionsException("destroy exception", SERVICE_NAME, Collections.EMPTY_SET), new RuntimeException("destroy exception"));
        register();
        destroy(new RuntimeException("destroy exception"), new RuntimeException("destroy exception"));
        register();
        destroy(new Error("destroy exception"), new RuntimeException("destroy exception"));
        register();
        destroy(null, new RuntimeException("destroy exception"));
        register();
        destroy(new UnsatisfiedConditionsException("destroy exception", SERVICE_NAME, Collections.EMPTY_SET), new Error("destroy exception"));
        register();
        destroy(new RuntimeException("destroy exception"), new Error("destroy exception"));
        register();
        destroy(new Error("destroy exception"), new Error("destroy exception"));
        register();
        destroy(null, new Error("destroy exception"));
    }

    /**
     * Tests that when a service manager blocks during registration, that the registry blocks isRegistered and
     * getServiceManager calls until the registration completes.
     *
     * @throws Exception if there is a failure
     */
    public void testRegisterWaiting() throws Exception {
        registerWaiting(null);
    }

    /**
     * Tests that when a service manager blocks during registration and then throws an exception, that the registry
     * blocks isRegistered and getServiceManager calls until the registration completes, and then returns the correct
     * values for an unregistered service.
     *
     * @throws Exception if there is a failure
     */
    public void testRegisterWaitingException() throws Exception {
        registerWaiting(new Exception("register exception"));
        registerWaiting(new RuntimeException("register runtime exception"));
        registerWaiting(new Error("register error"));
    }

    /**
     * Tests that when a blocking service throws an exception during registration a nother service can wait be waiting
     * to take over the registration from the failed thread.
     *
     * @throws Exception if there is a failure
     */
    public void testDoubleRegisterWaiting() throws Exception {
        // start thread to attempt to register but throw an exception
        FutureTask registerFailTask = registerWaitingTask(new Exception("register exception"));

        // start thread to successfully register
        final CountDownLatch registerStartedSignal = new CountDownLatch(1);
        final MockServiceManager newServiceManager = new MockServiceManager();
        newServiceManager.setWait();
        FutureTask registerTask = new FutureTask(new Callable() {
            public Object call() throws Exception {
                registerStartedSignal.countDown();
                register(null, newServiceManager);
                return Boolean.TRUE;
            }
        });
        Thread registerThread = new Thread(registerTask, "registerTask");
        registerThread.setDaemon(true);
        registerThread.start();
        registerStartedSignal.await(TIMEOUT_DURATION, TIMEOUT_UNITS);

        // sleep a bit to assure that the register thread entered the registry code
        Thread.sleep(100);

        // verify all are not done
        assertFalse(registerFailTask.isDone());
        assertFalse(registerTask.isDone());

        // finish register fail, and verify it failed
        serviceManager.signalExit();
        assertEquals(Boolean.FALSE, registerFailTask.get(TIMEOUT_DURATION, TIMEOUT_UNITS));

        // verify success registration and verify itworked
        newServiceManager.signalExit();
        assertEquals(Boolean.TRUE, registerTask.get(TIMEOUT_DURATION, TIMEOUT_UNITS));
        assertTrue(registry.isRegistered(SERVICE_NAME));
        assertEquals(newServiceManager, registry.getServiceManager(SERVICE_NAME));
    }

    /**
     * Tests that when a service manager blocks during unregistration, that the registry blocks isRegistered and
     * getServiceManager calls until the unregistration completes.
     *
     * @throws Exception if there is a failure
     */
    public void testUnregisterWaiting() throws Exception {
        register();
        unregisterWaiting(null);
    }

    /**
     * Tests that when a service manager blocks during unregistration and then throws an exception, that the registry
     * blocks isRegistered and getServiceManager calls until the unregistration completes, and then returns the correct
     * values for an unregistered service.
     *
     * @throws Exception if there is a failure
     */
    public void testUnregisterWaitingException() throws Exception {
        register();
        unregisterWaiting(new UnsatisfiedConditionsException("destroy exception", SERVICE_NAME, Collections.EMPTY_SET));
        unregisterWaiting(new IllegalServiceStateException("destroy exception", SERVICE_NAME));
        unregisterWaiting(new RuntimeException("destroy exception"));
        unregisterWaiting(new Error("destroy exception"));
        unregisterWaiting(null);
    }

    /**
     * Tests that when a service manager blocks during destroy, that the registry does not block isRegistered and
     * getServiceManager calls, and then returns the correct values for an unregistered service.
     *
     * @throws Exception if there is a failure
     */
    public void testDestroyWaiting() throws Exception {
        register();

        // start thread to destroy and wait
        FutureTask destroyTask = destroyWaitingTask();

        // verify all are not done
        assertFalse(destroyTask.isDone());

        // verify that the service already appears to be unregistered
        assertFalse(registry.isRegistered(SERVICE_NAME));
        try {
            assertNull(registry.getServiceManager(SERVICE_NAME));
            fail("should have thrown an exception");
        } catch (ServiceNotFoundException expected) {
            // expected
            assertEquals(SERVICE_NAME, expected.getServiceName());
        }

        // finish register
        serviceManager.signalExit();

        // verify registration worked
        assertEquals(Boolean.TRUE, destroyTask.get(TIMEOUT_DURATION, TIMEOUT_UNITS));
        assertFalse(registry.isRegistered(SERVICE_NAME));
        try {
            assertNull(registry.getServiceManager(SERVICE_NAME));
            fail("should have thrown an exception");
        } catch (ServiceNotFoundException expected) {
            // expected
            assertEquals(SERVICE_NAME, expected.getServiceName());
        }
    }

    private void registerWaiting(Throwable throwable) throws Exception {
        // start thread to register and wait
        FutureTask registerTask = registerWaitingTask(throwable);

        // start thread to attempt getService
        FutureTask getServiceManagerTask = getServiceWaiting();

        // start thread to attempt isRegistered
        FutureTask isRegisteredTask = isRegisteredWaiting();

        // not necessary, but sleep a bit anyway
        Thread.sleep(100);

        // verify all are not done
        assertFalse(registerTask.isDone());
        assertFalse(getServiceManagerTask.isDone());
        assertFalse(isRegisteredTask.isDone());

        // finish register
        serviceManager.signalExit();

        if (throwable == null) {
            // verify registration worked
            assertEquals(Boolean.TRUE, registerTask.get(TIMEOUT_DURATION, TIMEOUT_UNITS));
            assertTrue(registry.isRegistered(SERVICE_NAME));
            assertEquals(serviceManager, registry.getServiceManager(SERVICE_NAME));

            // verify waiting isRegistered worked
            assertEquals(Boolean.TRUE, isRegisteredTask.get(TIMEOUT_DURATION, TIMEOUT_UNITS));

            // verify getServiceManager worked
            assertEquals(serviceManager, getServiceManagerTask.get(TIMEOUT_DURATION, TIMEOUT_UNITS));
        } else {
            // verify registration failed
            assertEquals(Boolean.FALSE, registerTask.get(TIMEOUT_DURATION, TIMEOUT_UNITS));
            assertFalse(registry.isRegistered(SERVICE_NAME));
            try {
                assertNull(registry.getServiceManager(SERVICE_NAME));
                fail("should have thrown an exception");
            } catch (ServiceNotFoundException expected) {
                // expected
                assertEquals(SERVICE_NAME, expected.getServiceName());
            }

            // verify waiting isRegistered worked
            assertEquals(Boolean.FALSE, isRegisteredTask.get(TIMEOUT_DURATION, TIMEOUT_UNITS));

            // verify getServiceManager worked
            try {
                getServiceManagerTask.get(TIMEOUT_DURATION, TIMEOUT_UNITS);
                fail("should have thrown an exception");
            } catch (ExecutionException e) {
                assertTrue(e.getCause() instanceof ServiceNotFoundException);
                ServiceNotFoundException serviceNotFoundException = (ServiceNotFoundException) e.getCause();
                assertSame(SERVICE_NAME, serviceNotFoundException.getServiceName());
            }
        }
    }

    private FutureTask registerWaitingTask(final Throwable throwable) throws InterruptedException {
        serviceManager.setWait();
        FutureTask registerTask = new FutureTask(new Callable() {
            public Object call() throws Exception {
                register(throwable);
                return Boolean.valueOf(throwable == null);
            }
        });
        Thread registerThread = new Thread(registerTask, throwable == null ? "registerTask" : "registerExceptionTask");
        registerThread.setDaemon(true);
        registerThread.start();

        // wait for register to block
        assertTrue(serviceManager.awaitEnterSignal());
        return registerTask;
    }

    private void unregisterWaiting(Throwable throwable) throws Exception {
        // start thread to unregister and wait
        FutureTask unregisterTask = unregisterWaitingTask(throwable);

        // start thread to attempt getService
        FutureTask getServiceManagerTask = getServiceWaiting();

        // start thread to attempt isRegistered
        FutureTask isRegisteredTask = isRegisteredWaiting();

        // not necessary, but sleep a bit anyway
        Thread.sleep(100);

        // verify all are not done
        assertFalse(unregisterTask.isDone());
        assertFalse(getServiceManagerTask.isDone());
        assertFalse(isRegisteredTask.isDone());

        // finish register
        serviceManager.signalExit();

        if (throwable == null) {
            // verify registration worked
            assertEquals(Boolean.TRUE, unregisterTask.get(TIMEOUT_DURATION, TIMEOUT_UNITS));
            assertFalse(registry.isRegistered(SERVICE_NAME));
            try {
                assertNull(registry.getServiceManager(SERVICE_NAME));
                fail("should have thrown an exception");
            } catch (ServiceNotFoundException expected) {
                // expected
                assertEquals(SERVICE_NAME, expected.getServiceName());
            }

            // verify waiting isRegistered worked
            assertEquals(Boolean.FALSE, isRegisteredTask.get(TIMEOUT_DURATION, TIMEOUT_UNITS));

            // verify getServiceManager worked
            try {
                getServiceManagerTask.get(TIMEOUT_DURATION, TIMEOUT_UNITS);
                fail("should have thrown an exception");
            } catch (ExecutionException e) {
                assertTrue(e.getCause() instanceof ServiceNotFoundException);
                ServiceNotFoundException serviceNotFoundException = (ServiceNotFoundException) e.getCause();
                assertSame(SERVICE_NAME, serviceNotFoundException.getServiceName());
            }
        } else {
            // verify unregistration failed
            assertEquals(Boolean.FALSE, unregisterTask.get(TIMEOUT_DURATION, TIMEOUT_UNITS));
            assertTrue(registry.isRegistered(SERVICE_NAME));
            assertEquals(serviceManager, registry.getServiceManager(SERVICE_NAME));

            // verify waiting isRegistered worked
            assertEquals(Boolean.TRUE, isRegisteredTask.get(TIMEOUT_DURATION, TIMEOUT_UNITS));

            // verify getServiceManager worked
            assertEquals(serviceManager, getServiceManagerTask.get(TIMEOUT_DURATION, TIMEOUT_UNITS));
        }
    }

    private FutureTask unregisterWaitingTask(final Throwable throwable) throws InterruptedException {
        serviceManager.setWait();
        FutureTask unregisterTask = new FutureTask(new Callable() {
            public Object call() throws Exception {
                unregister(throwable);
                return Boolean.valueOf(throwable == null);
            }
        });
        Thread unregisterThread = new Thread(unregisterTask, throwable == null ? "unregisterTask" : "unregisterExceptionTask");
        unregisterThread.setDaemon(true);
        unregisterThread.start();

        // wait for register to block
        assertTrue(serviceManager.awaitEnterSignal());
        return unregisterTask;
    }

    private FutureTask destroyWaitingTask() throws InterruptedException {
        serviceManager.setWait();
        FutureTask unregisterTask = new FutureTask(new Callable() {
            public Object call() throws Exception {
                destroy();
                return Boolean.TRUE;
            }
        });
        Thread destroyThread = new Thread(unregisterTask, "destroyTask");
        destroyThread.setDaemon(true);
        destroyThread.start();

        // wait for register to block
        assertTrue(serviceManager.awaitEnterSignal());
        return unregisterTask;
    }

    private FutureTask getServiceWaiting() throws InterruptedException {
        final CountDownLatch getServiceManagerSignal = new CountDownLatch(1);
        FutureTask getServiceManagerTask = new FutureTask(new Callable() {
            public Object call() throws Exception {
                getServiceManagerSignal.countDown();
                return registry.getServiceManager(SERVICE_NAME);
            }
        });
        Thread getServiceManagerThread = new Thread(getServiceManagerTask, "getServiceManagerTask");
        getServiceManagerThread.setDaemon(true);
        getServiceManagerThread.start();

        // wait for thread started
        getServiceManagerSignal.await(TIMEOUT_DURATION, TIMEOUT_UNITS);
        return getServiceManagerTask;
    }

    private FutureTask isRegisteredWaiting() throws InterruptedException {
        final CountDownLatch isRegisteredSignal = new CountDownLatch(1);
        FutureTask isRegisteredTask = new FutureTask(new Callable() {
            public Object call() throws Exception {
                isRegisteredSignal.countDown();
                return Boolean.valueOf(registry.isRegistered(SERVICE_NAME));
            }
        });
        Thread isRegisteredThread = new Thread(isRegisteredTask, "isRegisteredTask");
        isRegisteredThread.setDaemon(true);
        isRegisteredThread.start();

        // wait for thread started
        isRegisteredSignal.await(TIMEOUT_DURATION, TIMEOUT_UNITS);
        return isRegisteredTask;
    }

    private void register() throws Exception {
        register(null, serviceManager);
    }

    private void register(Throwable throwable) throws Exception {
        register(throwable, serviceManager);
    }

    private void register(Throwable throwable, MockServiceManager serviceManager) throws Exception {
        serviceManager.reset();
        serviceManager.setInitializeException(throwable);
        serviceManagerFactory.addServiceManager(serviceManager);
        try {
            registry.registerService(SERVICE_NAME, SERVICE_FACTORY, CLASS_LOADER);
            assertNull(throwable);
        } catch (ServiceRegistrationException expected) {
            // expected
            assertSame(throwable, expected.getCause());
            assertSame(SERVICE_NAME, expected.getServiceName());
        }

        if (throwable == null) {
            assertTrue(registry.isRegistered(SERVICE_NAME));
            assertSame(serviceManager, registry.getServiceManager(SERVICE_NAME));
            for (int i = 0; i < EXPECTED_TYPES.length; i++) {
                assertSame(serviceManager, registry.getServiceManager(EXPECTED_TYPES[i]));
                assertTrue(registry.getServiceManagers(EXPECTED_TYPES[i]).contains(serviceManager));
            }
        } else {
            assertFalse(registry.isRegistered(SERVICE_NAME));
            try {
                assertNull(registry.getServiceManager(SERVICE_NAME));
                fail("should have thrown an exception");
            } catch (ServiceNotFoundException expected) {
                // expected
                assertEquals(SERVICE_NAME, expected.getServiceName());
            }
            for (int i = 0; i < EXPECTED_TYPES.length; i++) {
                assertNull(registry.getServiceManager(EXPECTED_TYPES[i]));
                assertTrue(registry.getServiceManagers(EXPECTED_TYPES[i]).isEmpty());
            }
        }
        assertTrue(serviceManager.isInitializeCalled());
        assertFalse(serviceManager.isDestroyCalled());
    }

    private void unregister() throws ServiceNotFoundException {
        unregister(null);
    }

    private void unregister(Throwable throwable) throws ServiceNotFoundException {
        serviceManager.reset();
        serviceManager.setDestroyException(throwable);
        try {
            registry.unregisterService(SERVICE_NAME, StopStrategies.SYNCHRONOUS);
            assertNull(throwable);
        } catch (ServiceRegistrationException expected) {
            // expected
            assertSame(SERVICE_NAME, expected.getServiceName());
            assertSame(throwable, expected.getCause());
        }

        if (throwable == null) {
            assertFalse(registry.isRegistered(SERVICE_NAME));
            try {
                assertNull(registry.getServiceManager(SERVICE_NAME));
                fail("should have thrown an exception");
            } catch (ServiceNotFoundException expected) {
                // expected
                assertEquals(SERVICE_NAME, expected.getServiceName());
            }
            for (int i = 0; i < EXPECTED_TYPES.length; i++) {
                assertNull(registry.getServiceManager(EXPECTED_TYPES[i]));
                assertTrue(registry.getServiceManagers(EXPECTED_TYPES[i]).isEmpty());
            }
        } else {
            assertTrue(registry.isRegistered(SERVICE_NAME));
            assertSame(serviceManager, registry.getServiceManager(SERVICE_NAME));
            for (int i = 0; i < EXPECTED_TYPES.length; i++) {
                assertSame(serviceManager, registry.getServiceManager(EXPECTED_TYPES[i]));
                assertTrue(registry.getServiceManagers(EXPECTED_TYPES[i]).contains(serviceManager));
            }
        }
        assertFalse(serviceManager.isInitializeCalled());
        assertTrue(serviceManager.isDestroyCalled());
    }

    private void destroy() {
        destroy(null, null);
    }

    private void destroy(Throwable stopException, Throwable destroyException) {
        serviceManager.reset();
        serviceManager.setStopException(stopException);
        serviceManager.setDestroyException(destroyException);

        try {
            registry.destroy();
            assertNull(stopException);
            assertNull(destroyException);
        } catch (KernelErrorsError kernelErrorsError) {
            List errors = new ArrayList(kernelErrorsError.getErrors());
            if (stopException != null) {
                assertTrue(errors.size() >= 3);
                assertTrue(errors.get(0) instanceof AssertionError);
                assertSame(stopException, ((AssertionError) errors.get(0)).getCause());
                assertTrue(errors.get(1) instanceof AssertionError);
                assertSame(stopException, ((AssertionError) errors.get(1)).getCause());
                assertTrue(errors.get(2) instanceof AssertionError);
                assertSame(stopException, ((AssertionError) errors.get(2)).getCause());
                errors = errors.subList(3, errors.size());
            }

            if (destroyException != null) {
                assertEquals(1, errors.size());
                assertTrue(errors.get(0) instanceof AssertionError);
                assertSame(destroyException, ((AssertionError) errors.get(0)).getCause());
                errors = Collections.EMPTY_LIST;
            }

            assertEquals(Collections.EMPTY_LIST, errors);
        }

        assertFalse(registry.isRegistered(SERVICE_NAME));
        try {
            assertNull(registry.getServiceManager(SERVICE_NAME));
            fail("should have thrown an exception");
        } catch (ServiceNotFoundException expected) {
            // expected
            assertEquals(SERVICE_NAME, expected.getServiceName());
        }
        assertFalse(serviceManager.isInitializeCalled());
        assertTrue(serviceManager.isStopCalled());
        assertTrue(serviceManager.isDestroyCalled());
    }

    private static class MockServiceManagerFactory extends ServiceManagerFactory {
        private final LinkedList serviceManagers = new LinkedList();

        private MockServiceManagerFactory() {
            super(null, null, null, 0, null);
        }

        public ServiceManager createServiceManager(long serviceId, ServiceName serviceName, ServiceFactory serviceFactory, ClassLoader classLoader) {
            assertEquals(SERVICE_NAME, serviceName);
            assertEquals(SERVICE_FACTORY, serviceFactory);
            assertEquals(CLASS_LOADER, classLoader);
            synchronized (serviceManagers) {
                return (ServiceManager) serviceManagers.removeFirst();
            }
        }

        public void addServiceManager(ServiceManager serviceManager) {
            synchronized (serviceManagers) {
                serviceManagers.add(serviceManager);
            }
        }
    }

    private static class MockServiceManager extends ServiceManager {
        private boolean initializeCalled;
        private boolean destroyCalled;
        private boolean stopCalled;
        private Throwable initializeException;
        private Throwable destroyException;
        private Throwable stopException;
        private CountDownLatch enterWaiting = new CountDownLatch(1);
        private CountDownLatch exitWaiting = new CountDownLatch(0);
        private static final Set TYPES = Collections.unmodifiableSet(new HashSet(Arrays.asList(
                new Class[] {TreeSet.class, List.class} )));


        private MockServiceManager() {
            super(null,
                    0,
                    new StringServiceName("MockService"),
                    new StaticServiceFactory(new Object()),
                    null,
                    new NullServiceMonitor(),
                    0,
                    null);
        }

        private synchronized void reset() {
            initializeCalled = false;
            destroyCalled = false;
            stopCalled = false;
            initializeException = null;
            destroyException = null;
            stopException = null;
        }

        public Set getServiceTypes() {
            return TYPES;
        }

        public void initialize() throws IllegalServiceStateException, UnsatisfiedConditionsException, Exception {
            synchronized (this) {
                assertFalse(initializeCalled);
                initializeCalled = true;
            }

            signalEnter();
            awaitExitSignal();

            synchronized (this) {
                if (initializeException instanceof Exception) {
                    throw (Exception) initializeException;
                } else if (initializeException instanceof Error) {
                    throw (Error) initializeException;
                }
            }
        }

        public void destroy(StopStrategy stopStrategy) throws IllegalServiceStateException, UnsatisfiedConditionsException {
            synchronized (this) {
                assertFalse(destroyCalled);
                destroyCalled = true;
            }

            try {
                signalEnter();
                awaitExitSignal();
            } catch (InterruptedException e) {
                fail("destroyCondition.await() threw an exception");
            }

            synchronized (this) {
                if (destroyException instanceof UnsatisfiedConditionsException) {
                    throw (UnsatisfiedConditionsException) destroyException;
                } else if (destroyException instanceof IllegalServiceStateException) {
                    throw (IllegalServiceStateException) destroyException;
                } else if (destroyException instanceof RuntimeException) {
                    throw (RuntimeException) destroyException;
                } else if (destroyException instanceof Error) {
                    throw (Error) destroyException;
                }
            }
        }

        public synchronized boolean stop(StopStrategy stopStrategy) throws UnsatisfiedConditionsException {
            stopCalled = true;

            if (stopException instanceof UnsatisfiedConditionsException) {
                throw (UnsatisfiedConditionsException) stopException;
            } else if (stopException instanceof RuntimeException) {
                throw (RuntimeException) stopException;
            } else if (stopException instanceof Error) {
                throw (Error) stopException;
            }
            return true;
        }

        public synchronized boolean isInitializeCalled() {
            return initializeCalled;
        }

        public synchronized boolean isDestroyCalled() {
            return destroyCalled;
        }

        public synchronized boolean isStopCalled() {
            return stopCalled;
        }

        public synchronized void setInitializeException(Throwable initializeException) {
            this.initializeException = initializeException;
        }

        public synchronized void setDestroyException(Throwable destroyException) {
            this.destroyException = destroyException;
        }

        public synchronized void setStopException(Throwable stopException) {
            this.stopException = stopException;
        }

        public boolean awaitEnterSignal() throws InterruptedException {
            CountDownLatch signal;
            synchronized (this) {
                signal = enterWaiting;
            }
            boolean worked = signal.await(TIMEOUT_DURATION, TIMEOUT_UNITS);
            return worked;
        }

        private void signalEnter() {
            CountDownLatch signal;
            synchronized (this) {
                signal = enterWaiting;
            }
            signal.countDown();
        }

        public synchronized void setWait() {
            exitWaiting = new CountDownLatch(1);
            enterWaiting = new CountDownLatch(1);
        }

        public void signalExit() {
            CountDownLatch signal;
            synchronized (this) {
                signal = exitWaiting;
            }
            signal.countDown();
        }

        private void awaitExitSignal() throws InterruptedException {
            CountDownLatch signal;
            synchronized (this) {
                signal = exitWaiting;
            }
            signal.await(TIMEOUT_DURATION, TIMEOUT_UNITS);
        }
    }
}
