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

package org.gbean.jmx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.JMException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gbean.kernel.Kernel;
import org.gbean.kernel.LifecycleListener;
import org.gbean.kernel.NoSuchAttributeException;
import org.gbean.kernel.NoSuchOperationException;
import org.gbean.kernel.OperationSignature;
import org.gbean.kernel.ServiceNotFoundException;
import org.gbean.kernel.runtime.ServiceState;
import org.gbean.reflect.OperationInvoker;
import org.gbean.reflect.PropertyInvoker;
import org.gbean.reflect.ServiceInvoker;
import org.gbean.reflect.ServiceInvokerManager;
import org.gbean.service.ConfigurableServiceFactory;
import org.gbean.service.ServiceFactory;

/**
 * @version $Rev: 109772 $ $Date: 2004-12-03 21:06:02 -0800 (Fri, 03 Dec 2004) $
 */
public final class ServiceMBean implements DynamicMBean, NotificationEmitter {
    private static final Log log = LogFactory.getLog(ServiceMBean.class);

    private static final String ATTRIBUTE_CLASS_LOADER = "classLoader";
    private static final String ATTRIBUTE_STATE = "state";
    private static final String ATTRIBUTE_START_TIME = "startTime";
    private static final String ATTRIBUTE_STATE_MANAGEABLE = "stateManageable";
    private static final String ATTRIBUTE_STATISTICS_PROVIDER = "statisticsProvider";
    private static final String ATTRIBUTE_EVENT_PROVIDER = "eventProvider";
    private static final String ATTRIBUTE_EVENT_TYPES = "eventTypes";
    private static final String ATTRIBUTE_GBEAN_ENABLED = "gbeanEnabled";

    private static final String OPERATION_START = "start";
    private static final String OPERATION_START_RECURSIVE = "startRecursive";
    private static final String OPERATION_STOP = "stop";

    /**
     * The kernel
     */
    private final Kernel kernel;

    /**
     * The unique name of this service.
     */
    private final ObjectName objectName;

    /**
     * The broadcaster for notifications
     */
    private final NotificationBroadcasterSupport notificationBroadcaster = new NotificationBroadcasterSupport();

    /**
     * Listenes for kernel lifecycle events for this service and broadcasts them via JMX.
     */
    private final LifecycleBridge lifecycleBridge;

    /**
     * The service invocation manager from which we get the service invoker
     */
    private final ServiceInvokerManager serviceInvokerManager;

    /**
     * The factory for this service
     */
    private final ServiceFactory serviceFactory;

    /**
     * The service invoker
     */
    private ServiceInvoker serviceInvoker;

    public ServiceMBean(Kernel kernel, ServiceInvokerManager serviceInvokerManager, ObjectName objectName) throws ServiceNotFoundException {
        this.kernel = kernel;
        serviceFactory = kernel.getServiceFactory(objectName);
        this.serviceInvokerManager = serviceInvokerManager;
        this.objectName = objectName;
        lifecycleBridge = new LifecycleBridge(notificationBroadcaster);
    }

    public ObjectName getObjectName() {
        return objectName;
    }

    public ObjectName preRegister(MBeanServer mBeanServer, ObjectName objectName) throws Exception {
        return objectName;
    }

    public synchronized void postRegister(Boolean registrationDone) {
        if (Boolean.TRUE.equals(registrationDone)) {
            // fire the loaded event from the mbean.. it was already fired from the GBeanInstance when it was created
            kernel.addLifecycleListener(lifecycleBridge, objectName);
            lifecycleBridge.loaded(objectName);
            updateState();
        }
    }

    public synchronized void preDeregister() {
        kernel.removeLifecycleListener(lifecycleBridge);
        lifecycleBridge.unloaded(objectName);
        serviceInvoker = null;
    }

    public void postDeregister() {
    }

    private synchronized void updateState() {
        try {
            int serviceState = kernel.getServiceState(objectName);
            boolean running = serviceState == ServiceState.RUNNING_INDEX || serviceState == ServiceState.STOPPING_INDEX;
            if (running) {
                serviceInvoker = serviceInvokerManager.getServiceInvoker(objectName);
                return;
            }
        } catch (Exception e) {
            // ignore cleaned up below
        }
        serviceInvoker = null;
    }

    public synchronized MBeanInfo getMBeanInfo() {
        try {
            String className;
            String description = "No description available";
            MBeanAttributeInfo[] attributes;
            MBeanOperationInfo[] operations;

            if (serviceInvoker != null) {
                className = serviceInvoker.getServiceType().getName();

                // attributes
                List propertyIndex = serviceInvoker.getPropertyIndex();
                attributes = new MBeanAttributeInfo[propertyIndex.size()];
                for (ListIterator iterator = propertyIndex.listIterator(); iterator.hasNext();) {
                    PropertyInvoker propertyInvoker = (PropertyInvoker) iterator.next();

                    boolean isIs = false;
                    if (propertyInvoker.isReadable()) {
                        isIs = propertyInvoker.getGetterSignature().getName().startsWith("is");
                    }

                    attributes[iterator.previousIndex()] = new MBeanAttributeInfo(propertyInvoker.getPropertyName(),
                            propertyInvoker.getType().getName(),
                            "no description available",
                            propertyInvoker.isReadable(),
                            propertyInvoker.isWritable(),
                            isIs);
                }


                // operations
                List operationIndex = serviceInvoker.getOperationIndex();
                operations = new MBeanOperationInfo[operationIndex.size()];
                for (ListIterator iterator = operationIndex.listIterator(); iterator.hasNext();) {
                    OperationInvoker operationInvoker = (OperationInvoker) iterator.next();

                    OperationSignature signature = operationInvoker.getSignature();

                    List argumentTypes = signature.getParameterTypes();
                    MBeanParameterInfo[] parameters = new MBeanParameterInfo[argumentTypes.size()];
                    for (ListIterator argIterator = argumentTypes.listIterator(); argIterator.hasNext();) {
                        String type = (String) argIterator.next();
                        parameters[argIterator.previousIndex()] = new MBeanParameterInfo("parameter" + argIterator.previousIndex(),
                                type,
                                "no description available");
                    }

                    operations[iterator.previousIndex()] = new MBeanOperationInfo(signature.getName(), "no description available", parameters, "java.lang.Object", MBeanOperationInfo.UNKNOWN);
                }
            } else {
                className = Object.class.getName();
                operations = new MBeanOperationInfo[0];
                if (serviceFactory instanceof ConfigurableServiceFactory) {
                    ConfigurableServiceFactory configurableServiceFactory = (ConfigurableServiceFactory) serviceFactory;
                    List propertyNames = new ArrayList(configurableServiceFactory.getPropertyNames());
                    attributes = new MBeanAttributeInfo[propertyNames.size()];
                    for (ListIterator iterator = propertyNames.listIterator(); iterator.hasNext();) {
                        String propertyName = (String) iterator.next();
                        attributes[iterator.previousIndex()] = new MBeanAttributeInfo(propertyName,
                                Object.class.getName(),
                                "no description available",
                                true,
                                true,
                                false);
                    }
                } else {
                    attributes = new MBeanAttributeInfo[0];
                }

            }

            MBeanNotificationInfo[] notifications = new MBeanNotificationInfo[1];
            notifications[0] = new MBeanNotificationInfo(NotificationType.TYPES, "javax.management.Notification", "J2EE Notifications");

            MBeanInfo mbeanInfo = new MBeanInfo(className, description, attributes, new MBeanConstructorInfo[0], operations, notifications);
            return mbeanInfo;
        } catch (RuntimeException e) {
            log.info("Unable to create MBeanInfo", e);
            throw e;
        }
    }

    public Object getAttribute(String attributeName) throws ReflectionException, AttributeNotFoundException {
        try {
            if (ATTRIBUTE_CLASS_LOADER.equals(attributeName)) {
                return kernel.getClassLoaderFor(objectName);
            } else if (ATTRIBUTE_STATE.equals(attributeName)) {
                return new Integer(kernel.getServiceState(objectName));
            } else if (ATTRIBUTE_START_TIME.equals(attributeName)) {
                return new Long(kernel.getServiceStartTime(objectName));
            } else if (ATTRIBUTE_STATE_MANAGEABLE.equals(attributeName)) {
                return Boolean.TRUE;
            } else if (ATTRIBUTE_STATISTICS_PROVIDER.equals(attributeName)) {
                return Boolean.FALSE;
            } else if (ATTRIBUTE_EVENT_PROVIDER.equals(attributeName)) {
                return Boolean.TRUE;
            } else if (ATTRIBUTE_EVENT_TYPES.equals(attributeName)) {
                return NotificationType.TYPES;
            } else if (ATTRIBUTE_GBEAN_ENABLED.equals(attributeName)) {
                return Boolean.valueOf(kernel.isServiceEnabled(objectName));
            }

            ServiceInvoker serviceInvoker;
            synchronized (this) {
                serviceInvoker = this.serviceInvoker;
            }

            if (serviceInvoker != null) {
                Object value = serviceInvoker.getAttribute(attributeName);
                return value;
            } else {
                if (!(serviceFactory instanceof ConfigurableServiceFactory)) {
                    throw new AttributeNotFoundException("Service is stopped and the service factory not configurable: objectName=" + objectName + ", propertyName=" + attributeName);
                }
                ConfigurableServiceFactory configurableServiceFactory = (ConfigurableServiceFactory) serviceFactory;
                return configurableServiceFactory.getProperty(attributeName);
            }
        } catch (AttributeNotFoundException e) {
            throw e;
        } catch (NoSuchAttributeException e) {
            throw new AttributeNotFoundException(attributeName);
        } catch (Exception e) {
            throw new ReflectionException(e);
        }
    }

    public void setAttribute(Attribute attribute) throws ReflectionException, AttributeNotFoundException {
        String attributeName = attribute.getName();
        Object attributeValue = attribute.getValue();
        try {
            if (ATTRIBUTE_GBEAN_ENABLED.equals(attributeName)) {
                kernel.setServiceEnabled(objectName, ((Boolean) attributeValue).booleanValue());
            } else {
                ServiceInvoker serviceInvoker;
                synchronized (this) {
                    serviceInvoker = this.serviceInvoker;
                }

                if (serviceInvoker != null) {
                    serviceInvoker.setAttribute(attributeName, attributeValue);
                } else {
                    if (!(serviceFactory instanceof ConfigurableServiceFactory)) {
                        throw new AttributeNotFoundException("Service is stopped and the service factory not configurable: objectName=" + objectName + ", propertyName=" + attributeName);
                    }
                    ConfigurableServiceFactory configurableServiceFactory = (ConfigurableServiceFactory) serviceFactory;
                    configurableServiceFactory.setProperty(attributeName, attributeValue);
                }
            }
        } catch (AttributeNotFoundException e) {
            throw e;
        } catch (NoSuchAttributeException e) {
            throw new AttributeNotFoundException(attributeName);
        } catch (Exception e) {
            throw new ReflectionException(e);
        }
    }

    public AttributeList getAttributes(String[] attributes) {
        AttributeList results = new AttributeList(attributes.length);
        for (int i = 0; i < attributes.length; i++) {
            String name = attributes[i];
            try {
                Object value = getAttribute(name);
                results.add(new Attribute(name, value));
            } catch (JMException e) {
                log.warn("Exception while getting attribute " + name, e);
            }
        }
        return results;
    }

    public AttributeList setAttributes(AttributeList attributes) {
        AttributeList results = new AttributeList(attributes.size());
        for (Iterator iterator = attributes.iterator(); iterator.hasNext();) {
            Attribute attribute = (Attribute) iterator.next();
            try {
                setAttribute(attribute);
                results.add(attribute);
            } catch (JMException e) {
                log.warn("Exception while setting attribute " + attribute.getName(), e);
            }
        }
        return results;
    }

    public Object invoke(String operationName, Object[] arguments, String[] types) throws ReflectionException {
        try {
            if (arguments.length == 0 && OPERATION_START.equals(operationName)) {
                kernel.startService(objectName);
                return null;
            } else if (arguments.length == 0 && OPERATION_START_RECURSIVE.equals(operationName)) {
                kernel.startRecursiveService(objectName);
                return null;
            } else if (arguments.length == 0 && OPERATION_STOP.equals(operationName)) {
                kernel.startService(objectName);
                return null;
            } else {
                ServiceInvoker serviceInvoker;
                synchronized (this) {
                    serviceInvoker = this.serviceInvoker;
                }
                if (serviceInvoker == null) {
                    throw new IllegalStateException("Service is not running: name=" + objectName);
                }
                return serviceInvoker.invoke(operationName, arguments, types);
            }
        } catch (NoSuchOperationException e) {
            throw new ReflectionException(new NoSuchMethodException(new OperationSignature(operationName, types).toString()));
        } catch (Exception e) {
            throw new ReflectionException(e);
        }
    }

    public MBeanNotificationInfo[] getNotificationInfo() {
        return new MBeanNotificationInfo[]{
            new MBeanNotificationInfo(NotificationType.TYPES, "javax.management.Notification", "J2EE Notifications")
        };
    }

    public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
        notificationBroadcaster.addNotificationListener(listener, filter, handback);
    }

    public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
        notificationBroadcaster.removeNotificationListener(listener);
    }

    public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws ListenerNotFoundException {
        notificationBroadcaster.removeNotificationListener(listener, filter, handback);
    }

    public String toString() {
        return objectName.toString();
    }

    private class LifecycleBridge implements LifecycleListener {
        /**
         * Sequence number used for notifications
         */
        private long sequence;

        /**
         * The notification broadcaster to use
         */
        private final NotificationBroadcasterSupport notificationBroadcaster;

        public LifecycleBridge(NotificationBroadcasterSupport notificationBroadcaster) {
            this.notificationBroadcaster = notificationBroadcaster;
        }

        public void loaded(ObjectName objectName) {
            if (objectName.equals(objectName)) {
                notificationBroadcaster.sendNotification(new Notification(NotificationType.OBJECT_CREATED, objectName, nextSequence()));
            }
        }

        public void starting(ObjectName objectName) {
            if (objectName.equals(objectName)) {
                notificationBroadcaster.sendNotification(new Notification(NotificationType.STATE_STARTING, objectName, nextSequence()));
            }
        }

        public void running(ObjectName objectName) {
            if (objectName.equals(objectName)) {
                updateState();
                notificationBroadcaster.sendNotification(new Notification(NotificationType.STATE_RUNNING, objectName, nextSequence()));
            }
        }

        public void stopping(ObjectName objectName) {
            if (objectName.equals(objectName)) {
                notificationBroadcaster.sendNotification(new Notification(NotificationType.STATE_STOPPING, objectName, nextSequence()));
            }
        }

        public void stopped(ObjectName objectName) {
            if (objectName.equals(objectName)) {
                updateState();
                notificationBroadcaster.sendNotification(new Notification(NotificationType.STATE_STOPPED, objectName, nextSequence()));
            }
        }

        public void unloaded(ObjectName objectName) {
            if (objectName.equals(objectName)) {
                updateState();
                notificationBroadcaster.sendNotification(new Notification(NotificationType.OBJECT_DELETED, objectName, nextSequence()));
            }
        }

        public synchronized long nextSequence() {
            return sequence++;
        }
    }
}
