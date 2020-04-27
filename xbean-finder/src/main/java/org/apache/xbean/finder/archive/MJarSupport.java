/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.xbean.finder.archive;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

// helper to share the multijar release logic in a single place and avoid to impl it in all archives
public class MJarSupport {
    private static final boolean SUPPORT_MJAR = asList("true", "force")
            .contains(System.getProperty("jdk.util.jar.enableMultiRelease", "true"));
    private static final int MJAR_VERSION = findMJarVersion();

    private static int findMJarVersion() {
        if (!SUPPORT_MJAR) {
            return -1;
        }
        final int version = major(System.getProperty("java.version"));
        final int jarVersion = major(System.getProperty("jdk.util.jar.version"));
        if (jarVersion > 0) {
            return Math.min(version, jarVersion);
        }
        return Math.min(7/*unexpected but just in case*/, version);
    }

    private static int major(final String version) {
        if (version == null) {
            return -1;
        }
        final String[] parts = version.split("\\.");
        try {
            final int i = Integer.parseInt(parts[0]);
            if (i == 1 && parts.length > 1) {
                return Integer.parseInt(parts[1]);
            }
            return i;
        } catch (final NumberFormatException nfe) {
            // unexpected
            return -1;
        }
    }

    private boolean mjar;
    private final Map<String, Clazz> classes = new HashMap<String, Clazz>();

    public boolean isMjar() {
        return mjar;
    }

    public Map<String, Clazz> getClasses() {
        return classes;
    }

    public void load(final InputStream is) throws IOException {
        if (!SUPPORT_MJAR) {
            return;
        }
        load(new Manifest(is));
    }

    public void load(final Manifest manifest) {
        if (!SUPPORT_MJAR) {
            return;
        }
        final Attributes mainAttributes = manifest.getMainAttributes();
        if (mainAttributes != null) {
            mjar = Boolean.parseBoolean(mainAttributes.getValue("Multi-Release"));
        }
    }

    // for exploded dirs since jars are handled by the JVM
    public void visit(final String name) {
        String normalized = name.replace('/', '.');
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith("META-INF.versions.")) {
            final String version = normalized.substring("META-INF.versions.".length());
            final int nextSep = version.indexOf('.');
            if (nextSep < 0) {
                return;
            }
            final String vStr = version.substring(0, nextSep);
            final int major;
            try {
                if ((major = Integer.parseInt(vStr)) > MJAR_VERSION) {
                    return;
                }
            } catch (final NumberFormatException nfe) {
                return;
            }
            if (nextSep < version.length()) {
                final String cname = version.substring(nextSep + 1);
                final Clazz existing = classes.get(cname);
                if (existing == null || existing.version < major) {
                    classes.put(cname, new Clazz(name + (!version.endsWith(".class") ? ".class" : ""), major));
                }
            }
        }
    }

    public static class Clazz {
        private final String path;
        private final int version;

        private Clazz(final String path, final int version) {
            this.path = path;
            this.version = version;
        }

        public String getPath() {
            return path;
        }
    }
}
