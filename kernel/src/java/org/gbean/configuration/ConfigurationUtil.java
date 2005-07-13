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
package org.gbean.configuration;

import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.gbean.classloader.NamedClassLoader;
import org.gbean.repository.Repository;

/**
 * @version $Revision$ $Date$
 */
public final class ConfigurationUtil {
    private ConfigurationUtil() {
    }

    public static ClassLoader createClassLoader(String name, List dependencies, ClassLoader parentClassLoader, Collection repositories) {
        List urls = new LinkedList();
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
            if (!"file".equals(url.getProtocol())) {
                throw new MissingDependencyException("Only local file jars are supported " + url);
            }
            urls.add(url);
        }
        return new NamedClassLoader(name, (URL[]) urls.toArray(new URL[urls.size()]), parentClassLoader);
    }
}
