/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.xbean.classpath;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

public abstract class SunURLClassPath implements ClassPath {

    public static ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    private java.lang.reflect.Field ucpField;

    protected void addJarToPath(final URL jar, final URLClassLoader loader) throws Exception {
        this.getURLClassPath(loader).addURL(jar);
    }

    protected void addJarsToPath(final File dir, final URLClassLoader loader) throws Exception {
        if (dir == null || !dir.exists()) return;
        //System.out.println("DIR "+dir);
        // Get the list of jars and zips
        String[] jarNames = dir.list(new java.io.FilenameFilter() {
            public boolean accept(File dir, String name) {
                //System.out.println("FILE "+name);
                return (name.endsWith(".jar") || name.endsWith(".zip"));
            }
        });

        // Create URLs from them
        final URL[] jars = new URL[jarNames.length];
        for (int j = 0; j < jarNames.length; j++) {
            jars[j] = new File(dir, jarNames[j]).toURI().toURL();
        }

        sun.misc.URLClassPath path = getURLClassPath(loader);
        for (int i = 0; i < jars.length; i++) {
            //System.out.println("URL "+jars[i]);
            path.addURL(jars[i]);
        }
    }

    protected sun.misc.URLClassPath getURLClassPath(URLClassLoader loader) throws Exception {
        return (sun.misc.URLClassPath) getUcpField().get(loader);
    }

    private java.lang.reflect.Field getUcpField() throws Exception {
        if (ucpField == null) {
            // Add them to the URLClassLoader's classpath
            ucpField = URLClassLoader.class.getDeclaredField("ucp");
            ucpField.setAccessible(true);
        }
        return ucpField;
    }

}
