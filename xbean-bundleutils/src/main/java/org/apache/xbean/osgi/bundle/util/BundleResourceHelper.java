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
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.zip.ZipEntry;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Helper for finding resources in a {@link Bundle}. 
 * <br/>
 * In OSGi, resource lookup on resources in the <i>META-INF</i> directory using {@link Bundle#getResource(String)} or 
 * {@link Bundle#getResources(String)} does not return the resources found in the wired bundles of the bundle 
 * (wired via <i>Import-Package</i> or <i>DynamicImport-Package</i>). This class loader implementation provides 
 * {@link #getResource(String) and {@link #getResources(String)} methods that do delegate <i>META-INF</i> resource lookups
 * to the wired bundles. 
 * <br/>
 * The URLs returned by {@link Bundle#getResource(String)} or {@link Bundle#getResources(String)} methods are 
 * OSGi framework specific &quot;bundle&quot; URLs. If enabled, this helper can convert the framework specific URLs into 
 * regular <tt>jar</tt> URLs. 
 * 
 * @version $Rev$ $Date$
 */
public class BundleResourceHelper {

    public static final String SEARCH_WIRED_BUNDLES = BundleResourceHelper.class.getName() + ".searchWiredBundles";
    public static final String CONVERT_RESOURCE_URLS = BundleResourceHelper.class.getName() + ".convertResourceUrls";
    
    private final static String META_INF_1 = "META-INF/";
    private final static String META_INF_2 = "/META-INF/";
    
    protected final Bundle bundle;
    private LinkedHashSet<Bundle> wiredBundles = null;
    protected boolean searchWiredBundles;
    protected boolean convertResourceUrls;
  
    public BundleResourceHelper(Bundle bundle) {
        this(bundle,      
             BundleResourceHelper.getSearchWiredBundles(false), 
             BundleResourceHelper.getConvertResourceUrls(false));
    }
    
    public BundleResourceHelper(Bundle bundle, boolean searchWiredBundles, boolean convertResourceUrls) {
        this.bundle = bundle;
        this.searchWiredBundles = searchWiredBundles;
        this.convertResourceUrls = convertResourceUrls;
    }

    public void setSearchWiredBundles(boolean search) {
        searchWiredBundles = search;
    }
    
    public boolean getSearchWiredBundles() {
        return searchWiredBundles;
    }
  
    public void setConvertResourceUrls(boolean convert) {
        convertResourceUrls = convert;
    }
    
    public boolean getConvertResourceUrls() {
        return convertResourceUrls;
    }
        
    public URL getResource(String name) {
        if (convertResourceUrls) {
            return convertedFindResource(name);
        } else {
            return findResource(name);
        }
    }
    
    public Enumeration<URL> getResources(String name) throws IOException {
        if (convertResourceUrls) {
            return convertedFindResources(name);
        } else {
            return findResources(name);
        }
    }
    
    protected URL convert(URL url) {
        return url;
    }
    
    private synchronized LinkedHashSet<Bundle> getWiredBundles() {
        if (wiredBundles == null) {
            wiredBundles = BundleUtils.getWiredBundles((bundle instanceof DelegatingBundle) ? ((DelegatingBundle) bundle).getMainBundle() : bundle);
        }
        return wiredBundles;
    }
    
    private boolean isMetaInfResource(String name) {
        return searchWiredBundles && name != null && (name.startsWith(META_INF_1) || name.startsWith(META_INF_2));
    }
      
    private List<URL> getList() {
        if (convertResourceUrls) {
            return new ArrayList<URL>() {
                public boolean add(URL u) {
                    return super.add(convert(u));
                }
            };
        } else {
            return new ArrayList<URL>();
        }
    }
    
    private void addToList(List<URL> list, Enumeration<URL> enumeration) {
        if (enumeration != null) {
            while (enumeration.hasMoreElements()) {
                list.add(enumeration.nextElement());
            }
        }
    }
           
    protected URL findResource(String name) {
        URL resource = bundle.getResource(name);
        if (resource == null && isMetaInfResource(name)) {
            LinkedHashSet<Bundle> wiredBundles = getWiredBundles();
            Iterator<Bundle> iterator = wiredBundles.iterator();
            while (iterator.hasNext() && resource == null) {                
                resource = iterator.next().getResource(name);
            }
        }
        if (resource != null && convertResourceUrls) {
            resource = convert(resource);
        }
        return resource;
    }

    protected Enumeration<URL> findResources(String name) throws IOException {
        Enumeration<URL> e = (Enumeration<URL>) bundle.getResources(name);
        if (isMetaInfResource(name)) {
            List<URL> allResources = getList();
            addToList(allResources, e);
            LinkedHashSet<Bundle> wiredBundles = getWiredBundles();
            for (Bundle wiredBundle : wiredBundles) {
                Enumeration<URL> resources = wiredBundle.getResources(name);
                addToList(allResources, resources);
            }
            return Collections.enumeration(allResources);            
        } else if (e == null) {
            return Collections.enumeration(Collections.<URL>emptyList());
        } else if (convertResourceUrls) {
            List<URL> allResources = getList();
            addToList(allResources, e);
            return Collections.enumeration(allResources);
        } else {
            return e;            
        }
    }    
    
    /**
     * Lookup resource and return converted URL (in a generic way).
     * 
     * @param name
     * @return
     */
    protected URL convertedFindResource(String name) {
        ServiceReference reference = bundle.getBundleContext().getServiceReference(PackageAdmin.class.getName());
        PackageAdmin packageAdmin = (PackageAdmin) bundle.getBundleContext().getService(reference);
        try {
            List<URL> resources = findResources(packageAdmin, bundle, name, false);
            if (resources.isEmpty() && isMetaInfResource(name)) {
                LinkedHashSet<Bundle> wiredBundles = getWiredBundles();
                Iterator<Bundle> iterator = wiredBundles.iterator();
                while (iterator.hasNext() && resources.isEmpty()) {    
                    Bundle wiredBundle = iterator.next();
                    resources = findResources(packageAdmin, wiredBundle, name, false);
                }
            }
            return (resources.isEmpty()) ? null : resources.get(0);
        } catch (Exception e) {
            return null;
        } finally {
            bundle.getBundleContext().ungetService(reference);
        }
    }

    /**
     * Lookup resources and return converted URLs (in a generic way).
     * 
     * @param name
     * @return
     */
    protected Enumeration<URL> convertedFindResources(String name) throws IOException {
        ServiceReference reference = bundle.getBundleContext().getServiceReference(PackageAdmin.class.getName());
        PackageAdmin packageAdmin = (PackageAdmin) bundle.getBundleContext().getService(reference);
        try {
            List<URL> resources = findResources(packageAdmin, bundle, name, true);
            if (isMetaInfResource(name)) {
                LinkedHashSet<Bundle> wiredBundles = getWiredBundles();
                for (Bundle wiredBundle : wiredBundles) {
                    resources.addAll(findResources(packageAdmin, wiredBundle, name, true));
                }
            }
            return Collections.enumeration(resources);
        } catch (Exception e) {
            throw (IOException) new IOException("Error discovering resources: " + e).initCause(e);
        } finally {
            bundle.getBundleContext().ungetService(reference);
        }
    }
    
    private static List<URL> findResources(PackageAdmin packageAdmin, 
                                           Bundle bundle, 
                                           String name, 
                                           final boolean continueScanning) throws Exception {
        BundleResourceFinder finder = new BundleResourceFinder(packageAdmin, bundle, "", name);
        final List<URL> resources = new ArrayList<URL>();
        finder.find(new BundleResourceFinder.ResourceFinderCallback() {

            public boolean foundInDirectory(Bundle bundle, String baseDir, URL url) throws Exception {
                resources.add(url);
                return continueScanning;
            }

            public boolean foundInJar(Bundle bundle, String jarName, ZipEntry entry, InputStream inputStream) throws Exception {
                URL jarURL = BundleUtils.getEntry(bundle, jarName);
                URL url = new URL("jar:" + jarURL.toString() + "!/" + entry.getName());
                resources.add(url);
                return continueScanning;
            }
        });                   
        return resources;           
    }
    
    public static boolean getSearchWiredBundles(boolean defaultValue) {
        String value = System.getProperty(SEARCH_WIRED_BUNDLES);
        return (value == null) ? defaultValue : Boolean.parseBoolean(value);        
    }
    
    public static boolean getConvertResourceUrls(boolean defaultValue) {
        String value = System.getProperty(CONVERT_RESOURCE_URLS);
        return (value == null) ? defaultValue : Boolean.parseBoolean(value);        
    }
}
