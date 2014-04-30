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
package org.apache.xbean.classloader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * @version $Rev$ $Date$
 */
public class UrlResourceFinder implements ResourceFinder {
    private final Object lock = new Object();

    private final LinkedHashSet urls = new LinkedHashSet();
    private final LinkedHashMap classPath = new LinkedHashMap();
    private final LinkedHashSet watchedFiles = new LinkedHashSet();

    private boolean destroyed = false;

    public UrlResourceFinder() {
    }

    public UrlResourceFinder(URL[] urls) {
        addUrls(urls);
    }

    public void destroy() {
        synchronized (lock) {
            if (destroyed) {
                return;
            }
            destroyed = true;
            urls.clear();
            for (Iterator iterator = classPath.values().iterator(); iterator.hasNext();) {
                ResourceLocation resourceLocation = (ResourceLocation) iterator.next();
                resourceLocation.close();
            }
            classPath.clear();
        }
    }

    public ResourceHandle getResource(String resourceName) {
        synchronized (lock) {
            if (destroyed) {
                return null;
            }
            for (Iterator iterator = getClassPath().entrySet().iterator(); iterator.hasNext();) {
                Map.Entry entry = (Map.Entry) iterator.next();
                ResourceLocation resourceLocation = (ResourceLocation) entry.getValue();
                ResourceHandle resourceHandle = resourceLocation.getResourceHandle(resourceName);
                if (resourceHandle != null && !resourceHandle.isDirectory()) {
                    return resourceHandle;
                }
            }
        }
        return null;
    }

    public URL findResource(String resourceName) {
        synchronized (lock) {
            if (destroyed) {
                return null;
            }
            for (Iterator iterator = getClassPath().entrySet().iterator(); iterator.hasNext();) {
                Map.Entry entry = (Map.Entry) iterator.next();
                ResourceLocation resourceLocation = (ResourceLocation) entry.getValue();
                ResourceHandle resourceHandle = resourceLocation.getResourceHandle(resourceName);
                if (resourceHandle != null) {
                    return resourceHandle.getUrl();
                }
            }
        }
        return null;
    }

    public Enumeration findResources(String resourceName) {
        synchronized (lock) {
            return new ResourceEnumeration(new ArrayList(getClassPath().values()), resourceName);
        }
    }

    public void addUrl(URL url) {
        addUrls(Collections.singletonList(url));
    }

    public URL[] getUrls() {
        synchronized (lock) {
            return (URL[]) urls.toArray(new URL[urls.size()]);
        }
    }

    /**
     * Adds an array of urls to the end of this class loader.
     * @param urls the URLs to add
     */
    protected void addUrls(URL[] urls) {
        addUrls(Arrays.asList(urls));
    }

    /**
     * Adds a list of urls to the end of this class loader.
     * @param urls the URLs to add
     */
    protected void addUrls(List urls) {
        synchronized (lock) {
            if (destroyed) {
                throw new IllegalStateException("UrlResourceFinder has been destroyed");
            }

            boolean shouldRebuild = this.urls.addAll(urls);
            if (shouldRebuild) {
                rebuildClassPath();
            }
        }
    }

    private LinkedHashMap getClassPath() {
        assert Thread.holdsLock(lock): "This method can only be called while holding the lock";

        for (Iterator iterator = watchedFiles.iterator(); iterator.hasNext();) {
            File file = (File) iterator.next();
            if (file.canRead()) {
                rebuildClassPath();
                break;
            }
        }

        return classPath;
    }

    /**
     * Rebuilds the entire class path.  This class is called when new URLs are added or one of the watched files
     * becomes readable.  This method will not open jar files again, but will add any new entries not alredy open
     * to the class path.  If any file based url is does not exist, we will watch for that file to appear.
     */
    private void rebuildClassPath() {
        assert Thread.holdsLock(lock): "This method can only be called while holding the lock";

        // copy all of the existing locations into a temp map and clear the class path
        Map existingJarFiles = new LinkedHashMap(classPath);
        classPath.clear();

        LinkedList locationStack = new LinkedList(urls);
        try {
            while (!locationStack.isEmpty()) {
                URL url = (URL) locationStack.removeFirst();

                // Skip any duplicate urls in the claspath
                if (classPath.containsKey(url)) {
                    continue;
                }

                // Check is this URL has already been opened
                ResourceLocation resourceLocation = (ResourceLocation) existingJarFiles.remove(url);

                // If not opened, cache the url and wrap it with a resource location
                if (resourceLocation == null) {
                    try {
                        File file = cacheUrl(url);
                        resourceLocation = createResourceLocation(url, file);
                    } catch (FileNotFoundException e) {
                        // if this is a file URL, the file doesn't exist yet... watch to see if it appears later
                        if ("file".equals(url.getProtocol())) {
                            File file = new File(url.getPath());
                            watchedFiles.add(file);
                            continue;

                        }
                    } catch (IOException ignored) {
                        // can't seem to open the file... this is most likely a bad jar file
                        // so don't keep a watch out for it because that would require lots of checking
                        // Dain: We may want to review this decision later
                        continue;
                    }
                }

                // add the jar to our class path
                classPath.put(resourceLocation.getCodeSource(), resourceLocation);

                // push the manifest classpath on the stack (make sure to maintain the order)
                List manifestClassPath = getManifestClassPath(resourceLocation);
                locationStack.addAll(0, manifestClassPath);
            }
        } catch (Error e) {
            destroy();
            throw e;
        }

        for (Iterator iterator = existingJarFiles.values().iterator(); iterator.hasNext();) {
            ResourceLocation resourceLocation = (ResourceLocation) iterator.next();
            resourceLocation.close();
        }
    }

    protected File cacheUrl(URL url) throws IOException {
        if (!"file".equals(url.getProtocol())) {
            // download the jar
            throw new Error("Only local file jars are supported " + url);
        }

        File file;
        try {
            file = new File(url.toURI());
        } catch (URISyntaxException e) {
            file = new File(url.getPath());
        }
        if (!file.exists()) {
            throw new FileNotFoundException(file.getAbsolutePath());
        }
        if (!file.canRead()) {
            throw new IOException("File is not readable: " + file.getAbsolutePath());
        }
        return file;
    }

    protected ResourceLocation createResourceLocation(URL codeSource, File cacheFile) throws IOException {
        if (!cacheFile.exists()) {
            throw new FileNotFoundException(cacheFile.getAbsolutePath());
        }
        if (!cacheFile.canRead()) {
            throw new IOException("File is not readable: " + cacheFile.getAbsolutePath());
        }

        ResourceLocation resourceLocation = null;
        if (cacheFile.isDirectory()) {
            // DirectoryResourceLocation will only return "file" URLs within this directory
            // do not user the DirectoryResourceLocation for non file based urls
            resourceLocation = new DirectoryResourceLocation(cacheFile);
        } else {
            resourceLocation = new JarResourceLocation(codeSource, new JarFile(cacheFile));
        }
        return resourceLocation;
    }

    private List getManifestClassPath(ResourceLocation resourceLocation) {
        try {
            // get the manifest, if possible
            Manifest manifest = resourceLocation.getManifest();
            if (manifest == null) {
                // some locations don't have a manifest
                return Collections.EMPTY_LIST;
            }

            // get the class-path attribute, if possible
            String manifestClassPath = manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
            if (manifestClassPath == null) {
                return Collections.EMPTY_LIST;
            }

            // build the urls...
            // the class-path attribute is space delimited
            URL codeSource = resourceLocation.getCodeSource();
            LinkedList classPathUrls = new LinkedList();
            for (StringTokenizer tokenizer = new StringTokenizer(manifestClassPath, " "); tokenizer.hasMoreTokens();) {
                String entry = tokenizer.nextToken();
                try {
                    // the class path entry is relative to the resource location code source
                    URL entryUrl = new URL(codeSource, entry);
                    classPathUrls.addLast(entryUrl);
                } catch (MalformedURLException ignored) {
                    // most likely a poorly named entry
                }
            }
            return classPathUrls;
        } catch (IOException ignored) {
            // error opening the manifest
            return Collections.EMPTY_LIST;
        }
    }
}
