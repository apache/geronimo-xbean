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

import javax.management.NotificationEmitter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


/**
 * @version $Rev$ $Date$
 */
public class MemoryUsageExampleTest extends AbstractJMXSNMPTest
{
    public void testHB() throws Exception
    {
        // Create SNMP stuff
        SnmpEmitter em = new SnmpEmitter();

        SnmpServer ss = new SnmpServer();
        ss.setHost("127.0.0.1");
        ss.setPort(SNMP_TEST_PORT);

        ss.setVarBindings(new ArrayList());

        /**
         *  Bind the JMX notification type "java.management.memory.threshold.exceeded"
         * to the specified OID. The payload of the snmp message will be the usage
         * attribute.
         */
        SnmpBinding binding = new SnmpBinding();
        binding.setOid("1.3.6.1.2.1.1.3");
        binding.setPayload("usage");
        binding.setType("java.management.memory.threshold.exceeded");
        ss.getVarBindings().add(binding);

        Set servers = new HashSet();
        servers.add(ss);
        em.setServers(servers);

        em.start();

        MemoryPoolMXBean pool = (MemoryPoolMXBean) ManagementFactory.getMemoryPoolMXBeans().iterator().next();

        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();

        // Add our SNMP emitter as a notification listener
        NotificationEmitter memEm = (NotificationEmitter) memBean;
        memEm.addNotificationListener(em, null, null);

        // Set the memory usage threshold real low to trigger an event
        pool.setUsageThreshold(1000);

        Thread.sleep(1000);

        if (count == 0) fail("Didn't receive any SNMP traps!");
    }
}
