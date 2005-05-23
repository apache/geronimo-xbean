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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import javax.management.ObjectName;

import org.gbean.kernel.Kernel;

/**
 * @version $Rev$ $Date$
 */
public abstract class BootstrapRepository {
    public static final String REPOSITORY_FACTORY_KEY = BootstrapRepository.class.getName();

    public static List loadRepositories(Kernel kernel, ClassLoader classLoader) {
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) {
                classLoader = BootstrapRepository.class.getClassLoader();
            }
        }

        List repositories = new LinkedList();

        // System property
        try {
            String repositoryFactoryName = System.getProperty(REPOSITORY_FACTORY_KEY);
            if (repositoryFactoryName != null) {
                List l = createRepositories(kernel, classLoader, repositoryFactoryName);
                repositories.addAll(l);
            }
        } catch (SecurityException se) {
        }

        // Jar Service Specification - http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html
        String serviceId = "META-INF/services/" + REPOSITORY_FACTORY_KEY;
        InputStream inputStream = null;
        try {
            classLoader.getResources(serviceId);
            inputStream = classLoader.getResourceAsStream(serviceId);
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                String repositoryFactoryName = reader.readLine();
                reader.close();

                if (repositoryFactoryName != null && repositoryFactoryName.length() > 0) {
                    List l = createRepositories(kernel, classLoader, repositoryFactoryName);
                    repositories.addAll(l);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                }
                inputStream = null;
            }
        }

        if (repositories.isEmpty()) {
            throw new RepositoryLocatorError("No repositories found");
        }
        return repositories;
    }

    private static List createRepositories(Kernel kernel, ClassLoader classLoader, String classNames) {
        List repositories = new LinkedList();
        for (StringTokenizer stringTokenizer = new StringTokenizer(classNames, ","); stringTokenizer.hasMoreTokens();) {
            String className = stringTokenizer.nextToken().trim();
            try {
                BootstrapRepository bootstrapRepository = (BootstrapRepository) classLoader.loadClass(className).newInstance();
                repositories.add(bootstrapRepository.load(kernel, classLoader));
            } catch (ClassCastException e) {
                throw new RepositoryLocatorError("Repository class does not implement org.gbean.repository.Repository: " + className);
            } catch (ClassNotFoundException e) {
                throw new RepositoryLocatorError("Repository class not found: " + className);
            } catch (Exception e) {
                throw new RepositoryLocatorError("Unable to instantiate repository class: " + className, e);
            }
        }
        return repositories;
    }

    public abstract ObjectName load(Kernel kernel, ClassLoader classLoader) throws Exception;
}
