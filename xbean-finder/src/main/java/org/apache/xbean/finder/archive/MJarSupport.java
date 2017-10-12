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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

// helper to share the multijar release logic in a single place and avoid to impl it in all archives
public class MJarSupport {
    private boolean mjar;
    private String prefix;
    private final Map<String, String> classes = new HashMap<String, String>();

    public boolean isMjar() {
        return mjar;
    }

    public Map<String, String> getClasses() {
        return classes;
    }

    public void load(final InputStream is) throws IOException {
        load(new Manifest(is));
    }

    public void load(final Manifest manifest) {
        final Attributes mainAttributes = manifest.getMainAttributes();
        if (mainAttributes != null) {
            mjar = Boolean.parseBoolean(mainAttributes.getValue("Multi-Release"));
            if (mjar) {
                String javaVersion = System.getProperty("java.version", "1"); // until (1.)8, == not using mjar
                final int sep = javaVersion.indexOf('.');
                if (sep > 0) {
                    javaVersion = javaVersion.substring(0, sep);
                }

                prefix = "META-INF.versions." + javaVersion + '.';
            }
        }
    }

    public void visit(final String name) {
        String normalized = name.replace('/', '.');
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith(prefix)) {
            classes.put(name.substring(prefix.length()), name + (!name.endsWith(".class") ? ".class" : ""));
        }
    }
}
