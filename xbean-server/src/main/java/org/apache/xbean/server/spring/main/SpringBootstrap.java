/**
 *
 * Copyright 2005-2006 The Apache Software Foundation or its licensors, as applicable.
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
package org.apache.xbean.server.spring.main;

import java.beans.PropertyEditorManager;
import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.xbean.bootstrap.Bootstrap;
import org.apache.xbean.bootstrap.BootstrapLoader;
import org.apache.xbean.server.main.FatalStartupError;
import org.apache.xbean.server.main.Main;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.apache.xbean.spring.context.FileSystemXmlApplicationContext;
import org.apache.xbean.spring.context.SpringApplicationContext;

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
 * @since 2.0
 */
public class SpringBootstrap implements BootstrapLoader {
    private static final String CONFIGURATION_FILE = "xbean.server.spring.configuration.file";
    private static final String CONFIGURATION_FILE_DEFAULT = "conf/server.xml";

    private static final List<String> DEFAULT_PROPERTY_EDITOR_PATHS = Collections.singletonList("org.apache.xbean.server.propertyeditor");

    private String homeDirectory;
    private String configurationFile;
    private List<String> propertyEditorPaths = DEFAULT_PROPERTY_EDITOR_PATHS;
    private ClassLoader classLoader;

    /**
     * Gets the configuration file from which the main instance is loaded.
     *
     * @return the configuration file from which the main instance is loaded
     */
    public String getConfigurationFile() {
        return configurationFile;
    }

    /**
     * Sets the configuration file from which the main instance is loaded.
     *
     * @param configurationFile the configuration file from which the main instance is loaded
     */
    public void setConfigurationFile(String configurationFile) {
        this.configurationFile = configurationFile;
    }

    /**
     * Gets the paths that are appended to the system property editors search path.
     *
     * @return the paths that are appended to the system property editors search path
     */
    public List<String> getPropertyEditorPaths() {
        return propertyEditorPaths;
    }

    /**
     * Sets the paths that are appended to the system property editors search path.
     *
     * @param propertyEditorPaths the paths that are appended to the system property editors search path
     */
    public void setPropertyEditorPaths(List<String> propertyEditorPaths) {
        this.propertyEditorPaths = propertyEditorPaths;
    }

    /**
     * String the home directory of the server.
     *
     * @return the home directory of the server
     */
    public String getHomeDirectory() {
        return homeDirectory;
    }

    /**
     * Sets the home directory of the server.
     *
     * @param homeDirectory the home directory of the server
     */
    public void setHomeDirectory(String homeDirectory) {
        this.homeDirectory = homeDirectory;
    }

    /**
     * Gets the class loader passed to the Spring application context.
     *
     * @return the class loader passed to the Spring application context
     */
    public ClassLoader getClassLoader() {
        if (classLoader != null) {
            return classLoader;
        }
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            return classLoader;
        }
        return getClass().getClassLoader();
    }

    /**
     * Sets the class loader passed to the Spring application context.
     *
     * @param classLoader the class loader passed to the Spring application context
     */
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Initialized this spring bootstrap instance using the specified properties and invokes main.
     */
    public void boot(String[] args, Properties properties, ClassLoader classLoader) {
        this.classLoader = classLoader;

        initialize(properties);

        main(args);
    }

    private void main(String[] args) {
        // load the main instance
        Main main = loadMain();

        // start it up
        main.main(args);
    }

    /**
     * Sets the home directory and configuration file using the specified properties.
     *
     * @param properties the properties for initialization
     */
    public void initialize(Properties properties) {
        File homeDirectory = new File(properties.getProperty(Bootstrap.HOME_DIR));
        if (!homeDirectory.isDirectory()) {
            throw new IllegalArgumentException("XBean home directory is not a directory: " + homeDirectory);
        }
        this.homeDirectory = homeDirectory.getAbsolutePath();

        if (configurationFile == null) {
            configurationFile = properties.getProperty(CONFIGURATION_FILE, CONFIGURATION_FILE_DEFAULT);
        }
    }

    public Main loadMain() {
        File homeDirectory = new File(this.homeDirectory);
        if (!homeDirectory.isDirectory()) {
            throw new IllegalArgumentException("XBean home directory is not a directory: " + homeDirectory);
        }

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            // add our property editors into the system
            if (propertyEditorPaths != null && !propertyEditorPaths.isEmpty()) {
                List<String> editorSearchPath = new LinkedList<String>(Arrays.asList(PropertyEditorManager.getEditorSearchPath()));
                editorSearchPath.addAll(propertyEditorPaths);
                PropertyEditorManager.setEditorSearchPath(editorSearchPath.toArray(new String[editorSearchPath.size()]));
            }

            List xmlPreprocessors = new ArrayList();
//            FileSystemRepository repository = new FileSystemRepository(new File(homeDir, "repository"));
//            ClassLoaderXmlPreprocessor classLoaderXmlPreprocessor = new ClassLoaderXmlPreprocessor(repository);
//            xmlPreprocessors.add(classLoaderXmlPreprocessor);

            // load the configuration file
            SpringApplicationContext factory;
            File file = new File(configurationFile);
            if (!file.isAbsolute()) {
                file = new File(homeDirectory, configurationFile);
            }
            if (file.canRead()) {
                try {
                    // configuration file is on the local file system
                    factory = new FileSystemXmlApplicationContext(file.toURL().toString(), xmlPreprocessors);
                } catch (MalformedURLException e) {
                    throw new FatalStartupError("Error creating url for bootstrap file", e);
                }
            } else {
                // assume it is a classpath resource
                factory = new ClassPathXmlApplicationContext(configurationFile, xmlPreprocessors);
            }

            // get the main service from the configuration file
            String[] names = factory.getBeanNamesForType(Main.class);
            Main main = null;
            if (names.length == 0) {
                throw new FatalStartupError("No bean of type: " + Main.class.getName() + " found in the bootstrap file: " + configurationFile, 10);
            }
            main = (Main) factory.getBean(names[0]);
            return main;
        }
        finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }
}
