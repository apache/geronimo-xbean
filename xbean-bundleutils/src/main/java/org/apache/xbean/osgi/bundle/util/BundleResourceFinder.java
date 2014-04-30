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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.xbean.osgi.bundle.util.BundleDescription.HeaderEntry;
import org.osgi.framework.Bundle;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Finds all available resources to a bundle by scanning Bundle-ClassPath header
 * of the given bundle and its fragments.
 * DynamicImport-Package header is not considered during scanning.
 *
 * @version $Rev$ $Date$
 */
public class BundleResourceFinder {

    public static final ResourceDiscoveryFilter FULL_DISCOVERY_FILTER = new DummyDiscoveryFilter();
    
    private final Bundle bundle;
    private final PackageAdmin packageAdmin;
    private final String prefix;
    private final String suffix;
    private final String osgiSuffix;
    private final boolean extendedMatching;
    private ResourceDiscoveryFilter discoveryFilter;

    public BundleResourceFinder(PackageAdmin packageAdmin, Bundle bundle, String prefix, String suffix) {
        this(packageAdmin, bundle, prefix, suffix, FULL_DISCOVERY_FILTER);
    }

    /**
     * Set up a BundleResourceFinder
     * The suffix may contain a path fragment, unlike the bundle.findEntries method.
     *
     * @param packageAdmin package admin for finding fragments
     * @param bundle bundle to search
     * @param prefix search only paths and zip files starting with this prefix
     * @param suffix return only entries ending in this suffix.
     * @param discoveryFilter filter for matching directories and zip files.
     */
    public BundleResourceFinder(PackageAdmin packageAdmin, Bundle bundle, String prefix, String suffix, ResourceDiscoveryFilter discoveryFilter) {
        this.packageAdmin = packageAdmin;
        this.bundle = BundleUtils.unwrapBundle(bundle);
        this.prefix = addSlash(prefix.trim());
        this.suffix = suffix.trim();
        int pos = this.suffix.lastIndexOf("/");
        if (pos > -1) {
            osgiSuffix = this.suffix.substring(pos + 1, this.suffix.length());
            extendedMatching = true;
        } else {
            osgiSuffix = "*" + this.suffix;
            extendedMatching = false;
        }
        this.discoveryFilter = discoveryFilter;
    }

    public void find(ResourceFinderCallback callback) throws Exception {
        if (discoveryFilter.rangeDiscoveryRequired(DiscoveryRange.BUNDLE_CLASSPATH)) {
            if (!scanBundleClassPath(callback, bundle)) {
                return;
            }
        }
        if (packageAdmin != null && discoveryFilter.rangeDiscoveryRequired(DiscoveryRange.FRAGMENT_BUNDLES)) {
            Bundle[] fragments = packageAdmin.getFragments(bundle);
            if (fragments != null) {
                for (Bundle fragment : fragments) {
                    if (!scanBundleClassPath(callback, fragment)) {
                        return;
                    }
                }
            }
        }
    }

    public Set<URL> find() {
        Set<URL> resources = new LinkedHashSet<URL>();
        try {
            find(new DefaultResourceFinderCallback(resources));
        } catch (Exception e) {
            // this should not happen
            throw new RuntimeException("Resource discovery failed", e);
        }
        return resources;
    }

    private boolean scanBundleClassPath(ResourceFinderCallback callback, Bundle bundle) throws Exception {
        BundleDescription desc = new BundleDescription(bundle.getHeaders());
        List<HeaderEntry> paths = desc.getBundleClassPath();
        boolean continueScanning = true;
        if (paths.isEmpty()) {
            continueScanning = scanDirectory(callback, bundle, prefix);
        } else {
            for (HeaderEntry path : paths) {
                String name = path.getName();
                if (name.equals(".") || name.equals("/")) {
                    // scan root
                    continueScanning = scanDirectory(callback, bundle, prefix);
                } else if (name.endsWith(".jar") || name.endsWith(".zip")) {
                    // scan embedded jar/zip
                    continueScanning = scanZip(callback, bundle, name);
                } else {
                    // assume it's a directory                    
                    continueScanning = scanDirectory(callback, bundle, prefix.startsWith("/") ? name + prefix : name + "/" + prefix);
                }
                if (!continueScanning) {
                    break;
                }
            }
        }
        return continueScanning;
    }

    private boolean scanDirectory(ResourceFinderCallback callback, Bundle bundle, String basePath) throws Exception {
        if (!discoveryFilter.directoryDiscoveryRequired(basePath)) {
            return true;
        }
        Enumeration e = bundle.findEntries(basePath, osgiSuffix, true);
        if (e != null) {
            while (e.hasMoreElements()) {
                URL url = (URL) e.nextElement();
                if (!extendedMatching || suffixMatches(url.getPath())) {
                    if (!callback.foundInDirectory(bundle, basePath, url)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean scanZip(ResourceFinderCallback callback, Bundle bundle, String zipName) throws Exception {
        if (!discoveryFilter.zipFileDiscoveryRequired(zipName)) {
            return true;
        }
        URL zipEntry = bundle.getEntry(zipName);
        if (zipEntry == null) {
            return true;
        }
        ZipInputStream in = null;
        try {
            in = new ZipInputStream(zipEntry.openStream());
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                String name = entry.getName();
                if (prefixMatches(name) && suffixMatches(name)) {
                    if (!callback.foundInJar(bundle, zipName, entry, new ZipEntryInputStream(in))) {
                        return false;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try { in.close(); } catch (Exception e) {}
            }
        }
        return true;
    }

    private static class ZipEntryInputStream extends FilterInputStream {
        public ZipEntryInputStream(ZipInputStream in) {
            super(in);
        }
        public void close() throws IOException {
            // not really necessary
            // ((ZipInputStream) in).closeEntry();
        }
    }

    private boolean prefixMatches(String name) {
        if (prefix.length() == 0 || prefix.equals(".") || prefix.equals("/")) {
            return true;
        } else if (prefix.startsWith("/")) {
            return name.startsWith(prefix, 1);
        } else {
            return name.startsWith(prefix);
        }
    }

    private boolean suffixMatches(String name) {
        return (suffix.length() == 0) ? true : name.endsWith(suffix);
    }

    private static String addSlash(String name) {
        if (name == null ) return "";
        name = name.trim();
        if (name.length() != 0 && !name.endsWith("/")) {
            name = name + "/";
        }
        return name;
    }

    public interface ResourceFinderCallback {
        /**
         * Resource found in a directory in a bundle.
         * 
         * @return true to continue scanning, false to abort scanning.
         */
        boolean foundInDirectory(Bundle bundle, String baseDir, URL url) throws Exception;

        /**         
         * Resource found in a jar file in a bundle.
         * 
         * @return true to continue scanning, false to abort scanning.
         */
        boolean foundInJar(Bundle bundle, String jarName, ZipEntry entry, InputStream in) throws Exception;
    }

    public static class DefaultResourceFinderCallback implements ResourceFinderCallback {

        private Set<URL> resources;

        public DefaultResourceFinderCallback() {
            this(new LinkedHashSet<URL>());
        }

        public DefaultResourceFinderCallback(Set<URL> resources) {
            this.resources = resources;
        }

        public Set<URL> getResources() {
            return resources;
        }

        public boolean foundInDirectory(Bundle bundle, String baseDir, URL url) throws Exception {
            resources.add(url);
            return true;
        }

        public boolean foundInJar(Bundle bundle, String jarName, ZipEntry entry, InputStream in) throws Exception {
            URL jarURL = bundle.getEntry(jarName);
            URL url = new URL("jar:" + jarURL.toString() + "!/" + entry.getName());
            resources.add(url);
            return true;
        }

    }

    public static class DummyDiscoveryFilter implements ResourceDiscoveryFilter {

        public boolean rangeDiscoveryRequired(DiscoveryRange discoveryRange) {
            return true;
        }
        
        public boolean directoryDiscoveryRequired(String url) {
            return true;
        }

        public boolean zipFileDiscoveryRequired(String url) {
            return true;
        }

    }
}
