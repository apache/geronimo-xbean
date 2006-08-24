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
package org.apache.xbean.terminal.telnet;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TelnetDaemon implements Runnable {

    private final TelnetShell shell;
    private final int port;

    private ServerSocket serverSocket;

    /**
     * We start out in a "stopped" state until someone calls the start method.
     */
    private boolean stop = true;

    public TelnetDaemon(String serverName, int port) {
        this.port = port;
        this.shell = new TelnetShell(serverName);
    }

    public void start() throws Exception {
        synchronized (this) {
            // Don't bother if we are already started/starting
            if (!stop)
                return;

            stop = false;

            // Do our stuff
            try {
                serverSocket = new ServerSocket(port, 20);
                Thread d = new Thread(this);
                d.setName("service.shell@" + d.hashCode());
                d.setDaemon(true);
                d.start();
            } catch (Exception e) {
                throw new Exception("Service failed to start.", e);
            }
        }
    }

    public void stop() throws Exception {
        synchronized (this) {
            if (stop) {
                return;
            }
            stop = true;
            try {
                this.notifyAll();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public synchronized void service(final Socket socket) throws IOException {
        Thread d = new Thread(new Runnable() {
            public void run() {
                try {
                    shell.service(socket);
                } catch (SecurityException e) {
                } catch (Throwable e) {
                } finally {
                    try {
                        if (socket != null)
                            socket.close();
                    } catch (Throwable t) {
                    }
                }
            }
        });
        d.setDaemon(true);
        d.start();
    }

    public void run() {

        Socket socket = null;

        while (!stop) {
            try {
                socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                if (!stop) service(socket);
            } catch (SecurityException e) {
            } catch (Throwable e) {
            }
        }
    }

}
