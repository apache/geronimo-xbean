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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Bundle that delegates ClassLoader operations to a collection of {@link Bundle} objects.
 *
 * @version $Rev$ $Date$
 */
public class DelegatingBundle implements Bundle {

    private CopyOnWriteArrayList<Bundle> bundles;
    private Bundle bundle;
    private BundleContext bundleContext;

    public DelegatingBundle(Collection<Bundle> bundles) {
        if (bundles.isEmpty()) {
            throw new IllegalArgumentException("At least one bundle is required");
        }
        this.bundles = new CopyOnWriteArrayList<Bundle>(bundles);
        // assume first Bundle is the main bundle
        this.bundle = bundles.iterator().next();
        this.bundleContext = new DelegatingBundleContext(this, bundle.getBundleContext());
    }

    public DelegatingBundle(Bundle bundle) {
        this(Collections.singletonList(bundle));
    }

    public Bundle getMainBundle() {
        return bundle;
    }

    public Class<?> loadClass(String name) throws ClassNotFoundException {
        try {
            return bundle.loadClass(name);
        } catch (ClassNotFoundException ex) {
            int index = name.lastIndexOf('.');
            if (index > 0 && bundles.size() > 1) {
                // see if there are any bundles exporting the package
                String packageName = name.substring(0, index);
                Set<Bundle> packageBundles = getPackageBundles(packageName);
                if (packageBundles == null) {
                    // package is NOT exported

                    Iterator<Bundle> iterator = bundles.iterator();
                    // skip first bundle
                    iterator.next();
                    // attempt to load the class from the remaining bundles
                    while (iterator.hasNext()) {
                        Bundle delegate = iterator.next();
                        try {
                            return delegate.loadClass(name);
                        } catch (ClassNotFoundException e) {
                            // ignore
                        }
                    }

                    throw ex;
                } else {
                    // package is exported

                    // see if any of our bundles is wired to the exporter
                    Bundle delegate = findFirstBundle(packageBundles);
                    if (delegate == null || delegate == bundle) {
                        // nope. no static wires but might need to check for dynamic wires in the future.
                        throw ex;
                    } else {
                        // yes. attempt to load the class from it
                        return delegate.loadClass(name);
                    }
                }
            }  else {
                // no package name
                throw ex;
            }
        }
    }

    private Set<Bundle> getPackageBundles(String packageName) {
        BundleContext context = bundle.getBundleContext();
        ServiceReference reference = context.getServiceReference(PackageAdmin.class.getName());
        PackageAdmin packageAdmin = (PackageAdmin) context.getService(reference);
        Set<Bundle> bundles = null;
        try {
            ExportedPackage[] exportedPackages = packageAdmin.getExportedPackages(packageName);
            if (exportedPackages != null && exportedPackages.length > 0) {
                bundles = new HashSet<Bundle>();
                for (ExportedPackage exportedPackage : exportedPackages) {
                    bundles.add(exportedPackage.getExportingBundle());
                    Bundle[] importingBundles = exportedPackage.getImportingBundles();
                    if (importingBundles != null) {
                        for (Bundle importingBundle : importingBundles) {
                            bundles.add(importingBundle);
                        }
                    }
                }
            }
            return bundles;
        } finally {
            context.ungetService(reference);
        }
    }

    private Bundle findFirstBundle(Set<Bundle> packageBundles) {
        Collection<Bundle> c1 = bundles;
        Collection<Bundle> c2 = packageBundles;

        if (bundles instanceof Set<?> && bundles.size() > packageBundles.size()) {
            c1 = packageBundles;
            c2 = bundles;
        }

        for (Bundle bundle : c1) {
            if (c2.contains(bundle)) {
                return bundle;
            }
        }

        return null;
    }

    public void addBundle(Bundle b) {
        bundles.add(b);
    }

    public void removeBundle(Bundle b) {
        bundles.remove(b);
    }

    public URL getResource(String name) {
        URL resource = null;
        for (Bundle bundle : bundles) {
            resource = bundle.getResource(name);
            if (resource != null) {
                return resource;
            }
        }
        return null;
    }

    public Enumeration<URL> getResources(String name) throws IOException {
        ArrayList<URL> allResources = new ArrayList<URL>();
        for (Bundle bundle : bundles) {
            Enumeration<URL> e = bundle.getResources(name);
            addToList(allResources, e);
        }
        return Collections.enumeration(allResources);
    }

    private static void addToList(List<URL> list, Enumeration<URL> enumeration) {
        if (enumeration != null) {
            while (enumeration.hasMoreElements()) {
                list.add(enumeration.nextElement());
            }
        }
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public Enumeration findEntries(String arg0, String arg1, boolean arg2) {
        return bundle.findEntries(arg0, arg1, arg2);
    }

    public long getBundleId() {
        return bundle.getBundleId();
    }

    public URL getEntry(String arg0) {
        return bundle.getEntry(arg0);
    }

    public Enumeration getEntryPaths(String arg0) {
        return bundle.getEntryPaths(arg0);
    }

    public Dictionary getHeaders() {
        return bundle.getHeaders();
    }

    public Dictionary getHeaders(String arg0) {
        return bundle.getHeaders(arg0);
    }

    public long getLastModified() {
        return bundle.getLastModified();
    }

    public String getLocation() {
        return bundle.getLocation();
    }

    public ServiceReference[] getRegisteredServices() {
        return bundle.getRegisteredServices();
    }

    public ServiceReference[] getServicesInUse() {
        return bundle.getServicesInUse();
    }

    public Map getSignerCertificates(int arg0) {
        return bundle.getSignerCertificates(arg0);
    }

    public int getState() {
        return bundle.getState();
    }

    public String getSymbolicName() {
        return bundle.getSymbolicName();
    }

    public Version getVersion() {
        return bundle.getVersion();
    }

    public boolean hasPermission(Object arg0) {
        return bundle.hasPermission(arg0);
    }

    public void start() throws BundleException {
        bundle.start();
    }

    public void start(int arg0) throws BundleException {
        bundle.start(arg0);
    }

    public void stop() throws BundleException {
        bundle.stop();
    }

    public void stop(int arg0) throws BundleException {
        bundle.stop(arg0);
    }

    public void uninstall() throws BundleException {
        bundle.uninstall();
    }

    public void update() throws BundleException {
        bundle.update();
    }

    public void update(InputStream arg0) throws BundleException {
        bundle.update(arg0);
    }

    public int compareTo(Bundle other) {
        return bundle.compareTo(other);
    }

    public <A> A adapt(Class<A> type) {
        return bundle.adapt(type);
    }

    public File getDataFile(String filename) {
        return bundle.getDataFile(filename);
    }
    
    public String toString() {
        return "[DelegatingBundle: " + bundles + "]";
    }

}
