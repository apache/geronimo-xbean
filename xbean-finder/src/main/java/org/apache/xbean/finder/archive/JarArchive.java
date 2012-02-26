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
package org.apache.xbean.finder.archive;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

/**
 * @version $Rev$ $Date$
 */
public class JarArchive implements Archive {

    private final ClassLoader loader;
    private final URL url;
    private List<String> list;

    public JarArchive(ClassLoader loader, URL url) {
        if (!"jar".equals(url.getProtocol())) throw new IllegalArgumentException("not a jar url: " + url);
        this.loader = loader;
        this.url = url;
    }

    public URL getUrl() {
        return url;
    }

    public InputStream getBytecode(String className) throws IOException, ClassNotFoundException {
        int pos = className.indexOf("<");
        if (pos > -1) {
            className = className.substring(0, pos);
        }
        pos = className.indexOf(">");
        if (pos > -1) {
            className = className.substring(0, pos);
        }
        if (!className.endsWith(".class")) {
            className = className.replace('.', '/') + ".class";
        }

        URL resource = loader.getResource(className);
        if (resource != null) return new BufferedInputStream(resource.openStream());

        throw new ClassNotFoundException(className);
    }


    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return loader.loadClass(className);
    }

    public Iterator<String> iterator() {
        if (list != null) return list.iterator();

        try {
            list = jar(url);
            return list.iterator();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<String> jar(URL location) throws IOException {
        String jarPath = location.getFile();
        if (jarPath.indexOf("!") > -1){
            jarPath = jarPath.substring(0, jarPath.indexOf("!"));
        }
        URL url = new URL(jarPath);
        if ("file".equals(url.getProtocol())) { // ZipFile is faster than ZipInputStream
            JarFile jarFile = new JarFile(url.getFile().replace("%20", " "));
            return jar(jarFile);
        } else {
            InputStream in = url.openStream();
            try {
                JarInputStream jarStream = new JarInputStream(in);
                return jar(jarStream);
            } finally {
                in.close();
            }
        }
    }

    private List<String> jar(JarFile jarFile) {
        List<String> classNames = new ArrayList<String>();

        Enumeration<? extends JarEntry> jarEntries =jarFile.entries();
        while (jarEntries.hasMoreElements()) {
            JarEntry entry = jarEntries.nextElement();
            addClassName(classNames, entry);
        }

        return classNames;
    }

    private List<String> jar(JarInputStream jarStream) throws IOException {
        List<String> classNames = new ArrayList<String>();

        JarEntry entry;
        while ((entry = jarStream.getNextJarEntry()) != null) {
            addClassName(classNames, entry);
        }

        return classNames;
    }

    private void addClassName(List<String> classNames, JarEntry entry) {
        if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
            return;
        }
        String className = entry.getName();
        className = className.replaceFirst(".class$", "");
        if (className.contains(".")) {
            return;
        }
        className = className.replace('/', '.');
        classNames.add(className);
    }
}

