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
package org.apache.xbean.jmx;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.util.Date;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Creates JMX heartbeat notifications for the specified timer and at the
 * specified interval.
 *
 * @version $Rev$ $Date$
 */
public class Heartbeat
{
    private static final Log log = LogFactory.getLog(Heartbeat.class);

    public static final String HEARTBEAT_TYPE = "heartbeat";
    public static final String HEARTBEAT_MESSAGE = "heartbeat message";

    private long interval = 10000;
    private ObjectName timerName;
    private MBeanServer server;
    private Integer timerId;

    public Heartbeat(MBeanServer server, ObjectName timerName, long interval)
    {
        this.interval = interval;
        this.timerName = timerName;
        this.server = server;
    }

    public void start()
    {
        Set lBeans = server.queryMBeans(timerName, null);

        if (lBeans.isEmpty())
        {
            throw new RuntimeException("Cannot find Timer MBean!");
        }

        ObjectInstance lTimer = (ObjectInstance) lBeans.iterator().next();

        try
        {
            log.debug("Adding heartbeat notification.");
            timerId = (Integer) server.invoke(lTimer.getObjectName(),
                                              "addNotification",
                                              new Object[]{
                                                      HEARTBEAT_TYPE,
                                                      HEARTBEAT_MESSAGE,
                                                      null, // No user object
                                                      new Date(), // start now
                                                      new Long(interval), // with this interval
                                                      new Long(0) // forever
                                              },
                                              new String[]{
                                                      String.class.getName(),
                                                      String.class.getName(),
                                                      Object.class.getName(),
                                                      Date.class.getName(),
                                                      Long.TYPE.getName(),
                                                      Long.TYPE.getName()
                                              });
        }
        catch (Exception e)
        {
            log.error("Could not register Timer notification!", e);
        }
    }

    public void stop()
    {
        try
        {
            server.invoke(timerName,
                          "removeNotification",
                          new Object[]{timerId},
                          new String[]{Integer.class.getName()});
        }
        catch (Exception e)
        {
            log.error("Could not remove Timer notification!", e);
        }
    }
}
