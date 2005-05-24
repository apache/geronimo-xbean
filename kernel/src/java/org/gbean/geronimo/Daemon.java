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
package org.gbean.geronimo;

import java.beans.PropertyEditorManager;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.JarURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.kernel.config.ConfigurationManager;
import org.apache.geronimo.kernel.config.ConfigurationUtil;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.gbean.kernel.Kernel;
import org.gbean.kernel.ServiceName;
import org.gbean.kernel.runtime.ServiceState;
import org.gbean.loader.Loader;
import org.gbean.repository.BootstrapRepository;
import org.gbean.spring.SpringLoader;

/**
 * @version $Revision$ $Date$
 */
public class Daemon {
    private static final Log log = LogFactory.getLog(Daemon.class);
    private static final String DEFAULT_LOCATION = "Default-Location";
    private static final ObjectName PERSISTENT_CONFIGURATION_LIST_NAME_QUERY = ServiceName.createName("*:j2eeType=PersistentConfigurationList,*");

    public static void main(String[] args) throws Exception {
        // lame hard coded log initialization
        Logger root = Logger.getRootLogger();
        root.addAppender(new ConsoleAppender(new PatternLayout("%d{ABSOLUTE} %-5p [%c{1}] %m%n")));
        root.setLevel(Level.INFO);

        log.info("Server startup begun");

        List editorSearchPath = new LinkedList(Arrays.asList(PropertyEditorManager.getEditorSearchPath()));
        editorSearchPath.add("org.gbean.propertyeditor");
        PropertyEditorManager.setEditorSearchPath((String[]) editorSearchPath.toArray(new String[editorSearchPath.size()]));

        try {
            // Determine the gbean installation directory
            // guess from the location of the jar
            URL url = Daemon.class.getClassLoader().getResource("META-INF/startup-jar");

            String location;
            int configStart;
            File baseDirectory;
            if (url != null) {
                try {
                    JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
                    url = jarConnection.getJarFileURL();

                    URI baseURI = new URI(url.toString()).resolve("..");
                    baseDirectory = new File(baseURI);

                    Manifest manifest;
                    manifest = jarConnection.getManifest();
                    Attributes mainAttributes = manifest.getMainAttributes();
                    String defaultLocation = mainAttributes.getValue(DEFAULT_LOCATION);
                    location = new File(baseDirectory, defaultLocation).getAbsolutePath();
                    configStart = 0;
                } catch (Exception ignored) {
                    log.error("Error while determining the gbean installation directory", ignored);
                    System.err.println("Could not determine gbean installation directory");
                    System.exit(1);
                    throw new AssertionError();
                }
            } else {
                String dir = System.getProperty("gbean.base.dir", System.getProperty("user.dir"));
                baseDirectory = new File(dir);
                location = args[0];
                configStart = 1;
            }
            System.setProperty("gbean.base.dir", baseDirectory.getAbsolutePath());
            System.setProperty("geronimo.base.dir", baseDirectory.getAbsolutePath());


            ClassLoader classLoader = Daemon.class.getClassLoader();

            // create the kernel

            // create a geronimo kernel bridge so services needing a geronio kernel can get one
            final KernelBridge kernelBridge = new KernelBridge("geronimo.server");

            // boot the kernel
            try {
                kernelBridge.boot();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(2);
                throw new AssertionError();
            }

            // Get the gbean kernel
            Kernel kernel = kernelBridge.getKernel();

            // load the bootstrap repositories
            List repositories = BootstrapRepository.loadRepositories(kernel, classLoader);
            for (Iterator iterator = repositories.iterator(); iterator.hasNext();) {
                ObjectName objectName = (ObjectName) iterator.next();
                log.info("Loaded bootstrap repository " + objectName);
            }

            Loader loader = new SpringLoader();
            ObjectName rootConfigurationName = loader.load(kernel, location);
            kernel.startRecursiveService(rootConfigurationName);

            // add our shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread("Shutdown Thread") {
                public void run() {
                    log.info("Server shutdown begun");
                    kernelBridge.shutdown();
                    log.info("Server shutdown completed");
                }
            });

            // get a list of the configuration uris from the command line
            List configs = new ArrayList();
            for (int i = configStart; i < args.length; i++) {
                try {
                    configs.add(new URI(args[i]));
                } catch (URISyntaxException e) {
                    System.err.println("Invalid configuration-id: " + args[i]);
                    e.printStackTrace();
                    System.exit(1);
                    throw new AssertionError();
                }
            }

            if (configs.isEmpty()) {
                // nothing explicit, see what was running before
                Set configLists = kernelBridge.listGBeans(PERSISTENT_CONFIGURATION_LIST_NAME_QUERY);
                for (Iterator i = configLists.iterator(); i.hasNext();) {
                    ObjectName configListName = (ObjectName) i.next();
                    try {
                        configs.addAll((List) kernelBridge.invoke(configListName, "restore"));
                    } catch (IOException e) {
                        System.err.println("Unable to restore last known configurations");
                        e.printStackTrace();
                        kernel.shutdown();
                        System.exit(3);
                        throw new AssertionError();
                    }
                }
            }

            // load the rest of the configurations
            try {
                ConfigurationManager configurationManager = ConfigurationUtil.getConfigurationManager(kernelBridge);
                for (Iterator i = configs.iterator(); i.hasNext();) {
                    URI configID = (URI) i.next();
                    List list = configurationManager.loadRecursive(configID);
                    for (Iterator iterator = list.iterator(); iterator.hasNext();) {
                        ObjectName name = (ObjectName) iterator.next();
                        kernelBridge.startRecursiveGBean(name);
                    }
                }
            } catch (Exception e) {
                System.err.println("Exception caught when starting configurations, starting kernel shutdown");
                e.printStackTrace();
                try {
                    kernel.shutdown();
                } catch (Exception e1) {
                    System.err.println("Exception caught during kernel shutdown");
                    e1.printStackTrace();
                }
                System.exit(3);
                throw new AssertionError();
            }

            // Tell every persistent configuration list that the kernel is now fully started
            Set configLists = kernel.listServices(PERSISTENT_CONFIGURATION_LIST_NAME_QUERY);
            for (Iterator i = configLists.iterator(); i.hasNext();) {
                ObjectName configListName = (ObjectName) i.next();
                kernelBridge.setAttribute(configListName, "kernelFullyStarted", Boolean.TRUE);
            }

            Set allServices = kernel.listServices(ServiceName.createName("*:*"));
            for (Iterator iterator = allServices.iterator(); iterator.hasNext();) {
                ObjectName objectName = (ObjectName) iterator.next();
                int state = kernel.getServiceState(objectName);
                if (state != ServiceState.RUNNING_INDEX) {
                    log.info("Service " + objectName + " is not running. Current state: " + ServiceState.fromIndex(state).getName());
                }
            }
            log.info("Server startup completed");

            // capture this thread until the kernel is ready to exit
            while (kernelBridge.isRunning()) {
                try {
                    synchronized (kernelBridge) {
                        kernelBridge.wait();
                    }
                } catch (InterruptedException e) {
                    // continue
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(3);
            throw new AssertionError();
        }
    }
}
