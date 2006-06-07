/**
 *
 * Copyright 2005-2006 The Apache Software Foundation or its licensors, as applicable.
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
package org.apache.xbean.kernel.standard;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.emory.mathcs.backport.java.util.concurrent.Callable;
import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.FutureTask;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import junit.framework.TestCase;
import org.apache.xbean.kernel.ForcedStopException;
import org.apache.xbean.kernel.IllegalServiceStateException;
import org.apache.xbean.kernel.Kernel;
import org.apache.xbean.kernel.KernelMonitor;
import org.apache.xbean.kernel.ServiceAlreadyExistsException;
import org.apache.xbean.kernel.ServiceCondition;
import org.apache.xbean.kernel.ServiceConditionContext;
import org.apache.xbean.kernel.ServiceContext;
import org.apache.xbean.kernel.ServiceEvent;
import org.apache.xbean.kernel.ServiceFactory;
import org.apache.xbean.kernel.ServiceMonitor;
import org.apache.xbean.kernel.ServiceName;
import org.apache.xbean.kernel.ServiceNotFoundException;
import org.apache.xbean.kernel.ServiceRegistrationException;
import org.apache.xbean.kernel.ServiceState;
import org.apache.xbean.kernel.StartStrategies;
import org.apache.xbean.kernel.StartStrategy;
import org.apache.xbean.kernel.StaticServiceFactory;
import org.apache.xbean.kernel.StopStrategies;
import org.apache.xbean.kernel.StopStrategy;
import org.apache.xbean.kernel.StringServiceName;
import org.apache.xbean.kernel.UnregisterServiceException;
import org.apache.xbean.kernel.UnsatisfiedConditionsException;

/**
 * Test ServiceManager.
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class ServiceManagerTest extends TestCase {
    private static final Object SERVICE = new Object();
    private final MockKernel kernel = new MockKernel();
    private final StringServiceName serviceName = new StringServiceName("Service");
    private final StringServiceName ownedServiceName = new StringServiceName("OwnedService");
    private final MockStartCondition startCondition = new MockStartCondition();
    private final MockStopCondition stopCondition = new MockStopCondition();
    private final MockServiceFactory serviceFactory = new MockServiceFactory();
    private final ClassLoader classLoader = new URLClassLoader(new URL[0]);
    private final MockServiceMonitor serviceMonitor = new MockServiceMonitor();
    private ServiceManager serviceManager;

    /**
     * Tests that the initial state of the service manager is as expected.
     */
    public void testInitialState() {
        assertSame(serviceName, serviceManager.getServiceName());
        assertSame(serviceFactory, serviceManager.getServiceFactory());
        assertSame(classLoader, serviceManager.getClassLoader());
        assertEquals(0, serviceManager.getStartTime());
        assertNull(serviceManager.getService());
        assertSame(ServiceState.STOPPED, serviceManager.getState());
    }

    /**
     * Tests that initialize and destroy work without exception.
     * @throws Exception if a problem occurs
     */
    public void testInitializeDestroy() throws Exception {
        initialize();
        destroy(StopStrategies.SYNCHRONOUS);
        initialize();
        destroy(StopStrategies.SYNCHRONOUS);
    }

    /**
     * Tests that start and stop work without exception.
     * @throws Exception if a problem occurs
     */
    public void testStartStop() throws Exception {
        startStop(StartStrategies.ASYNCHRONOUS, false);
        startStop(StartStrategies.SYNCHRONOUS, false);
        startStop(StartStrategies.UNREGISTER, false);
        startStop(StartStrategies.BLOCK, false);

        startStop(StartStrategies.ASYNCHRONOUS, false);
        startStop(StartStrategies.SYNCHRONOUS, false);
        startStop(StartStrategies.UNREGISTER, false);
        startStop(StartStrategies.BLOCK, false);
    }

    /**
     * Tests that startRecursive results in recursive start calls on the owned services.
     * @throws Exception if a problem occurs
     */
    public void testStartRecursive() throws Exception {
        startStop(StartStrategies.ASYNCHRONOUS, true);
        startStop(StartStrategies.SYNCHRONOUS, true);
        startStop(StartStrategies.UNREGISTER, true);
        startStop(StartStrategies.BLOCK, true);

        startStop(StartStrategies.ASYNCHRONOUS, true);
        startStop(StartStrategies.SYNCHRONOUS, true);
        startStop(StartStrategies.UNREGISTER, true);
        startStop(StartStrategies.BLOCK, true);
    }

    private void startStop(StartStrategy startStrategy, boolean recursive) throws Exception {
        start(recursive, startStrategy);
        stop(StopStrategies.SYNCHRONOUS);

        start(recursive, startStrategy);
        start(recursive, startStrategy);
        stop(StopStrategies.SYNCHRONOUS);
        stop(StopStrategies.SYNCHRONOUS);
    }

    /**
     * Tests how the start strategies respond when an Exception is thrown.
     * @throws Exception if a problem occurs
     */
    public void testStartException() throws Exception {
        startException(StartStrategies.ASYNCHRONOUS, false);
        startException(StartStrategies.SYNCHRONOUS, false);
        startException(StartStrategies.UNREGISTER, false);
        startException(StartStrategies.BLOCK, false);

        startException(StartStrategies.ASYNCHRONOUS, false);
        startException(StartStrategies.SYNCHRONOUS, false);
        startException(StartStrategies.UNREGISTER, false);
        startException(StartStrategies.BLOCK, false);
    }

    /**
     * Tests how startRecursive start strategies respond when an Exception is thrown.
     * @throws Exception if a problem occurs
     */
    public void testStartExceptionRecursive() throws Exception {
        startException(StartStrategies.ASYNCHRONOUS, true);
        startException(StartStrategies.SYNCHRONOUS, true);
        startException(StartStrategies.UNREGISTER, true);
        startException(StartStrategies.BLOCK, true);

        startException(StartStrategies.ASYNCHRONOUS, true);
        startException(StartStrategies.SYNCHRONOUS, true);
        startException(StartStrategies.UNREGISTER, true);
        startException(StartStrategies.BLOCK, true);
    }

    private void startException(StartStrategy startStrategy, boolean recursive) throws Exception {
        serviceFactory.throwExceptionFromCreate = true;
        try {
            start(recursive, startStrategy);
        } catch (MockCreateException e) {
            assertTrue(startStrategy == StartStrategies.SYNCHRONOUS || startStrategy == StartStrategies.BLOCK);
            assertEquals(serviceFactory.createException, e);
        } catch (UnregisterServiceException e) {
            assertEquals(StartStrategies.UNREGISTER, startStrategy);
            assertSame(serviceFactory.createException, e.getCause());
            assertEquals(serviceName, e.getServiceName());
        }
        stop(StopStrategies.SYNCHRONOUS);
    }

    /**
     * Tests how the start strategies respond when confronted with an unsatisfied start condition.
     * @throws Exception if a problem occurs
     */
    public void testStartWaiting() throws Exception {
        startWaiting(StartStrategies.ASYNCHRONOUS, false);
        startWaiting(StartStrategies.SYNCHRONOUS, false);
        startWaiting(StartStrategies.UNREGISTER, false);

        startWaiting(StartStrategies.ASYNCHRONOUS, false);
        startWaiting(StartStrategies.SYNCHRONOUS, false);
        startWaiting(StartStrategies.UNREGISTER, false);
    }

    /**
     * Tests how the startRecursive start strategies responed when confronted with an unsatisfied start condition.
     * @throws Exception
     */
    public void testStartWaitingRecursive() throws Exception {
        startWaiting(StartStrategies.ASYNCHRONOUS, true);
        startWaiting(StartStrategies.SYNCHRONOUS, true);
        startWaiting(StartStrategies.UNREGISTER, true);

        startWaiting(StartStrategies.ASYNCHRONOUS, true);
        startWaiting(StartStrategies.SYNCHRONOUS, true);
        startWaiting(StartStrategies.UNREGISTER, true);
    }

    private void startWaiting(StartStrategy startStrategy, boolean recursive) throws Exception {
        startCondition.satisfied = false;
        try {
            start(recursive, startStrategy);
        } catch (UnsatisfiedConditionsException e) {
            assertTrue(startStrategy == StartStrategies.SYNCHRONOUS);
            assertTrue(e.getUnsatisfiedConditions().contains(startCondition));
            assertEquals(serviceName, e.getServiceName());
        } catch (UnregisterServiceException e) {
            assertEquals(StartStrategies.UNREGISTER, startStrategy);
            assertEquals(serviceName, e.getServiceName());
            UnsatisfiedConditionsException cause = (UnsatisfiedConditionsException) e.getCause();
            assertTrue(cause.getUnsatisfiedConditions().contains(startCondition));
            assertEquals(serviceName, cause.getServiceName());
        }
        stop(StopStrategies.SYNCHRONOUS);
    }


    /**
     * Tests how the start strategies respond once a once unsatisfied start condition becomes satisfied.
     * @throws Exception if a problem occurs
     */
    public void testStartWaitingStart() throws Exception {
        startWaitingStart(StartStrategies.ASYNCHRONOUS, false);
        startWaitingStart(StartStrategies.SYNCHRONOUS, false);
        startWaitingStart(StartStrategies.UNREGISTER, false);

        startWaitingStart(StartStrategies.ASYNCHRONOUS, false);
        startWaitingStart(StartStrategies.SYNCHRONOUS, false);
        startWaitingStart(StartStrategies.UNREGISTER, false);
    }

    /**
     * Tests how startRecursive start strategies respond once a once unsatisfied start condition becomes satisfied.
     * @throws Exception if a problem occurs
     */
    public void testStartWaitingStartRecursive() throws Exception {
        startWaitingStart(StartStrategies.ASYNCHRONOUS, true);
        startWaitingStart(StartStrategies.SYNCHRONOUS, true);
        startWaitingStart(StartStrategies.UNREGISTER, true);

        startWaitingStart(StartStrategies.ASYNCHRONOUS, true);
        startWaitingStart(StartStrategies.SYNCHRONOUS, true);
        startWaitingStart(StartStrategies.UNREGISTER, true);
    }

    private void startWaitingStart(StartStrategy startStrategy, boolean recursive) throws Exception {
        startCondition.satisfied = recursive;
        try {
            start(recursive, startStrategy);
        } catch (UnsatisfiedConditionsException e) {
            assertTrue(startStrategy == StartStrategies.SYNCHRONOUS);
            assertTrue(e.getUnsatisfiedConditions().contains(startCondition));
            assertEquals(serviceName, e.getServiceName());
        } catch (UnregisterServiceException e) {
            assertEquals(StartStrategies.UNREGISTER, startStrategy);
            assertEquals(serviceName, e.getServiceName());
            UnsatisfiedConditionsException cause = (UnsatisfiedConditionsException) e.getCause();
            assertTrue(cause.getUnsatisfiedConditions().contains(startCondition));
            assertEquals(serviceName, cause.getServiceName());
        }
        try {
            start(recursive, startStrategy);
        } catch (UnsatisfiedConditionsException e) {
            assertTrue(startStrategy == StartStrategies.SYNCHRONOUS);
            assertTrue(e.getUnsatisfiedConditions().contains(startCondition));
            assertEquals(serviceName, e.getServiceName());
        } catch (UnregisterServiceException e) {
            assertEquals(StartStrategies.UNREGISTER, startStrategy);
            assertEquals(serviceName, e.getServiceName());
            UnsatisfiedConditionsException cause = (UnsatisfiedConditionsException) e.getCause();
            assertTrue(cause.getUnsatisfiedConditions().contains(startCondition));
            assertEquals(serviceName, cause.getServiceName());
        }
        startCondition.satisfied = true;
        start(recursive, startStrategy);
        start(recursive, startStrategy);
        stop(StopStrategies.SYNCHRONOUS);
    }

    /**
     * Tests the BLOCK start stragegy.
     * @throws Exception if a problem occurs
     */
    public void testBlockStartWaiting() throws Exception {
        startCondition.satisfied = false;
        startCondition.isSatisfiedSignal = new CountDownLatch(1);
        FutureTask startTask = new FutureTask(new Callable() {
            public Object call() throws Exception {
                start(false, StartStrategies.BLOCK);
                return Boolean.TRUE;
            }
        });
        Thread startThread = new Thread(startTask, "StartTask");
        startThread.setDaemon(true);
        startThread.start();

        // wait for the start thread to reach the startContion initialize method
        assertTrue(startCondition.isSatisfiedSignal.await(5, TimeUnit.SECONDS));

        // we should not have a service instance and be in the starting state
        assertSame(ServiceState.STARTING, serviceManager.getState());
        assertNull(serviceManager.getService());
        assertEquals(0, serviceManager.getStartTime());
        assertNotNull(serviceMonitor.starting);
        assertNull(serviceMonitor.waitingToStart);
        assertNull(serviceMonitor.startError);
        assertNull(serviceMonitor.running);
        assertNull(serviceMonitor.stopping);
        assertNull(serviceMonitor.stopError);
        assertNull(serviceMonitor.stopped);
        assertTrue(startCondition.initializeCalled);
        assertTrue(startCondition.isSatisfiedCalled);
        assertFalse(startCondition.destroyCalled);

        long now = System.currentTimeMillis();
        // introduce a bit of delay so subsequent times are much less likely to equal to not
        Thread.sleep(500);

        startCondition.satisfied = true;
        startCondition.context.setSatisfied();

        // wait for the start task to complete
        assertEquals(Boolean.TRUE, startTask.get(5, TimeUnit.SECONDS));

        // we should be running
        assertSame(ServiceState.RUNNING, serviceManager.getState());
        assertSame(SERVICE, serviceManager.getService());
        assertTrue(serviceManager.getStartTime() >= now);
        assertNotNull(serviceMonitor.starting);
        assertNull(serviceMonitor.waitingToStart);
        assertNull(serviceMonitor.startError);
        assertNotNull(serviceMonitor.running);
        assertNull(serviceMonitor.stopping);
        assertNull(serviceMonitor.stopError);
        assertNull(serviceMonitor.stopped);
        assertTrue(startCondition.initializeCalled);
        assertTrue(startCondition.isSatisfiedCalled);
        assertFalse(startCondition.destroyCalled);

        stop(StopStrategies.SYNCHRONOUS);
    }

    /**
     * Tests how start responds when the service factory is not enabled.
     * @throws Exception if a problem occurs
     */
    public void testDisabledStart() throws Exception {
        disabledStart(StartStrategies.ASYNCHRONOUS);
        disabledStart(StartStrategies.SYNCHRONOUS);
        disabledStart(StartStrategies.UNREGISTER);
        disabledStart(StartStrategies.BLOCK);

        disabledStart(StartStrategies.ASYNCHRONOUS);
        disabledStart(StartStrategies.SYNCHRONOUS);
        disabledStart(StartStrategies.UNREGISTER);
        disabledStart(StartStrategies.BLOCK);
    }

    private void disabledStart(StartStrategy startStrategy) throws Exception {
        serviceFactory.setEnabled(false);
        try {
            serviceManager.start(false, startStrategy);
            fail("A disabled service should throw an IllegalServiceStateException from start");
        } catch (IllegalServiceStateException e) {
            // expected
            assertEquals(serviceName, e.getServiceName());
        }

        // move to starting disable, move to running, and try to restart
        serviceFactory.setEnabled(true);
        startCondition.satisfied = false;
        start(false, StartStrategies.ASYNCHRONOUS);

        serviceFactory.setEnabled(false);
        startCondition.satisfied = true;
        start(false, startStrategy);
        start(false, startStrategy);
        stop(StopStrategies.ASYNCHRONOUS);
        try {
            serviceManager.start(false, startStrategy);
            fail("A disabled service should throw an IllegalServiceStateException from start");
        } catch (IllegalServiceStateException e) {
            // expected
            assertEquals(serviceName, e.getServiceName());
        }
    }

    /**
     * Tests how the stop strategies respond when destroyService throws an Exception.
     * @throws Exception if a problem occurs
     */
    public void testStopException() throws Exception {
        stopException(StopStrategies.ASYNCHRONOUS);
        stopException(StopStrategies.SYNCHRONOUS);
        stopException(StopStrategies.FORCE);
        stopException(StopStrategies.BLOCK);
    }

    private void stopException(StopStrategy stopStrategy) throws Exception {
        serviceFactory.throwExceptionFromDestroy = true;
        start(false, StartStrategies.SYNCHRONOUS);
        stop(stopStrategy);
    }

    /**
     * Tests how the start and stop strategies work when both create and destroy throw exceptions.
     * @throws Exception if a problem occurs
     */
    public void testStartStopsException() throws Exception {
        startStopsException(StartStrategies.ASYNCHRONOUS, StopStrategies.ASYNCHRONOUS);
        startStopsException(StartStrategies.ASYNCHRONOUS, StopStrategies.SYNCHRONOUS);
        startStopsException(StartStrategies.ASYNCHRONOUS, StopStrategies.FORCE);
        startStopsException(StartStrategies.ASYNCHRONOUS, StopStrategies.BLOCK);
        startStopsException(StartStrategies.SYNCHRONOUS, StopStrategies.ASYNCHRONOUS);
        startStopsException(StartStrategies.SYNCHRONOUS, StopStrategies.SYNCHRONOUS);
        startStopsException(StartStrategies.SYNCHRONOUS, StopStrategies.FORCE);
        startStopsException(StartStrategies.SYNCHRONOUS, StopStrategies.BLOCK);
        startStopsException(StartStrategies.UNREGISTER, StopStrategies.ASYNCHRONOUS);
        startStopsException(StartStrategies.UNREGISTER, StopStrategies.SYNCHRONOUS);
        startStopsException(StartStrategies.UNREGISTER, StopStrategies.FORCE);
        startStopsException(StartStrategies.UNREGISTER, StopStrategies.BLOCK);
        startStopsException(StartStrategies.BLOCK, StopStrategies.ASYNCHRONOUS);
        startStopsException(StartStrategies.BLOCK, StopStrategies.SYNCHRONOUS);
        startStopsException(StartStrategies.BLOCK, StopStrategies.FORCE);
        startStopsException(StartStrategies.BLOCK, StopStrategies.BLOCK);
    }

    private void startStopsException(StartStrategy startStrategy, StopStrategy stopStrategy) throws Exception {
        serviceFactory.throwExceptionFromCreate = true;
        serviceFactory.throwExceptionFromDestroy = true;
        try {
            start(false, startStrategy);
        } catch (MockCreateException e) {
            assertTrue(startStrategy == StartStrategies.SYNCHRONOUS || startStrategy == StartStrategies.BLOCK);
            assertEquals(serviceFactory.createException, e);
        } catch (UnregisterServiceException e) {
            assertEquals(StartStrategies.UNREGISTER, startStrategy);
            assertEquals(serviceName, e.getServiceName());
            assertSame(serviceFactory.createException, e.getCause());
        }
        stop(stopStrategy);
    }

    /**
     * Tests how stop strategies respond when confronted with an unsatisfied stop condition.
     * @throws Exception if a problem occurs
     */
    public void testWaitingStop() throws Exception {
        waitingStop(StopStrategies.ASYNCHRONOUS);
        waitingStop(StopStrategies.SYNCHRONOUS);
        waitingStop(StopStrategies.FORCE);
    }

    private void waitingStop(StopStrategy stopStrategy) throws Exception {
        start(false, StartStrategies.SYNCHRONOUS);
        stopCondition.satisfied = false;
        try {
            stop(stopStrategy);
        } catch (UnsatisfiedConditionsException e) {
            assertTrue(stopStrategy == StopStrategies.SYNCHRONOUS);
            assertTrue(e.getUnsatisfiedConditions().contains(stopCondition));
            assertEquals(serviceName, e.getServiceName());
        }
        try {
            stop(stopStrategy);
        } catch (UnsatisfiedConditionsException e) {
            assertTrue(stopStrategy == StopStrategies.SYNCHRONOUS);
            assertTrue(e.getUnsatisfiedConditions().contains(stopCondition));
            assertEquals(serviceName, e.getServiceName());
        }

        stopCondition.satisfied = true;
        stop(stopStrategy);
    }

    /**
     * Tests the BLOCK stop strategy.
     * @throws Exception if a problem occurs
     */
    public void testBlockStopWaiting() throws Exception {
        start(false, StartStrategies.SYNCHRONOUS);
        stopCondition.satisfied = false;
        stopCondition.isStatisfiedSignal = new CountDownLatch(1);
        FutureTask stopTask = new FutureTask(new Callable() {
            public Object call() throws Exception {
                stop(StopStrategies.BLOCK);
                return Boolean.TRUE;
            }
        });
        Thread stopThread = new Thread(stopTask, "StopTask");
        stopThread.setDaemon(true);
        stopThread.start();

        // wait for the stop thread to reach the stopContion initialize method
        assertTrue(stopCondition.isStatisfiedSignal.await(5, TimeUnit.SECONDS));

        // we should blocked waiting to stop
        assertSame(ServiceState.STOPPING, serviceManager.getState());
        assertTrue(serviceManager.getStartTime() > 0);
        assertSame(SERVICE, serviceManager.getService());
        assertNull(serviceMonitor.waitingToStop);
        assertNull(serviceMonitor.stopError);
        assertNull(serviceMonitor.stopped);
        assertNull(serviceMonitor.unregistered);
        assertFalse(startCondition.initializeCalled);
        assertFalse(startCondition.isSatisfiedCalled);
        assertFalse(startCondition.destroyCalled);
        assertTrue(stopCondition.initializeCalled);
        assertTrue(stopCondition.isSatisfiedCalled);
        assertFalse(stopCondition.destroyCalled);

        // wait for the start task to complete
        stopCondition.satisfied = true;
        stopCondition.context.setSatisfied();
        assertEquals(Boolean.TRUE, stopTask.get(5, TimeUnit.SECONDS));

        // we should be STOPPED
        assertSame(ServiceState.STOPPED, serviceManager.getState());
        assertEquals(0, serviceManager.getStartTime());
        assertNull(serviceManager.getService());
        assertNotNull(serviceMonitor.stopping);
        assertNull(serviceMonitor.waitingToStop);
        assertNull(serviceMonitor.stopError);
        assertNotNull(serviceMonitor.stopped);
        assertNull(serviceMonitor.unregistered);
        assertEquals(serviceMonitor.stopping.getEventId() + 1, serviceMonitor.stopped.getEventId());
        assertFalse(startCondition.initializeCalled);
        assertFalse(startCondition.isSatisfiedCalled);
        assertTrue(startCondition.destroyCalled);
        assertTrue(stopCondition.initializeCalled);
        assertTrue(stopCondition.isSatisfiedCalled);
        assertTrue(stopCondition.destroyCalled);
    }


    /**
     * Tests that start throws an exception when the service is in the stoping state.
     * @throws Exception if a problem occurs
     */
    public void testStartFromStopping() throws Exception {
        startFromStopping(StartStrategies.ASYNCHRONOUS);
        startFromStopping(StartStrategies.SYNCHRONOUS);
        startFromStopping(StartStrategies.UNREGISTER);
        startFromStopping(StartStrategies.BLOCK);
    }

    private void startFromStopping(StartStrategy startStrategy) throws Exception {
        start(false, StartStrategies.ASYNCHRONOUS);
        stopCondition.satisfied = false;
        stop(StopStrategies.ASYNCHRONOUS);
        try {
            serviceManager.start(false, startStrategy);
            fail("Should have thrown an IllegalServiceStateException since start on a stopping service is an error");
        } catch (IllegalServiceStateException e) {
            // expected
            assertEquals(serviceName, e.getServiceName());
        }
        stopCondition.satisfied = true;
        stop(StopStrategies.ASYNCHRONOUS);
    }

    /**
     * Tests that a non-restartable servie is immedately started when initialized and stopped in destroy.
     * @throws Exception if a problem occurs
     */
    public void testNotRestartableInitDestroy() throws Exception {
        notRestartableInitDestroy(StopStrategies.ASYNCHRONOUS);
        notRestartableInitDestroy(StopStrategies.SYNCHRONOUS);
        notRestartableInitDestroy(StopStrategies.FORCE);
        notRestartableInitDestroy(StopStrategies.BLOCK);
    }

    private void notRestartableInitDestroy(StopStrategy stopStrategy) throws Exception {
        serviceFactory.restartable = false;
        initialize();
        destroy(stopStrategy);
    }

    /**
     * Tests how initialize on a non-restartable service responds when an Exception is thrown from create service.
     * @throws Exception if a problem occurs
     */
    public void testNotRestartableInitException() throws Exception {
        serviceFactory.throwExceptionFromCreate = true;
        serviceFactory.restartable = false;
        initialize();
    }

    /**
     * Tests how initialize on a non-restartable service responds when confronted with an unsatisfied start condition.
     * @throws Exception if a problem occurs
     */
    public void testNotRestartableInitWaiting() throws Exception {
        startCondition.satisfied = false;
        serviceFactory.restartable = false;
        initialize();
    }

    /**
     * Tests how initialize on a non-restartable service responds when the service factory is disabled.
     * @throws Exception if a problem occurs
     */
    public void testNotRestartableInitDisabled() throws Exception {
        serviceFactory.setEnabled(false);
        serviceFactory.restartable = false;
        initialize();
    }

    /**
     * Tests how destroy on a non-restartable service responds when destroyService throws an exception.
     * @throws Exception if a problem occurs
     */
    public void testNotRestartableDestroyException() throws Exception {
        notRestartableDestroyException(StopStrategies.ASYNCHRONOUS);
        notRestartableDestroyException(StopStrategies.SYNCHRONOUS);
        notRestartableDestroyException(StopStrategies.FORCE);
        notRestartableDestroyException(StopStrategies.BLOCK);
    }

    private void notRestartableDestroyException(StopStrategy stopStrategy) throws Exception {
        serviceFactory.throwExceptionFromDestroy = true;
        serviceFactory.restartable = false;
        initialize();
        destroy(stopStrategy);
    }


    /**
     * Tests how destroy on a non-restartable service responds when createService and destroyService throws an exception.
     * @throws Exception if a problem occurs
     */
    public void testNotRestartableInitDestroyException() throws Exception {
        notRestartableInitDestroyException(StopStrategies.ASYNCHRONOUS);
        notRestartableInitDestroyException(StopStrategies.SYNCHRONOUS);
        notRestartableInitDestroyException(StopStrategies.FORCE);
        notRestartableInitDestroyException(StopStrategies.BLOCK);
    }

    private void notRestartableInitDestroyException(StopStrategy stopStrategy) throws Exception {
        serviceFactory.throwExceptionFromCreate = true;
        serviceFactory.throwExceptionFromDestroy = true;
        serviceFactory.restartable = false;
        initialize();
        destroy(stopStrategy);
    }

    /**
     * Tests how destroy on a non-restartable service responds when confronted with an unsatisfied stop condition.
     * @throws Exception if a problem occurs
     */
    public void testNotRestartableWaitingStop() throws Exception {
        notRestartableWaitingDestroy(StopStrategies.ASYNCHRONOUS);
        notRestartableWaitingDestroy(StopStrategies.SYNCHRONOUS);
        notRestartableWaitingDestroy(StopStrategies.FORCE);
    }

    private void notRestartableWaitingDestroy(StopStrategy stopStrategy) throws Exception {
        serviceFactory.restartable = false;
        initialize();
        stopCondition.satisfied = false;
        destroy(stopStrategy);
        destroy(stopStrategy);
        stopCondition.satisfied = true;
        destroy(stopStrategy);
    }

    /**
     * Tests the BLOCK stop strategy on a non-restartable service.
     * @throws Exception if a problem occurs
     */
    public void testNotRestartableBlockWaitingStop() throws Exception {
        serviceFactory.restartable = false;
        initialize();
        stopCondition.satisfied = false;
        stopCondition.isStatisfiedSignal = new CountDownLatch(1);
        FutureTask destroyTask = new FutureTask(new Callable() {
            public Object call() throws Exception {
                destroy(StopStrategies.BLOCK);
                return Boolean.TRUE;
            }
        });
        Thread destroyThread = new Thread(destroyTask, "DestroyTask");
        destroyThread.setDaemon(true);
        destroyThread.start();

        // wait for the stop thread to reach the stopContion initialize method
        assertTrue(stopCondition.isStatisfiedSignal.await(5, TimeUnit.SECONDS));

        // we should blocked waiting to stop
        assertSame(ServiceState.RUNNING, serviceManager.getState());
        assertSame(serviceName, serviceManager.getServiceName());
        assertSame(serviceFactory, serviceManager.getServiceFactory());
        assertSame(classLoader, serviceManager.getClassLoader());
        assertTrue(serviceManager.getStartTime() > 0);
        assertNotNull(serviceManager.getService());
        assertNull(serviceMonitor.registered);
        assertNull(serviceMonitor.starting);
        assertNull(serviceMonitor.waitingToStart);
        assertNull(serviceMonitor.startError);
        assertNull(serviceMonitor.running);
        assertNull(serviceMonitor.stopping);
        assertNull(serviceMonitor.waitingToStop);
        assertNull(serviceMonitor.stopError);
        assertNull(serviceMonitor.stopped);
        assertNull(serviceMonitor.unregistered);

        // wait for the destroy task to complete
        stopCondition.satisfied = true;
        stopCondition.context.setSatisfied();
        assertEquals(Boolean.TRUE, destroyTask.get(5, TimeUnit.SECONDS));

        // we should be STOPPED
        assertSame(ServiceState.STOPPED, serviceManager.getState());
        assertEquals(0, serviceManager.getStartTime());
        assertNull(serviceManager.getService());
        assertNull(serviceMonitor.registered);
        assertNull(serviceMonitor.starting);
        assertNull(serviceMonitor.waitingToStart);
        assertNull(serviceMonitor.startError);
        assertNull(serviceMonitor.running);
        assertNotNull(serviceMonitor.stopping);
        assertNull(serviceMonitor.waitingToStop);
        assertNull(serviceMonitor.stopError);
        assertNotNull(serviceMonitor.stopped);
        assertNotNull(serviceMonitor.unregistered);
        assertEquals(serviceMonitor.stopping.getEventId() + 1, serviceMonitor.stopped.getEventId());
        assertEquals(serviceMonitor.stopping.getEventId() + 2, serviceMonitor.unregistered.getEventId());
        assertFalse(startCondition.initializeCalled);
        assertFalse(startCondition.isSatisfiedCalled);
        assertTrue(startCondition.destroyCalled);
        assertTrue(stopCondition.initializeCalled);
        assertTrue(stopCondition.isSatisfiedCalled);
        assertTrue(stopCondition.destroyCalled);
    }

    private void initialize() throws Exception {
        long now = System.currentTimeMillis();
        // introduce a bit of delay so subsequent times are much less likely to equal to not
        Thread.sleep(50);

        serviceMonitor.reset();
        startCondition.reset();
        stopCondition.reset();
        kernel.reset();
        try {
            serviceManager.initialize();
        } catch (MockCreateException e) {
            assertTrue(serviceFactory.throwExceptionFromCreate);
            assertSame(serviceFactory.createException, e);
        } catch (UnsatisfiedConditionsException e) {
            assertTrue(!startCondition.satisfied);
            assertTrue(e.getUnsatisfiedConditions().contains(startCondition));
            assertEquals(serviceName, e.getServiceName());
        } catch (IllegalServiceStateException e) {
            assertFalse(serviceFactory.isEnabled());
            assertEquals(serviceName, e.getServiceName());
        }

        assertSame(serviceName, serviceManager.getServiceName());
        assertSame(serviceFactory, serviceManager.getServiceFactory());
        assertSame(classLoader, serviceManager.getClassLoader());

        if (serviceFactory.restartable) {
            assertSame(ServiceState.STOPPED, serviceManager.getState());
            assertEquals(0, serviceManager.getStartTime());
            assertNull(serviceManager.getService());
            assertNotNull(serviceMonitor.registered);
            assertNull(serviceMonitor.starting);
            assertNull(serviceMonitor.waitingToStart);
            assertNull(serviceMonitor.startError);
            assertNull(serviceMonitor.running);
            assertNull(serviceMonitor.stopping);
            assertNull(serviceMonitor.waitingToStop);
            assertNull(serviceMonitor.stopError);
            assertNull(serviceMonitor.stopped);
            assertNull(serviceMonitor.unregistered);
            assertFalse(startCondition.initializeCalled);
            assertFalse(startCondition.isSatisfiedCalled);
            assertFalse(startCondition.destroyCalled);
            assertFalse(stopCondition.initializeCalled);
            assertFalse(stopCondition.isSatisfiedCalled);
            assertFalse(stopCondition.destroyCalled);
        } else if (!serviceFactory.isEnabled()) {
            assertSame(ServiceState.STOPPED, serviceManager.getState());
            assertEquals(0, serviceManager.getStartTime());
            assertNull(serviceManager.getService());
            assertNull(serviceMonitor.registered);
            assertNull(serviceMonitor.starting);
            assertNull(serviceMonitor.waitingToStart);
            assertNull(serviceMonitor.startError);
            assertNull(serviceMonitor.running);
            assertNull(serviceMonitor.stopping);
            assertNull(serviceMonitor.stopError);
            assertNull(serviceMonitor.stopped);
            assertNull(serviceMonitor.unregistered);
            assertFalse(startCondition.initializeCalled);
            assertFalse(startCondition.isSatisfiedCalled);
            assertFalse(startCondition.destroyCalled);
            assertFalse(stopCondition.initializeCalled);
            assertFalse(stopCondition.isSatisfiedCalled);
            assertFalse(stopCondition.destroyCalled);
        } else if (!serviceFactory.throwExceptionFromCreate && startCondition.satisfied) {
            assertSame(ServiceState.RUNNING, serviceManager.getState());
            assertSame(SERVICE, serviceManager.getService());
            assertTrue(serviceManager.getStartTime() > now);
            assertNotNull(serviceMonitor.registered);
            assertNotNull(serviceMonitor.starting);
            assertNull(serviceMonitor.waitingToStart);
            assertNull(serviceMonitor.startError);
            assertNotNull(serviceMonitor.running);
            assertNull(serviceMonitor.stopping);
            assertNull(serviceMonitor.stopError);
            assertNull(serviceMonitor.stopped);
            assertNull(serviceMonitor.unregistered);
            assertEquals(serviceMonitor.registered.getEventId() + 1, serviceMonitor.starting.getEventId());
            assertEquals(serviceMonitor.registered.getEventId() + 2, serviceMonitor.running.getEventId());
            assertTrue(startCondition.initializeCalled);
            assertTrue(startCondition.isSatisfiedCalled);
            assertFalse(startCondition.destroyCalled);
            assertFalse(stopCondition.initializeCalled);
            assertFalse(stopCondition.isSatisfiedCalled);
            assertFalse(stopCondition.destroyCalled);
        } else {
            assertSame(ServiceState.STOPPED, serviceManager.getState());
            assertEquals(0, serviceManager.getStartTime());
            assertNull(serviceManager.getService());
            assertNotNull(serviceMonitor.registered);
            assertNotNull(serviceMonitor.starting);
            assertNull(serviceMonitor.waitingToStart);
            assertNull(serviceMonitor.startError);
            assertNull(serviceMonitor.running);
            assertNotNull(serviceMonitor.stopping);
            assertNull(serviceMonitor.stopError);
            assertNotNull(serviceMonitor.stopped);
            assertNotNull(serviceMonitor.unregistered);
            assertEquals(serviceMonitor.registered.getEventId() + 1, serviceMonitor.starting.getEventId());
            assertEquals(serviceMonitor.registered.getEventId() + 2, serviceMonitor.stopping.getEventId());
            assertEquals(serviceMonitor.registered.getEventId() + 3, serviceMonitor.stopped.getEventId());
            assertEquals(serviceMonitor.registered.getEventId() + 4, serviceMonitor.unregistered.getEventId());
            assertTrue(startCondition.initializeCalled);
            assertTrue(startCondition.isSatisfiedCalled);
            assertTrue(startCondition.destroyCalled);
            assertFalse(stopCondition.initializeCalled);
            assertFalse(stopCondition.isSatisfiedCalled);
            assertFalse(stopCondition.destroyCalled);
        }
    }

    private void start(boolean recursive, StartStrategy startStrategy) throws Exception {
        ServiceState initialState = serviceManager.getState();

        long now = System.currentTimeMillis();
        // introduce a bit of delay so subsequent times are much less likely to equal to not
        Thread.sleep(50);

        serviceMonitor.reset();
        startCondition.reset();
        stopCondition.reset();
        kernel.reset();
        serviceManager.start(recursive, startStrategy);

        assertSame(serviceName, serviceManager.getServiceName());
        assertSame(serviceFactory, serviceManager.getServiceFactory());
        assertSame(classLoader, serviceManager.getClassLoader());

        // these events should never fire in response to start
        assertNull(serviceMonitor.registered);
        assertNull(serviceMonitor.waitingToStop);
        assertNull(serviceMonitor.unregistered);

        if (initialState == ServiceState.RUNNING) {
            //
            // We were alredy running so nothing should have happened
            //
            assertSame(ServiceState.RUNNING, serviceManager.getState());
            assertSame(SERVICE, serviceManager.getService());
            assertTrue(serviceManager.getStartTime() > 0);
            assertTrue(serviceManager.getStartTime() <= now);

            // verify expected events fired
            assertNull(serviceMonitor.starting);
            assertNull(serviceMonitor.waitingToStart);
            assertNull(serviceMonitor.startError);
            assertNull(serviceMonitor.running);
            assertNull(serviceMonitor.stopping);
            assertNull(serviceMonitor.stopError);
            assertNull(serviceMonitor.stopped);

            // check if recursive fired
            if (recursive) {
                assertEquals(1, kernel.startRecursive.size());
                assertTrue(kernel.startRecursive.contains(ownedServiceName));
            } else {
                assertTrue(kernel.startRecursive.isEmpty());
            }

            // start condition methods
            assertFalse(startCondition.initializeCalled);
            assertFalse(startCondition.isSatisfiedCalled);
            assertFalse(startCondition.destroyCalled);

            // stop condition methods
            assertFalse(stopCondition.initializeCalled);
            assertFalse(stopCondition.isSatisfiedCalled);
            assertFalse(stopCondition.destroyCalled);
        } else if (!startCondition.satisfied) {
            //
            // watiting to start
            //
            assertTrue(initialState == ServiceState.STOPPED || initialState == ServiceState.STARTING);

            // we should have not a service instance and be in the starting state
            assertSame(ServiceState.STARTING, serviceManager.getState());
            assertNull(serviceManager.getService());
            assertEquals(0, serviceManager.getStartTime());


            // verify expected events fired
            assertNotNull(serviceMonitor.waitingToStart);

            assertNull(serviceMonitor.startError);
            assertNull(serviceMonitor.running);
            assertNull(serviceMonitor.stopping);
            assertNull(serviceMonitor.stopError);
            assertNull(serviceMonitor.stopped);

            // verify events fired in the correct order
            if (initialState == ServiceState.STOPPED) {
                assertEquals(serviceMonitor.starting.getEventId() + 1, serviceMonitor.waitingToStart.getEventId());
            }

            // our condition should be in the unsatisfied condition list
            assertNotNull(serviceMonitor.waitingToStart.getUnsatisfiedConditions());
            assertTrue(serviceMonitor.waitingToStart.getUnsatisfiedConditions().contains(startCondition));

            // check if recursive fired
            assertTrue(kernel.startRecursive.isEmpty());

            // start condition methods
            if (initialState == ServiceState.STOPPED) {
                assertTrue(startCondition.initializeCalled);
            } else {
                assertFalse(startCondition.initializeCalled);
            }
            assertTrue(startCondition.isSatisfiedCalled);
            assertFalse(startCondition.destroyCalled);

            // stop condition methods
            assertFalse(stopCondition.initializeCalled);
            assertFalse(stopCondition.isSatisfiedCalled);
            assertFalse(stopCondition.destroyCalled);
        } else if (!serviceFactory.throwExceptionFromCreate) {
            //
            // Normal transition to RUNNING from either STOPPED or STARTING
            //
            assertTrue(initialState == ServiceState.STOPPED || initialState == ServiceState.STARTING);

            // we should have a service instance and be in the running state
            assertSame(ServiceState.RUNNING, serviceManager.getState());
            assertSame(SERVICE, serviceManager.getService());
            assertTrue(serviceManager.getStartTime() > now);

            if (initialState == ServiceState.STOPPED) {
                assertNotNull(serviceMonitor.starting);
            } else {
                assertNull(serviceMonitor.starting);
            }
            assertNull(serviceMonitor.waitingToStart);
            assertNull(serviceMonitor.startError);
            assertNotNull(serviceMonitor.running);
            assertNull(serviceMonitor.stopping);
            assertNull(serviceMonitor.stopError);
            assertNull(serviceMonitor.stopped);

            // verify events fired in the correct order
            if (initialState == ServiceState.STOPPED) {
                assertEquals(serviceMonitor.starting.getEventId() + 1, serviceMonitor.running.getEventId());
            }

            // check if recursive fired
            if (recursive) {
                assertEquals(1, kernel.startRecursive.size());
                assertTrue(kernel.startRecursive.contains(ownedServiceName));
            } else {
                assertTrue(kernel.startRecursive.isEmpty());
            }

            // start condition methods
            if (initialState == ServiceState.STOPPED) {
                assertTrue(startCondition.initializeCalled);
            } else {
                assertFalse(startCondition.initializeCalled);
            }
            assertTrue(startCondition.isSatisfiedCalled);
            assertFalse(startCondition.destroyCalled);

            // stop condition methods
            assertFalse(stopCondition.initializeCalled);
            assertFalse(stopCondition.isSatisfiedCalled);
            assertFalse(stopCondition.destroyCalled);
        } else {
            //
            // Throw an exception from the create method
            //

            // we should be stopped
            assertSame(ServiceState.STOPPED, serviceManager.getState());
            assertNull(serviceManager.getService());
            assertEquals(0, serviceManager.getStartTime());

            // verify expected events fired
            assertNotNull(serviceMonitor.starting);
            assertNull(serviceMonitor.waitingToStart);
            assertNotNull(serviceMonitor.startError);
            assertNull(serviceMonitor.running);
            assertNotNull(serviceMonitor.stopping);
            assertNull(serviceMonitor.stopError);
            assertNotNull(serviceMonitor.stopped);

            // verify events fired in the correct order
            assertEquals(serviceMonitor.starting.getEventId() + 1, serviceMonitor.startError.getEventId());
            assertEquals(serviceMonitor.starting.getEventId() + 2, serviceMonitor.stopping.getEventId());
            assertEquals(serviceMonitor.starting.getEventId() + 3, serviceMonitor.stopped.getEventId());

            // verify that the exception is in the reson field
            assertSame(serviceFactory.createException, serviceMonitor.startError.getCause());

            // check if recursive fired
            assertTrue(kernel.startRecursive.isEmpty());

            // start condition methods
            if (initialState == ServiceState.STOPPED) {
                assertTrue(startCondition.initializeCalled);
            } else {
                assertFalse(startCondition.initializeCalled);
            }
            assertTrue(startCondition.isSatisfiedCalled);
            assertTrue(startCondition.destroyCalled);

            // stop condition methods
            assertFalse(stopCondition.initializeCalled);
            assertFalse(stopCondition.isSatisfiedCalled);
            assertFalse(stopCondition.destroyCalled);
        }
    }

    private void stop(StopStrategy stopStrategy) throws Exception {
        serviceMonitor.reset();
        startCondition.reset();
        stopCondition.reset();
        kernel.reset();
        ServiceState initialState = serviceManager.getState();
        serviceManager.stop(stopStrategy);

        assertSame(serviceName, serviceManager.getServiceName());
        assertSame(serviceFactory, serviceManager.getServiceFactory());
        assertSame(classLoader, serviceManager.getClassLoader());

        // these events should never fire in response to start
        assertNull(serviceMonitor.registered);
        assertNull(serviceMonitor.starting);
        assertNull(serviceMonitor.waitingToStart);
        assertNull(serviceMonitor.startError);
        assertNull(serviceMonitor.running);

        if (initialState == ServiceState.STOPPED) {
            //
            // We were alredy stopped so nothing should have happened
            //
            assertEquals(0, serviceManager.getStartTime());
            assertNull(serviceManager.getService());
            assertSame(ServiceState.STOPPED, serviceManager.getState());

            assertNull(serviceMonitor.stopping);
            assertNull(serviceMonitor.waitingToStop);
            assertNull(serviceMonitor.stopError);
            assertNull(serviceMonitor.stopped);
            assertNull(serviceMonitor.unregistered);

            // start condition methods
            assertFalse(startCondition.initializeCalled);
            assertFalse(startCondition.isSatisfiedCalled);
            assertFalse(startCondition.destroyCalled);

            // stop condition methods
            assertFalse(stopCondition.initializeCalled);
            assertFalse(stopCondition.isSatisfiedCalled);
            assertFalse(stopCondition.destroyCalled);
        } else if (!stopCondition.satisfied && stopStrategy != StopStrategies.FORCE) {
            //
            // waiting to stop
            //
            assertSame(ServiceState.STOPPING, serviceManager.getState());
            assertTrue(serviceManager.getStartTime() > 0);
            assertSame(SERVICE, serviceManager.getService());

            // verify expected events fired
            if (initialState != ServiceState.STOPPING) {
                assertNotNull(serviceMonitor.stopping);
            }
            assertNotNull(serviceMonitor.waitingToStop);
            assertNull(serviceMonitor.stopError);
            assertNull(serviceMonitor.stopped);
            assertNull(serviceMonitor.unregistered);

            // verify events fired in the correct order
            if (initialState != ServiceState.STOPPING) {
                assertEquals(serviceMonitor.stopping.getEventId() + 1, serviceMonitor.waitingToStop.getEventId());
            }

            // our condition should be in the unsatisfied condition list
            assertNotNull(serviceMonitor.waitingToStop.getUnsatisfiedConditions());
            assertTrue(serviceMonitor.waitingToStop.getUnsatisfiedConditions().contains(stopCondition));

            // start condition methods
            assertFalse(startCondition.initializeCalled);
            assertFalse(startCondition.isSatisfiedCalled);
            assertFalse(startCondition.destroyCalled);

            // stop condition methods
            if (initialState == ServiceState.RUNNING) {
                assertTrue(stopCondition.initializeCalled);
            } else {
                assertFalse(stopCondition.initializeCalled);
            }
            assertTrue(stopCondition.isSatisfiedCalled);
            assertFalse(stopCondition.destroyCalled);
        } else if (!serviceFactory.throwExceptionFromDestroy) {
            //
            // Normal transition to STOPPED from either STARTING, RUNNING or STOPPING
            //
            assertSame(ServiceState.STOPPED, serviceManager.getState());
            assertEquals(0, serviceManager.getStartTime());
            assertNull(serviceManager.getService());

            // verify expected events fired
            if (initialState != ServiceState.STOPPING) {
                assertNotNull(serviceMonitor.stopping);
            }
            assertNull(serviceMonitor.waitingToStop);
            if (stopStrategy == StopStrategies.FORCE) {
                assertNotNull(serviceMonitor.stopError);
                ForcedStopException cause = (ForcedStopException) serviceMonitor.stopError.getCause();
                assertTrue(cause.getUnsatisfiedConditions().contains(stopCondition));
                assertEquals(serviceName, cause.getServiceName());
            } else {
                assertNull(serviceMonitor.stopError);
            }
            assertNotNull(serviceMonitor.stopped);
            assertNull(serviceMonitor.unregistered);

            // verify events fired in the correct order
            if (stopStrategy == StopStrategies.FORCE) {
                assertEquals(serviceMonitor.stopping.getEventId() + 1, serviceMonitor.stopError.getEventId());
                assertEquals(serviceMonitor.stopping.getEventId() + 2, serviceMonitor.stopped.getEventId());
            } else if (initialState != ServiceState.STOPPING) {
                assertEquals(serviceMonitor.stopping.getEventId() + 1, serviceMonitor.stopped.getEventId());
            }

            // start condition methods
            assertFalse(startCondition.initializeCalled);
            assertFalse(startCondition.isSatisfiedCalled);
            assertTrue(startCondition.destroyCalled);

            // stop condition methods
            if (initialState == ServiceState.STOPPING) {
                assertFalse(stopCondition.initializeCalled);
                assertTrue(stopCondition.isSatisfiedCalled);
                assertTrue(stopCondition.destroyCalled);
            } else {
                assertTrue(stopCondition.initializeCalled);
                assertTrue(stopCondition.isSatisfiedCalled);
                assertTrue(stopCondition.destroyCalled);
            }
        } else {
            //
            // Throw an exception from the destroy method
            //
            assertEquals(0, serviceManager.getStartTime());
            assertNull(serviceManager.getService());
            assertSame(ServiceState.STOPPED, serviceManager.getState());

            // verify expected events fired
            if (initialState != ServiceState.STOPPING) {
                assertNotNull(serviceMonitor.stopping);
            }
            assertNull(serviceMonitor.waitingToStop);
            assertNotNull(serviceMonitor.stopError);
            assertNotNull(serviceMonitor.stopped);
            assertNull(serviceMonitor.unregistered);

            // verify events fired in the correct order
            if (initialState != ServiceState.STOPPING) {
                assertEquals(serviceMonitor.stopping.getEventId() + 1, serviceMonitor.stopError.getEventId());
                assertEquals(serviceMonitor.stopping.getEventId() + 2, serviceMonitor.stopped.getEventId());
            } else {
                assertEquals(serviceMonitor.stopError.getEventId() + 1, serviceMonitor.stopped.getEventId());
            }

            // verify that the exception is in the reson field
            assertSame(serviceFactory.destroyException, serviceMonitor.stopError.getCause());

            // start condition methods
            assertFalse(startCondition.initializeCalled);
            assertFalse(startCondition.isSatisfiedCalled);
            assertTrue(startCondition.destroyCalled);

            // stop condition methods
            if (initialState == ServiceState.RUNNING) {
                assertTrue(stopCondition.initializeCalled);
            } else {
                assertFalse(stopCondition.initializeCalled);
            }
            assertTrue(stopCondition.isSatisfiedCalled);
            assertTrue(stopCondition.destroyCalled);
        }
    }

    private void destroy(StopStrategy stopStrategy) {
        ServiceState initialState = serviceManager.getState();
        serviceMonitor.reset();
        startCondition.reset();
        stopCondition.reset();
        kernel.reset();
        try {
            serviceManager.destroy(stopStrategy);
        } catch (IllegalServiceStateException e) {
            assertFalse(stopCondition.satisfied);
            assertSame(StopStrategies.ASYNCHRONOUS, stopStrategy);
            assertEquals(serviceName, e.getServiceName());
        } catch (UnsatisfiedConditionsException e) {
            assertFalse(stopCondition.satisfied);
            assertSame(StopStrategies.SYNCHRONOUS, stopStrategy);
            assertTrue(e.getUnsatisfiedConditions().contains(stopCondition));
            assertEquals(serviceName, e.getServiceName());
        }

        if (serviceFactory.restartable || initialState == ServiceState.STOPPED) {
            assertSame(ServiceState.STOPPED, serviceManager.getState());
            assertSame(serviceName, serviceManager.getServiceName());
            assertSame(serviceFactory, serviceManager.getServiceFactory());
            assertSame(classLoader, serviceManager.getClassLoader());
            assertEquals(0, serviceManager.getStartTime());
            assertNull(serviceManager.getService());
            // verify expected events fired
            assertNull(serviceMonitor.registered);
            assertNull(serviceMonitor.starting);
            assertNull(serviceMonitor.waitingToStart);
            assertNull(serviceMonitor.startError);
            assertNull(serviceMonitor.running);
            assertNull(serviceMonitor.stopping);
            assertNull(serviceMonitor.waitingToStop);
            assertNull(serviceMonitor.stopError);
            assertNull(serviceMonitor.stopped);
            assertNotNull(serviceMonitor.unregistered);
        } else if (!stopCondition.satisfied && stopStrategy != StopStrategies.FORCE) {
            assertSame(ServiceState.RUNNING, serviceManager.getState());
            assertSame(serviceName, serviceManager.getServiceName());
            assertSame(serviceFactory, serviceManager.getServiceFactory());
            assertSame(classLoader, serviceManager.getClassLoader());
            assertTrue(serviceManager.getStartTime() > 0);
            assertNotNull(serviceManager.getService());
            assertNull(serviceMonitor.registered);
            assertNull(serviceMonitor.starting);
            assertNull(serviceMonitor.waitingToStart);
            assertNull(serviceMonitor.startError);
            assertNull(serviceMonitor.running);
            assertNull(serviceMonitor.stopping);
            if (stopStrategy == StopStrategies.ASYNCHRONOUS) {
                assertNotNull(serviceMonitor.waitingToStop);
                assertTrue(serviceMonitor.waitingToStop.getUnsatisfiedConditions().contains(stopCondition));
            } else {
                assertNull(serviceMonitor.waitingToStop);
            }
            assertNull(serviceMonitor.stopError);
            assertNull(serviceMonitor.stopped);
            assertNull(serviceMonitor.unregistered);
        } else if (!serviceFactory.throwExceptionFromDestroy) {
            assertSame(ServiceState.STOPPED, serviceManager.getState());
            assertEquals(0, serviceManager.getStartTime());
            assertNull(serviceManager.getService());
            assertNull(serviceMonitor.registered);
            assertNull(serviceMonitor.starting);
            assertNull(serviceMonitor.waitingToStart);
            assertNull(serviceMonitor.startError);
            assertNull(serviceMonitor.running);
            assertNotNull(serviceMonitor.stopping);
            assertNull(serviceMonitor.waitingToStop);
            if (!stopCondition.satisfied && stopStrategy == StopStrategies.FORCE) {
                assertNotNull(serviceMonitor.stopError);
            } else {
                assertNull(serviceMonitor.stopError);
            }
            assertNotNull(serviceMonitor.stopped);
            assertNotNull(serviceMonitor.unregistered);
            if (!stopCondition.satisfied && stopStrategy == StopStrategies.FORCE) {
                assertEquals(serviceMonitor.stopError.getEventId() + 1, serviceMonitor.stopping.getEventId());
                assertEquals(serviceMonitor.stopError.getEventId() + 2, serviceMonitor.stopped.getEventId());
                assertEquals(serviceMonitor.stopError.getEventId() + 3, serviceMonitor.unregistered.getEventId());
            } else {
                assertEquals(serviceMonitor.stopping.getEventId() + 1, serviceMonitor.stopped.getEventId());
                assertEquals(serviceMonitor.stopping.getEventId() + 2, serviceMonitor.unregistered.getEventId());

            }
            assertFalse(startCondition.initializeCalled);
            assertFalse(startCondition.isSatisfiedCalled);
            assertTrue(startCondition.destroyCalled);
            // There is no way to determine if the init should have been called
            // stopCondition.initializeCalled
            assertTrue(stopCondition.isSatisfiedCalled);
            assertTrue(stopCondition.destroyCalled);
        } else {
            assertSame(ServiceState.STOPPED, serviceManager.getState());
            assertEquals(0, serviceManager.getStartTime());
            assertNull(serviceManager.getService());
            assertNull(serviceMonitor.registered);
            assertNull(serviceMonitor.starting);
            assertNull(serviceMonitor.waitingToStart);
            assertNull(serviceMonitor.startError);
            assertNull(serviceMonitor.running);
            assertNotNull(serviceMonitor.stopping);
            assertNull(serviceMonitor.waitingToStop);
            assertNotNull(serviceMonitor.stopError);
            assertNotNull(serviceMonitor.stopped);
            assertNotNull(serviceMonitor.unregistered);
            assertEquals(serviceMonitor.stopping.getEventId() + 1, serviceMonitor.stopError.getEventId());
            assertEquals(serviceMonitor.stopping.getEventId() + 2, serviceMonitor.stopped.getEventId());
            assertEquals(serviceMonitor.stopping.getEventId() + 3, serviceMonitor.unregistered.getEventId());
            assertFalse(startCondition.initializeCalled);
            assertFalse(startCondition.isSatisfiedCalled);
            assertTrue(startCondition.destroyCalled);
            assertTrue(stopCondition.initializeCalled);
            assertTrue(stopCondition.isSatisfiedCalled);
            assertTrue(stopCondition.destroyCalled);
        }
    }

    protected void setUp() throws Exception {
        super.setUp();
        serviceManager = new ServiceManager(kernel,
                0,
                serviceName,
                serviceFactory,
                classLoader,
                serviceMonitor,
                10,
                TimeUnit.SECONDS);
    }

    private class MockServiceFactory extends StaticServiceFactory {
        private boolean restartable = true;
        private boolean throwExceptionFromCreate = false;
        private boolean throwExceptionFromDestroy = false;
        private MockCreateException createException;
        private MockDestroyException destroyException;
        private ServiceContext serviceContext;

        private MockServiceFactory() throws NullPointerException {
            super(SERVICE);
        }

        public Set getOwnedServices() {
            return Collections.singleton(ownedServiceName);
        }

        public boolean isRestartable() {
            return restartable;
        }

        public synchronized Set getStartConditions() {
            Set startConditions = new HashSet(super.getStartConditions());
            startConditions.add(startCondition);
            return startConditions;
        }

        public synchronized Set getStopConditions() {
            Set stopConditions = new HashSet(super.getStopConditions());
            stopConditions.add(stopCondition);
            return stopConditions;
        }

        public Object createService(ServiceContext serviceContext) {
            assertValidServiceContext(serviceContext);
            this.serviceContext = serviceContext;

            createException = new MockCreateException("MockCreateException");
            if (throwExceptionFromCreate) throw createException;
            return super.createService(serviceContext);
        }

        public void destroyService(ServiceContext serviceContext) {
            assertSame(this.serviceContext, serviceContext);
            assertValidServiceContext(serviceContext);

            destroyException = new MockDestroyException("MockDestroyException");
            if (throwExceptionFromDestroy) throw destroyException;
            super.destroyService(serviceContext);
        }

        private void assertValidServiceContext(ServiceContext serviceContext) {
            assertSame(serviceName, serviceContext.getServiceName());
            assertSame(kernel, serviceContext.getKernel());
            assertSame(classLoader, serviceContext.getClassLoader());
        }

    }

    private static class MockCreateException extends RuntimeException {
        private MockCreateException(String message) {
            super(message);
        }
    }

    private static class MockDestroyException extends RuntimeException {
        private MockDestroyException(String message) {
            super(message);
        }
    }

    private class MockStartCondition implements ServiceCondition {
        private boolean satisfied = true;
        private boolean initializeCalled = false;
        private boolean isSatisfiedCalled = false;
        private boolean destroyCalled = false;
        private ServiceConditionContext context;
        private CountDownLatch isSatisfiedSignal;

        private void reset() {
            initializeCalled = false;
            isSatisfiedCalled = false;
            destroyCalled = false;
        }

        public void initialize(ServiceConditionContext context) {
            assertValidServiceConditionContext(context);
            initializeCalled = true;
            this.context = context;
        }

        public boolean isSatisfied() {
            isSatisfiedCalled = true;
            if (isSatisfiedSignal != null) {
                isSatisfiedSignal.countDown();
            }
            return satisfied;
        }

        public void destroy() {
            destroyCalled = true;
        }
    }

    private class MockStopCondition implements ServiceCondition {
        private boolean satisfied = true;
        private boolean initializeCalled = false;
        private boolean isSatisfiedCalled = false;
        private boolean destroyCalled = false;
        private ServiceConditionContext context;
        private CountDownLatch isStatisfiedSignal;

        private void reset() {
            initializeCalled = false;
            isSatisfiedCalled = false;
            destroyCalled = false;
        }

        public void initialize(ServiceConditionContext context) {
            assertValidServiceConditionContext(context);
            initializeCalled = true;
            this.context = context;
        }

        public boolean isSatisfied() {
            isSatisfiedCalled = true;
            if (isStatisfiedSignal != null) {
                isStatisfiedSignal.countDown();
            }
            return satisfied;
        }

        public void destroy() {
            destroyCalled = true;
        }
    }

    private void assertValidServiceConditionContext(ServiceConditionContext context) {
        assertSame(serviceName, context.getServiceName());
        assertSame(kernel, context.getKernel());
        assertSame(classLoader, context.getClassLoader());
    }

    private class MockServiceMonitor implements ServiceMonitor {
        private ServiceEvent registered;
        private ServiceEvent starting;
        private ServiceEvent waitingToStart;
        private ServiceEvent startError;
        private ServiceEvent running;
        private ServiceEvent stopping;
        private ServiceEvent waitingToStop;
        private ServiceEvent stopError;
        private ServiceEvent stopped;
        private ServiceEvent unregistered;

        private void reset() {
            registered = null;
            starting = null;
            waitingToStart = null;
            startError = null;
            running = null;
            stopping = null;
            waitingToStop = null;
            stopError = null;
            stopped = null;
            unregistered = null;
        }

        public void serviceRegistered(ServiceEvent serviceEvent) {
            registered = serviceEvent;
            assertValidEvent(serviceEvent, false);
        }

        public void serviceStarting(ServiceEvent serviceEvent) {
            starting = serviceEvent;
            assertValidEvent(serviceEvent, false);
        }

        public void serviceWaitingToStart(ServiceEvent serviceEvent) {
            waitingToStart = serviceEvent;
            assertValidEvent(serviceEvent, false);
        }

        public void serviceStartError(ServiceEvent serviceEvent) {
            startError = serviceEvent;
            assertValidEvent(serviceEvent, false);
        }

        public void serviceRunning(ServiceEvent serviceEvent) {
            running = serviceEvent;
            assertValidEvent(serviceEvent, true);
        }

        public void serviceStopping(ServiceEvent serviceEvent) {
            stopping = serviceEvent;
            assertValidEvent(serviceEvent, false);
        }

        public void serviceWaitingToStop(ServiceEvent serviceEvent) {
            waitingToStop = serviceEvent;
            assertValidEvent(serviceEvent, true);
        }

        public void serviceStopError(ServiceEvent serviceEvent) {
            stopError = serviceEvent;
            assertValidEvent(serviceEvent, false);
        }

        public void serviceStopped(ServiceEvent serviceEvent) {
            stopped = serviceEvent;
            assertValidEvent(serviceEvent, false);
        }

        public void serviceUnregistered(ServiceEvent serviceEvent) {
            unregistered = serviceEvent;
            assertValidEvent(serviceEvent, false);
        }

        private void assertValidEvent(ServiceEvent serviceEvent, boolean mustHaveService) {
            assertSame(serviceName, serviceEvent.getServiceName());
            assertSame(kernel, serviceEvent.getKernel());
            assertSame(classLoader, serviceEvent.getClassLoader());
            assertSame(serviceFactory, serviceEvent.getServiceFactory());
            if (mustHaveService) {
                assertSame(SERVICE, serviceEvent.getService());
            }
        }
    }

    private static class MockKernel implements Kernel {
        private List startRecursive = new LinkedList();

        private void reset() {
            startRecursive.clear();
        }

        public void startServiceRecursive(ServiceName serviceName) throws ServiceNotFoundException, IllegalStateException {
            startRecursive.add(serviceName);
        }

        public void startServiceRecursive(ServiceName serviceName, StartStrategy startStrategy) throws ServiceNotFoundException, IllegalServiceStateException, UnsatisfiedConditionsException, Exception {
            startRecursive.add(serviceName);
        }

        //
        // Unimplemented methods
        //
        public void waitForDestruction() {
            throw new UnsupportedOperationException();
        }

        public void startService(ServiceName serviceName) throws ServiceNotFoundException, IllegalStateException {
            throw new UnsupportedOperationException();
        }

        public void startService(ServiceName serviceName, StartStrategy startStrategy) throws ServiceNotFoundException, IllegalServiceStateException, UnsatisfiedConditionsException, Exception {
            throw new UnsupportedOperationException();
        }

        public void stopService(ServiceName serviceName) throws ServiceNotFoundException {
            throw new UnsupportedOperationException();
        }

        public void stopService(ServiceName serviceName, StopStrategy stopStrategy) throws ServiceNotFoundException, UnsatisfiedConditionsException {
            throw new UnsupportedOperationException();
        }

        public void destroy() {
            throw new UnsupportedOperationException();
        }

        public boolean isRunning() {
            throw new UnsupportedOperationException();
        }

        public String getKernelName() {
            throw new UnsupportedOperationException();
        }

        public void registerService(ServiceName serviceName, ServiceFactory serviceFactory, ClassLoader classLoader) throws ServiceAlreadyExistsException, ServiceRegistrationException {
            throw new UnsupportedOperationException();
        }

        public void unregisterService(ServiceName serviceName) throws ServiceNotFoundException, IllegalStateException {
            throw new UnsupportedOperationException();
        }

        public void unregisterService(ServiceName serviceName, StopStrategy stopStrategy) throws ServiceNotFoundException, ServiceRegistrationException {
            throw new UnsupportedOperationException();
        }

        public boolean isRegistered(ServiceName serviceName) {
            throw new UnsupportedOperationException();
        }

        public ServiceState getServiceState(ServiceName serviceName) throws ServiceNotFoundException {
            throw new UnsupportedOperationException();
        }

        public long getServiceStartTime(ServiceName serviceName) throws ServiceNotFoundException {
            throw new UnsupportedOperationException();
        }

        public boolean isServiceEnabled(ServiceName serviceName) throws ServiceNotFoundException {
            throw new UnsupportedOperationException();
        }

        public void setServiceEnabled(ServiceName serviceName, boolean enabled) throws ServiceNotFoundException {
            throw new UnsupportedOperationException();
        }

        public Object getService(ServiceName serviceName) throws ServiceNotFoundException, IllegalArgumentException {
            throw new UnsupportedOperationException();
        }

        public Object getService(Class type) {
            throw new UnsupportedOperationException();
        }

        public List getServices(Class type) {
            throw new UnsupportedOperationException();
        }

        public ServiceFactory getServiceFactory(ServiceName serviceName) throws ServiceNotFoundException {
            throw new UnsupportedOperationException();
        }

        public ServiceFactory getServiceFactory(Class type) {
            throw new UnsupportedOperationException();
        }

        public List getServiceFactories(Class type) {
            throw new UnsupportedOperationException();
        }

        public ClassLoader getClassLoaderFor(ServiceName serviceName) throws ServiceNotFoundException {
            throw new UnsupportedOperationException();
        }

        public void addKernelMonitor(KernelMonitor kernelMonitor) {
            throw new UnsupportedOperationException();
        }

        public void removeKernelMonitor(KernelMonitor kernelMonitor) {
            throw new UnsupportedOperationException();
        }

        public void addServiceMonitor(ServiceMonitor serviceMonitor) {
            throw new UnsupportedOperationException();
        }

        public void addServiceMonitor(ServiceMonitor serviceMonitor, ServiceName serviceName) {
            throw new UnsupportedOperationException();
        }

        public void removeServiceMonitor(ServiceMonitor serviceMonitor) {
            throw new UnsupportedOperationException();
        }
    }
}
