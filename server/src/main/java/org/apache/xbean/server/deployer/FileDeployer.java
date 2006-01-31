/**
 *
 * Copyright 2005-2006 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.xbean.server.deployer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xbean.kernel.Kernel;
import org.apache.xbean.kernel.ServiceAlreadyExistsException;
import org.apache.xbean.kernel.ServiceFactory;
import org.apache.xbean.kernel.ServiceRegistrationException;
import org.apache.xbean.kernel.StringServiceName;
import org.apache.xbean.server.classloader.NamedClassLoader;
import org.apache.xbean.server.spring.configuration.SpringConfigurationServiceFactory;
import org.apache.xbean.spring.context.ResourceXmlApplicationContext;
import org.apache.xbean.spring.context.SpringApplicationContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * A service which auto-deploys services within a recursive file system.
 * 
 * @org.apache.xbean.XBean namespace="http://xbean.apache.org/schemas/server"
 *                         element="deployer" description="Deploys services in a
 *                         file system"
 * @version $Revision$
 */
public class FileDeployer implements Runnable, InitializingBean {

    private static final Log log = LogFactory.getLog(FileDeployer.class);

    private File directory;
    private Kernel kernel;
    private ClassLoader classLoader;
    private boolean verbose;
    private String[] jarDirectoryNames = { "lib", "classes" };

    public void afterPropertiesSet() throws Exception {
        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }
        if (directory == null) {
            log.warn("No directory specified so using current directory");
            directory = new File(".");
        }
        directory = directory.getAbsoluteFile();
        log.info("Starting to load components from: " + directory);
        System.out.println("Starting to load components from: " + directory);

        // lets load the deployment
        processDirectory(classLoader, "", directory);
    }

    public void run() {
        try {
            processDirectory(classLoader, "", directory);
        }
        catch (Exception e) {
            log.error("Failed to deploy services: " + e, e);
        }
    }

    // Properties
    // -------------------------------------------------------------------------
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public File getDirectory() {
        return directory;
    }

    public void setDirectory(File directory) {
        this.directory = directory;
    }

    public Kernel getKernel() {
        return kernel;
    }

    public void setKernel(Kernel kernel) {
        this.kernel = kernel;
    }

    public boolean isVerbose() {
        return verbose;
    }

    /**
     * Allows verbose logging to show what classpaths are being created
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public String[] getJarDirectoryNames() {
        return jarDirectoryNames;
    }

    /**
     * Sets the names of the directories to be treated as folders of jars or
     * class loader files. Defaults to "lib", "classes". If you wish to disable
     * the use of lib and classes as being special folders containing jars or
     * config files then just set this property to null or an empty array.
     */
    public void setJarDirectoryNames(String[] jarDirectoryNames) {
        this.jarDirectoryNames = jarDirectoryNames;
    }

    // Implementation methods
    // -------------------------------------------------------------------------
    protected void processDirectory(ClassLoader classLoader, String parentName, File directory) throws ServiceAlreadyExistsException,
            ServiceRegistrationException, BeansException, IOException {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        // lets create a new classloader...
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (isClassLoaderDirectory(file)) {
                classLoader = createChildClassLoader(parentName, file, classLoader);
            }
        }

        // now lets recurse through files or directories
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (isClassLoaderDirectory(file)) {
                continue;
            }
            String name = getChildName(parentName, file);
            if (file.isDirectory()) {
                processDirectory(classLoader, name, file);
            }
            else {
                createServiceForFile(name, file, classLoader);
            }
        }
    }

    protected ClassLoader createChildClassLoader(String name, File dir, ClassLoader parentClassLoader) throws MalformedURLException {
        List urls = new ArrayList();
        if (verbose) {
            try {
                System.out.println("Adding to classpath: " + dir.getCanonicalPath());
            }
            catch (Exception e) {
            }
        }
        File[] files = dir.listFiles();
        if (files != null) {
            for (int j = 0; j < files.length; j++) {
                if (files[j].getName().endsWith(".zip") || files[j].getName().endsWith(".jar")) {
                    if (verbose) {
                        try {
                            System.out.println("Adding to classpath: " + name + " jar: " + files[j].getCanonicalPath());
                        }
                        catch (Exception e) {
                        }
                    }
                    urls.add(files[j].toURL());
                }
            }
        }
        URL u[] = new URL[urls.size()];
        urls.toArray(u);
        return new NamedClassLoader(name + ".ClassLoader", u, parentClassLoader);
    }

    protected boolean isClassLoaderDirectory(File file) {
        if (jarDirectoryNames != null) {
            for (int i = 0; i < jarDirectoryNames.length; i++) {
                String name = jarDirectoryNames[i];
                if (file.getName().equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void createServiceForFile(String name, File file, ClassLoader classLoader) throws ServiceAlreadyExistsException, ServiceRegistrationException,
            BeansException, IOException {
        String fileName = file.getName();
        ServiceFactory serviceFactory = null;
        if (fileName.equalsIgnoreCase("spring.xml") || fileName.equalsIgnoreCase("xbean.xml")) {
            serviceFactory = createSpringService(name, file, classLoader);
        }
        else {
            log.info("Ignoring file: " + fileName + " in directory: " + file.getParent());
        }
        if (serviceFactory != null) {
            log.info("Registering spring services service: " + name + " from: " + file.getAbsolutePath() + " into the Kernel");
            kernel.registerService(new StringServiceName(name), serviceFactory, classLoader);
        }
    }

    protected ServiceFactory createSpringService(String name, File file, ClassLoader classLoader) throws BeansException, IOException {
        log.info("Loading spring config file: " + file);

        // we have to set the context class loader while loading the spring file
        // so we can auto-discover xbean configurations
        // and perform introspection
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            SpringApplicationContext applicationContext = new ResourceXmlApplicationContext(new FileSystemResource(file));
            return new SpringConfigurationServiceFactory(applicationContext);
        }
        finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    protected String getChildName(String parentName, File file) {
        StringBuffer buffer = new StringBuffer(parentName);
        if (parentName.length() > 0) {
            buffer.append("/");
        }
        buffer.append(file.getName());
        return buffer.toString();
    }

}
