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
package org.gbean.spring;

import java.beans.PropertyEditorManager;
import java.io.File;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.gbean.kernel.Main;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

/**
 * @version $Revision$ $Date$
 */
public class SpringBootstrap {
    private static final String GBEAN_BOOTSTRAP_MANIFEST = "GBean-Bootstrap";
    private static final String BOOTSTRAP_FLAG = "--bootstrap";
    private static final String DEFAULT_BOOTSTRAP = "META-INF/gbean-bootstrap.xml";

    public static void main(String[] args) {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(SpringBootstrap.class.getClassLoader());
        try {
            // lame hard coded log initialization
            Logger root = Logger.getRootLogger();
            root.addAppender(new ConsoleAppender(new PatternLayout("%d{ABSOLUTE} %-5p [%c{1}] %m%n")));
            root.setLevel(Level.INFO);

            // add our property editors into the system
            List editorSearchPath = new LinkedList(Arrays.asList(PropertyEditorManager.getEditorSearchPath()));
            editorSearchPath.add("org.gbean.propertyeditor");
            PropertyEditorManager.setEditorSearchPath((String[]) editorSearchPath.toArray(new String[editorSearchPath.size()]));

            // check if bootstrap configuration was specified on the command line
            String gbeanBootstrap = null;
            if (args.length > 1 && BOOTSTRAP_FLAG.equals(args[0])) {
                gbeanBootstrap = args[1];
            }

            // Determine the gbean installation directory
            // guess from the location of the jar
            File baseDirectory;
            URL url = SpringBootstrap.class.getClassLoader().getResource("META-INF/startup-jar");
            if (url != null) {
                try {
                    JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
                    url = jarConnection.getJarFileURL();

                    URI baseURI = new URI(url.toString()).resolve("..");
                    baseDirectory = new File(baseURI);

                    Manifest manifest;
                    manifest = jarConnection.getManifest();
                    Attributes mainAttributes = manifest.getMainAttributes();
                    if (gbeanBootstrap == null) {
                        gbeanBootstrap = mainAttributes.getValue(GBEAN_BOOTSTRAP_MANIFEST);
                    }
                } catch (Exception e) {
                    System.err.println("Could not determine gbean installation directory");
                    e.printStackTrace();
                    System.exit(9);
                    return;
                }
            } else {
                String dir = System.getProperty("gbean.base.dir", System.getProperty("user.dir"));
                baseDirectory = new File(dir);
            }
            System.setProperty("gbean.base.dir", baseDirectory.getAbsolutePath());

            if (gbeanBootstrap == null) {
                gbeanBootstrap = DEFAULT_BOOTSTRAP;
            }

            // check if bootstrap is a file
            BeanFactory factory = createBeanFactory(baseDirectory, gbeanBootstrap);
            Main main = (Main) factory.getBean("Main");

            // start it up
            try {
                main.main(args);
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
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private static BeanFactory createBeanFactory(File baseDirectory, String gbeanBootstrap) {
        // check if bootstrap file is on the local file system first
        URI uri = baseDirectory.toURI().resolve(gbeanBootstrap);
        File bootstrapFile = new File(uri);
        if (bootstrapFile.canRead()) {
            try {
                return new GBeanXmlApplicationContext(bootstrapFile.toURL().toString());
            } catch (MalformedURLException e) {
                throw new FatalBeanException("Error creating url for bootstrap file", e);
            }
        }

        // assume it is a classpath resource
        return new GBeanXmlApplicationContext("classpath:" + gbeanBootstrap);
    }

    public static class GBeanXmlApplicationContext extends AbstractXmlApplicationContext {
        private String[] configLocations;

        public GBeanXmlApplicationContext(String configLocation) throws BeansException {
            this.configLocations = new String[]{configLocation};
            refresh();
        }

        public GBeanXmlApplicationContext(String configLocation, ApplicationContext parent) throws BeansException {
            super(parent);
            this.configLocations = new String[]{configLocation};
            refresh();
        }

        protected String[] getConfigLocations() {
            return this.configLocations;
        }
    }
}
