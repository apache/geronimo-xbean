/**
 *
 * Copyright 2006 The Apache Software Foundation
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
package org.apache.xbean.bootstrap;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Set;
import java.util.LinkedHashSet;

/**
 * SpringBootstrap is the main class used by a Spring based server.  This class uses the following strategies to determine
 * the configuration file to load:
 * <p/>
 * Command line parameter --bootstrap FILE
 * Manifest entry XBean-Bootstrap in the startup jar
 * META-INF/xbean-bootstrap.xml
 * <p/>
 * This class atempts to first load the configuration file from the local file system and if that fails it attempts to
 * load it from the classpath.
 * <p/>
 * SpringBootstrap expects the configuration to contain a service with the id "main" which is an implementation of
 * org.apache.xbean.server.main.Main.
 * <p/>
 * This class will set the system property xbean.base.dir to the directory containing the startup jar if the property
 * has not alredy been set (on the command line).
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.5-colossus
 */
public class Bootstrap {
    public static final String BOOTSTRAP_DIR = "xbean.bootstrap.dir";

    public static final String HOME_DIR = "xbean.home.dir";
    private static final String HOME_DIR_DEFAULT = "..";

    public static final String CLASS_PATH = "xbean.bootstrap.ClassPath";
    private static final String CLASS_PATH_DEFAULT = "lib/*.jar,lib/*.zip";

    public static final String CLASS_LOADER_FACTORY = "xbean.bootstrap.ClassLoaderFactory";

    public static final String LOADER = "xbean.bootstrap.BootstrapLoader";
    private static final String LOADER_MANIFEST_SERVICE = "META-INF/org/apache/xbean/bootstrap/BootstrapLoader";

    private List<String> arguments;
    private Properties properties;

    private String bootstrapDirectory;
    private String homeDirectory;
    private List<String> classPath;
    private ClassLoaderFactory classLoaderFactory;
    private String bootstrapLoader;

    /**
     * Initializes and boots using the supplied arguments.  If an error is thrown from the boot method,
     * this method will print the error to standard error along with the stack trace and exit with the exit specified
     * in the FatalStartupError or exit code 9 if the error was not a FatalStartupError.
     *
     * @param args the arguments used to start the server
     */
    public static void main(String[] args) {
        Bootstrap bootstrap = new Bootstrap();
        Bootstrap.main(new LinkedList<String>(Arrays.asList(args)), bootstrap);
    }

    /**
     * Like the main(args) method but allows a configured bootstrap instance to be passed in.
     *
     * @see #main(String[])
     */
    public static void main(List<String> args, Bootstrap bootstrap) {
        bootstrap.initialize(args);

        try {
            bootstrap.boot();
        } catch (FatalStartupError e) {
            System.err.println(e.getMessage());
            if (e.getCause() != null) {
                e.getCause().printStackTrace();
            }
            System.exit(e.getExitCode());
        } catch (Throwable e) {
            System.err.println("Unknown error");
            e.printStackTrace();
            System.exit(9);
        }
    }

    /**
     * Gets the arguments passed to the bootstrap loader.
     *
     * @return the arguments passed to the bootstrap loader
     */
    public List<String> getArguments() {
        return Collections.unmodifiableList(arguments);
    }

    /**
     * Sets the arguments passed to the bootstrap loader.
     *
     * @param arguments the arguments passed to the bootstrap loader
     */
    public void setArguments(List<String> arguments) {
        if (arguments != null) {
            this.arguments = new LinkedList<String>(arguments);
        } else {
            this.arguments = null;
        }
    }

    /**
     * Gets the properties passed to the bootstrap loader.
     *
     * @return the properties passed to the bootstrap loader
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Sets the properties passed to the bootstrap loader.
     *
     * @param properties the properties passed to the bootstrap loader
     */
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    /**
     * Gets the bootstrap directory.
     *
     * @return the base directory
     */
    public String getBootstrapDirectory() {
        return bootstrapDirectory;
    }

    /**
     * Sets the bootstrap directory.
     *
     * @param bootstrapDirectory the bootstrap directory
     */
    public void setBootstrapDirectory(String bootstrapDirectory) {
        this.bootstrapDirectory = bootstrapDirectory;
    }

    /**
     * Gets the home directory relative to the bootstrap directory.
     * @return the home directory relative to the bootstrap directory
     */
    public String getHomeDirectory() {
        return homeDirectory;
    }

    /**
     * Sets the home directory relative to the bootstrap directory.
     * @param homeDirectory the home directory relative to the bootstrap directory
     */
    public void setHomeDirectory(String homeDirectory) {
        this.homeDirectory = homeDirectory;
    }

    /**
     * Gets the class path used to create the bootstrap class loader.
     *
     * @return class path used to create the bootstrap class loader
     */
    public List<String> getClassPath() {
        return classPath;
    }

    /**
     * Sets the class path used to create the bootstrap class loader.
     *
     * @param classPath the class path used to create the bootstrap class loader
     */
    public void setClassPath(List<String> classPath) {
        this.classPath = classPath;
    }

    /**
     * Gets the factory used to create the bootstrap class loader.
     *
     * @return factory used to create the bootstrap class loader
     */
    public ClassLoaderFactory getClassLoaderFactory() {
        return classLoaderFactory;
    }

    /**
     * Sets the factory used to create the bootstrap class loader.
     *
     * @param classLoaderFactory the factory used to create the bootstrap class loader
     */
    public void setClassLoaderFactory(ClassLoaderFactory classLoaderFactory) {
        this.classLoaderFactory = classLoaderFactory;
    }

    /**
     * Gets the name of the loader class to use for bootstraping.
     *
     * @return the name of the loader class to use for bootstraping
     */
    public String getBootstrapLoader() {
        return bootstrapLoader;
    }

    /**
     * Sets the name of the loader class to use for bootstraping.
     *
     * @param bootstrapLoader the name of the loader class to use for bootstraping
     */
    public void setBootstrapLoader(String bootstrapLoader) {
        this.bootstrapLoader = bootstrapLoader;
    }

    /**
     * Initializes the bootstrap directory, properties, class path, class loader factory, and bootstrap loader.
     *
     * @param args the arguments passed to main
     */
    public void initialize(List<String> args) {
        arguments = new LinkedList<String>(args);
        extractProperties(arguments, System.getProperties());

        // Determine the xbean bootstrap directory
        // this must be done before the properties are initialized because the initialization
        // code loads a properties file from the bootstrap directory
        if (bootstrapDirectory == null) {
            bootstrapDirectory = System.getProperty(BOOTSTRAP_DIR);
        }

        // Determine the startup dir
        if (bootstrapDirectory == null) {
            URL bootstrapClassUrl = getClass().getClassLoader().getResource(Bootstrap.class.getName().replace('.', '/') + ".class");
            if (bootstrapClassUrl == null) {
                throw new FatalStartupError("Could not determine XBean installation directory", 9);
            }

            try {
                URLConnection urlConnection = bootstrapClassUrl.openConnection();

                if (urlConnection instanceof JarURLConnection) {
                    JarURLConnection jarConnection = (JarURLConnection) urlConnection;
                    bootstrapClassUrl = jarConnection.getJarFileURL();

                    bootstrapDirectory = new File(new URI(bootstrapClassUrl.toString())).getParentFile().getAbsolutePath();
                } else {
                    File startupDir = new File(new URI(bootstrapClassUrl.toString())).getParentFile();
                    for (char c : getClass().getName().toCharArray()) {
                        if (c == '.') {
                            startupDir = startupDir.getParentFile();
                        }
                    }
                    bootstrapDirectory = startupDir.getAbsolutePath();
                }
            } catch (Exception e) {
                throw new FatalStartupError("Could not determine XBean installation directory", 9, e);
            }
        }

        // properties
        initalizeProperties();

        // home directory
        initializeHomeDirectory();

        // class path
        initializeClassPath();

        // class loader factory
        initializeClassLoaderFactory();

        // bootstrap loader
        initalizeBootstrapLoader();
    }

    private void initalizeProperties() {
        properties = new Properties();

        // load the meta-inf properties
        loadProperties(getClass().getClassLoader().getResourceAsStream("META-INF/xbean-bootstrap.properties"));

        // load the bin/bootstrap.properties file
        try {
            loadProperties(new FileInputStream(new File(bootstrapDirectory, "bootstrap.properties")));
        } catch (FileNotFoundException ignored) {
        }

        // Add system properties
        properties.putAll(System.getProperties());
    }

    private void initializeHomeDirectory() {
        if (homeDirectory == null) {
            homeDirectory = properties.getProperty(HOME_DIR, HOME_DIR_DEFAULT);
        }
    }

    private void loadProperties(InputStream in) {
        if (in != null) {
            try {
                properties.load(in);
            } catch (IOException e) {
            } finally {
                close(in);
                in = null;
            }
        }
    }

    private void initializeClassPath() {
        String cp = properties.getProperty(CLASS_PATH, CLASS_PATH_DEFAULT);

        classPath = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(cp, ",");
        while (tokenizer.hasMoreElements()) {
            String element = tokenizer.nextToken();

            // strip of quotes
            if (element.length() > 2 && element.startsWith("\"") && element.endsWith("\"")) {
                element = element.substring(1, element.length() - 1);
            }

            classPath.add(element);
        }
    }

    private void initializeClassLoaderFactory() {
        String factoryName = properties.getProperty(CLASS_LOADER_FACTORY);

        if (factoryName == null) {
            return;
        }

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }

        try {
            Class<?> factoryClass = classLoader.loadClass(factoryName);
            classLoaderFactory = (ClassLoaderFactory) factoryClass.newInstance();
        } catch (Exception e) {
            throw new FatalStartupError("Unable to instantiate the class loader factory " + factoryName, e);
        }
    }

    private void initalizeBootstrapLoader() {
        bootstrapLoader = properties.getProperty(LOADER, bootstrapLoader);
    }

    /**
     * Loads the main instance from the Spring configuration file and executes it.
     */
    public void boot() {
        // verify the xbean bootstrap directory
        if (bootstrapDirectory == null) {
            throw new FatalStartupError("Bootstrap directory was not set");

        }
        if (!new File(bootstrapDirectory).isDirectory()) {
            throw new FatalStartupError("Bootstrap directory is not a directory: " + bootstrapDirectory);

        }

        File homeDirectory = new File(bootstrapDirectory, this.homeDirectory);
        if (!homeDirectory.isDirectory()) {
            throw new IllegalArgumentException("XBean home directory is not a directory: " + homeDirectory.getAbsolutePath());
        }

        // verify main arguments
        if (arguments == null) {
            arguments = Collections.emptyList();
        }

        // create the class loader
        ClassLoader classLoader = createClassLoader();

        // set the thread context class loader
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            // set directory properties
            System.setProperty(BOOTSTRAP_DIR, bootstrapDirectory);
            System.setProperty(HOME_DIR, homeDirectory.getAbsolutePath());

            properties.setProperty(BOOTSTRAP_DIR, bootstrapDirectory);
            properties.setProperty(HOME_DIR, homeDirectory.getAbsolutePath());

            // load the bootstrap class
            BootstrapLoader bootstrapLoader = loadBootstrap(classLoader);

            // boot
            bootstrapLoader.boot(arguments.toArray(new String[arguments.size()]), properties, classLoader);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }

    }

    private ClassLoader createClassLoader() {
        if (classPath == null) {
            return getClass().getClassLoader();
        }

        File homeDirectory = new File(bootstrapDirectory, this.homeDirectory);
        Set<URL> urls = new LinkedHashSet<URL>();
        for (String entry : classPath) {
            urls.addAll(BootstrapFinder.search(homeDirectory, entry));
        }

        ClassLoaderFactory factory = classLoaderFactory;
        if (factory == null) {
            factory = new UrlClassLoaderFactory();
        }
        ClassLoader classLoader = factory.createClassLoader(getClass().getClassLoader(), new ArrayList<URL>(urls));
        return classLoader;
    }

    private BootstrapLoader loadBootstrap(ClassLoader classLoader) {
        if (bootstrapLoader == null) {
            // attempt to find the loader via manifest services
            bootstrapLoader = findBootstrapLoader(classLoader);
        }
        try {
            Class<?> factoryClass = classLoader.loadClass(bootstrapLoader);
            BootstrapLoader bootstrapLoader = (BootstrapLoader) factoryClass.newInstance();
            return bootstrapLoader;
        } catch (Exception e) {
            throw new FatalStartupError("Unable to instantiate the bootstrap loader " + bootstrapLoader, e);
        }
    }

    public static String findBootstrapLoader(ClassLoader classLoader) {
        URL resource = classLoader.getResource(LOADER_MANIFEST_SERVICE);
        if (resource == null) {
            throw new FatalStartupError("Could not find a bootstrap loader in " + LOADER_MANIFEST_SERVICE);
        }

        InputStream in = null;
        BufferedInputStream reader = null;
        try {
            in = resource.openStream();
            StringBuffer sb = new StringBuffer();

            reader = new BufferedInputStream(in);

            int b = reader.read();
            while (b != -1) {
                sb.append((char) b);
                b = reader.read();
            }

            return sb.toString().trim();
        } catch (IOException ignored) {
            throw new FatalStartupError("Unable to read bootstrap loader name from " + resource);
        } finally {
            close(in);
            close(reader);
        }
    }

    private static void extractProperties(List<String> arguments, Properties properties) {
        for (Iterator<String> iterator = arguments.iterator(); iterator.hasNext();) {
            String arg = iterator.next();
            if (arg.startsWith("-D")) {
                int equalsIndex = arg.indexOf("=");
                String name = arg.substring(2, equalsIndex);
                String value = arg.substring(equalsIndex + 1);

                properties.setProperty(name, value);

                iterator.remove();
            }
        }
    }

    private static void close(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
            }
        }
    }
}