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
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class TomcatClassPath extends SunURLClassPath {

    /**
     * The Tomcat Common ClassLoader
     */
    private final ClassLoader classLoader;

    /**
     * The addRepository(String jar) method of the Tomcat Common ClassLoader
     */
    private Method addRepositoryMethod;
    private Method addURLMethod;


    public TomcatClassPath() {
        this(getCommonLoader(getContextClassLoader()).getParent());
    }

    public TomcatClassPath(ClassLoader classLoader){
        this.classLoader = classLoader;
        try {
            addRepositoryMethod = getAddRepositoryMethod();
        } catch (Exception tomcat4Exception) {
            // Must be tomcat 5
            try {
                addURLMethod = getAddURLMethod();
            } catch (Exception tomcat5Exception) {
                throw new RuntimeException("Failed accessing classloader for Tomcat 4 or 5", tomcat5Exception);
            }
        }
    }

    private static ClassLoader getCommonLoader(ClassLoader loader) {
        if (loader.getClass().getName().equals("org.apache.catalina.loader.StandardClassLoader")) {
            return loader;
        } else {
            return getCommonLoader(loader.getParent());
        }
    }
    
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void addJarsToPath(File dir) throws Exception {
        String[] jarNames = dir.list(new java.io.FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (name.endsWith(".jar") || name.endsWith(".zip"));
            }
        });

        if (jarNames == null) {
            return;
        }

        for (int j = 0; j < jarNames.length; j++) {
            this.addJarToPath(new File(dir, jarNames[j]).toURI().toURL());
        }
        rebuild();
    }

    public void addJarToPath(URL jar) throws Exception {
        this._addJarToPath(jar);
        rebuild();
    }

    public void _addJarToPath(URL jar) throws Exception {
        String path = jar.toExternalForm();
        this.addRepository(path);
    }

    public void addRepository(String path) throws Exception {
        if (addRepositoryMethod != null){
            addRepositoryMethod.invoke(getClassLoader(), new Object[]{path});
        } else {
            addURLMethod.invoke(getClassLoader(), new Object[]{new File(path).toURI().toURL()});
        }
    }

    protected void rebuild() {
        try {
            sun.misc.URLClassPath cp = getURLClassPath((URLClassLoader) getClassLoader());
            URL[] urls = cp.getURLs();
            //for (int i=0; i < urls.length; i++){
            //    System.out.println(urls[i].toExternalForm());
            //}
            if (urls.length < 1)
                return;

            StringBuffer path = new StringBuffer(urls.length * 32);

            File s = new File(urls[0].getFile());
            path.append(s.getPath());
            //System.out.println(s.getPath());

            for (int i = 1; i < urls.length; i++) {
                path.append(File.pathSeparator);

                s = new File(urls[i].getFile());
                //System.out.println(s.getPath());
                path.append(s.getPath());
            }
            System.setProperty("java.class.path", path.toString());
        } catch (Exception e) {
        }

    }

    /**
     * This method gets the Tomcat StandardClassLoader.addRepository method
     * via reflection. This allows us to call the addRepository method for
     * Tomcat integration, but doesn't require us to include or ship any
     * Tomcat libraries.
     *
     * @return URLClassLoader.addURL method instance
     */
    private Method getAddURLMethod() throws Exception {
        Method method = null;
        Class clazz = URLClassLoader.class;
        method = clazz.getDeclaredMethod("addURL", new Class[]{URL.class});
        method.setAccessible(true);
        return method;
    }

    private Method getAddRepositoryMethod() throws Exception {
        Method method = null;
        Class clazz = getClassLoader().getClass();
        method = clazz.getDeclaredMethod("addRepository", new Class[]{String.class});
        method.setAccessible(true);
        return method;
    }

}
