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
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.xbean.server.main.FatalStartupError;
import org.apache.xbean.server.main.Main;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.apache.xbean.spring.context.FileSystemXmlApplicationContext;
import org.apache.xbean.spring.context.SpringApplicationContext;

/**
 * SpringBootstrap is the main class used by a Spring based server.  This class uses the following strategies to determine
 * the configuration file to load:
 *
 * Command line parameter --bootstrap FILE
 * Manifest entry XBean-Bootstrap in the startup jar
 * META-INF/xbean-bootstrap.xml
 *
 * This class atempts to first load the configuration file from the local file system and if that fails it attempts to
 * load it from the classpath.
 *
 * SpringBootstrap expects the configuration to contain a service with the id "main" which is an implementation of
 * org.apache.xbean.server.main.Main.
 *
 * This class will set the system property xbean.base.dir to the directory containing the startup jar if the property
 * has not alredy been set (on the command line).
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class SpringBootstrap {
    private static final String XBEAN_BOOTSTRAP_MANIFEST = "XBean-Bootstrap";
    private static final String BOOTSTRAP_FLAG = "--bootstrap";
    private static final String DEFAULT_BOOTSTRAP = "META-INF/xbean-bootstrap.xml";
    private static final List DEFAULT_PROPERTY_EDITOR_PATHS = Collections.singletonList("org.apache.xbean.server.propertyeditor");

    private String configurationFile;
    private String[] mainArguments;
    private List propertyEditorPaths = DEFAULT_PROPERTY_EDITOR_PATHS;
    private String serverBaseDirectory;

    /**
     * Initializes and boots the server using the supplied arguments.  If an error is thrown from the boot method,
     * this method will pring the error to standard error along with the stack trace and exit with the exit specified
     * in the FatalStartupError or exit code 9 if the error was not a FatalStartupError.
     * @param args the arguments used to start the server
     */
    public static void main(String[] args) {
        SpringBootstrap springBootstrap = new SpringBootstrap();
        main(args, springBootstrap);
    }

    /**
     * Like the main(args) method but allows a configured bootstrap instance to be passed in.
     * 
     * @see #main(String[])
     */
    public static void main(String[] args, SpringBootstrap springBootstrap) {
        springBootstrap.initialize(args);

        try {
            springBootstrap.boot();
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
     * Gets the configuration file from which the main instance is loaded.
     * @return the configuration file from which the main instance is loaded
     */
    public String getConfigurationFile() {
        return configurationFile;
    }

    /**
     * Sets the configuration file from which the main instance is loaded.
     * @param configurationFile the configuration file from which the main instance is loaded
     */
    public void setConfigurationFile(String configurationFile) {
        this.configurationFile = configurationFile;
    }

    /**
     * Gets the arguments passed to the main instance.
     * @return the arguments passed to the main instance
     */
    public String[] getMainArguments() {
        return mainArguments;
    }

    /**
     * Sets the arguments passed to the main instance.
     * @param mainArguments the arguments passed to the main instance
     */
    public void setMainArguments(String[] mainArguments) {
        this.mainArguments = mainArguments;
    }

    /**
     * Gets the paths that are appended to the system property editors search path.
     * @return the paths that are appended to the system property editors search path
     */
    public List getPropertyEditorPaths() {
        return propertyEditorPaths;
    }

    /**
     * Sets the paths that are appended to the system property editors search path.
     * @param propertyEditorPaths the paths that are appended to the system property editors search path
     */
    public void setPropertyEditorPaths(List propertyEditorPaths) {
        this.propertyEditorPaths = propertyEditorPaths;
    }

    /**
     * Gets the base directory of the server.
     * @return the base directory of the server
     */
    public String getServerBaseDirectory() {
        return serverBaseDirectory;
    }

    /**
     * Sets the base directory of the server.
     * @param serverBaseDirectory the base directory of the server
     */
    public void setServerBaseDirectory(String serverBaseDirectory) {
        this.serverBaseDirectory = serverBaseDirectory;
    }

    /**
     * Determines the configuration file and server base directory.
     * @param args the arguments passed to main
     */
    public void initialize(String[] args) {
        // check if bootstrap configuration was specified on the command line
        if (args.length > 1 && BOOTSTRAP_FLAG.equals(args[0])) {
            configurationFile = args[1];
            this.mainArguments = new String[args.length - 2];
            System.arraycopy(args, 2, this.mainArguments, 0, args.length);
        } else {
            if (configurationFile == null) {
                configurationFile = DEFAULT_BOOTSTRAP;
            }
            this.mainArguments = args;
        }

        // Determine the xbean installation directory
        // guess from the location of the jar
        URL url = SpringBootstrap.class.getClassLoader().getResource("META-INF/startup-jar");
        if (url != null) {
            try {
                JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
                url = jarConnection.getJarFileURL();

                if (serverBaseDirectory == null) {
                    URI baseURI = new URI(url.toString()).resolve("..");
                    serverBaseDirectory = new File(baseURI).getAbsolutePath();
                }

                Manifest manifest;
                manifest = jarConnection.getManifest();
                Attributes mainAttributes = manifest.getMainAttributes();
                if (configurationFile == null) {
                    configurationFile = mainAttributes.getValue(XBEAN_BOOTSTRAP_MANIFEST);
                }
            } catch (Exception e) {
                System.err.println("Could not determine xbean installation directory");
                e.printStackTrace();
                System.exit(9);
                return;
            }
        } else {
            if (serverBaseDirectory == null) {
                String dir = System.getProperty("xbean.base.dir", System.getProperty("user.dir"));
                serverBaseDirectory = new File(dir).getAbsolutePath();
            }
        }
    }

    /**
     * Loads the main instance from the configuration file.
     * @return the main instance
     */
    public Main loadMain() {
        if (serverBaseDirectory == null) {
            throw new NullPointerException("serverBaseDirectory is null");

        }
        File baseDirectory = new File(serverBaseDirectory);
        if (!baseDirectory.isDirectory()) {
            throw new IllegalArgumentException("serverBaseDirectory is not a directory: " + serverBaseDirectory);

        }
        if (configurationFile == null) {
            throw new NullPointerException("configurationFile is null");

        }

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(SpringBootstrap.class.getClassLoader());
        try {
            // add our property editors into the system
            if (propertyEditorPaths != null && !propertyEditorPaths.isEmpty()) {
                List editorSearchPath = new LinkedList(Arrays.asList(PropertyEditorManager.getEditorSearchPath()));
                editorSearchPath.addAll(propertyEditorPaths);
                PropertyEditorManager.setEditorSearchPath((String[]) editorSearchPath.toArray(new String[editorSearchPath.size()]));
            }

            // set the server base directory system property
            System.setProperty("xbean.base.dir", baseDirectory.getAbsolutePath());

            // load the configuration file
            SpringApplicationContext factory;
            File file = new File(configurationFile);
            if (!file.isAbsolute()) {
                file = new File(baseDirectory, configurationFile);
            }
            if (file.canRead()) {
                try {
                    // configuration file is on the local file system
                    factory = new FileSystemXmlApplicationContext(file.toURL().toString());
                } catch (MalformedURLException e) {
                    throw new FatalStartupError("Error creating url for bootstrap file", e);
                }
            } else {
                // assume it is a classpath resource
                factory = new ClassPathXmlApplicationContext(configurationFile);
            }

            // get the main service from the configuration file
            String[] names = factory.getBeanNamesForType(Main.class);
            Main main = null;
            if (names.length == 0) {
                throw new FatalStartupError("No bean of type: " + Main.class.getName() + " found in the bootstrap.xml", 10);
            }
            main = (Main) factory.getBean(names[0]);
            return main;
        }
        finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    /**
     * Loads the main instance from the Spring configuration file and executes it.
     */
    public void boot() {
        // load the main instance
        Main main = loadMain();

        // start it up
        main.main(mainArguments);

    }
}
