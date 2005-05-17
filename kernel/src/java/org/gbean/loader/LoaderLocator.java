/**
 *
 * Copyright 2005 The Apache Software Foundation
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @version $Rev$ $Date$
 */
public class LoaderLocator {
    public static final String LOADER_FACTORY_KEY = LoaderLocator.class.getName();

    private LoaderLocator() {
    }

    public static List findLoaders() {
        return findLoaders(null);
    }

    public static List findLoaders(ClassLoader classLoader) {
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) {
                classLoader = LoaderLocator.class.getClassLoader();
            }
        }

        List loaders = new LinkedList();

        // System property
        try {
            String loaderFactoryName = System.getProperty(LOADER_FACTORY_KEY);
            if (loaderFactoryName != null) {
                List l = createLoaders(loaderFactoryName, classLoader);
                loaders.addAll(l);
            }
        } catch (SecurityException se) {
        }

        // Jar Service Specification - http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html
        String serviceId = "META-INF/services/" + LOADER_FACTORY_KEY;
        InputStream inputStream = null;
        try {
            classLoader.getResources(serviceId);
            inputStream = classLoader.getResourceAsStream(serviceId);
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                String loaderFactoryName = reader.readLine();
                reader.close();

                if (loaderFactoryName != null && loaderFactoryName.length() > 0) {
                    List l = createLoaders(loaderFactoryName, classLoader);
                    loaders.addAll(l);
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

        if (loaders.isEmpty()) {
            throw new LoaderLocatorError("No loaders found");
        }
        return loaders;
    }

    private static List createLoaders(String classNames, ClassLoader classLoader) {
        List loaders = new LinkedList();
        for (StringTokenizer stringTokenizer = new StringTokenizer(classNames, ","); stringTokenizer.hasMoreTokens();) {
            String className = stringTokenizer.nextToken().trim();
            try {
                Loader loader = (Loader) classLoader.loadClass(className).newInstance();
                loaders.add(loader);
            } catch (ClassCastException e) {
                throw new LoaderLocatorError("Loader class does not implement org.gbean.loader.Loader: " + className);
            } catch (ClassNotFoundException e) {
                throw new LoaderLocatorError("Loader class not found: " + className);
            } catch (Exception e) {
                throw new LoaderLocatorError("Unable to instantiate loader class: " + className, e);
            }
        }
        return loaders;
    }
}
