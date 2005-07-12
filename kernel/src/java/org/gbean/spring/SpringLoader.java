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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.management.ObjectName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gbean.configuration.ConfigurationInfo;
import org.gbean.configuration.ConfigurationUtil;
import org.gbean.configuration.InvalidConfigurationException;
import org.gbean.kernel.Kernel;
import org.gbean.kernel.simple.SimpleServiceFactory;
import org.gbean.loader.Loader;
import org.gbean.metadata.MetadataManager;
import org.gbean.service.ServiceFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * @version $Revision$ $Date$
 */
public class SpringLoader implements Loader {
    private static final Log log = LogFactory.getLog(SpringLoader.class);
    private static final String CONFIG_ID_NAME = "configId";
    private static final String PARENT_ID_NAME = "parentId";
    private static final String DOMAIN_NAME = "domain";
    private static final String SERVER_NAME = "server";

    private Kernel kernel;
    private Collection repositories;
    private MetadataManager metadataManager;
    private List beanFactoryPostProcessors;
    private File baseDir;

    public SpringLoader() {
    }

    public Kernel getKernel() {
        return kernel;
    }

    public void setKernel(Kernel kernel) {
        this.kernel = kernel;
    }

    public Collection getRepositories() {
        return repositories;
    }

    public void setRepositories(Collection repositories) {
        this.repositories = repositories;
    }

    public MetadataManager getMetadataManager() {
        return metadataManager;
    }

    public void setMetadataManager(MetadataManager metadataManager) {
        this.metadataManager = metadataManager;
    }

    public List getBeanFactoryPostProcessors() {
        return beanFactoryPostProcessors;
    }

    public void setBeanFactoryPostProcessors(List beanFactoryPostProcessors) {
        this.beanFactoryPostProcessors = beanFactoryPostProcessors;
    }

    public File getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
    }

    public ObjectName load(String location) {
        String resolvedLocation = resolveLocation(location);
        try {
            ConfigurationInfo configurationInfo = loadConfigurationInfo(resolvedLocation);
            final ClassLoader classLoader = ConfigurationUtil.createClassLoader(configurationInfo.getConfigurationId().toString(),
                    configurationInfo.getDependencies(),
                    getClass().getClassLoader(),
                    repositories);

            DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
            try {
                Resource resource = new FileSystemResource(new File(resolvedLocation + ".xml"));
                XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory) {
                    public ClassLoader getBeanClassLoader() {
                        return classLoader;
                    }
                };
                reader.loadBeanDefinitions(resource);
            } catch (BeansException e) {
                // not a spring file
                return null;
            }

            // create object names for all of the beans
            ObjectNameBuilder objectNameBuilder =  new ObjectNameBuilder(metadataManager,
                    configurationInfo.getDomain(),
                    configurationInfo.getServer(),
                    configurationInfo.getConfigurationId().toString());
            objectNameBuilder.postProcessBeanFactory(factory);

            Map serviceFactories = new LinkedHashMap();
            String[] beanDefinitionNames = factory.getBeanDefinitionNames();
            if (beanDefinitionNames != null) {
                for (int i = 0; i < beanDefinitionNames.length; i++) {
                    String beanName = beanDefinitionNames[i];
                    BeanDefinition beanDefinition = factory.getBeanDefinition(beanName);
                    ServiceFactory serviceFactory = new SpringServiceFactory((RootBeanDefinition) beanDefinition, objectNameBuilder.getObjectNameMap(), beanFactoryPostProcessors);

                    ObjectName objectName = objectNameBuilder.getObjectName(beanName);
                    serviceFactories.put(objectName, serviceFactory);
                }
            }
            SpringConfiguration springConfiguration = new SpringConfiguration(kernel, configurationInfo, serviceFactories, classLoader);
            SimpleServiceFactory springServiceFactory = new SimpleServiceFactory(springConfiguration);
            ObjectName configurationObjectName = springConfiguration.getObjectName();
            kernel.loadService(configurationObjectName, springServiceFactory, classLoader);
            return configurationObjectName;
        } catch (InvalidConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidConfigurationException("Unable to load configuration: " + location, e);
        }
    }

    private String resolveLocation(String location) {
        location = location.replace('/', '.');
        return baseDir.toURI().resolve(location).getPath();
    }

    private ConfigurationInfo loadConfigurationInfo(String location) {
        ConfigurationInfo configurationInfo = new ConfigurationInfo();

        String gbeanLocation = location + "-gbean.xml";

        File file = new File(gbeanLocation);
        if (!file.canRead()) {
            configurationInfo.setConfigurationId(createURI(CONFIG_ID_NAME, location));
            return configurationInfo;
        }

        InputStream in = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new SimpleSaxErrorHandler(log));
            in = new FileInputStream(file);
            Document document = builder.parse(in);
            loadGBeanConfiguration(configurationInfo, location, document);
        } catch (ParserConfigurationException e) {
            throw new InvalidConfigurationException("Parser configuration exception parsing XML from " + gbeanLocation, e);
        } catch (SAXParseException e) {
            throw new InvalidConfigurationException("Line " + e.getLineNumber() + " in XML document from " + gbeanLocation + " is invalid", e);
        } catch (SAXException e) {
            throw new InvalidConfigurationException("XML document from " + gbeanLocation + " is invalid", e);
        } catch (IOException ex) {
            throw new InvalidConfigurationException("IOException parsing XML document from " + gbeanLocation, ex);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    log.warn("Could not close InputStream", ex);
                }
            }
        }
        return configurationInfo;
    }

    private void loadGBeanConfiguration(ConfigurationInfo configurationInfo, String location, Document document) {
        Element root = document.getDocumentElement();

        // get the configuration id... if not specified use the location name
        String configurationIdString = root.getAttribute(CONFIG_ID_NAME);
        URI configurationId;
        if (configurationIdString.length() > 0) {
            configurationId = createURI(CONFIG_ID_NAME, configurationIdString);
        } else {
            configurationId = createURI(CONFIG_ID_NAME, location);
        }
        configurationInfo.setConfigurationId(configurationId);

        // set the parent id if specified
        String parentIdString = root.getAttribute(PARENT_ID_NAME);
        URI parentId;
        if (parentIdString.length() > 0) {
            parentId = createURI(PARENT_ID_NAME, parentIdString);
            configurationInfo.setParentId(parentId);
        }

        // set domain name if specifiec
        String domain = root.getAttribute(DOMAIN_NAME);
        if (domain.length() > 0) {
            configurationInfo.setDomain(domain);
        }

        // set server name if specified
        String server = root.getAttribute(SERVER_NAME);
        if (server.length() > 0) {
            configurationInfo.setServer(server);
        }

        // add the dependencies
        // todo add support for more complex maven style dependencies
        NodeList dependencies = root.getElementsByTagName("dependency");
        for (int i = 0; i < dependencies.getLength(); i++) {
            Element dependencyElement = (Element) dependencies.item(i);

            Element uriElement = (Element) dependencyElement.getElementsByTagName("uri").item(0);
            if (uriElement == null) {
                throw new RuntimeException("Excpected one uri element in dependency");
            }

            String uriString = ((Text) uriElement.getFirstChild()).getData().trim();
            URI dependency = createURI("dependency uri", uriString);
            configurationInfo.addDependency(dependency);
        }
    }

    private static URI createURI(String name, String uri) {
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new InvalidConfigurationException("Unvalid uri specified for " + name, e);
        }
    }

    private static class SimpleSaxErrorHandler implements ErrorHandler {
        private final Log log;

        public SimpleSaxErrorHandler(Log logger) {
            this.log = logger;
        }

        public void warning(SAXParseException e) throws SAXException {
            log.warn("Ignored XML validation warning: " + e.getMessage(), e);
        }

        public void error(SAXParseException e) throws SAXException {
            throw e;
        }

        public void fatalError(SAXParseException e) throws SAXException {
            throw e;
        }
    }
}
