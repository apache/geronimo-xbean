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

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Session;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.Counter64;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.Null;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;


/**
 * Listens for JMX notifications and then emits them as SNMP traps.
 * The class is configured with the SnmpServer and VarBinding POJOs.
 * <pre>
 * // Create an SnmpServer which describes where the messages are going to go
 * SnmpServer ss = new SnmpServer();
 * ss.setHost("127.0.0.1");
 * ss.setPort(162);
 * <p/>
 * // Create a binding from a notification to the SNMP trap message
 * VarBinding binding = new VarBinding();
 * binding.setOid("1.3.6.1.2.1.1.1");
 * binding.setPayload(SnmpEmitter.PAYLOAD_NOTIFICATION_MESSAGE);
 * ss.getVarBindings().add(binding);
 * <p/>
 * // Create the SNMP emitter
 * SnmpEmitter snmpEmitter = new SnmpEmitter();
 * snmpEmitter.getServers().add(ss);
 * snmpEmitter.start();
 * <p/>
 * // Register the emitter as a JMX listener
 * MBeanServer server = ....;
 * server.addNotificationListener(objectName,
 *                                snmpEmitter,
 *                                null, // No filter
 *                                null // No object handback necessary);
 * </pre>
 * VarBindings specify what data to send to the SNMP server. The OID is the
 * object ID that identifies the message on the SNMP server. The payload is
 * what you desire to send to the server. It can be one of the following items:
 * <ol>
 * <li>PAYLOAD_NOTIFICATION_MESSAGE: The notification message.</li>
 * <li>PAYLOAD_NOTIFICATION_TIMESTAMP: The notification timestamp.</li>
 * <li>PAYLOAD_NOTIFICATION_TYPE: The notification type.</li>
 * <li>PAYLOAD_NOTIFICATION_SEQUENCE: The notification sequence number.
 * <li>UserData: If the payload is not one of the above, the SnmpEmitter w</li>ill
 * see if the Notification's UserData is a Map. If so it will use the payload
 * as the key to lookup data from the Map and send the result. Otherwise, Null
 * will be sent.
 * </li>
 * </ol>
 *
 * @version $Rev$ $Date$
 */
public class SnmpEmitter implements NotificationListener
{

    private static final Log log = LogFactory.getLog(SnmpEmitter.class);

    public static final String PAYLOAD_NOTIFICATION_MESSAGE = "notification-message";
    public static final String PAYLOAD_NOTIFICATION_TIMESTAMP = "notification-date";
    public static final String PAYLOAD_NOTIFICATION_TYPE = "notification-type";
    public static final String PAYLOAD_NOTIFICATION_SEQUENCE = "notification-sequence";

    private Set servers = new HashSet();

    private Map sessions = new HashMap();

    public Set getServers()
    {
        return servers;
    }

    public void setServers(Set servers)
    {
        this.servers = servers;
    }

    public void start() throws Exception
    {
        log.info("Starting SnmpEmitter");
        for (Iterator itr = servers.iterator(); itr.hasNext();)
        {
            SnmpServer server = (SnmpServer) itr.next();

            Snmp sess = createSession(server);
            sessions.put(server, sess);
        }
    }

    public void stop()
    {
        for (Iterator itr = sessions.values().iterator(); itr.hasNext();)
        {
            Session session = (Session) itr.next();
            try
            {
                session.close();
            }
            catch (IOException e)
            {
                log.error("Could not close session.", e);
            }
            itr.remove();
        }
    }

    public void handleNotification(Notification n, Object o)
    {
        log.debug("Received JMX notification");
        for (Iterator itr = servers.iterator(); itr.hasNext();)
        {
            SnmpServer server = (SnmpServer) itr.next();
            Snmp session = getSession(server);
            Address targetAddress = GenericAddress.parse("udp:" + server.getHost() + "/" + server.getPort());

            // setting up target
            CommunityTarget target = new CommunityTarget();
            target.setCommunity(new OctetString("public"));
            target.setAddress(targetAddress);
            target.setRetries(server.getRetries());
            target.setTimeout(server.getTimeout());
            target.setVersion(SnmpConstants.version2c);

            // creating PDU
            PDU pdu = new PDU();

            addVarBindings(n, server, pdu);

            pdu.setType(PDU.TRAP);

            // sending request
            ResponseListener listener = new ResponseListener()
            {
                public void onResponse(ResponseEvent event)
                {
                    // Always cancel async request when response has been received
                    // otherwise a memory leak is created! Not canceling a request
                    // immediately can be useful when sending a request to a broadcast
                    // address.
                    ((Snmp) event.getSource()).cancel(event.getRequest(), this);
                    log.debug("Received response PDU is: " + event.getResponse());
                }
            };
            try
            {
                log.debug("Sending SNMP trap to " + targetAddress);
                session.send(pdu, target, null, listener);
            }
            catch (IOException e)
            {
                log.error("Could not send SNMP trap!", e);
            }
        }
    }

    protected void addVarBindings(Notification n, SnmpServer server, PDU pdu)
    {
        for (Iterator itr = server.getVarBindings().iterator(); itr.hasNext();)
        {
            addVarBinding(n, (SnmpBinding) itr.next(), pdu);
        }
    }

    protected void addVarBinding(Notification n, SnmpBinding binding, PDU pdu)
    {
        String type = binding.getType();
        // See if this VarBinding applies to this notification
        if (type != null && !type.equals(n.getType())) return;

        pdu.add(new VariableBinding(new OID(binding.getOid()), getVariable(n, binding)));
    }

    protected Variable getVariable(Notification n, SnmpBinding binding)
    {
        String payload = binding.getPayload();
        if (payload.equals(PAYLOAD_NOTIFICATION_TIMESTAMP))
        {
            return new Counter64(n.getTimeStamp());
        }
        else if (payload.equals(PAYLOAD_NOTIFICATION_MESSAGE))
        {
            return new OctetString(n.getMessage());
        }
        else if (payload.equals(PAYLOAD_NOTIFICATION_SEQUENCE))
        {
            return new Counter64(n.getSequenceNumber());
        }
        else if (payload.equals(PAYLOAD_NOTIFICATION_TYPE))
        {
            return new OctetString(n.getType());
        }
        else
        {
            // This isn't a payload that we recognize, so attempt to pull it
            // out of a mMap in the UserData.
            Object data = n.getUserData();
            if (data instanceof Map)
            {
                Object pdata = ((Map) data).get(payload);
                if (pdata != null)
                    return new OctetString(pdata.toString());
                else
                    return new Null();
            }
            else if (data instanceof CompositeDataSupport)
            {
                CompositeData cdata = (CompositeData) data;
                if (cdata.containsKey(payload))
                    return new OctetString(cdata.get(payload).toString());
                else
                    return new Null();
            }
            else
            {
                return new Null();
            }
        }
    }

    private Snmp getSession(SnmpServer server)
    {
        return (Snmp) sessions.get(server);
    }

    private Snmp createSession(SnmpServer server) throws IOException
    {
        TransportMapping transport = new DefaultUdpTransportMapping();
        Snmp snmp = new Snmp(transport);
        transport.listen();

        return snmp;
    }
}
