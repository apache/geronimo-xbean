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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.emory.mathcs.backport.java.util.concurrent.Executor;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import junit.framework.TestCase;
import org.gbean.kernel.IllegalServiceStateException;
import org.gbean.kernel.Kernel;
import org.gbean.kernel.KernelMonitor;
import org.gbean.kernel.ServiceAlreadyExistsException;
import org.gbean.kernel.ServiceCondition;
import org.gbean.kernel.ServiceConditionContext;
import org.gbean.kernel.ServiceContext;
import org.gbean.kernel.ServiceEvent;
import org.gbean.kernel.ServiceFactory;
import org.gbean.kernel.ServiceMonitor;
import org.gbean.kernel.ServiceName;
import org.gbean.kernel.ServiceNotFoundException;
import org.gbean.kernel.ServiceRegistrationException;
import org.gbean.kernel.ServiceState;
import org.gbean.kernel.StartStrategies;
import org.gbean.kernel.StartStrategy;
import org.gbean.kernel.StaticServiceFactory;
import org.gbean.kernel.StopStrategies;
import org.gbean.kernel.StopStrategy;
import org.gbean.kernel.StringServiceName;
import org.gbean.kernel.UnsatisfiedConditionsException;
import org.gbean.kernel.UnregisterServiceException;

/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class ServiceManagerTest extends TestCase {
    private static final Object SERVICE = new Object();
    private MockKernel kernel = new MockKernel();
    private StringServiceName serviceName = new StringServiceName("Service");
    private StringServiceName ownedServiceName = new StringServiceName("OwnedService");
    private MockStartCondition startCondition = new MockStartCondition();
    private MockStopCondition stopCondition = new MockStopCondition();
    private MockServiceFactory serviceFactory = new MockServiceFactory();
    private ClassLoader classLoader = new URLClassLoader(new URL[0]);
    private MockServiceMonitor serviceMonitor = new MockServiceMonitor();
    private ServiceManager serviceManager;

    public void testInitialState() {
        assertSame(serviceName, serviceManager.getServiceName());
        assertSame(serviceFactory, serviceManager.getServiceFactory());
        assertSame(classLoader, serviceManager.getClassLoader());
        assertEquals(0, serviceManager.getStartTime());
        assertNull(serviceManager.getService());
        assertSame(ServiceState.STOPPED, serviceManager.getState());
    }

    public void testInitializeDestroy() throws Exception {
        initialize();
        destroy();
        initialize();
        destroy();
    }

    public void testStartStop() throws Exception {
        startStop(StartStrategies.ASYNCHRONOUS);
        startStop(StartStrategies.SYNCHRONOUS);
        startStop(StartStrategies.UNREGISTER);
        startStop(StartStrategies.BLOCK);

        startStop(StartStrategies.ASYNCHRONOUS);
        startStop(StartStrategies.SYNCHRONOUS);
        startStop(StartStrategies.UNREGISTER);
        startStop(StartStrategies.BLOCK);
    }

    private void startStop(StartStrategy startStrategy) throws Exception {
        start(false, startStrategy);
        stop(StopStrategies.SYNCHRONOUS);

        start(false, startStrategy);
        start(false, startStrategy);
        stop(StopStrategies.SYNCHRONOUS);
        stop(StopStrategies.SYNCHRONOUS);
    }

    public void testStartException() throws Exception {
        startException(StartStrategies.ASYNCHRONOUS);
        startException(StartStrategies.SYNCHRONOUS);
        startException(StartStrategies.UNREGISTER);
        startException(StartStrategies.BLOCK);

        startException(StartStrategies.ASYNCHRONOUS);
        startException(StartStrategies.SYNCHRONOUS);
        startException(StartStrategies.UNREGISTER);
        startException(StartStrategies.BLOCK);
    }

    private void startException(StartStrategy startStrategy) throws Exception {
        serviceFactory.throwExceptionFromCreate = true;
        try {
            start(false, startStrategy);
        } catch (MockCreateException e) {
            assertTrue(startStrategy == StartStrategies.SYNCHRONOUS || startStrategy == StartStrategies.BLOCK);
            assertEquals(serviceFactory.createException, e);
        } catch (UnregisterServiceException e) {
            assertEquals(StartStrategies.UNREGISTER, startStrategy);
            assertSame(serviceFactory.createException, e.getCause());
        }
        stop(StopStrategies.SYNCHRONOUS);
    }

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
            assertSame(serviceFactory.createException, e.getCause());
        }
        stop(stopStrategy);
    }

    //todo
    public void testStartWaiting() throws Exception {
        startCondition.satisfied = false;
        start(false, StartStrategies.ASYNCHRONOUS);
        stop(StopStrategies.ASYNCHRONOUS);

        start(false, StartStrategies.ASYNCHRONOUS);
        stop(StopStrategies.ASYNCHRONOUS);
    }

    //todo
    public void testStartWaitingStart() throws Exception {
        startCondition.satisfied = false;
        start(false, StartStrategies.ASYNCHRONOUS);
        start(false, StartStrategies.ASYNCHRONOUS);
        startCondition.satisfied = true;
        start(false, StartStrategies.ASYNCHRONOUS);
        start(false, StartStrategies.ASYNCHRONOUS);
        stop(StopStrategies.ASYNCHRONOUS);

        startCondition.satisfied = false;
        start(false, StartStrategies.ASYNCHRONOUS);
        start(false, StartStrategies.ASYNCHRONOUS);
        startCondition.satisfied = true;
        start(false, StartStrategies.ASYNCHRONOUS);
        start(false, StartStrategies.ASYNCHRONOUS);
        stop(StopStrategies.ASYNCHRONOUS);
    }

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
        }

        // move to starting disable, move to running, and try to restart
        serviceFactory.setEnabled(true);
        startCondition.satisfied = false;
        start(false, StartStrategies.ASYNCHRONOUS);

        serviceFactory.setEnabled(false);
//        try {
//            start(false, startStrategy);
//        } catch (IllegalServiceStateException e) {
//            assertTrue(startStrategy == StartStrategies.UNREGISTER);
//        } catch (UnsatisfiedConditionsException e) {
//            assertTrue(startStrategy == StartStrategies.SYNCHRONOUS);
//            assertTrue(e.getUnsatisfiedConditions().contains(startCondition));
//        } catch (UnregisterServiceException e) {
//            assertEquals(StartStrategies.UNREGISTER, startStrategy);
//            UnsatisfiedConditionsException cause = (UnsatisfiedConditionsException) e.getCause();
//            assertTrue(cause.getUnsatisfiedConditions().contains(startCondition));
//        }

        startCondition.satisfied = true;
        try {
            start(false, startStrategy);
        } catch (IllegalServiceStateException e) {
            assertTrue(startStrategy == StartStrategies.UNREGISTER);
        }
        try {
            start(false, startStrategy);
        } catch (IllegalServiceStateException e) {
            assertTrue(startStrategy == StartStrategies.UNREGISTER);
        }
        stop(StopStrategies.ASYNCHRONOUS);
        try {
            serviceManager.start(false, startStrategy);
            fail("A disabled service should throw an IllegalServiceStateException from start");
        } catch (IllegalServiceStateException e) {
            // expected
        }
    }

    public void testStartRecursive() throws Exception {
        startRecursive(StartStrategies.ASYNCHRONOUS);
        startRecursive(StartStrategies.SYNCHRONOUS);
        startRecursive(StartStrategies.UNREGISTER);
        startRecursive(StartStrategies.BLOCK);

        startRecursive(StartStrategies.ASYNCHRONOUS);
        startRecursive(StartStrategies.SYNCHRONOUS);
        startRecursive(StartStrategies.UNREGISTER);
        startRecursive(StartStrategies.BLOCK);
    }

    private void startRecursive(StartStrategy startStrategy) throws Exception {
        start(true, startStrategy);
        stop(StopStrategies.ASYNCHRONOUS);
    }

    public void testExceptionStartRecursive() throws Exception {
        exceptionStartRecursive(StartStrategies.ASYNCHRONOUS);
        exceptionStartRecursive(StartStrategies.SYNCHRONOUS);
        exceptionStartRecursive(StartStrategies.UNREGISTER);
        exceptionStartRecursive(StartStrategies.BLOCK);
    }

    private void exceptionStartRecursive(StartStrategy startStrategy) throws Exception {
        serviceFactory.throwExceptionFromCreate = true;
        try {
            start(true, startStrategy);
        } catch (MockCreateException e) {
            assertTrue(startStrategy == StartStrategies.SYNCHRONOUS || startStrategy == StartStrategies.BLOCK);
            assertEquals(serviceFactory.createException, e);
        } catch (UnregisterServiceException e) {
            assertEquals(StartStrategies.UNREGISTER, startStrategy);
            assertSame(serviceFactory.createException, e.getCause());
        }
        stop(StopStrategies.ASYNCHRONOUS);
    }

    public void testWaitingStartRecursive() throws Exception {
        startCondition.satisfied = false;
        start(true, StartStrategies.ASYNCHRONOUS);
        start(true, StartStrategies.ASYNCHRONOUS);
        startCondition.satisfied = true;
        start(true, StartStrategies.ASYNCHRONOUS);
        start(true, StartStrategies.ASYNCHRONOUS);
        stop(StopStrategies.ASYNCHRONOUS);

        startCondition.satisfied = false;
        start(true, StartStrategies.ASYNCHRONOUS);
        start(true, StartStrategies.ASYNCHRONOUS) ;
        startCondition.satisfied = true;
        start(true, StartStrategies.ASYNCHRONOUS);
        start(true, StartStrategies.ASYNCHRONOUS);
        stop(StopStrategies.ASYNCHRONOUS);
    }

    public void testWaitingStop() throws Exception {
        start(false, StartStrategies.ASYNCHRONOUS);
        stopCondition.satisfied = false;
        stop(StopStrategies.ASYNCHRONOUS);
        stop(StopStrategies.ASYNCHRONOUS);
        stopCondition.satisfied = true;
        stop(StopStrategies.ASYNCHRONOUS);

        start(false, StartStrategies.ASYNCHRONOUS);
        stopCondition.satisfied = false;
        stop(StopStrategies.ASYNCHRONOUS);
        stop(StopStrategies.ASYNCHRONOUS);
        stopCondition.satisfied = true;
        stop(StopStrategies.ASYNCHRONOUS);
    }

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
        } catch (IllegalServiceStateException excpected) {
            // expected
        }
        stopCondition.satisfied = true;
        stop(StopStrategies.ASYNCHRONOUS);
    }

    private void initialize() throws Exception {
        serviceMonitor.reset();
        serviceManager.initialize();

        assertSame(serviceName, serviceManager.getServiceName());
        assertSame(serviceFactory, serviceManager.getServiceFactory());
        assertSame(classLoader, serviceManager.getClassLoader());
        assertEquals(0, serviceManager.getStartTime());
        assertNull(serviceManager.getService());
        assertSame(ServiceState.STOPPED, serviceManager.getState());
        // verify expected events fired
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
        } else if (!stopCondition.satisfied) {
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
            assertNull(serviceMonitor.stopError);
            assertNotNull(serviceMonitor.stopped);
            assertNull(serviceMonitor.unregistered);

            // verify events fired in the correct order
            if (initialState != ServiceState.STOPPING) {
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

    private void destroy() throws UnsatisfiedConditionsException, IllegalServiceStateException {
        serviceMonitor.reset();
        serviceManager.destroy(StopStrategies.SYNCHRONOUS);

        assertSame(serviceName, serviceManager.getServiceName());
        assertSame(serviceFactory, serviceManager.getServiceFactory());
        assertSame(classLoader, serviceManager.getClassLoader());
        assertEquals(0, serviceManager.getStartTime());
        assertNull(serviceManager.getService());
        assertSame(ServiceState.STOPPED, serviceManager.getState());
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
    }

    protected void setUp() throws Exception {
        super.setUp();
        serviceManager = new ServiceManager(kernel,
                serviceName,
                serviceFactory,
                classLoader,
                serviceMonitor,
                new Executor() {
                    public void execute(Runnable command) {
                        command.run();
                    }
                },
                10,
                TimeUnit.SECONDS);
    }

    private class MockServiceFactory extends StaticServiceFactory {
        boolean restartable = true;
        boolean throwExceptionFromCreate = false;
        boolean throwExceptionFromDestroy = false;
        MockCreateException createException;
        MockDestroyException destroyException;

        private MockServiceFactory() throws NullPointerException {
            super(SERVICE);
        }

        public Set getOwnedServices() {
            return Collections.singleton(ownedServiceName);
        }

        public boolean isRestartable() {
            return restartable;
        }

        public Set getStartConditions() {
            Set startConditions = new HashSet(super.getStartConditions());
            startConditions.add(startCondition);
            return startConditions;
        }

        public Set getStopConditions() {
            Set stopConditions = new HashSet(super.getStopConditions());
            stopConditions.add(stopCondition);
            return stopConditions;
        }

        public Object createService(ServiceContext serviceContext) {
            createException = new MockCreateException();
            if (throwExceptionFromCreate) throw createException;
            return super.createService(serviceContext);
        }

        public void destroyService(ServiceContext serviceContext) {
            destroyException = new MockDestroyException();
            if (throwExceptionFromDestroy) throw destroyException;
            super.destroyService(serviceContext);
        }
    }

    private static class MockCreateException extends RuntimeException {
    }

    private static class MockDestroyException extends RuntimeException {
    }

    private static class MockStartCondition implements ServiceCondition {
        boolean satisfied = true;
        boolean initializeCalled = false;
        boolean isSatisfiedCalled = false;
        boolean destroyCalled = false;

        private void reset() {
            initializeCalled = false;
            isSatisfiedCalled = false;
            destroyCalled = false;
        }

        public void initialize(ServiceConditionContext context) {
            initializeCalled = true;
        }

        public boolean isSatisfied() {
            isSatisfiedCalled = true;
            return satisfied;
        }

        public void destroy() {
            destroyCalled = true;
        }
    }

    private static class MockStopCondition implements ServiceCondition {
        boolean satisfied = true;
        boolean initializeCalled = false;
        boolean isSatisfiedCalled = false;
        boolean destroyCalled = false;

        private void reset() {
            initializeCalled = false;
            isSatisfiedCalled = false;
            destroyCalled = false;
        }

        public void initialize(ServiceConditionContext context) {
            initializeCalled = true;
        }

        public boolean isSatisfied() {
            isSatisfiedCalled = true;
            return satisfied;
        }

        public void destroy() {
            destroyCalled = true;
        }
    }

    private static class MockServiceMonitor implements ServiceMonitor {
        ServiceEvent registered;
        ServiceEvent starting;
        ServiceEvent waitingToStart;
        ServiceEvent startError;
        ServiceEvent running;
        ServiceEvent stopping;
        ServiceEvent waitingToStop;
        ServiceEvent stopError;
        ServiceEvent stopped;
        ServiceEvent unregistered;

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
        }

        public void serviceStarting(ServiceEvent serviceEvent) {
            starting = serviceEvent;
        }

        public void serviceWaitingToStart(ServiceEvent serviceEvent) {
            waitingToStart = serviceEvent;
        }

        public void serviceStartError(ServiceEvent serviceEvent) {
            startError = serviceEvent;
        }

        public void serviceRunning(ServiceEvent serviceEvent) {
            running = serviceEvent;
        }

        public void serviceStopping(ServiceEvent serviceEvent) {
            stopping = serviceEvent;
        }

        public void serviceWaitingToStop(ServiceEvent serviceEvent) {
            waitingToStop = serviceEvent;
        }

        public void serviceStopError(ServiceEvent serviceEvent) {
            stopError = serviceEvent;
        }

        public void serviceStopped(ServiceEvent serviceEvent) {
            stopped = serviceEvent;
        }

        public void serviceUnregistered(ServiceEvent serviceEvent) {
            unregistered = serviceEvent;
        }
    }

    private static class MockKernel implements Kernel {
        List startRecursive = new LinkedList();

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

        public ServiceFactory getServiceFactory(ServiceName serviceName) throws ServiceNotFoundException {
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
