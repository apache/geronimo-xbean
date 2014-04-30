/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.xbean.finder;

import org.apache.xbean.finder.util.Files;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class ClassLoaders {
    private static final boolean DONT_USE_GET_URLS = Boolean.getBoolean("xbean.finder.use.get-resources");
    private static final ClassLoader SYSTEM = ClassLoader.getSystemClassLoader();

    private static final boolean UNIX = !System.getProperty("os.name").toLowerCase().contains("win");

    public static Set<URL> findUrls(final ClassLoader classLoader) throws IOException {
        if (classLoader == null || (SYSTEM.getParent() != null && classLoader == SYSTEM.getParent())) {
            return Collections.emptySet();
        }

        final Set<URL> urls =  new HashSet<URL>();

        if (URLClassLoader.class.isInstance(classLoader) && !DONT_USE_GET_URLS) {
            if (!isSurefire(classLoader)) {
                for (final Collection<URL> item : Arrays.asList(
                        Arrays.asList(URLClassLoader.class.cast(classLoader).getURLs()), findUrls(classLoader.getParent()))) {
                    for (final URL url : item) {
                        addIfNotSo(urls, url);
                    }
                }
            } else { // http://jira.codehaus.org/browse/SUREFIRE-928 - we could reuse findUrlFromResources but this seems faster
                for (final URL url : fromClassPath()) {
                    urls.add(url);
                }
            }
        } else {
            for (final URL url : findUrlFromResources(classLoader)) {
                urls.add(url);
            }
        }

        return urls;
    }

    private static void addIfNotSo(final Set<URL> urls, final URL url) {
        if (UNIX && isNative(url)) {
            return;
        }

        urls.add(url);
    }

    public static boolean isNative(final URL url) {
        final File file = Files.toFile(url);
        if (file != null) {
            final String name = file.getName();
            if (!name.endsWith(".jar") && !file.isDirectory()
                    && name.contains(".so") && file.getAbsolutePath().startsWith("/usr/lib")) {
                return true;
            }
        }
        return false;
    }


    private static boolean isSurefire(ClassLoader classLoader) {
        return System.getProperty("surefire.real.class.path") != null && classLoader == SYSTEM;
    }

    private static Collection<URL> fromClassPath() {
        final String[] cp = System.getProperty("java.class.path").split(System.getProperty("path.separator", ":"));
        final Set<URL> urls = new HashSet<URL>();
        for (final String path : cp) {
            try {
                urls.add(new File(path).toURI().toURL()); // don't build the url in plain String since it is not portable
            } catch (final MalformedURLException e) {
                // ignore
            }
        }
        return urls;
    }

    public static Set<URL> findUrlFromResources(final ClassLoader classLoader) throws IOException {
        final Set<URL> set = new HashSet<URL>();
        for (final URL url : Collections.list(classLoader.getResources("META-INF"))) {
            final String externalForm = url.toExternalForm();
            set.add(new URL(externalForm.substring(0, externalForm.lastIndexOf("META-INF"))));
        }
        set.addAll(Collections.list(classLoader.getResources("")));
        return set;
    }

    private ClassLoaders() {
        // no-op
    }
}
