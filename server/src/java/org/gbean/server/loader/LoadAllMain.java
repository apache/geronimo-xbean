/**
 *
 * Copyright 2005 the original author or authors.
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
package org.gbean.server.loader;

import java.util.List;
import java.util.Iterator;

import org.gbean.server.main.Main;
import org.gbean.server.main.FatalStartupError;
import org.gbean.kernel.Kernel;
import org.gbean.kernel.ServiceName;

/**
 * LoadAllMain loads all configurations specified in the arguments passed to main.
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class LoadAllMain implements Main {
    private Kernel kernel;
    private Main next;

    /**
     * Gets the kernel in which configurations are loaded.
     * @return the kernel in which configurations are loaded
     */
    public Kernel getKernel() {
        return kernel;
    }

    /**
     * Sets the kernel in which configurations are loaded.
     * @param kernel the kernel in which configurations are loaded
     */
    public void setKernel(Kernel kernel) {
        this.kernel = kernel;
    }

    /**
     * Gets the next main instance to call.
     * @return the next main instance to call
     */
    public Main getNext() {
        return next;
    }

    /**
     * Sets the next main instance to call.
     * @param next the next main instance to call
     */
    public void setNext(Main next) {
        this.next = next;
    }

    /**
     * Loads all configurations specified in the args and call the next main instance with no arguments.
     * @param args the configurations to load
     */
    public void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String location = args[i];
            load(location);
        }

        if (next != null) {
            next.main(new String[0]);
        }
    }

    private void load(String location) {
        List loaders = kernel.getServices(Loader.class);
        for (Iterator iterator = loaders.iterator(); iterator.hasNext();) {
            Loader loader = (Loader) iterator.next();
            try {
                ServiceName configurationName = loader.load(location);
                if (configurationName != null) {
                    kernel.startServiceRecursive(configurationName);
                    return;
                }
            } catch (Exception e) {
                throw new FatalStartupError("Error loading '" + location + "' with " + loader, e);
            }
        }
        String message = "No loaders were able to load '" + location + "' : Available loaders ";
        for (Iterator iterator = loaders.iterator(); iterator.hasNext();) {
            message += iterator.next();
            if (iterator.hasNext()) {
                message += ", ";
            }
        }
        throw new FatalStartupError(message);
    }
}
