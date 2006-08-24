/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.xbean.snmp;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.NotificationEmitter;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.xbean.jmx.Heartbeat;


/**
 * @version $Rev$ $Date$
 */
public class HeartbeatTest extends AbstractJMXSNMPTest
{
    public void testHB() throws Exception
    {
        // Create SNMP stuff
        SnmpEmitter em = new SnmpEmitter();

        SnmpServer ss = new SnmpServer();
        ss.setHost("127.0.0.1");
        ss.setPort(SNMP_TEST_PORT);

        ss.setVarBindings(new ArrayList());

        SnmpBinding binding = new SnmpBinding();
        binding.setOid("1.3.6.1.2.1.1.1");
        binding.setPayload(SnmpEmitter.PAYLOAD_NOTIFICATION_MESSAGE);
        ss.getVarBindings().add(binding);

        binding = new SnmpBinding();
        binding.setOid("1.3.6.1.2.1.1.2");
        binding.setPayload(SnmpEmitter.PAYLOAD_NOTIFICATION_TYPE);
        ss.getVarBindings().add(binding);

        binding = new SnmpBinding();
        binding.setOid("1.3.6.1.2.1.1.3");
        binding.setPayload("foo");
        binding.setType("asdf");
        ss.getVarBindings().add(binding);


        Set servers = new HashSet();
        servers.add(ss);
        em.setServers(servers);

        em.start();

        MBeanServer lServer = (MBeanServer) MBeanServerFactory.findMBeanServer(null).get(0);
        Set lBeans = lServer.queryMBeans(new ObjectName("DefaultDomain", "service", "timer"), null);
        ObjectInstance lTimer = (ObjectInstance) lBeans.iterator().next();

        lServer.addNotificationListener(lTimer.getObjectName(), em,
                                        // No filter
                                        null,
                                        // No object handback necessary
                                        null);

        MemoryPoolMXBean pool = (MemoryPoolMXBean) ManagementFactory.getMemoryPoolMXBeans().iterator().next();

        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();

        NotificationEmitter memEm = (NotificationEmitter) memBean;
        memEm.addNotificationListener(em, null, null);

        pool.setUsageThreshold(1000);
        Heartbeat hb = new Heartbeat(lServer, new ObjectName("DefaultDomain", "service", "timer"), 1000);
        hb.start();

        Thread.sleep(1000);

        if (count == 0) fail("Didn't receive any SNMP traps!");

        hb.stop();
    }

}
