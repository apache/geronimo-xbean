/**
 *
 * Copyright 2005 GBean.org
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
import org.gbean.reflect.OperationInvoker;
import org.gbean.reflect.PropertyInvoker;
import org.gbean.reflect.ServiceInvoker;

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
     * Listenes for kernel lifecycle events for this gbean and broadcasts them via JMX.
     */
    private final LifecycleBridge lifecycleBridge;
    private final ServiceInvoker serviceInvoker;

    public ServiceMBean(Kernel kernel, ObjectName objectName) {
        this.kernel = kernel;
        this.objectName = objectName;
        lifecycleBridge = new LifecycleBridge(objectName, notificationBroadcaster);
        serviceInvoker = new ServiceInvoker(kernel, objectName);
    }

    public ObjectName getObjectName() {
        return objectName;
    }

    public ObjectName preRegister(MBeanServer mBeanServer, ObjectName objectName) throws Exception {
        return objectName;
    }

    public void postRegister(Boolean registrationDone) {
        if (Boolean.TRUE.equals(registrationDone)) {
            // fire the loaded event from the gbeanMBean.. it was already fired from the GBeanInstance when it was created
            kernel.addLifecycleListener(lifecycleBridge, objectName);
            serviceInvoker.start();
            lifecycleBridge.loaded(objectName);
        }
    }

    public void preDeregister() {
        serviceInvoker.stop();
        kernel.removeLifecycleListener(lifecycleBridge);
        lifecycleBridge.unloaded(objectName);
    }

    public void postDeregister() {
    }

    public MBeanInfo getMBeanInfo() {
        String className = serviceInvoker.getServiceType().getName();
        String description = "No description available";

        // attributes
        List propertyIndex = serviceInvoker.getPropertyIndex();
        MBeanAttributeInfo[] attributes = new MBeanAttributeInfo[propertyIndex.size()];
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

        //we don't expose managed constructors
        MBeanConstructorInfo[] constructors = new MBeanConstructorInfo[0];

        // operations
        List operationIndex = serviceInvoker.getOperationIndex();
        MBeanOperationInfo[] operations = new MBeanOperationInfo[operationIndex.size()];
        for (ListIterator iterator = operationIndex.listIterator(); iterator.hasNext();) {
            OperationInvoker operationInvoker = (OperationInvoker) iterator.next();

            OperationSignature signature = operationInvoker.getSignature();

            List argumentTypes = signature.getArgumentTypes();
            MBeanParameterInfo[] parameters = new MBeanParameterInfo[argumentTypes.size()];
            for (ListIterator argIterator = operationIndex.listIterator(); argIterator.hasNext();) {
                String type = (String) argIterator.next();
                parameters[argIterator.previousIndex()] = new MBeanParameterInfo("parameter" + argIterator.previousIndex(),
                        type,
                        "no description available");
            }

            operations[iterator.previousIndex()] = new MBeanOperationInfo(signature.getName(), "no description available", parameters, "java.lang.Object", MBeanOperationInfo.UNKNOWN);
        }

        MBeanNotificationInfo[] notifications = new MBeanNotificationInfo[1];
        notifications[0] = new MBeanNotificationInfo(NotificationType.TYPES, "javax.management.Notification", "J2EE Notifications");

        MBeanInfo mbeanInfo = new MBeanInfo(className, description, attributes, constructors, operations, notifications);
        return mbeanInfo;
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
            return serviceInvoker.getAttribute(attributeName);
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
                serviceInvoker.setAttribute(attributeName, attributeValue);
            }
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

    private static class LifecycleBridge implements LifecycleListener {
        /**
         * Sequence number used for notifications
         */
        private long sequence;

        /**
         * Name of the MBeanGBean
         */
        private final ObjectName mbeanGBeanName;

        /**
         * The notification broadcaster to use
         */
        private final NotificationBroadcasterSupport notificationBroadcaster;

        public LifecycleBridge(ObjectName mbeanGBeanName, NotificationBroadcasterSupport notificationBroadcaster) {
            this.mbeanGBeanName = mbeanGBeanName;
            this.notificationBroadcaster = notificationBroadcaster;
        }

        public void loaded(ObjectName objectName) {
            if (mbeanGBeanName.equals(objectName)) {
                notificationBroadcaster.sendNotification(new Notification(NotificationType.OBJECT_CREATED, objectName, nextSequence()));
            }
        }

        public void starting(ObjectName objectName) {
            if (mbeanGBeanName.equals(objectName)) {
                notificationBroadcaster.sendNotification(new Notification(NotificationType.STATE_STARTING, objectName, nextSequence()));
            }
        }

        public void running(ObjectName objectName) {
            if (mbeanGBeanName.equals(objectName)) {
                notificationBroadcaster.sendNotification(new Notification(NotificationType.STATE_RUNNING, objectName, nextSequence()));
            }
        }

        public void stopping(ObjectName objectName) {
            if (mbeanGBeanName.equals(objectName)) {
                notificationBroadcaster.sendNotification(new Notification(NotificationType.STATE_STOPPING, objectName, nextSequence()));
            }
        }

        public void stopped(ObjectName objectName) {
            if (mbeanGBeanName.equals(objectName)) {
                notificationBroadcaster.sendNotification(new Notification(NotificationType.STATE_STOPPED, objectName, nextSequence()));
            }
        }

        public void unloaded(ObjectName objectName) {
            if (mbeanGBeanName.equals(objectName)) {
                notificationBroadcaster.sendNotification(new Notification(NotificationType.OBJECT_DELETED, objectName, nextSequence()));
            }
        }

        public synchronized long nextSequence() {
            return sequence++;
        }
    }
}
