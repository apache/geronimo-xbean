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
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.xbean.osgi.bundle.util.BundleDescription.ExportPackage;
import org.apache.xbean.osgi.bundle.util.BundleDescription.HeaderEntry;
import org.apache.xbean.osgi.bundle.util.BundleDescription.RequireBundle;
import org.osgi.framework.Bundle;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds all available classes to a bundle by scanning Bundle-ClassPath,
 * Import-Package, and Require-Bundle headers of the given bundle and its fragments.
 * DynamicImport-Package header is not considered during scanning.
 *
 * @version $Rev$ $Date$
 */
public class BundleClassFinder {

    private static final Logger logger = LoggerFactory.getLogger(BundleClassFinder.class);

    public static final ClassDiscoveryFilter FULL_CLASS_DISCOVERY_FILTER = new DummyDiscoveryFilter();

    public static final ClassDiscoveryFilter IMPORTED_PACKAGE_EXCLUSIVE_FILTER = new NonImportedPackageDiscoveryFilter();

    protected static final String EXT = ".class";

    protected static final String PATTERN = "*.class";

    protected Bundle bundle;

    protected PackageAdmin packageAdmin;

    private Map<Bundle, Set<String>> classMap;

    protected ClassDiscoveryFilter discoveryFilter;

    public BundleClassFinder(PackageAdmin packageAdmin, Bundle bundle) {
        this(packageAdmin, bundle, FULL_CLASS_DISCOVERY_FILTER);
    }

    public BundleClassFinder(PackageAdmin packageAdmin, Bundle bundle, ClassDiscoveryFilter discoveryFilter) {
        this.packageAdmin = packageAdmin;
        this.bundle = BundleUtils.unwrapBundle(bundle);
        this.discoveryFilter = discoveryFilter;
    }

    public List<Class<?>> loadClasses(Set<String> classes) {
        List<Class<?>> loadedClasses = new ArrayList<Class<?>>(classes.size());
        for (String clazz : classes) {
            try {
                loadedClasses.add(bundle.loadClass(clazz));
            } catch (Exception ignore) {
                // ignore
            }
        }
        return loadedClasses;
    }

    /**
     * Finds all available classes to the bundle. Some of the classes in the returned set
     * might not be loadable.
     *
     * @return classes visible to the bundle. Not all classes returned might be loadable.
     */
    public Set<String> find() {
        Set<String> classes = new LinkedHashSet<String>();
        classMap = new HashMap<Bundle, Set<String>>();
        if (discoveryFilter.rangeDiscoveryRequired(DiscoveryRange.IMPORT_PACKAGES)) {
            scanImportPackages(classes, bundle, bundle);
        }
        if (discoveryFilter.rangeDiscoveryRequired(DiscoveryRange.REQUIRED_BUNDLES)) {
            scanRequireBundles(classes, bundle);
        }
        if (discoveryFilter.rangeDiscoveryRequired(DiscoveryRange.BUNDLE_CLASSPATH)) {
            scanBundleClassPath(classes, bundle);
        }
        if (discoveryFilter.rangeDiscoveryRequired(DiscoveryRange.FRAGMENT_BUNDLES)) {
            Bundle[] fragments = packageAdmin.getFragments(bundle);
            if (fragments != null) {
                for (Bundle fragment : fragments) {
                    scanImportPackages(classes, bundle, fragment);
                    scanRequireBundles(classes, fragment);
                    scanBundleClassPath(classes, fragment);
                }
            }
        }
        classMap.clear();
        return classes;
    }

    protected boolean isClassAcceptable(String name, InputStream in) throws IOException {
        return true;
    }

    protected boolean isClassAcceptable(URL url) {
        return true;
    }

    protected BundleClassFinder createSubBundleClassFinder(PackageAdmin packageAdmin, Bundle bundle, ClassDiscoveryFilter classDiscoveryFilter) {
        return new BundleClassFinder(packageAdmin, bundle, classDiscoveryFilter);
    }

    protected String toJavaStyleClassName(String name) {
        if (name.endsWith(EXT)) {
            name = name.substring(0, name.length() - EXT.length());
        }
        name = name.replace('/', '.');
        return name;
    }

    /**
     * Get the normal Java style package name from the parameter className.
     * If the className is ended with .class extension, e.g.  /org/apache/geronimo/TestCass.class or org.apache.geronimo.TestClass.class,
     *      then org/apache/geronimo is returned
     * If the className is not ended with .class extension, e.g.  /org/apache/geronimo/TestCass or org.apache.geronimo.TestClass,
     *      then org/apache/geronimo is returned
     * @return Normal Java style package name, should be like org.apache.geronimo
     */
    protected String toJavaStylePackageName(String className) {
        if (className.endsWith(EXT)) {
            className = className.substring(0, className.length() - EXT.length());
        }
        className = className.replace('/', '.');
        int iLastDotIndex = className.lastIndexOf('.');
        if (iLastDotIndex != -1) {
            return className.substring(0, iLastDotIndex);
        } else {
            return "";
        }
    }

    private Set<String> findAllClasses(Bundle bundle, ClassDiscoveryFilter userClassDiscoveryFilter, Set<String> exportedPackageNames) {
        Set<String> allClasses = classMap.get(bundle);
        if (allClasses == null) {
            BundleClassFinder finder = createSubBundleClassFinder(packageAdmin, bundle, new ImportExclusivePackageDiscoveryFilterAdapter(userClassDiscoveryFilter, exportedPackageNames));
            allClasses = finder.find();
            classMap.put(bundle, allClasses);
        }
        return allClasses;
    }

    private Set<String> findAllClasses(Bundle bundle, String packageName) {
        Set<String> allClasses = classMap.get(bundle);
        if (allClasses == null) {
            BundleClassFinder finder = createSubBundleClassFinder(packageAdmin, bundle, new ImportExclusivePackageDiscoveryFilter(packageName));
            allClasses = finder.find();
            classMap.put(bundle, allClasses);
        }
        return allClasses;
    }

    private void scanImportPackages(Collection<String> classes, Bundle host, Bundle fragment) {
        BundleDescription description = new BundleDescription(fragment.getHeaders());
        List<BundleDescription.ImportPackage> imports = description.getExternalImports();
        for (BundleDescription.ImportPackage packageImport : imports) {
            String packageName = packageImport.getName();
            if (discoveryFilter.packageDiscoveryRequired(packageName)) {
                ExportedPackage[] exports = packageAdmin.getExportedPackages(packageName);
                Bundle wiredBundle = isWired(host, exports);
                if (wiredBundle != null) {
                    Set<String> allClasses = findAllClasses(wiredBundle, packageName);
                    classes.addAll(allClasses);
                }
            }
        }
    }

    private void scanRequireBundles(Collection<String> classes, Bundle bundle) {
        BundleDescription description = new BundleDescription(bundle.getHeaders());
        List<RequireBundle> requiredBundleList = description.getRequireBundle();
        for (RequireBundle requiredBundle : requiredBundleList) {
            RequiredBundle[] requiredBundles = packageAdmin.getRequiredBundles(requiredBundle.getName());
            Bundle wiredBundle = isWired(bundle, requiredBundles);
            if (wiredBundle != null) {
                BundleDescription wiredBundleDescription = new BundleDescription(wiredBundle.getHeaders());
                List<ExportPackage> exportPackages = wiredBundleDescription.getExportPackage();
                Set<String> exportedPackageNames = new HashSet<String>();
                for (ExportPackage exportPackage : exportPackages) {
                    exportedPackageNames.add(exportPackage.getName());
                }
                Set<String> allClasses = findAllClasses(wiredBundle, discoveryFilter, exportedPackageNames);
                classes.addAll(allClasses);
            }
        }
    }

    private void scanBundleClassPath(Collection<String> resources, Bundle bundle) {
        BundleDescription description = new BundleDescription(bundle.getHeaders());
        List<HeaderEntry> paths = description.getBundleClassPath();
        if (paths.isEmpty()) {
            scanDirectory(resources, bundle, "/");
        } else {
            for (HeaderEntry path : paths) {
                String name = path.getName();
                if (name.equals(".") || name.equals("/")) {
                    // scan root
                    scanDirectory(resources, bundle, "/");
                } else if (name.endsWith(".jar") || name.endsWith(".zip")) {
                    // scan embedded jar/zip
                    scanZip(resources, bundle, name);
                } else {
                    // assume it's a directory
                    scanDirectory(resources, bundle, "/" + name);
                }
            }
        }
    }

    private void scanDirectory(Collection<String> classes, Bundle bundle, String basePath) {
        basePath = addSlash(basePath);
        if (!discoveryFilter.directoryDiscoveryRequired(basePath)) {
            return;
        }
        Enumeration<URL> e = bundle.findEntries(basePath, PATTERN, true);
        if (e != null) {
            while (e.hasMoreElements()) {
                URL u = e.nextElement();
                String entryName = u.getPath().substring(basePath.length());
                if (discoveryFilter.packageDiscoveryRequired(toJavaStylePackageName(entryName))) {
                    if (isClassAcceptable(u)) {
                        classes.add(toJavaStyleClassName(entryName));
                    }
                }
            }
        }
    }

    private void scanZip(Collection<String> classes, Bundle bundle, String zipName) {
        if (!discoveryFilter.jarFileDiscoveryRequired(zipName)) {
            return;
        }
        URL zipEntry = bundle.getEntry(zipName);
        if (zipEntry == null) {
            return;
        }
        ZipInputStream in = null;
        try {
            in = new ZipInputStream(zipEntry.openStream());
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.endsWith(EXT) && discoveryFilter.packageDiscoveryRequired(toJavaStylePackageName(name))) {
                    if (isClassAcceptable(name, in)) {
                        classes.add(toJavaStyleClassName(name));
                    }
                }
            }
        } catch (IOException ignore) {
            logger.warn("Fail to check zip file " + zipName, ignore);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }

    protected String addSlash(String name) {
        if (!name.endsWith("/")) {
            name = name + "/";
        }
        return name;
    }

    protected Bundle isWired(Bundle bundle, ExportedPackage[] exports) {
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

    protected Bundle isWired(Bundle bundle, RequiredBundle[] requiredBundles) {
        if (requiredBundles != null) {
            for (RequiredBundle requiredBundle : requiredBundles) {
                Bundle[] requiringBundles = requiredBundle.getRequiringBundles();
                if (requiringBundles != null) {
                    for (Bundle requiringBundle : requiringBundles) {
                        if (requiringBundle == bundle) {
                            return requiredBundle.getBundle();
                        }
                    }
                }
            }
        }
        return null;
    }

    public static class DummyDiscoveryFilter implements ClassDiscoveryFilter {


        public boolean directoryDiscoveryRequired(String url) {
            return true;
        }


        public boolean rangeDiscoveryRequired(DiscoveryRange discoveryRange) {
            return true;
        }


        public boolean jarFileDiscoveryRequired(String url) {
            return true;
        }


        public boolean packageDiscoveryRequired(String packageName) {
            return true;
        }
    }

    public static class NonImportedPackageDiscoveryFilter implements ClassDiscoveryFilter {


        public boolean directoryDiscoveryRequired(String url) {
            return true;
        }


        public boolean jarFileDiscoveryRequired(String url) {
            return true;
        }


        public boolean packageDiscoveryRequired(String packageName) {
            return true;
        }


        public boolean rangeDiscoveryRequired(DiscoveryRange discoveryRange) {
            return !discoveryRange.equals(DiscoveryRange.IMPORT_PACKAGES);
        }
    }

    private static class ImportExclusivePackageDiscoveryFilter implements ClassDiscoveryFilter {

        private String expectedPckageName;

        public ImportExclusivePackageDiscoveryFilter(String expectedPckageName) {
            this.expectedPckageName = expectedPckageName;
        }


        public boolean directoryDiscoveryRequired(String url) {
            return true;
        }


        public boolean jarFileDiscoveryRequired(String url) {
            return true;
        }


        public boolean packageDiscoveryRequired(String packageName) {
            return expectedPckageName.equals(packageName);
        }


        public boolean rangeDiscoveryRequired(DiscoveryRange discoveryRange) {
            return !discoveryRange.equals(DiscoveryRange.IMPORT_PACKAGES);
        }
    }

    private static class ImportExclusivePackageDiscoveryFilterAdapter implements ClassDiscoveryFilter {

        private Set<String> acceptedPackageNames;

        private ClassDiscoveryFilter classDiscoveryFilter;

        public ImportExclusivePackageDiscoveryFilterAdapter(ClassDiscoveryFilter classDiscoveryFilter, Set<String> acceptedPackageNames) {
            this.classDiscoveryFilter = classDiscoveryFilter;
            this.acceptedPackageNames = acceptedPackageNames;
        }


        public boolean directoryDiscoveryRequired(String url) {
            return true;
        }


        public boolean jarFileDiscoveryRequired(String url) {
            return true;
        }


        public boolean packageDiscoveryRequired(String packageName) {
            return acceptedPackageNames.contains(packageName) && classDiscoveryFilter.packageDiscoveryRequired(packageName);
        }


        public boolean rangeDiscoveryRequired(DiscoveryRange discoveryRange) {
            return !discoveryRange.equals(DiscoveryRange.IMPORT_PACKAGES);
        }
    }
}
