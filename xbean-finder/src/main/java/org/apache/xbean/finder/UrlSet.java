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

import org.apache.xbean.finder.filter.Filter;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.io.IOException;
import java.io.File;

import static org.apache.xbean.finder.filter.Filters.invert;
import static org.apache.xbean.finder.filter.Filters.patterns;

/**
 * @version $Rev$ $Date$
 */
public class UrlSet implements Iterable<URL> {

    private final Map<String,URL> urls;

    public UrlSet(ClassLoader classLoader) throws IOException {
        this(ClassLoaders.findUrls(classLoader));
    }

    public UrlSet(URL... urls){
        this(Arrays.asList(urls));
    }
    /**
     * Ignores all URLs that are not "jar" or "file"
     * @param urls
     */
    public UrlSet(Collection<URL> urls){
        this.urls = new HashMap<String,URL>();
        for (URL location : urls) {
            try {
//                if (location.getProtocol().equals("file")) {
//                    try {
//                        // See if it's actually a jar
//                        URL jarUrl = new URL("jar", "", location.toExternalForm() + "!/");
//                        JarURLConnection juc = (JarURLConnection) jarUrl.openConnection();
//                        juc.getJarFile();
//                        location = jarUrl;
//                    } catch (IOException e) {
//                    }
//                    this.urls.put(location.toExternalForm(), location);
//                }
                this.urls.put(location.toExternalForm(), location);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private UrlSet(Map<String, URL> urls) {
        this.urls = urls;
    }

    public UrlSet include(UrlSet urlSet){
        Map<String, URL> urls = new HashMap<String, URL>(this.urls);
        urls.putAll(urlSet.urls);
        return new UrlSet(urls);
    }


    public UrlSet include(URL url){
        Map<String, URL> urls = new HashMap<String, URL>(this.urls);
        urls.put(url.toExternalForm(), url);
        return new UrlSet(urls);
    }

    public UrlSet exclude(UrlSet urlSet) {
        Map<String, URL> urls = new HashMap<String, URL>(this.urls);
        Map<String, URL> parentUrls = urlSet.urls;
        for (String url : parentUrls.keySet()) {
            urls.remove(url);
        }
        return new UrlSet(urls);
    }

    public UrlSet exclude(URL url) {
        Map<String, URL> urls = new HashMap<String, URL>(this.urls);
        urls.remove(url.toExternalForm());
        return new UrlSet(urls);
    }

    public UrlSet exclude(ClassLoader parent) throws IOException {
        return exclude(new UrlSet(parent));
    }

    public UrlSet exclude(File file) throws MalformedURLException {
        return exclude(relative(file));
    }

    public UrlSet exclude(String pattern) throws MalformedURLException {
        return filter(invert(patterns(pattern)));
    }

    /**
     * Calls excludePaths(System.getProperty("java.ext.dirs"))
     * @return
     * @throws MalformedURLException
     */
    public UrlSet excludeJavaExtDirs() throws MalformedURLException {
        String extDirs = System.getProperty("java.ext.dirs");
        return extDirs == null ? this : excludePaths(extDirs);
    }

    /**
     * Calls excludePaths(System.getProperty("java.endorsed.dirs"))
     *
     * @return
     * @throws MalformedURLException
     */
    public UrlSet excludeJavaEndorsedDirs() throws MalformedURLException {
        String endorsedDirs = System.getProperty("java.endorsed.dirs");
        return endorsedDirs == null ? this : excludePaths(endorsedDirs);
    }

    public UrlSet excludeJavaHome() throws MalformedURLException {
        String path = System.getProperty("java.home");

        File java = new File(path);

        if (isOsx() && path.endsWith("/Contents/Home")) {
            java = java.getParentFile().getParentFile();
        }

        return exclude(java);
    }

    public UrlSet excludeJvm() throws MalformedURLException  {
        UrlSet urls = excludeJavaHome().excludeJavaExtDirs().excludeJavaEndorsedDirs();
        if (isOsx()) {
            urls = urls.exclude(new File("/System/Library/Java/Support"));
        }
        return urls;
    }

    public UrlSet excludePaths(String pathString) throws MalformedURLException {
        String[] paths = pathString.split(File.pathSeparator);
        UrlSet urlSet = this;
        for (String path : paths) {
            File file = new File(path);
            urlSet = urlSet.exclude(file);
        }
        return urlSet;
    }

    public UrlSet filter(Filter filter) {
        Map<String, URL> urls = new HashMap<String, URL>();
        for (Map.Entry<String, URL> entry : this.urls.entrySet()) {
            String url = entry.getKey();
            if (filter.accept(url)){
                urls.put(url, entry.getValue());
            }
        }
        return new UrlSet(urls);
    }

    public UrlSet matching(String pattern) {
        return filter(patterns(pattern));
    }

    public UrlSet relative(File file) throws MalformedURLException {
        String urlPath = file.toURI().toURL().toExternalForm();
        Map<String, URL> urls = new HashMap<String, URL>();
        for (Map.Entry<String, URL> entry : this.urls.entrySet()) {
            String url = entry.getKey();
            if (url.startsWith(urlPath) || url.startsWith("jar:"+urlPath)){
                urls.put(url, entry.getValue());
            }
        }
        return new UrlSet(urls);
    }

    public List<URL> getUrls() {
        return new ArrayList<URL>(urls.values());
    }

    public int size() {
        return urls.size();
    }

    public Iterator<URL> iterator() {
        return getUrls().iterator();
    }

    @Override
    public String toString() {
        return super.toString() + "[" + urls.size() + "]";
    }

    public boolean isOsx() {
        return "Mac OS X".equals(System.getProperty("os.name"));
    }
}
