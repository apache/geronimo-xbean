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
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * A service which auto-deploys services within a recursive file system.
 * 
 * @org.apache.xbean.XBean namespace="http://xbean.apache.org/schemas/server"
 *                         element="file-deployer" description="Deploys services in a file system"
 * @version $Revision$
 */
public class FileDeployer implements Runnable, InitializingBean, ApplicationContextAware {

    private static final Log log = LogFactory.getLog(FileDeployer.class);

    private File baseDir;
    private Kernel kernel;
    private ClassLoader classLoader;
    private boolean verbose;
    private String[] jarDirectoryNames = { "lib", "classes" };
    private List beanFactoryPostProcessors = Collections.EMPTY_LIST;
    private List xmlPreprocessors = Collections.EMPTY_LIST;
    private ApplicationContext applicationContext;
    private boolean showIgnoredFiles;

    public void afterPropertiesSet() throws Exception {
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }
        if (baseDir == null) {
            log.warn("No directory specified so using current directory");
            baseDir = new File(".");
        }
        baseDir = baseDir.getAbsoluteFile();
        log.info("Starting to load components from: " + baseDir);

        // lets load the deployment
        processDirectory("", classLoader, applicationContext, baseDir);

        log.info("Loading completed");
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void run() {
        try {
            String name = "";
            if (applicationContext != null) {
                name = applicationContext.getDisplayName();
            }
            processDirectory(name, classLoader, applicationContext, baseDir);
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

    /**
     * Sets the kernel in which configurations are loaded.
     * 
     * @param kernel
     *            the kernel in which configurations are loaded
     */
    public void setKernel(Kernel kernel) {
        this.kernel = kernel;
    }

    /**
     * Gets the base directory from which configuration locations are resolved.
     * 
     * @return the base directory from which configuration locations are
     *         resolved
     */
    public File getBaseDir() {
        return baseDir;
    }

    /**
     * Sets the base directory from which configuration locations are resolved.
     * 
     * @param baseDir
     *            the base directory from which configuration locations are
     *            resolved
     */
    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Gets the SpringXmlPreprocessors applied to the configuration.
     * 
     * @return the SpringXmlPreprocessors applied to the configuration
     */
    public List getXmlPreprocessors() {
        return xmlPreprocessors;
    }

    /**
     * Sets the SpringXmlPreprocessors applied to the configuration.
     * 
     * @param xmlPreprocessors
     *            the SpringXmlPreprocessors applied to the configuration
     */
    public void setXmlPreprocessors(List xmlPreprocessors) {
        this.xmlPreprocessors = xmlPreprocessors;
    }

    /**
     * Gets the BeanFactoryPostProcessors to apply to the configuration.
     * 
     * @return the BeanFactoryPostProcessors to apply to the configuration
     */
    public List getBeanFactoryPostProcessors() {
        return beanFactoryPostProcessors;
    }

    /**
     * Sets the BeanFactoryPostProcessors to apply to the configuration.
     * 
     * @param beanFactoryPostProcessors
     *            the BeanFactoryPostProcessors to apply to the configuration
     */
    public void setBeanFactoryPostProcessors(List beanFactoryPostProcessors) {
        this.beanFactoryPostProcessors = beanFactoryPostProcessors;
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

    public boolean isShowIgnoredFiles() {
        return showIgnoredFiles;
    }

    /**
     * Sets whether or not ignored files should be logged as they are
     * encountered. This can sometimes be useful to catch typeos.
     */
    public void setShowIgnoredFiles(boolean showIgnoredFiles) {
        this.showIgnoredFiles = showIgnoredFiles;
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
    protected void processDirectory(String parentName, ClassLoader classLoader, ApplicationContext parentContext, File directory)
            throws ServiceAlreadyExistsException, ServiceRegistrationException, BeansException, IOException {
        log.debug("Processing directory: " + directory);
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        // lets create a new classloader...
        Properties properties = new Properties();
        Map fileMap = new LinkedHashMap();
        Map directoryMap = new LinkedHashMap();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (isClassLoaderDirectory(file)) {
                classLoader = createChildClassLoader(parentName, file, classLoader);
                log.debug("Created class loader: " + classLoader);
            }
            else if (isXBeansPropertyFile(file)) {
                properties.load(new FileInputStream(file));
            }
            else {
                if (file.isDirectory()) {
                    directoryMap.put(file.getName(), file);
                }
                else {
                    fileMap.put(file.getName(), file);
                }
            }
        }

        String[] names = getFileNameOrder(properties);

        // Lets process the files first

        // process ordered files first in order
        for (int i = 0; i < names.length; i++) {
            String orderName = names[i];
            File file = (File) fileMap.remove(orderName);
            if (file != null) {
                String name = getChildName(parentName, file);
                createServiceForFile(name, file, classLoader, parentContext);
            }
        }

        // now lets process whats left
        for (Iterator iter = fileMap.values().iterator(); iter.hasNext();) {
            File file = (File) iter.next();
            String name = getChildName(parentName, file);
            createServiceForFile(name, file, classLoader, parentContext);
        }

        // now lets process the child directories

        // process ordered files first in order
        for (int i = 0; i < names.length; i++) {
            String orderName = names[i];
            File file = (File) directoryMap.remove(orderName);
            if (file != null) {
                String name = getChildName(parentName, file);
                processDirectory(name, classLoader, parentContext, file);
            }
        }

        // now lets process whats left
        for (Iterator iter = directoryMap.values().iterator(); iter.hasNext();) {
            File file = (File) iter.next();
            String name = getChildName(parentName, file);
            processDirectory(name, classLoader, parentContext, file);
        }
    }

    protected ClassLoader createChildClassLoader(String name, File dir, ClassLoader parentClassLoader) throws MalformedURLException {
        List urls = new ArrayList();
        if (verbose) {
            try {
                log.info("Adding to classpath: " + dir.getCanonicalPath());
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
                            log.info("Adding to classpath: " + name + " jar: " + files[j].getCanonicalPath());
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

    protected void createServiceForFile(String name, File file, ClassLoader classLoader, ApplicationContext parentContext)
            throws ServiceAlreadyExistsException, ServiceRegistrationException, BeansException, IOException {
        if (isSpringConfigFile(file)) {
            // make the current directory available to spring files
            System.setProperty("xbean.current.dir", file.getAbsolutePath());

            // we have to set the context class loader while loading the spring
            // file
            // so we can auto-discover xbean configurations
            // and perform introspection
            ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);
            log.debug("Loading file: " + file + " using classLoader: " + classLoader);
            try {
                SpringApplicationContext applicationContext = new ResourceXmlApplicationContext(new FileSystemResource(file), xmlPreprocessors, parentContext);
                applicationContext.setDisplayName(name);
                addBeanPostProcessors(applicationContext);

                ServiceFactory serviceFactory = new SpringConfigurationServiceFactory(applicationContext);

                log.info("Registering spring services service: " + name + " from: " + file.getAbsolutePath() + " into the Kernel");

                // TODO should we use the classLaoder we used to load the
                // context
                // or, if the XBean config file defined a new classloader,
                // should we use the one
                // from the ApplicationContext?
                // classLoader = applicationContext.getClassLoader();

                kernel.registerService(new StringServiceName(name), serviceFactory, classLoader);
            }
            finally {
                Thread.currentThread().setContextClassLoader(oldClassLoader);
            }
        }
        else {
            if (showIgnoredFiles) {
                log.info("Ignoring file: " + file.getName() + " in directory: " + file.getParent());
            }
        }
    }

    protected void addBeanPostProcessors(SpringApplicationContext applicationContext) {
        for (Iterator iter = beanFactoryPostProcessors.iterator(); iter.hasNext();) {
            BeanFactoryPostProcessor postProcessor = (BeanFactoryPostProcessor) iter.next();
            applicationContext.addBeanFactoryPostProcessor(postProcessor);
        }
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

    protected boolean isSpringConfigFile(File file) {
        String fileName = file.getName();
        return fileName.endsWith("spring.xml") || fileName.endsWith("xbean.xml");
    }

    private boolean isXBeansPropertyFile(File file) {
        String fileName = file.getName();
        return fileName.equalsIgnoreCase("xbean.properties");
    }

    /**
     * Extracts the file names from the properties file for the order in which
     * things should be deployed
     */
    protected String[] getFileNameOrder(Properties properties) {
        String order = properties.getProperty("order", "");
        List list = new ArrayList();
        StringTokenizer iter = new StringTokenizer(order, ",");
        while (iter.hasMoreTokens()) {
            list.add(iter.nextToken());
        }
        String[] answer = new String[list.size()];
        list.toArray(answer);
        return answer;
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
