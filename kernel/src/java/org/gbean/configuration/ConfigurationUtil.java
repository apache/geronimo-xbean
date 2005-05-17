/**
 *
 * Copyright 2005 GBean.org
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
package org.gbean.configuration;

import java.util.List;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Iterator;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;

import org.gbean.repository.Repository;
import org.apache.geronimo.kernel.config.ConfigurationClassLoader;

/**
 * @version $Revision$ $Date$
 */
public final class ConfigurationUtil {
    private ConfigurationUtil() {
    }

    public static ClassLoader createClassLoader(URI configurationId, List dependencies, ClassLoader parentClassLoader, Collection repositories) {
        List urlList = new LinkedList();
        for (Iterator i = dependencies.iterator(); i.hasNext();) {
            URI uri = (URI) i.next();
            URL url = null;
            for (Iterator j = repositories.iterator(); j.hasNext();) {
                Repository repository = (Repository) j.next();
                if (repository.containsResource(uri)) {
                    url = repository.getResource(uri);
                    break;
                }
            }
            if (url == null) {
                throw new MissingDependencyException("Unable to resolve dependency " + uri);
            }
            urlList.add(url);
        }
        URL[] urls = (URL[]) urlList.toArray(new URL[urlList.size()]);
        return new ConfigurationClassLoader(configurationId, urls, parentClassLoader);
    }
}
