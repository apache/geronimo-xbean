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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * TODO Unfinished
 * @version $Rev$ $Date$
 */
public class ClasspathArchive implements Archive {

    private final List<URL> urls = new ArrayList<URL>();
    private final ClassLoader loader;

    public ClasspathArchive(ClassLoader loader, URL... urls) {
        this(loader, Arrays.asList(urls));
    }

    public ClasspathArchive(ClassLoader loader, Iterable<URL> urls) {
        this.loader = loader;
        for (URL url : urls) {
            this.urls.add(url);
        }
    }

    @Override
    public Iterator<String> iterator() {
        // TODO
        return Collections.EMPTY_LIST.iterator();
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
}