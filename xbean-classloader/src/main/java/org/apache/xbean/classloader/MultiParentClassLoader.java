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

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A MultiParentClassLoader is a simple extension of the URLClassLoader that simply changes the single parent class
 * loader model to support a list of parent class loaders.  Each operation that accesses a parent, has been replaced
 * with a operation that checks each parent in order.  This getParent method of this class will always return null,
 * which may be interperated by the calling code to mean that this class loader is a direct child of the system class
 * loader.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class MultiParentClassLoader extends NamedClassLoader {
    private final ClassLoader[] parents;
    private final boolean inverseClassLoading;
    private final String[] hiddenClasses;
    private final String[] nonOverridableClasses;
    private final String[] hiddenResources;
    private final String[] nonOverridableResources;
    private final Map<String, SoftReference<Class>> cache = new ConcurrentHashMap<String, SoftReference<Class>>();

    /**
     * Creates a named class loader with no parents.
     * @param name the name of this class loader
     * @param urls the urls from which this class loader will classes and resources
     */
    public MultiParentClassLoader(String name, URL[] urls) {
        this(name, urls, ClassLoader.getSystemClassLoader());
    }

    /**
     * Creates a named class loader as a child of the specified parent.
     * @param name the name of this class loader
     * @param urls the urls from which this class loader will classes and resources
     * @param parent the parent of this class loader
     */
    public MultiParentClassLoader(String name, URL[] urls, ClassLoader parent) {
        this(name, urls, new ClassLoader[] {parent});
    }

    /**
     * Creates a named class loader as a child of the specified parent and using the specified URLStreamHandlerFactory
     * for accessing the urls..
     * @param name the name of this class loader
     * @param urls the urls from which this class loader will classes and resources
     * @param parent the parent of this class loader
     * @param factory the URLStreamHandlerFactory used to access the urls
     */
    public MultiParentClassLoader(String name, URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        this(name, urls, new ClassLoader[] {parent}, factory);
    }

    /**
     * Creates a named class loader as a child of the specified parents.
     * @param name the name of this class loader
     * @param urls the urls from which this class loader will classes and resources
     * @param parents the parents of this class loader
     */
    public MultiParentClassLoader(String name, URL[] urls, ClassLoader[] parents) {
        this(name, urls, parents, false, new String[0], new String[0]);
    }

    public MultiParentClassLoader(String name, URL[] urls, ClassLoader parent, boolean inverseClassLoading, String[] hiddenClasses, String[] nonOverridableClasses) {
        this(name, urls, new ClassLoader[]{parent}, inverseClassLoading, hiddenClasses, nonOverridableClasses);
    }
    
    /**
     * Creates a named class loader as a child of the specified parents and using the specified URLStreamHandlerFactory
     * for accessing the urls..
     * @param name the name of this class loader
     * @param urls the urls from which this class loader will classes and resources
     * @param parents the parents of this class loader
     * @param factory the URLStreamHandlerFactory used to access the urls
     */
    public MultiParentClassLoader(String name, URL[] urls, ClassLoader[] parents, URLStreamHandlerFactory factory) {
        super(name, urls, null, factory);
        this.parents = copyParents(parents);
        this.inverseClassLoading = false;
        this.hiddenClasses = new String[0];
        this.nonOverridableClasses = new String[0];
        this.hiddenResources = new String[0];
        this.nonOverridableResources = new String[0];
    }

    public MultiParentClassLoader(String name, URL[] urls, ClassLoader[] parents, boolean inverseClassLoading, Collection hiddenClasses, Collection nonOverridableClasses) {
        this(name, urls, parents, inverseClassLoading, (String[]) hiddenClasses.toArray(new String[hiddenClasses.size()]), (String[]) nonOverridableClasses.toArray(new String[nonOverridableClasses.size()]));
    }

    public MultiParentClassLoader(String name, URL[] urls, ClassLoader[] parents, boolean inverseClassLoading, String[] hiddenClasses, String[] nonOverridableClasses) {
        super(name, urls);
        this.parents = copyParents(parents);
        this.inverseClassLoading = inverseClassLoading;
        this.hiddenClasses = hiddenClasses;
        this.nonOverridableClasses = nonOverridableClasses;
        hiddenResources = toResources(hiddenClasses);
        nonOverridableResources = toResources(nonOverridableClasses);
    }

    private static String[] toResources(String[] classes) {
        String[] resources = new String[classes.length];
        for (int i = 0; i < classes.length; i++) {
            String className = classes[i];
            resources[i] = className.replace('.', '/');
        }
        return resources;
    }
    
    private static ClassLoader[] copyParents(ClassLoader[] parents) {
        ClassLoader[] newParentsArray = new ClassLoader[parents.length];
        for (int i = 0; i < parents.length; i++) {
            ClassLoader parent = parents[i];
            if (parent == null) {
                throw new NullPointerException("parent[" + i + "] is null");
            }
            newParentsArray[i] = parent;
        }
        return newParentsArray;
    }

    /**
     * Gets the parents of this class loader.
     * @return the parents of this class loader
     */
    public ClassLoader[] getParents() {
        return parents;
    }

    /**
     * {@inheritDoc}
     */
    protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class result = null;

        //
        // check if the class is already in the local cache
        //
        SoftReference<Class> reference = cache.get(name);
        if (reference != null) {
            result = reference.get();
        }
        if (result == null) {
            result = doLoadClass(name, resolve);
            cache.put(name, new SoftReference<Class>(result));
        }

        return result;
    }

    private synchronized Class doLoadClass(String name, boolean resolve) throws ClassNotFoundException {
        //
        // Check if class is in the loaded classes cache
        //
        Class cachedClass = findLoadedClass(name);
        if (cachedClass != null) {
            return resolveClass(cachedClass, resolve);
        }
        
        //
        // if we are using inverse class loading, check local urls first
        //
        if (inverseClassLoading && !isDestroyed() && !isNonOverridableClass(name)) {
            try {
                Class clazz = findClass(name);
                return resolveClass(clazz, resolve);
            } catch (ClassNotFoundException ignored) {
            }
        }

        //
        // Check parent class loaders
        //
        if (!isHiddenClass(name)) {
            for (int i = 0; i < parents.length; i++) {
                ClassLoader parent = parents[i];
                try {
                    Class clazz = parent.loadClass(name);
                    return resolveClass(clazz, resolve);
                } catch (ClassNotFoundException ignored) {
                    // this parent didn't have the class; try the next one
                }
            }
        }

        //
        // if we are not using inverse class loading, check local urls now
        //
        // don't worry about excluding non-overridable classes here... we
        // have alredy checked he parent and the parent didn't have the
        // class, so we can override now
        if (!isDestroyed()) {
            try {
                Class clazz = findClass(name);
                return resolveClass(clazz, resolve);
            } catch (ClassNotFoundException ignored) {
            }
        }

        throw new ClassNotFoundException(name + " in classloader " + getName());
    }

    private boolean isNonOverridableClass(String name) {
        for (int i = 0; i < nonOverridableClasses.length; i++) {
            if (name.startsWith(nonOverridableClasses[i])) {
                return true;
            }
        }
        return false;
    }

    private boolean isHiddenClass(String name) {
        for (int i = 0; i < hiddenClasses.length; i++) {
            if (name.startsWith(hiddenClasses[i])) {
                return true;
            }
        }
        return false;
    }

    private Class resolveClass(Class clazz, boolean resolve) {
        if (resolve) {
            resolveClass(clazz);
        }
        return clazz;
    }

    /**
     * {@inheritDoc}
     */
    public URL getResource(String name) {
        if (isDestroyed()) {
            return null;
        }

        //
        // if we are using inverse class loading, check local urls first
        //
        if (inverseClassLoading && !isDestroyed() && !isNonOverridableResource(name)) {
            URL url = findResource(name);
            if (url != null) {
                return url;
            }
        }

        //
        // Check parent class loaders
        //
        if (!isHiddenResource(name)) {
            for (int i = 0; i < parents.length; i++) {
                ClassLoader parent = parents[i];
                URL url = parent.getResource(name);
                if (url != null) {
                    return url;
                }
            }
        }

        //
        // if we are not using inverse class loading, check local urls now
        //
        // don't worry about excluding non-overridable resources here... we
        // have alredy checked he parent and the parent didn't have the
        // resource, so we can override now
        if (!isDestroyed()) {
            // parents didn't have the resource; attempt to load it from my urls
            return findResource(name);
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Enumeration findResources(String name) throws IOException {
        if (isDestroyed()) {
            return Collections.enumeration(Collections.EMPTY_SET);
        }

        List resources = new ArrayList();

        //
        // if we are using inverse class loading, add the resources from local urls first
        //
        if (inverseClassLoading && !isDestroyed()) {
            List myResources = Collections.list(super.findResources(name));
            resources.addAll(myResources);
        }

        //
        // Add parent resources
        //
        for (int i = 0; i < parents.length; i++) {
            ClassLoader parent = parents[i];
            List parentResources = Collections.list(parent.getResources(name));
            resources.addAll(parentResources);
        }

        //
        // if we are not using inverse class loading, add the resources from local urls now
        //
        if (!inverseClassLoading && !isDestroyed()) {
            List myResources = Collections.list(super.findResources(name));
            resources.addAll(myResources);
        }

        return Collections.enumeration(resources);
    }

    private boolean isNonOverridableResource(String name) {
        for (int i = 0; i < nonOverridableResources.length; i++) {
            if (name.startsWith(nonOverridableResources[i])) {
                return true;
            }
        }
        return false;
    }

    private boolean isHiddenResource(String name) {
        for (int i = 0; i < hiddenResources.length; i++) {
            if (name.startsWith(hiddenResources[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "[" + getClass().getName() + ":" +
                " name=" + getName() +
                " urls=" + Arrays.asList(getURLs()) +
                " parents=" + Arrays.asList(parents) +
                "]";
    }
}
