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

import java.util.ArrayList;
import java.util.List;


/**
 * @version $Rev$ $Date$
 */
public class SnmpServer
{
    private String host;
    private int port;
    private int timeout = 1000;
    private int retries = 0;
    private List varBindings = new ArrayList();

    public int getRetries()
    {
        return retries;
    }

    public void setRetries(int retries)
    {
        this.retries = retries;
    }

    public List getVarBindings()
    {
        return varBindings;
    }

    public void setVarBindings(List varBindings)
    {
        this.varBindings = varBindings;
    }

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public int getTimeout()
    {
        return timeout;
    }

    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }
}
