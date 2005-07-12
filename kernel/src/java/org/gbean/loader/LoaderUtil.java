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
package org.gbean.loader;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import javax.management.ObjectName;

import org.gbean.kernel.ServiceName;
import org.gbean.kernel.Kernel;
import org.gbean.kernel.ServiceNotFoundException;
import org.gbean.kernel.runtime.ServiceState;
import org.gbean.spring.FatalStartupError;

/**
 * @version $Revision$ $Date$
 */
public class LoaderUtil {
    private static final ObjectName LOADER_NAME_QUERY = ServiceName.createName("*:j2eeType=Loader,*");

    private LoaderUtil() {
    }

    public static void load(Kernel kernel, String location) {
        Collection loaders = kernel.listServices(LOADER_NAME_QUERY);
        if (loaders.isEmpty()) {
            throw new FatalStartupError("No loaders avalible in kernel");
        }

        for (Iterator iterator = loaders.iterator(); iterator.hasNext();) {
            Loader loader = (Loader) iterator.next();
            try {
                ObjectName rootConfigurationName = loader.load(location);
                if (rootConfigurationName != null) {
                    kernel.startRecursiveService(rootConfigurationName);
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

    public static void verifyAllServicesRunning(Kernel kernel) {
        Set allServices = kernel.listServiceNames(ServiceName.createName("*:*"));
        for (Iterator iterator = allServices.iterator(); iterator.hasNext();) {
            ObjectName objectName = (ObjectName) iterator.next();
            try {
                int state = kernel.getServiceState(objectName);
                if (state != ServiceState.RUNNING_INDEX) {
                    throw new FatalStartupError("Service '" + objectName + "' failed to start");
                }
            } catch (ServiceNotFoundException e) {
                throw new FatalStartupError("Service '" + objectName + "' was unloaded");
            }
        }
    }
}
