/**
 *
 * Copyright 2006 The Apache Software Foundation
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
package org.apache.xbean.server.main;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * DaemonMain is a simple server entry point.  This class will optionally call a next main and then hold the
 * startup execution thread until System exit is triggered via the system shutdown hook.
 *
 * @org.apache.xbean.XBean namespace="http://xbean.apache.org/schemas/server" element="daemon-main"
 *     description="Simple server entry point."
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.5-colossus
 */
public class DaemonMain implements Main {
    /**
     * Lock that should be acquired before accessing the running boolean flag.
     */
    private final Lock destroyLock = new ReentrantLock();

    /**
     * The condition that is notified when the kernel has been destroyed.
     */
    private final Condition destroyCondition = destroyLock.newCondition();

    /**
     * If true, the process is still running.
     */
    private boolean running;

    /**
     * The next main to call before capturing the startup execution thread.
     */
    private Main next;

    /**
     * Gets the next main to call before capturing the startup execution thread.
     * @return the next main to call before capturing the startup execution thread
     */
    public Main getNext() {
        return next;
    }

    /**
     * Sets the next main to call before capturing the startup execution thread.
     * @param next the next main to call before capturing the startup execution thread
     */
    public void setNext(Main next) {
        this.next = next;
    }

    /**
     * Calls the next (if present) and waits for the vm to be shutdown.
     * @param args the arguments passed the next main
     */
    public void main(String[] args) {
        // mark the daemon as started
        destroyLock.lock();
        try {
            if (running) {
                throw new IllegalStateException("The daemon is already running");
            }
            running = true;
        } finally {
            destroyLock.unlock();
        }

        try {
            // if we have a child main class call it
            if (next != null) {
                next.main(args);
            }

            // add our shutdown hook
            Runtime.getRuntime().addShutdownHook(new DemonShutdownHook());

            // wait for the vm to be destroyed
            awaitDestruction();
        } finally {
            // assure the daemon has been destroyed
            destroy();
        }
    }

    /**
     * Destroy the daemon and notify all threads waiting for destruction.
     */
    public void destroy()  {
        destroyLock.lock();
        try {
            // if we are already stopped simply return
            if (!running) {
                return;
            }
            running = false;

            destroyCondition.signalAll();
        } finally {
            destroyLock.unlock();
        }
    }

    /**
     * Uninterruptibly waits for the daemon to be destroyed via the system shudown hook or by a direct call of destroy.
     */
    public void awaitDestruction() {
        while (true) {
            destroyLock.lock();
            try {
                // if we are already stopped simply return
                if (!running) {
                    return;
                }

                // wait until destroy completes
                destroyCondition.awaitUninterruptibly();
            } finally {
                destroyLock.unlock();
            }
        }
    }

    /**
     * Is this deamon still running?
     * @return true if this daemon is still running.
     */
    public boolean isRunning() {
        destroyLock.lock();
        try {
            return running;
        } finally {
            destroyLock.unlock();
        }
    }

    private class DemonShutdownHook extends Thread {
        /**
         * Destroys the daemon.
         */
        public void run() {
            DaemonMain.this.destroy();
        }
    }
}
