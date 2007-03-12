/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.xbean.classloader;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.Arrays;


/**
 * The NamedClassLoader is a simple extension to URLClassLoader that adds a name and a destroy method that cleans up
 * the commons logging and JavaVM caches of the classloader.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class NamedClassLoader extends URLClassLoader implements DestroyableClassLoader {
    private final String name;
    private volatile boolean destroyed = false;

    /**
     * Creates a named class loader with no parents.
     * @param name the name of this class loader
     * @param urls the urls from which this class loader will classes and resources
     */
    public NamedClassLoader(String name, URL[] urls) {
        super(urls);
        this.name = name;
    }

    /**
     * Creates a named class loader as a child of the specified parent.
     * @param name the name of this class loader
     * @param urls the urls from which this class loader will classes and resources
     * @param parent the parent of this class loader
     */
    public NamedClassLoader(String name, URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.name = name;
    }

    /**
     * Creates a named class loader as a child of the specified parent and using the specified URLStreamHandlerFactory
     * for accessing the urls..
     * @param name the name of this class loader
     * @param urls the urls from which this class loader will classes and resources
     * @param parent the parent of this class loader
     * @param factory the URLStreamHandlerFactory used to access the urls
     */
    public NamedClassLoader(String name, URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
        this.name = name;
    }

    /**
     * Check if this classloader has been destroyed 
     * @return
     */
    public boolean isDestroyed() {
        return destroyed;
    }

    /**
     * {@inheritDoc}
     */
    public void destroy() {
        synchronized(this) {
            if (destroyed) return;
            destroyed = true;
        }
        ClassLoaderUtil.destroy(this);
    }

    /**
     * Gets the name of this class loader.
     * @return the name of this class loader
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "[" + getClass().getName() + ":" +
                " name=" + getName() +
                " urls=" + Arrays.asList(getURLs()) +
                "]";
    }
}
