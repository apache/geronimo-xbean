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
package org.apache.xbean.finder;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Arrays;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;

/**
 * @version $Rev$ $Date$
 */
public class ClassFinder {
    private final ClassLoader classLoader;
    private final List<Class> classes;
    private final List<String> classesNotLoaded = new ArrayList();

    public ClassFinder(ClassLoader classLoader) throws Exception {
        this(classLoader, excludeParentUrls(classLoader));
    }

    public ClassFinder(ClassLoader classLoader, URL url) {
        this(classLoader, Arrays.asList(new URL[]{url}));
    }

    public ClassFinder(ClassLoader classLoader, Collection<URL> urls) {
        this.classLoader = classLoader;

        List<String> classNames = new ArrayList();
        for (URL location : urls) {
            try {
                if (location.getProtocol().equals("jar")) {
                    classNames.addAll(jar(location));
                } else if (location.getProtocol().equals("file")) {
                    classNames.addAll(file(location));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        classes = new ArrayList();
        for (String className : classNames) {
            try {
                Class clazz = classLoader.loadClass(className);
                classes.add(clazz);
            } catch (ClassNotFoundException e) {
                classesNotLoaded.add(className);
            } catch (NoClassDefFoundError e) {
                classesNotLoaded.add(className);
            }
        }
    }

    public List<String> getClassesNotLoaded() {
        return classesNotLoaded;
    }

    public List<Class> findAnnotatedClasses(Class<? extends Annotation> annotation) {
        List<Class> allClasses = getClasses();
        List<Class> classes = new ArrayList<Class>();
        for (Class clazz : allClasses) {
            if (clazz.isAnnotationPresent(annotation)) {
                classes.add(clazz);
            }
        }
        return classes;
    }

    public Map<Class<? extends Annotation>,List<Class>> mapAnnotatedClasses() {
        List<Class> allClasses = getClasses();
        Map<Class<? extends Annotation>,List<Class>> mappedClasses = new HashMap();
        for (Class clazz : allClasses) {
            if (Annotation.class.isAssignableFrom(clazz)){
                continue;
            }
            for (Annotation annotation : clazz.getAnnotations()) {
                Class<? extends Annotation> annotationType = annotation.annotationType();
                List<Class> classes = mappedClasses.get(annotationType);
                if (classes == null){
                    classes = new ArrayList();
                    mappedClasses.put(annotationType, classes);
                }
                classes.add(clazz);
            }
        }
        return mappedClasses;
    }

    public List<Class> getClasses() {
        return this.classes;
    }

    private static Collection<URL> excludeParentUrls(ClassLoader classLoader) throws IOException {
        ClassLoader parent = classLoader.getParent();
        Map<String,URL> parentUrls = toMap(parent.getResources("META-INF"));
        Map<String,URL> urls = toMap(classLoader.getResources("META-INF"));

        for (String url : parentUrls.keySet()) {
            urls.remove(url);
        }

        return urls.values();
    }

    private static Map<String,URL> toMap(Enumeration<URL> enumeration){
        Map<String,URL> urls = new HashMap();
        while (enumeration.hasMoreElements()) {
            URL url = enumeration.nextElement();
            urls.put(url.toExternalForm(), url);
        }
        return urls;
    }

    private List<String> file(URL location) {
        List<String> classNames = new ArrayList();
        File dir = new File(location.getPath());
        if (dir.getName().equals("META-INF")){
            dir = dir.getParentFile(); // Scrape "META-INF" off
        }
        if (dir.isDirectory()) {
            scanDir(dir, classNames, "");
        }
        return classNames;
    }

    private void scanDir(File dir, List<String> classNames, String packageName) {
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()){
                scanDir(file, classNames, packageName + file.getName() + ".");
            } else if (file.getName().endsWith(".class")) {
                String name = file.getName();
                name = name.replaceFirst(".class$","");
                classNames.add(packageName + name);
            }
        }
    }

    private List<String> jar(URL location) throws IOException {
        List<String> classNames = new ArrayList();

        String jarPath = location.getFile().replaceFirst("./META-INF","");
        URL url = new URL(jarPath);
        InputStream in = url.openStream();
        JarInputStream jarStream = new JarInputStream(in);

        JarEntry entry;
        while ((entry = jarStream.getNextJarEntry()) != null) {
            if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                continue;
            }
            String className = entry.getName();
            className = className.replaceFirst(".class$","");
            className = className.replace('/','.');
            classNames.add(className);
        }

        return classNames;
    }
}
