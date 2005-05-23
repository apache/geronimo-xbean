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
package org.gbean.repository;

import java.io.File;
import javax.management.ObjectName;

import org.gbean.kernel.Kernel;
import org.gbean.kernel.simple.SimpleServiceFactory;
import org.gbean.service.ServiceFactory;

/**
 * @version $Revision$ $Date$
 */
public class BootstrapReadOnlyRepository extends BootstrapRepository {
    public ObjectName load(Kernel kernel, ClassLoader classLoader) throws Exception {
        File rootDir;
        String gbeanRepoLocal = System.getProperty("gbean.repo.local");
        if (gbeanRepoLocal != null) {
            rootDir = new File(gbeanRepoLocal);
        } else {
            String baseDir = System.getProperty("gbean.base.dir", System.getProperty("user.dir"));
            rootDir = new File(baseDir, "repository");
        }

        ObjectName objectName = new ObjectName(":j2eeType=Repository,dir=" + ObjectName.quote(rootDir.getAbsolutePath()));
        ReadOnlyRepository readOnlyRepository = new ReadOnlyRepository(rootDir);
        ServiceFactory serviceFactory = new SimpleServiceFactory(readOnlyRepository);
        kernel.loadService(objectName, serviceFactory, classLoader);
        kernel.startService(objectName);
        return objectName;
    }

}
