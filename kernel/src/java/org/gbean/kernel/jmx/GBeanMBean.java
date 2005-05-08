/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
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

package org.gbean.kernel.jmx;

import java.util.Iterator;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.JMException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.gbean.GOperationSignature;
import org.apache.geronimo.kernel.NoSuchAttributeException;
import org.apache.geronimo.kernel.NoSuchOperationException;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.kernel.management.NotificationType;
import org.gbean.beans.LifecycleBroadcaster;

/**
 * @version $Rev: 109772 $ $Date: 2004-12-03 21:06:02 -0800 (Fri, 03 Dec 2004) $
 */
public final class GBeanMBean implements DynamicMBean, NotificationEmitter, LifecycleBroadcaster {
    private static final Log log = LogFactory.getLog(GBeanMBean.class);

    private static final String ATTRIBUTE_GBEAN_INFO = "gbeanInfo";
    private static final String ATTRIBUTE_CLASS_LOADER = "classLoader";
    private static final String ATTRIBUTE_STATE = "state";
    private static final String ATTRIBUTE_START_TIME = "startTime";
    private static final String ATTRIBUTE_STATE_MANAGEABLE = "stateManageable";
    private static final String ATTRIBUTE_STATISTICS_PROVIDER= "statisticsProvider";
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
     * The mbean info
     */
    private final MBeanInfo mbeanInfo;

    /**
     * The broadcaster for notifications
     */
    private final NotificationBroadcasterSupport notificationBroadcaster = new NotificationBroadcasterSupport();

    /**
     * Sequence number used for notifications
     */
    private long sequence;

    public GBeanMBean(Kernel kernel, ObjectName objectName, MBeanInfo mbeanInfo) {
        this.kernel = kernel;
        this.objectName = objectName;
        this.mbeanInfo = mbeanInfo;
    }

    public MBeanInfo getMBeanInfo() {
        return mbeanInfo;
    }

    public Object getAttribute(String attributeName) throws ReflectionException, AttributeNotFoundException {
        try {
            if (ATTRIBUTE_GBEAN_INFO.equals(attributeName)) {
                return kernel.getGBeanInfo(objectName);
            } else if (ATTRIBUTE_CLASS_LOADER.equals(attributeName)) {
                return kernel.getClassLoaderFor(objectName);
            } else if (ATTRIBUTE_STATE.equals(attributeName)) {
                return new Integer(kernel.getGBeanState(objectName));
            } else if (ATTRIBUTE_START_TIME.equals(attributeName)) {
                return new Long(kernel.getGBeanStartTime(objectName));
            } else if (ATTRIBUTE_STATE_MANAGEABLE.equals(attributeName)) {
                return Boolean.TRUE;
            } else if (ATTRIBUTE_STATISTICS_PROVIDER.equals(attributeName)) {
                return Boolean.FALSE;
            } else if (ATTRIBUTE_EVENT_PROVIDER.equals(attributeName)) {
                return Boolean.TRUE;
            } else if (ATTRIBUTE_EVENT_TYPES.equals(attributeName)) {
                return NotificationType.TYPES;
            } else if (ATTRIBUTE_GBEAN_ENABLED.equals(attributeName)) {
                return Boolean.valueOf(kernel.isGBeanEnabled(objectName));
            }
            return kernel.getAttribute(objectName, attributeName);
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
                kernel.setGBeanEnabled(objectName, ((Boolean) attributeValue).booleanValue());
            } else {
                kernel.setAttribute(objectName, attributeName, attributeValue);
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
                kernel.startGBean(objectName);
                return null;
            } else if (arguments.length == 0 && OPERATION_START_RECURSIVE.equals(operationName)) {
                kernel.startRecursiveGBean(objectName);
                return null;
            } else if (arguments.length == 0 && OPERATION_STOP.equals(operationName)) {
                kernel.startGBean(objectName);
                return null;
            } else {
                return kernel.invoke(objectName, operationName, arguments, types);
            }
        } catch (NoSuchOperationException e) {
            throw new ReflectionException(new NoSuchMethodException(new GOperationSignature(operationName, types).toString()));
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

    public void fireLoadedEvent() {
        notificationBroadcaster.sendNotification(new Notification(NotificationType.OBJECT_CREATED, objectName, nextSequence()));
    }

    public void fireStartingEvent() {
        notificationBroadcaster.sendNotification(new Notification(NotificationType.STATE_STARTING, objectName, nextSequence()));
    }

    public void fireRunningEvent() {
        notificationBroadcaster.sendNotification(new Notification(NotificationType.STATE_RUNNING, objectName, nextSequence()));
    }

    public void fireStoppingEvent() {
        notificationBroadcaster.sendNotification(new Notification(NotificationType.STATE_STOPPING, objectName, nextSequence()));
    }

    public void fireStoppedEvent() {
        notificationBroadcaster.sendNotification(new Notification(NotificationType.STATE_STOPPED, objectName, nextSequence()));
    }

    public void fireUnloadedEvent() {
        notificationBroadcaster.sendNotification(new Notification(NotificationType.OBJECT_DELETED, objectName, nextSequence()));
    }

    private synchronized long nextSequence() {
        return sequence++;
    }

    public String toString() {
        return objectName.toString();
    }
}
