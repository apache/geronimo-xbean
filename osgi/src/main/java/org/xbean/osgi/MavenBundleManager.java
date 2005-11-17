/**
 *
 * Copyright 2005 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.xbean.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class MavenBundleManager {
    private final BundleContext bundleContext;
    private final File localRepository;

    public MavenBundleManager(BundleContext bundleContext, File localRepository) {
        this.bundleContext = bundleContext;
        this.localRepository = localRepository;
    }

    public Project loadProject(Artifact artifact) {
        if (artifact instanceof Project) {
            return (Project) artifact;
        } else {
            return new Project(artifact.getGroupId(),
                    artifact.getArtifactId(),
                    artifact.getVersion(),
                    artifact.getType(),
                    Collections.EMPTY_SET);
        }
    }

    public Project loadProject(String groupId, String artifactId, String version) {
        return new Project(groupId, artifactId, version, "jar", Collections.EMPTY_SET);
    }

    public Bundle installBundle(String groupId, String artifactId, String version) throws Exception {
        return installBundle(loadProject(groupId, artifactId, version));
    }

    public Bundle installBundle(Artifact artifact) throws Exception {
        String symbolicName = artifact.getGroupId() + "." + artifact.getArtifactId();
        String bundleVersion = coerceToOsgiVersion(artifact.getVersion());

        // check if we already loaded this bundle
        Bundle[] bundles = bundleContext.getBundles();
        for (int i = 0; i < bundles.length; i++) {
            Bundle bundle = bundles[i];
            if (symbolicName.equals(bundle.getSymbolicName()) &&
                    bundleVersion.equals(bundle.getHeaders().get(Constants.BUNDLE_VERSION))) {
                return bundle;
            }
        }

        // load the project object model for this artiface
        Project project = loadProject(artifact);

        // build an OSGi manifest for the project
        Manifest manifest = createOsgiManifest(project);

        // create a jar in memory for the manifest
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JarOutputStream jarOut = new JarOutputStream(out, manifest);
        jarOut.close();
        out.close();
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

        // install the in memory jar
        Bundle bundle = bundleContext.installBundle(symbolicName, in);

        // install bundles for all of the dependencies
        for (Iterator iterator = project.getDependencies().iterator(); iterator.hasNext();) {
            Artifact dependency = (Artifact) iterator.next();
            installBundle(dependency);
        }

        return bundle;
    }

    public Manifest createOsgiManifest(Project project) throws IOException {
        String groupId = project.getGroupId();
        String artifactId = project.getArtifactId();
        String version = project.getVersion();
        String jar = groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + project.getJar();

        StringBuffer requireBundle = new StringBuffer();
        for (Iterator iterator = project.getDependencies().iterator(); iterator.hasNext();) {
            Artifact dependency = (Artifact) iterator.next();
            if (requireBundle.length() > 0) requireBundle.append(',');

            requireBundle.append(dependency.getGroupId()).append('.').append(dependency.getArtifactId());
            requireBundle.append(";visibility:=reexport;bundle-version:=").append(coerceToOsgiVersion(dependency.getVersion()));

        }

        String jarPath = new File(localRepository, jar).getAbsolutePath();
        StringBuffer exports = createExportList(jarPath);

        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
        attributes.putValue(Constants.BUNDLE_VENDOR, groupId);
        attributes.putValue(Constants.BUNDLE_NAME, artifactId);
        attributes.putValue(Constants.BUNDLE_VERSION, coerceToOsgiVersion(version));
        attributes.putValue(Constants.BUNDLE_SYMBOLICNAME, groupId + "." + artifactId);
        attributes.putValue("Eclipse-AutoStart", "true");


        attributes.putValue(Constants.BUNDLE_CLASSPATH, ".,external:" + jarPath);

        attributes.putValue(Constants.EXPORT_PACKAGE, exports.toString());
        if (requireBundle != null && requireBundle.length() > 0) {
            attributes.putValue(Constants.REQUIRE_BUNDLE, requireBundle.toString());
        }

        return manifest;
    }

    private static String coerceToOsgiVersion(String version) {
        int partsFound = 0;
        String[] versionParts = new String[] { "0", "0", "0"};
        StringBuffer qualifier = new StringBuffer();
        for (StringTokenizer stringTokenizer = new StringTokenizer(version, ".-"); stringTokenizer.hasMoreTokens();) {
            String part = stringTokenizer.nextToken();
            if (partsFound < 4) {
                try {
                    Integer.parseInt(part);
                    versionParts[partsFound++] = part;
                } catch (NumberFormatException e) {
                    partsFound = 4;
                    qualifier.append(coerceToOsgiQualifier(part));
                }
            } else {
                if (qualifier.length() > 0) qualifier.append("_");
                qualifier.append(coerceToOsgiQualifier(part));
            }
        }

        StringBuffer osgiVersion = new StringBuffer();
        osgiVersion.append(versionParts[0]).append(".").append(versionParts[1]).append(".").append(versionParts[2]);
        if (qualifier.length() > 0) {
            osgiVersion.append(".").append(qualifier);
        }
        return osgiVersion.toString();
    }

    private static String coerceToOsgiQualifier(String qualifier) {
        char[] chars = qualifier.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '-') {
                chars[i] = '_';
            }
        }
        return new String(chars);
    }


    private static StringBuffer createExportList(String jarPath) throws IOException {
        Set packages = new HashSet();
        FileInputStream in = null;
        try {
            in = new FileInputStream(jarPath);
            JarInputStream jarIn = new JarInputStream(in);
            for (JarEntry jarEntry = jarIn.getNextJarEntry(); jarEntry != null; jarEntry = jarIn.getNextJarEntry()) {
                String packageName = jarEntry.getName();
                if (!jarEntry.isDirectory()) {
                    int index = packageName.lastIndexOf("/");
                    // we can't export the default package
                    if (index > 0) {
                        packageName = packageName.substring(0, index);
                        if (!packageName.equals("META-INF")) {
                            packageName = packageName.replace('/', '.');
                            packages.add(packageName);
                        }
                    }
                }
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }

        StringBuffer exports = new StringBuffer();
        for (Iterator iterator = packages.iterator(); iterator.hasNext();) {
            String packageName = (String) iterator.next();
            if (exports.length() > 0) exports.append(";");
            exports.append(packageName);
        }
        return exports;
    }
}
