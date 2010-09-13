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
 * ClassLoader for a {@link Bundle}. 
 * <br/>
 * This ClassLoader implementation extends the {@link BundleClassLoader} and returns resources embedded
 * in jar files in a bundle with <tt>jar</tt> URLs. 
 * 
 * @version $Rev$ $Date$
 */
public class BundleResourceClassLoader extends BundleClassLoader {

    public BundleResourceClassLoader(Bundle bundle) {
        super(bundle);
    }

    @Override
    public String toString() {
        return "[BundleResourceClassLoader] " + bundle;
    }
  
    @Override
    public URL getResource(String name) {
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

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
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
            throw new IOException("Error discovering resources", e);
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
                URL jarURL = bundle.getEntry(jarName);
                URL url = new URL("jar:" + jarURL.toString() + "!/" + entry.getName());
                resources.add(url);
                return continueScanning;
            }
        });                   
        return resources;           
    }
   
}
