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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * @version $Rev$ $Date$
 */
public class BundleUtils {
    
    private static final boolean isOSGi43 = isOSGi43();
    
    private static boolean isOSGi43() {
        try {
            Class.forName("org.osgi.framework.wiring.BundleWiring");
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public final static String REFERENCE_SCHEME = "reference:";

    public final static String FILE_SCHEMA = "file:";

    public final static String REFERENCE_FILE_SCHEMA = "reference:file:";
    
    /**
     *  Based on the constant field values, if it is bigger than the RESOLVED status value, the bundle has been resolved by the framework
     * @param bundle
     * @return true if the bundle is resolved, or false if not.
     */
    public static boolean isResolved(Bundle bundle) {
        return bundle.getState() >= Bundle.RESOLVED;
    }

    /**
     * resolve method will try to load the Object.class, the behavior triggers a resolved request to the OSGI framework.
     * @param bundle
     */
    public static void resolve(Bundle bundle) {
        if (isFragment(bundle)) {
            return;
        }
        try {
            bundle.loadClass(Object.class.getName());
        } catch (Exception e) {
        }
    }

    /**
     * If the bundle fulfills the conditions below, it could be started
     * a. Not in the UNINSTALLED status.
     * b. Not in the STARTING status.
     * c. Not a fragment bundle.
     * @param bundle
     * @return
     */
    public static boolean canStart(Bundle bundle) {
        return (bundle.getState() != Bundle.UNINSTALLED) && (bundle.getState() != Bundle.STARTING) && (!isFragment(bundle));
    }

    /**
     * If the bundle fulfills the conditions below, it could be stopped
     * a. Not in the UNINSTALLED status.
     * b. Not in the STOPPING status.
     * c. Not a fragment bundle.
     * @param bundle
     * @return
     */
    public static boolean canStop(Bundle bundle) {
        return (bundle.getState() != Bundle.UNINSTALLED) && (bundle.getState() != Bundle.STOPPING) && (!isFragment(bundle));
    }

    /**
     * If the bundle fulfills the conditions below, it could be un-installed
     * a. Not in the UNINSTALLED status.
     * @param bundle
     * @return
     */
    public static boolean canUninstall(Bundle bundle) {
        return bundle.getState() != Bundle.UNINSTALLED;
    }

    public static boolean isFragment(Bundle bundle) {
        Dictionary headers = bundle.getHeaders();
        return (headers != null && headers.get(Constants.FRAGMENT_HOST) != null);
    }

    /**
     * Returns bundle (if any) associated with current thread's context classloader.
     * Invoking this method is equivalent to getBundle(Thread.currentThread().getContextClassLoader(), unwrap)
     */
    public static Bundle getContextBundle(boolean unwrap) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return classLoader == null ? null : getBundle(classLoader, unwrap);
    }

    /**
     *  Returns bundle (if any) associated with the classloader.
     * @param classLoader
     * @param unwrap if true and if the bundle associated with the context classloader is a
     *        {@link DelegatingBundle}, this function will return the main application bundle
     *        backing the {@link DelegatingBundle}. Otherwise, the bundle associated with
     *        the context classloader is returned as is. See {@link BundleClassLoader#getBundle(boolean)}
     *        for more information.
     * @return The bundle associated with the classloader. Might be null.
     */
    public static Bundle getBundle(ClassLoader classLoader, boolean unwrap) {
        if (classLoader instanceof DelegatingBundleReference) {
            return ((DelegatingBundleReference) classLoader).getBundle(unwrap);
        } else if (classLoader instanceof BundleReference) {
            return ((BundleReference) classLoader).getBundle();
        } else {
            return null;
        }
    }

    /**
     * If the given bundle is a {@link DelegatingBundle} this function will return the main
     * application bundle backing the {@link DelegatingBundle}. Otherwise, the bundle
     * passed in is returned as is.
     */
    public static Bundle unwrapBundle(Bundle bundle) {
        if (bundle instanceof DelegatingBundle) {
            return ((DelegatingBundle) bundle).getMainBundle();
        }
        return bundle;
    }

    /**
     * Works like {@link Bundle#getEntryPaths(String)} but also returns paths
     * in attached fragment bundles.
     *
     * @param bundle
     * @param name
     * @return
     */
    public static Enumeration<String> getEntryPaths(Bundle bundle, String name) {
        Enumeration<URL> entries = bundle.findEntries(name, null, false);
        if (entries == null) {
            return null;
        }
        LinkedHashSet<String> paths = new LinkedHashSet<String>();
        while (entries.hasMoreElements()) {
            URL url = entries.nextElement();
            String path = url.getPath();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            paths.add(path);
        }
        return Collections.enumeration(paths);
    }

    /**
     * 1, If the bundle was installed with reference directory mode
     * return the file URL directly.  
     * 2, For traditional package bundle, Works like {@link Bundle#getEntry(String)} 
     * 
     * In addition to the searching abaove, it also checks attached fragment bundles for the given entry.
     *
     * @param bundle
     * @param name
     * @return
     * @throws MalformedURLException 
     */
    public static URL getEntry(Bundle bundle, String name) throws MalformedURLException {
    	
    	if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        }    	
    	
        File bundleFile = toFile(bundle);
        if (bundleFile != null && bundleFile.isDirectory()) {
            File entryFile = new File(bundleFile, name);
            if (entryFile.exists()) {
				return entryFile.toURI().toURL();
            } 
        }
    	
    	if (name.equals("/")) {
            return bundle.getEntry(name);
        } 
    	
        String path;
        String pattern;
        int pos = name.lastIndexOf("/");
        if (pos == -1) {
            path = "/";
            pattern = name;
        } else if (pos == 0) {
            path = "/";
            pattern = name.substring(1);
        } else {
            path = name.substring(0, pos);
            pattern = name.substring(pos + 1);
        }
        Enumeration<URL> entries = bundle.findEntries(path, pattern, false);
        if (entries != null && entries.hasMoreElements()) {
            return entries.nextElement();
        } else {
            return null;
        }
    }
    
    public static URL getNestedEntry(Bundle bundle, String jarEntryName, String subEntryName) throws MalformedURLException {
        File bundleFile = toFile(bundle);
        if (bundleFile != null && bundleFile.isDirectory()) {
            File entryFile = new File(bundleFile, jarEntryName);
            if (entryFile.exists()) {
                if (entryFile.isFile()) {
                    return new URL("jar:" + entryFile.toURI().toURL() + "!/" + subEntryName);
                } else {
                    return new File(entryFile, subEntryName).toURI().toURL();
                }
            }
            return null;
        }
        return new URL("jar:" + bundle.getEntry(jarEntryName).toString() + "!/" + subEntryName);
    }    
    

    public static File toFile(Bundle bundle) {
        return toFile(bundle.getLocation());
    }

    public static File toFile(URL url) {
        return toFile(url.toExternalForm());
    }
    
    /**
     * Translate the reference:file:// style URL  to the underlying file instance
     * @param url
     * @return
     */
    public static File toFile(String url) {
        if (url !=null && url.startsWith(REFERENCE_FILE_SCHEMA)) {
            File file = null;
            try {
                file = new File(new URL(url.substring(REFERENCE_SCHEME.length())).toURI());
                if (file.exists()) {
                    return file;
                }
            } catch (Exception e) {
                // If url includes special chars: { } [ ] % < > # ^ ?
                // URISyntaxException or MalformedURLException will be thrown, 
                // so try to use File(String) directly
                file = new File(url.substring(REFERENCE_FILE_SCHEMA.length()));
                if (file.exists()) {
                    return file;
                }
            }
        }
        return null;
    }

    public static String toReferenceFileLocation(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("file not exist " + file.getAbsolutePath());
        }
        return REFERENCE_SCHEME + file.toURI();
    }


    public static LinkedHashSet<Bundle> getWiredBundles(Bundle bundle) {
        if (isOSGi43) {
            return getWiredBundles43(bundle);
        } else {
            return getWiredBundles42(bundle);
        }
    }
    
    private static LinkedHashSet<Bundle> getWiredBundles42(Bundle bundle) {
        ServiceReference reference = bundle.getBundleContext().getServiceReference(PackageAdmin.class.getName());
        PackageAdmin packageAdmin = (PackageAdmin) bundle.getBundleContext().getService(reference);
        try {
            return getWiredBundles(packageAdmin, bundle);
        } finally {
            bundle.getBundleContext().ungetService(reference);
        }
    }
    
    public static LinkedHashSet<Bundle> getWiredBundles(PackageAdmin packageAdmin, Bundle bundle) {
        BundleDescription description = new BundleDescription(bundle.getHeaders());
        // handle static wire via Import-Package
        List<BundleDescription.ImportPackage> imports = description.getExternalImports();
        LinkedHashSet<Bundle> wiredBundles = new LinkedHashSet<Bundle>();
        for (BundleDescription.ImportPackage packageImport : imports) {
            ExportedPackage[] exports = packageAdmin.getExportedPackages(packageImport.getName());
            Bundle wiredBundle = getWiredBundle(bundle, exports);
            if (wiredBundle != null) {
                wiredBundles.add(wiredBundle);
            }
        }
        // handle dynamic wire via DynamicImport-Package
        if (!description.getDynamicImportPackage().isEmpty()) {
            for (Bundle b : bundle.getBundleContext().getBundles()) {
                if (!wiredBundles.contains(b)) {
                    ExportedPackage[] exports = packageAdmin.getExportedPackages(b);
                    Bundle wiredBundle = getWiredBundle(bundle, exports);
                    if (wiredBundle != null) {
                        wiredBundles.add(wiredBundle);
                    }
                }
            }
        }
        return wiredBundles;
    }

    static Bundle getWiredBundle(Bundle bundle, ExportedPackage[] exports) {
        if (exports != null) {
            for (ExportedPackage exportedPackage : exports) {
                Bundle[] importingBundles = exportedPackage.getImportingBundles();
                if (importingBundles != null) {
                    for (Bundle importingBundle : importingBundles) {
                        if (importingBundle == bundle) {
                            return exportedPackage.getExportingBundle();
                        }
                    }
                }
            }
        }
        return null;
    }
    
    // OSGi 4.3 API
    
    private static LinkedHashSet<Bundle> getWiredBundles43(Bundle bundle) {
        LinkedHashSet<Bundle> wiredBundles = new LinkedHashSet<Bundle>();
        BundleWiring wiring = bundle.adapt(BundleWiring.class);
        if (wiring != null) {
            List<BundleWire> wires;
            wires = wiring.getRequiredWires(BundleRevision.PACKAGE_NAMESPACE);
            for (BundleWire wire : wires) {
                wiredBundles.add(wire.getProviderWiring().getBundle());
            }
            wires = wiring.getRequiredWires(BundleRevision.BUNDLE_NAMESPACE);
            for (BundleWire wire : wires) {
                wiredBundles.add(wire.getProviderWiring().getBundle());
            }
        }        
        return wiredBundles;
    }
    
}
