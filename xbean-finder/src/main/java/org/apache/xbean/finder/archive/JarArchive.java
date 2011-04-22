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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * @version $Rev$ $Date$
 */
public class JarArchive implements Archive {

    private final ClassLoader loader;
    private final URL url;

    public JarArchive(ClassLoader loader, URL url) {
        if (!"jar".equals(url.getProtocol())) throw new IllegalArgumentException("not a file url: " + url);
        this.loader = loader;
        this.url = url;
    }

    @Override
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
        if (resource != null) return resource.openStream();

        throw new ClassNotFoundException(className);
    }


    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return loader.loadClass(className);
    }

    @Override
    public Iterator<String> iterator() {
        try {
            return jar(url).iterator();
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
        InputStream in = url.openStream();
        try {
            JarInputStream jarStream = new JarInputStream(in);
            return jar(jarStream);
        } finally {
            in.close();
        }
    }

    private List<String> jar(JarInputStream jarStream) throws IOException {
        List<String> classNames = new ArrayList<String>();

        JarEntry entry;
        while ((entry = jarStream.getNextJarEntry()) != null) {
            if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                continue;
            }
            String className = entry.getName();
            className = className.replaceFirst(".class$", "");
            if (className.contains(".")) continue;
            className = className.replace(File.separatorChar, '.');
            classNames.add(className);
        }

        return classNames;
    }

}
