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
package org.apache.xbean.finder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Iterator;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author David Blevins
 * 
 * @version $Rev$ $Date$
 */
public class ResourceFinder {

    private final String path;
    private final ClassLoader classLoader;
    private final List<String> resourcesNotLoaded = new ArrayList<String>();

    public ResourceFinder(String path) {
        this(path, Thread.currentThread().getContextClassLoader());
    }

    public ResourceFinder(String path, ClassLoader classLoader) {
        this.path = (path == null)? "" : path;
        this.classLoader = classLoader;
    }

    /**
     * Returns a list of resources that could not be loaded in the last invoked findAvailable* or
     * mapAvailable* methods.
     * <p/>
     * The list will only contain entries of resources that match the requirements
     * of the last invoked findAvailable* or mapAvailable* methods, but were unable to be
     * loaded and included in their results.
     * <p/>
     * The list returned is unmodifiable and the results of this method will change
     * after each invocation of a findAvailable* or mapAvailable* methods.
     * <p/>
     * This method is not thread safe.
     */
    public List<String> getResourcesNotLoaded() {
        return Collections.unmodifiableList(resourcesNotLoaded);
    }

    // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
    //
    //   Find String
    //
    // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *

    /**
     * Reads the contents of the URL as a {@link String}'s and returns it.
     *
     * @param uri
     * @return a stringified content of a resource
     * @throws IOException if a resource pointed out by the uri param could not be find
     * 
     * @see ClassLoader#getResource(String)
     */
    public String findString(String uri) throws IOException {
        String fullUri = path + uri;

        URL resource = classLoader.getResource(fullUri);
        if (resource == null) {
            throw new IOException("Could not find a resource in : " + fullUri);
        }

        return readContents(resource);
    }

    /**
     * Reads the contents of the found URLs as a list of {@link String}'s and returns them.
     *
     * @param uri
     * @return a list of the content of each resource URL found
     * @throws IOException if any of the found URLs are unable to be read.
     */
    public List<String> findAllStrings(String uri) throws IOException {
        String fulluri = path + uri;

        List<String> strings = new ArrayList<String>();

        Enumeration<URL> resources = classLoader.getResources(fulluri);
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            String string = readContents(url);
            strings.add(string);
        }
        return strings;
    }

    /**
     * Reads the contents of the found URLs as a Strings and returns them.
     * Individual URLs that cannot be read are skipped and added to the
     * list of 'resourcesNotLoaded'
     *
     * @param uri
     * @return a list of the content of each resource URL found
     * @throws IOException if classLoader.getResources throws an exception
     */
    public List<String> findAvailableStrings(String uri) throws IOException {
        resourcesNotLoaded.clear();
        String fulluri = path + uri;

        List<String> strings = new ArrayList<String>();

        Enumeration<URL> resources = classLoader.getResources(fulluri);
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            try {
                String string = readContents(url);
                strings.add(string);
            } catch (IOException notAvailable) {
                resourcesNotLoaded.add(url.toExternalForm());
            }
        }
        return strings;
    }

    /**
     * Reads the contents of all non-directory URLs immediately under the specified
     * location and returns them in a map keyed by the file name.
     *
     * Any URLs that cannot be read will cause an exception to be thrown.
     *
     * Example classpath:
     *
     * META-INF/serializables/one
     * META-INF/serializables/two
     * META-INF/serializables/three
     * META-INF/serializables/four/foo.txt
     *
     * ResourceFinder finder = new ResourceFinder("META-INF/");
     * Map map = finder.mapAvailableStrings("serializables");
     * map.contains("one");  // true
     * map.contains("two");  // true
     * map.contains("three");  // true
     * map.contains("four");  // false
     *
     * @param uri
     * @return a list of the content of each resource URL found
     * @throws IOException if any of the urls cannot be read
     */
    public Map<String,String> mapAllStrings(String uri) throws IOException {
        Map<String,String> strings = new HashMap<String,String>();
        Map<String, URL> resourcesMap = getResourcesMap(uri);
        for (Iterator iterator = resourcesMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String name = (String) entry.getKey();
            URL url = (URL) entry.getValue();
            String value = readContents(url);
            strings.put(name,value);
        }
        return strings;
    }

    /**
     * Reads the contents of all non-directory URLs immediately under the specified
     * location and returns them in a map keyed by the file name.
     *
     * Individual URLs that cannot be read are skipped and added to the
     * list of 'resourcesNotLoaded'
     *
     * Example classpath:
     *
     * META-INF/serializables/one
     * META-INF/serializables/two      # not readable
     * META-INF/serializables/three
     * META-INF/serializables/four/foo.txt
     *
     * ResourceFinder finder = new ResourceFinder("META-INF/");
     * Map map = finder.mapAvailableStrings("serializables");
     * map.contains("one");  // true
     * map.contains("two");  // false
     * map.contains("three");  // true
     * map.contains("four");  // false
     *
     * @param uri
     * @return a list of the content of each resource URL found
     * @throws IOException if classLoader.getResources throws an exception
     */
    public Map<String,String> mapAvailableStrings(String uri) throws IOException {
        resourcesNotLoaded.clear();
        Map<String,String> strings = new HashMap<String,String>();
        Map<String, URL> resourcesMap = getResourcesMap(uri);
        for (Iterator iterator = resourcesMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String name = (String) entry.getKey();
            URL url = (URL) entry.getValue();
            try {
                String value = readContents(url);
                strings.put(name,value);
            } catch (IOException notAvailable) {
                resourcesNotLoaded.add(url.toExternalForm());
            }
        }
        return strings;
    }


    // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
    //
    //   Find Class
    //
    // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *

    /**
     * Executes {@link #findString(String)} assuming the contents URL found is the name of
     * a class that should be loaded and returned.
     *
     * @param uri
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public Class findClass(String uri) throws IOException, ClassNotFoundException {
        String className = findString(uri);
        return (Class)classLoader.loadClass(className);
    }

    /**
     * Executes findAllStrings assuming the strings are
     * the names of a classes that should be loaded and returned.
     *
     * Any URL or class that cannot be loaded will cause an exception to be thrown.
     *
     * @param uri
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public List<Class> findAllClasses(String uri) throws IOException, ClassNotFoundException {
        List<Class> classes = new ArrayList<Class>();
        List<String> strings = findAllStrings(uri);
        for (String className : strings) {
            Class clazz = classLoader.loadClass(className);
            classes.add(clazz);
        }
        return classes;
    }

    /**
     * Executes findAvailableStrings assuming the strings are
     * the names of a classes that should be loaded and returned.
     *
     * Any class that cannot be loaded will be skipped and placed in the
     * 'resourcesNotLoaded' collection.
     *
     * @param uri
     * @return
     * @throws IOException if classLoader.getResources throws an exception
     */
    public List<Class> findAvailableClasses(String uri) throws IOException {
        resourcesNotLoaded.clear();
        List<Class> classes = new ArrayList<Class>();
        List<String> strings = findAvailableStrings(uri);
        for (String className : strings) {
            try {
                Class clazz = classLoader.loadClass(className);
                classes.add(clazz);
            } catch (Exception notAvailable) {
                resourcesNotLoaded.add(className);
            }
        }
        return classes;
    }

    /**
     * Executes mapAllStrings assuming the value of each entry in the
     * map is the name of a class that should be loaded.
     *
     * Any class that cannot be loaded will be cause an exception to be thrown.
     *
     * Example classpath:
     *
     * META-INF/xmlparsers/xerces
     * META-INF/xmlparsers/crimson
     *
     * ResourceFinder finder = new ResourceFinder("META-INF/");
     * Map map = finder.mapAvailableStrings("xmlparsers");
     * map.contains("xerces");  // true
     * map.contains("crimson");  // true
     * Class xercesClass = map.get("xerces");
     * Class crimsonClass = map.get("crimson");
     *
     * @param uri
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public Map<String, Class> mapAllClasses(String uri) throws IOException, ClassNotFoundException {
        Map<String, Class> classes = new HashMap<String, Class>();
        Map<String, String> map = mapAllStrings(uri);
        for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String string = (String) entry.getKey();
            String className = (String) entry.getValue();
            Class clazz = classLoader.loadClass(className);
            classes.put(string, clazz);
        }
        return classes;
    }

    /**
     * Executes mapAvailableStrings assuming the value of each entry in the
     * map is the name of a class that should be loaded.
     *
     * Any class that cannot be loaded will be skipped and placed in the
     * 'resourcesNotLoaded' collection.
     *
     * Example classpath:
     *
     * META-INF/xmlparsers/xerces
     * META-INF/xmlparsers/crimson
     *
     * ResourceFinder finder = new ResourceFinder("META-INF/");
     * Map map = finder.mapAvailableStrings("xmlparsers");
     * map.contains("xerces");  // true
     * map.contains("crimson");  // true
     * Class xercesClass = map.get("xerces");
     * Class crimsonClass = map.get("crimson");
     *
     * @param uri
     * @return
     * @throws IOException if classLoader.getResources throws an exception
     */
    public Map<String, Class> mapAvailableClasses(String uri) throws IOException {
        resourcesNotLoaded.clear();
        Map<String, Class> classes = new HashMap<String, Class>();
        Map<String, String> map = mapAvailableStrings(uri);
        for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String string = (String) entry.getKey();
            String className = (String) entry.getValue();
            try {
                Class clazz = classLoader.loadClass(className);
                classes.put(string, clazz);
            } catch (Exception notAvailable) {
                resourcesNotLoaded.add(className);
            }
        }
        return classes;
    }

    // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
    //
    //   Find Implementation
    //
    // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *

    /**
     * Assumes the class specified points to a file in the classpath that contains
     * the name of a class that implements or is a subclass of the specfied class.
     *
     * Any class that cannot be loaded will be cause an exception to be thrown.
     *
     * Example classpath:
     *
     * META-INF/java.io.InputStream    # contains the classname org.acme.AcmeInputStream
     * META-INF/java.io.OutputStream
     *
     * ResourceFinder finder = new ResourceFinder("META-INF/");
     * Class clazz = finder.findImplementation(java.io.InputStream.class);
     * clazz.getName();  // returns "org.acme.AcmeInputStream"
     *
     * @param interfase a superclass or interface
     * @return
     * @throws IOException if the URL cannot be read
     * @throws ClassNotFoundException if the class found is not loadable
     * @throws ClassCastException if the class found is not assignable to the specified superclass or interface
     */
    public Class findImplementation(Class interfase) throws IOException, ClassNotFoundException {
        String className = findString(interfase.getName());
        Class impl = classLoader.loadClass(className);
        if (!interfase.isAssignableFrom(impl)) {
            throw new ClassCastException("Class not of type: " + interfase.getName());
        }
        return impl;
    }

    /**
     * Assumes the class specified points to a file in the classpath that contains
     * the name of a class that implements or is a subclass of the specfied class.
     *
     * Any class that cannot be loaded or assigned to the specified interface will be cause
     * an exception to be thrown.
     *
     * Example classpath:
     *
     * META-INF/java.io.InputStream    # contains the classname org.acme.AcmeInputStream
     * META-INF/java.io.InputStream    # contains the classname org.widget.NeatoInputStream
     * META-INF/java.io.InputStream    # contains the classname com.foo.BarInputStream
     *
     * ResourceFinder finder = new ResourceFinder("META-INF/");
     * List classes = finder.findAllImplementations(java.io.InputStream.class);
     * classes.contains("org.acme.AcmeInputStream");  // true
     * classes.contains("org.widget.NeatoInputStream");  // true
     * classes.contains("com.foo.BarInputStream");  // true
     *
     * @param interfase a superclass or interface
     * @return
     * @throws IOException if the URL cannot be read
     * @throws ClassNotFoundException if the class found is not loadable
     * @throws ClassCastException if the class found is not assignable to the specified superclass or interface
     */
    public List<Class> findAllImplementations(Class interfase) throws IOException, ClassNotFoundException {
        List<Class> implementations = new ArrayList<Class>();
        List<String> strings = findAllStrings(interfase.getName());
        for (String className : strings) {
            Class impl = classLoader.loadClass(className);
            if (!interfase.isAssignableFrom(impl)) {
                throw new ClassCastException("Class not of type: " + interfase.getName());
            }
            implementations.add(impl);
        }
        return implementations;
    }

    /**
     * Assumes the class specified points to a file in the classpath that contains
     * the name of a class that implements or is a subclass of the specfied class.
     *
     * Any class that cannot be loaded or are not assignable to the specified class will be
     * skipped and placed in the 'resourcesNotLoaded' collection.
     *
     * Example classpath:
     *
     * META-INF/java.io.InputStream    # contains the classname org.acme.AcmeInputStream
     * META-INF/java.io.InputStream    # contains the classname org.widget.NeatoInputStream
     * META-INF/java.io.InputStream    # contains the classname com.foo.BarInputStream
     *
     * ResourceFinder finder = new ResourceFinder("META-INF/");
     * List classes = finder.findAllImplementations(java.io.InputStream.class);
     * classes.contains("org.acme.AcmeInputStream");  // true
     * classes.contains("org.widget.NeatoInputStream");  // true
     * classes.contains("com.foo.BarInputStream");  // true
     *
     * @param interfase a superclass or interface
     * @return
     * @throws IOException if classLoader.getResources throws an exception
     */
    public List<Class> findAvailableImplementations(Class interfase) throws IOException {
        resourcesNotLoaded.clear();
        List<Class> implementations = new ArrayList<Class>();
        List<String> strings = findAvailableStrings(interfase.getName());
        for (String className : strings) {
            try {
                Class impl = classLoader.loadClass(className);
                if (interfase.isAssignableFrom(impl)) {
                    implementations.add(impl);
                } else {
                    resourcesNotLoaded.add(className);
                }
            } catch (Exception notAvailable) {
                resourcesNotLoaded.add(className);
            }
        }
        return implementations;
    }

    /**
     * Assumes the class specified points to a directory in the classpath that holds files
     * containing the name of a class that implements or is a subclass of the specfied class.
     *
     * Any class that cannot be loaded or assigned to the specified interface will be cause
     * an exception to be thrown.
     *
     * Example classpath:
     *
     * META-INF/java.net.URLStreamHandler/jar
     * META-INF/java.net.URLStreamHandler/file
     * META-INF/java.net.URLStreamHandler/http
     *
     * ResourceFinder finder = new ResourceFinder("META-INF/");
     * Map map = finder.mapAllImplementations(java.net.URLStreamHandler.class);
     * Class jarUrlHandler = map.get("jar");
     * Class fileUrlHandler = map.get("file");
     * Class httpUrlHandler = map.get("http");
     *
     * @param interfase a superclass or interface
     * @return
     * @throws IOException if the URL cannot be read
     * @throws ClassNotFoundException if the class found is not loadable
     * @throws ClassCastException if the class found is not assignable to the specified superclass or interface
     */
    public Map<String, Class> mapAllImplementations(Class interfase) throws IOException, ClassNotFoundException {
        Map<String, Class> implementations = new HashMap<String, Class>();
        Map<String, String> map = mapAllStrings(interfase.getName());
        for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String string = (String) entry.getKey();
            String className = (String) entry.getValue();
            Class impl = classLoader.loadClass(className);
            if (!interfase.isAssignableFrom(impl)) {
                throw new ClassCastException("Class not of type: " + interfase.getName());
            }
            implementations.put(string, impl);
        }
        return implementations;
    }

    /**
     * Assumes the class specified points to a directory in the classpath that holds files
     * containing the name of a class that implements or is a subclass of the specfied class.
     *
     * Any class that cannot be loaded or are not assignable to the specified class will be
     * skipped and placed in the 'resourcesNotLoaded' collection.
     *
     * Example classpath:
     *
     * META-INF/java.net.URLStreamHandler/jar
     * META-INF/java.net.URLStreamHandler/file
     * META-INF/java.net.URLStreamHandler/http
     *
     * ResourceFinder finder = new ResourceFinder("META-INF/");
     * Map map = finder.mapAllImplementations(java.net.URLStreamHandler.class);
     * Class jarUrlHandler = map.get("jar");
     * Class fileUrlHandler = map.get("file");
     * Class httpUrlHandler = map.get("http");
     *
     * @param interfase a superclass or interface
     * @return
     * @throws IOException if classLoader.getResources throws an exception
     */
    public Map<String, Class> mapAvailableImplementations(Class interfase) throws IOException {
        resourcesNotLoaded.clear();
        Map<String, Class> implementations = new HashMap<String, Class>();
        Map<String, String> map = mapAvailableStrings(interfase.getName());
        for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String string = (String) entry.getKey();
            String className = (String) entry.getValue();
            try {
                Class impl = classLoader.loadClass(className);
                if (interfase.isAssignableFrom(impl)) {
                    implementations.put(string, impl);
                } else {
                    resourcesNotLoaded.add(className);
                }
            } catch (Exception notAvailable) {
                resourcesNotLoaded.add(className);
            }
        }
        return implementations;
    }


    // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
    //
    //   Find Properties
    //
    // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *

    /**
     * Finds the corresponding resource and reads it in as a properties file
     *
     * Example classpath:
     *
     * META-INF/widget.properties
     *
     * ResourceFinder finder = new ResourceFinder("META-INF/");
     * Properties widgetProps = finder.findProperties("widget.properties");
     *
     * @param uri
     * @return
     * @throws IOException if the URL cannot be read or is not in properties file format
     */
    public Properties findProperties(String uri) throws IOException {
        String fulluri = path + uri;

        URL resource = classLoader.getResource(fulluri);
        if (resource == null) {
            throw new IOException("Could not find command in : " + fulluri);
        }

        return loadProperties(resource);
    }

    /**
     * Finds the corresponding resources and reads them in as a properties files
     *
     * Any URL that cannot be read in as a properties file will cause an exception to be thrown.
     *
     * Example classpath:
     *
     * META-INF/app.properties
     * META-INF/app.properties
     * META-INF/app.properties
     *
     * ResourceFinder finder = new ResourceFinder("META-INF/");
     * List<Properties> appProps = finder.findAllProperties("app.properties");
     *
     * @param uri
     * @return
     * @throws IOException if the URL cannot be read or is not in properties file format
     */
    public List<Properties> findAllProperties(String uri) throws IOException {
        String fulluri = path + uri;

        List<Properties> properties = new ArrayList<Properties>();

        Enumeration<URL> resources = classLoader.getResources(fulluri);
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            Properties props = loadProperties(url);
            properties.add(props);
        }
        return properties;
    }

    /**
     * Finds the corresponding resources and reads them in as a properties files
     *
     * Any URL that cannot be read in as a properties file will be added to the
     * 'resourcesNotLoaded' collection.
     *
     * Example classpath:
     *
     * META-INF/app.properties
     * META-INF/app.properties
     * META-INF/app.properties
     *
     * ResourceFinder finder = new ResourceFinder("META-INF/");
     * List<Properties> appProps = finder.findAvailableProperties("app.properties");
     *
     * @param uri
     * @return
     * @throws IOException if classLoader.getResources throws an exception
     */
    public List<Properties> findAvailableProperties(String uri) throws IOException {
        resourcesNotLoaded.clear();
        String fulluri = path + uri;

        List<Properties> properties = new ArrayList<Properties>();

        Enumeration<URL> resources = classLoader.getResources(fulluri);
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            try {
                Properties props = loadProperties(url);
                properties.add(props);
            } catch (Exception notAvailable) {
                resourcesNotLoaded.add(url.toExternalForm());
            }
        }
        return properties;
    }

    /**
     * Finds the corresponding resources and reads them in as a properties files
     *
     * Any URL that cannot be read in as a properties file will cause an exception to be thrown.
     *
     * Example classpath:
     *
     * META-INF/jdbcDrivers/oracle.properties
     * META-INF/jdbcDrivers/mysql.props
     * META-INF/jdbcDrivers/derby
     *
     * ResourceFinder finder = new ResourceFinder("META-INF/");
     * List<Properties> driversList = finder.findAvailableProperties("jdbcDrivers");
     * Properties oracleProps = driversList.get("oracle.properties");
     * Properties mysqlProps = driversList.get("mysql.props");
     * Properties derbyProps = driversList.get("derby");
     *
     * @param uri
     * @return
     * @throws IOException if the URL cannot be read or is not in properties file format
     */
    public Map<String, Properties> mapAllProperties(String uri) throws IOException {
        Map<String, Properties> propertiesMap = new HashMap<String, Properties>();
        Map<String, URL> map = getResourcesMap(uri);
        for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String string = (String) entry.getKey();
            URL url = (URL) entry.getValue();
            Properties properties = loadProperties(url);
            propertiesMap.put(string, properties);
        }
        return propertiesMap;
    }

    /**
     * Finds the corresponding resources and reads them in as a properties files
     *
     * Any URL that cannot be read in as a properties file will be added to the
     * 'resourcesNotLoaded' collection.
     *
     * Example classpath:
     *
     * META-INF/jdbcDrivers/oracle.properties
     * META-INF/jdbcDrivers/mysql.props
     * META-INF/jdbcDrivers/derby
     *
     * ResourceFinder finder = new ResourceFinder("META-INF/");
     * List<Properties> driversList = finder.findAvailableProperties("jdbcDrivers");
     * Properties oracleProps = driversList.get("oracle.properties");
     * Properties mysqlProps = driversList.get("mysql.props");
     * Properties derbyProps = driversList.get("derby");
     *
     * @param uri
     * @return
     * @throws IOException if classLoader.getResources throws an exception
     */
    public Map<String, Properties> mapAvailableProperties(String uri) throws IOException {
        resourcesNotLoaded.clear();
        Map<String, Properties> propertiesMap = new HashMap<String, Properties>();
        Map<String, URL> map = getResourcesMap(uri);
        for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String string = (String) entry.getKey();
            URL url = (URL) entry.getValue();
            try {
                Properties properties = loadProperties(url);
                propertiesMap.put(string, properties);
            } catch (Exception notAvailable) {
                resourcesNotLoaded.add(url.toExternalForm());
            }
        }
        return propertiesMap;
    }

    // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
    //
    //   Map Resources
    //
    // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *

    public Map<String, URL> getResourcesMap(String uri) throws IOException {
        String basePath = path + uri;

        Map<String, URL> resources = new HashMap<String, URL>();
        if (!basePath.endsWith("/")){
            basePath += "/";
        }
        Enumeration<URL> urls = classLoader.getResources(basePath);

        while (urls.hasMoreElements()) {
            URL location = urls.nextElement();

            try {
                if (location.getProtocol().equals("jar")) {

                    readJarEntries(location, basePath, resources);

                } else if (location.getProtocol().equals("file")) {

                    readDirectoryEntries(location, resources);

                }
            } catch (Exception e) {
            }
        }

        return resources;
    }

    private static void readDirectoryEntries(URL location, Map<String, URL> resources) throws MalformedURLException {
        File dir = new File(location.getPath());
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            for (File file : files) {
                if (!file.isDirectory()){
                    String name = file.getName();
                    URL url = file.toURL();
                    resources.put(name, url);
                }
            }
        }
    }

    private static void readJarEntries(URL location, String basePath, Map<String, URL> resources) throws IOException {
        JarURLConnection conn = (JarURLConnection) location.openConnection();
        JarFile jarfile = conn.getJarFile();

        Enumeration<JarEntry> entries = jarfile.entries();
        while (entries != null && entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();

            if (entry.isDirectory() || !name.startsWith(basePath) || name.length() == basePath.length()) {
                continue;
            }

            name = name.substring(basePath.length());

            if (name.contains("/")) {
                continue;
            }

            URL resource = new URL(location, name);
            resources.put(name, resource);
        }
    }

    private Properties loadProperties(URL resource) throws IOException {
        InputStream in = resource.openStream();

        BufferedInputStream reader = null;
        try {
            reader = new BufferedInputStream(in);
            Properties properties = new Properties();
            properties.load(reader);

            return properties;
        } finally {
            try {
                in.close();
                reader.close();
            } catch (Exception e) {
            }
        }
    }

    private String readContents(URL resource) throws IOException {
        InputStream in = resource.openStream();
        BufferedInputStream reader = null;
        StringBuffer sb = new StringBuffer();

        try {
            reader = new BufferedInputStream(in);

            int b = reader.read();
            while (b != -1) {
                sb.append((char) b);
                b = reader.read();
            }

            return sb.toString().trim();
        } finally {
            try {
                in.close();
                reader.close();
            } catch (Exception e) {
            }
        }
    }

}
