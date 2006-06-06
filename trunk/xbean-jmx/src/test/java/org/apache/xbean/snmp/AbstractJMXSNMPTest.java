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
package org.apache.xbean.snmp;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import junit.framework.TestCase;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;


/**
 * @version $Rev$ $Date$
 */
public class AbstractJMXSNMPTest extends TestCase
{
    protected final int SNMP_TEST_PORT = 1620;
    protected static int count = 0;
    private Snmp snmp;

    public void setUp() throws Exception
    {
        createServer();
        createSNMPServer();
    }

    protected void createServer() throws Exception
    {
        MBeanServer server = MBeanServerFactory.createMBeanServer();

        ObjectName timerName = new ObjectName("DefaultDomain", "service", "timer");
        ObjectInstance lTimer = server.createMBean("javax.management.timer.Timer", timerName);

        server.invoke(lTimer.getObjectName(), "start", new Object[]{}, new String[]{});
    }

    public void createSNMPServer() throws Exception
    {
        Address targetAddress = GenericAddress.parse("udp:0.0.0.0/" + SNMP_TEST_PORT);
        TransportMapping transport = new DefaultUdpTransportMapping((UdpAddress) targetAddress);
        snmp = new Snmp(transport);

        snmp.listen();

        CommandResponder trapPrinter = new CommandResponder()
        {
            public synchronized void processPdu(CommandResponderEvent e)
            {
                PDU command = e.getPDU();
                if (command != null)
                {
                    count++;
                    System.out.println(command.toString());
                }
            }
        };
        snmp.addCommandResponder(trapPrinter);
    }

    public void tearDown() throws Exception
    {
        super.tearDown();

        snmp.close();
    }
}
