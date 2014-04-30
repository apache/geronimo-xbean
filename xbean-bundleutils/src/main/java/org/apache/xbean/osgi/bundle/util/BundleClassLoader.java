/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.xbean.osgi.bundle.util;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;

/**
 * ClassLoader for a {@link Bundle}. 
 * <br/>
 * In OSGi, resource lookup on resources in the <i>META-INF</i> directory using {@link Bundle#getResource(String)} or 
 * {@link Bundle#getResources(String)} does not return the resources found in the wired bundles of the bundle 
 * (wired via <i>Import-Package</i> or <i>DynamicImport-Package</i>). This class loader implementation provides 
 * {@link #getResource(String) and {@link #getResources(String)} methods that do delegate such resource lookups to
 * the wired bundles. 
 * <br/>
 * The URLs returned by {@link Bundle#getResource(String)} or {@link Bundle#getResources(String)} methods are 
 * OSGi framework specific &quot;bundle&quot; URLs. This sometimes can cause problems with 3rd party libraries
 * which do not understand how to interpret the &quot;bundle&quot; URLs. This ClassLoader implementation, if enabled,
 * can return <tt>jar</tt> URLs for resources found in embedded jars in the bundle. If a resource is found within a
 * directory in the bundle the URL returned for that resource is unconverted.
 * 
 * @version $Rev$ $Date$
 */
public class BundleClassLoader extends ClassLoader implements DelegatingBundleReference {

    protected final Bundle bundle;
    protected final BundleResourceHelper resourceHelper;

    public BundleClassLoader(Bundle bundle) {
        this(bundle, 
             BundleResourceHelper.getSearchWiredBundles(true), 
             BundleResourceHelper.getConvertResourceUrls(false));
    }
        
    public BundleClassLoader(Bundle bundle, boolean searchWiredBundles) {
        this(bundle, 
             searchWiredBundles, 
             BundleResourceHelper.getConvertResourceUrls(false));
    }
    
    public BundleClassLoader(Bundle bundle, boolean searchWiredBundles, boolean convertResourceUrls) {
        this.bundle = bundle;
        this.resourceHelper = new BundleResourceHelper(bundle, searchWiredBundles, convertResourceUrls);
    }

    @Override
    public String toString() {
        return "[BundleClassLoader] " + bundle;
    }
    
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class clazz = bundle.loadClass(name);
        if (resolve) {
            resolveClass(clazz);
        }
        return clazz;
    }
    
    @Override
    public URL getResource(String name) {
        return resourceHelper.getResource(name);
    }
    
    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return resourceHelper.getResources(name);
    }
    
    @Override
    public Enumeration<URL> findResources (String name) throws IOException {
        return this.getResources(name);
    }
    
    public void setSearchWiredBundles(boolean search) {
        resourceHelper.setSearchWiredBundles(search);
    }
    
    public boolean getSearchWiredBundles() {
        return resourceHelper.getSearchWiredBundles();
    }
           
    public void setConvertResourceUrls(boolean convert) {
        resourceHelper.setConvertResourceUrls(convert);
    }
    
    public boolean getConvertResourceUrls() {
        return resourceHelper.getConvertResourceUrls();
    }
    
    /**
     * Return the bundle associated with this classloader.
     * 
     * In most cases the bundle associated with the classloader is a regular framework bundle. 
     * However, in some cases the bundle associated with the classloader is a {@link DelegatingBundle}.
     * In such cases, the <tt>unwrap</tt> parameter controls whether this function returns the
     * {@link DelegatingBundle} instance or the main application bundle backing with the {@link DelegatingBundle}.
     *
     * @param unwrap If true and if the bundle associated with this classloader is a {@link DelegatingBundle}, 
     *        this function will return the main application bundle backing with the {@link DelegatingBundle}. 
     *        Otherwise, the bundle associated with this classloader is returned as is.
     * @return The bundle associated with this classloader.
     */
    public Bundle getBundle(boolean unwrap) {
        if (unwrap && bundle instanceof DelegatingBundle) {
            return ((DelegatingBundle) bundle).getMainBundle();
        }
        return bundle;
    }
    
    /**
     * Return the bundle associated with this classloader.
     * 
     * This method calls {@link #getBundle(boolean) getBundle(true)} and therefore always returns a regular 
     * framework bundle.  
     * <br><br>
     * Note: Some libraries use {@link BundleReference#getBundle()} to obtain a bundle for the given 
     * classloader and expect the returned bundle instance to be work with any OSGi API. Some of these API might
     * not work if {@link DelegatingBundle} is returned. That is why this function will always return
     * a regular framework bundle. See {@link #getBundle(boolean)} for more information.
     *
     * @return The bundle associated with this classloader.
     */
    public Bundle getBundle() {
        return getBundle(true);
    }

    @Override
    public int hashCode() {
        return bundle.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null || !other.getClass().equals(getClass())) {
            return false;
        }
        BundleClassLoader otherBundleClassLoader = (BundleClassLoader) other;
        return this.bundle == otherBundleClassLoader.bundle;
    }

}
