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
package org.gbean.server.classloader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * The JarFileClassLoader that loads classes and resources from a list of JarFiles.  This method is simmilar to URLClassLoader
 * except it properly closes JarFiles when the classloader is destroyed so that the file read lock will be released, and
 * the jar file can be modified and deleted.
 * <p>
 * Note: This implementation currently does not work reliably on windows, since the jar URL handler included with the Sun JavaVM
 * holds a read lock on the JarFile, and this lock is not released when the jar url is dereferenced.  To fix this a
 * replacement for the jar url handler must be written.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class JarFileClassLoader extends MultiParentClassLoader {
    private static final URL[] EMPTY_URLS = new URL[0];
    private final Object lock = new Object();
    private final LinkedHashMap classPath = new LinkedHashMap();
    private boolean destroyed = false;

    /**
     * Creates a JarFileClassLoader that is a child of the system class loader.
     * @param name the name of this class loader
     * @param urls a list of URLs from which classes and resources should be loaded
     */
    public JarFileClassLoader(String name, URL[] urls) {
        super(name, EMPTY_URLS);
        addURLs(urls);
    }

    /**
     * Creates a JarFileClassLoader that is a child of the specified class loader.
     * @param name the name of this class loader
     * @param urls a list of URLs from which classes and resources should be loaded
     * @param parent the parent of this class loader
     */
    public JarFileClassLoader(String name, URL[] urls, ClassLoader parent) {
        this(name, urls, new ClassLoader[] {parent});
    }

    /**
     * Creates a named class loader as a child of the specified parent and using the specified URLStreamHandlerFactory
     * for accessing the urls..
     * @param name the name of this class loader
     * @param urls the urls from which this class loader will classes and resources
     * @param parent the parent of this class loader
     * @param factory the URLStreamHandlerFactory used to access the urls
     */
    public JarFileClassLoader(String name, URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        this(name, urls, new ClassLoader[] {parent}, factory);
    }

    /**
     * Creates a named class loader as a child of the specified parents.
     * @param name the name of this class loader
     * @param urls the urls from which this class loader will classes and resources
     * @param parents the parents of this class loader
     */
    public JarFileClassLoader(String name, URL[] urls, ClassLoader[] parents) {
        super(name, EMPTY_URLS, parents);
        addURLs(urls);
    }

    /**
     * Creates a named class loader as a child of the specified parents and using the specified URLStreamHandlerFactory
     * for accessing the urls..
     * @param name the name of this class loader
     * @param urls the urls from which this class loader will classes and resources
     * @param parents the parents of this class loader
     * @param factory the URLStreamHandlerFactory used to access the urls
     */
    public JarFileClassLoader(String name, URL[] urls, ClassLoader[] parents, URLStreamHandlerFactory factory) {
        super(name, EMPTY_URLS, parents, factory);
        addURLs(urls);
    }

    public URL[] getURLs() {
        return (URL[]) classPath.keySet().toArray(new URL[classPath.keySet().size()]);
    }

    protected void addURL(URL url) {
        addURLs(Collections.singletonList(url));
    }

    /**
     * Adds an array of urls to the end of this class loader.
     * @param urls the URLs to add
     */
    protected void addURLs(URL[] urls) {
        addURLs(Arrays.asList(urls));
    }

    /**
     * Adds a list of urls to the end of this class loader.
     * @param urls the URLs to add
     */
    protected void addURLs(List urls) {
        LinkedList locationStack = new LinkedList(urls);
        try {
            while (!locationStack.isEmpty()) {
                URL url = (URL) locationStack.removeFirst();

                if (!"file".equals(url.getProtocol())) {
                    // download the jar
                    throw new Error("Only local file jars are supported " + url);
                }

                String path = url.getPath();
                if (classPath.containsKey(path)) {
                    continue;
                }

                File file = new File(path);
                if (!file.canRead()) {
                    // can't read file...
                    continue;
                }

                // open the jar file
                JarFile jarFile = null;
                try {
                    jarFile = new JarFile(file);
                } catch (IOException e) {
                    // can't seem to open the file
                    continue;
                }
                classPath.put(url, jarFile);

                // push the manifest classpath on the stack (make sure to maintain the order)
                Manifest manifest = null;
                try {
                    manifest = jarFile.getManifest();
                } catch (IOException ignored) {
                }

                if (manifest != null) {
                    Attributes mainAttributes = manifest.getMainAttributes();
                    String manifestClassPath = mainAttributes.getValue(Attributes.Name.CLASS_PATH);
                    if (manifestClassPath != null) {
                        LinkedList classPathUrls = new LinkedList();
                        for (StringTokenizer tokenizer = new StringTokenizer(manifestClassPath, " "); tokenizer.hasMoreTokens();) {
                            String entry = tokenizer.nextToken();
                            File parentDir = file.getParentFile();
                            File entryFile = new File(parentDir, entry);
                            // manifest entries are optional... if they aren't there it is ok
                            if (entryFile.canRead()) {
                                classPathUrls.addLast(entryFile.getAbsolutePath());
                            }
                        }
                        locationStack.addAll(0, classPathUrls);
                    }
                }
            }
        } catch (Error e) {
            destroy();
            throw e;
        }
    }

    public void destroy() {
        synchronized (lock) {
            if (destroyed) {
                return;
            }
            destroyed = true;
            for (Iterator iterator = classPath.values().iterator(); iterator.hasNext();) {
                JarFile jarFile = (JarFile) iterator.next();
                try {
                    jarFile.close();
                } catch (IOException ignored) {
                }
            }
            classPath.clear();
        }
        super.destroy();
    }

    public URL findResource(String resourceName) {
        URL jarUrl = null;
        synchronized (lock) {
            if (destroyed) {
                return null;
            }
            for (Iterator iterator = classPath.entrySet().iterator(); iterator.hasNext() && jarUrl == null;) {
                Map.Entry entry = (Map.Entry) iterator.next();
                JarFile jarFile = (JarFile) entry.getValue();
                JarEntry jarEntry = jarFile.getJarEntry(resourceName);
                if (jarEntry != null && !jarEntry.isDirectory()) {
                    jarUrl = (URL) entry.getKey();
                }
            }
        }


        try {
            String urlString = "jar:" + jarUrl + "!/" + resourceName;
            return new URL(jarUrl, urlString);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public Enumeration findResources(String resourceName) throws IOException {
        List resources = new ArrayList();
        List superResources = Collections.list(super.findResources(resourceName));
        resources.addAll(superResources);

        synchronized (lock) {
            if (destroyed) {
                return Collections.enumeration(Collections.EMPTY_LIST);
            }
            for (Iterator iterator = classPath.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry entry = (Map.Entry) iterator.next();
                JarFile jarFile = (JarFile) entry.getValue();
                JarEntry jarEntry = jarFile.getJarEntry(resourceName);
                if (jarEntry != null && !jarEntry.isDirectory()) {
                    try {
                        URL url = (URL) entry.getKey();
                        String urlString = "jar:" + url + "!/" + resourceName;
                        resources.add(new URL(url, urlString));
                    } catch (MalformedURLException e) {
                    }
                }
            }
        }

        return Collections.enumeration(resources);
    }

    protected Class findClass(String className) throws ClassNotFoundException {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            String packageName = null;
            int packageEnd = className.lastIndexOf('.');
            if (packageEnd >= 0) {
                packageName = className.substring(0, packageEnd);
                securityManager.checkPackageDefinition(packageName);
            }
        }

        Certificate[] certificates = null;
        URL jarUrl = null;
        Manifest manifest = null;
        byte[] bytes;
        synchronized (lock) {
            if (destroyed) {
                throw new ClassNotFoundException("Class loader has been destroyed: " + className);
            }

            try {
                String entryName = className.replace('.', '/') + ".class";
                InputStream inputStream = null;
                for (Iterator iterator = classPath.entrySet().iterator(); iterator.hasNext() && inputStream == null;) {
                    Map.Entry entry = (Map.Entry) iterator.next();
                    jarUrl = (URL) entry.getKey();
                    JarFile jarFile = (JarFile) entry.getValue();
                    JarEntry jarEntry = jarFile.getJarEntry(entryName);
                    if (jarEntry != null && !jarEntry.isDirectory()) {
                        inputStream = jarFile.getInputStream(jarEntry);
                        certificates = jarEntry.getCertificates();
                        manifest = jarFile.getManifest();
                    }
                }
                if (inputStream == null) {
                    throw new ClassNotFoundException(className);
                }

                try {
                    byte[] buffer = new byte[4096];
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    for (int count = inputStream.read(buffer); count >= 0; count = inputStream.read(buffer)) {
                        out.write(buffer, 0, count);
                    }
                    bytes = out.toByteArray();
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }
            } catch (IOException e) {
                throw new ClassNotFoundException(className, e);
            }
        }

        definePackage(className, jarUrl, manifest);
        CodeSource codeSource = new CodeSource(jarUrl, certificates);
        Class clazz = defineClass(className, bytes, 0, bytes.length, codeSource);
        return clazz;
    }

    private void definePackage(String className, URL jarUrl, Manifest manifest) {
        int packageEnd = className.lastIndexOf('.');
        if (packageEnd < 0) {
            return;
        }

        String packageName = className.substring(0, packageEnd);
        String packagePath = packageName.replace('.', '/') + "/";

        Attributes packageAttributes = null;
        Attributes mainAttributes = null;
        if (manifest != null) {
            packageAttributes = manifest.getAttributes(packagePath);
            mainAttributes = manifest.getMainAttributes();
        }
        Package pkg = getPackage(packageName);
        if (pkg != null) {
            if (pkg.isSealed()) {
                if (!pkg.isSealed(jarUrl)) {
                    throw new SecurityException("Package was already sealed with another URL: package=" + packageName + ", url=" + jarUrl);
                }
            } else {
                if (isSealed(packageAttributes, mainAttributes)) {
                    throw new SecurityException("Package was already been loaded and not sealed: package=" + packageName + ", url=" + jarUrl);
                }
            }
        } else {
            String specTitle = getAttribute(Attributes.Name.SPECIFICATION_TITLE, packageAttributes, mainAttributes);
            String specVendor = getAttribute(Attributes.Name.SPECIFICATION_VENDOR, packageAttributes, mainAttributes);
            String specVersion = getAttribute(Attributes.Name.SPECIFICATION_VERSION, packageAttributes, mainAttributes);
            String implTitle = getAttribute(Attributes.Name.IMPLEMENTATION_TITLE, packageAttributes, mainAttributes);
            String implVendor = getAttribute(Attributes.Name.IMPLEMENTATION_VENDOR, packageAttributes, mainAttributes);
            String implVersion = getAttribute(Attributes.Name.IMPLEMENTATION_VERSION, packageAttributes, mainAttributes);

            URL sealBase = null;
            if (isSealed(packageAttributes, mainAttributes)) {
                sealBase = jarUrl;
            }

            definePackage(packageName, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
        }
    }

    private String getAttribute(Attributes.Name name, Attributes packageAttributes, Attributes mainAttributes) {
        if (packageAttributes != null) {
            String value = packageAttributes.getValue(name);
            if (value != null) {
                return value;
            }
        }
        if (mainAttributes != null) {
            return mainAttributes.getValue(name);
        }
        return null;
    }

    private boolean isSealed(Attributes packageAttributes, Attributes mainAttributes) {
        String sealed = getAttribute(Attributes.Name.SEALED, packageAttributes, mainAttributes);
        if (sealed == null) {
            return false;
        }
        if (sealed == null) {
            return false;
        }
        return "true".equalsIgnoreCase(sealed);
    }
}