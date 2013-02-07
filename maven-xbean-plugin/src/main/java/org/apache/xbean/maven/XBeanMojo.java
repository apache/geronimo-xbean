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
package org.apache.xbean.maven;

import java.beans.PropertyEditorManager;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.tools.ant.BuildException;
import org.apache.xbean.spring.generator.DocumentationGenerator;
import org.apache.xbean.spring.generator.GeneratorPlugin;
import org.apache.xbean.spring.generator.LogFacade;
import org.apache.xbean.spring.generator.MappingLoader;
import org.apache.xbean.spring.generator.NamespaceMapping;
import org.apache.xbean.spring.generator.QdoxMappingLoader;
import org.apache.xbean.spring.generator.WikiDocumentationGenerator;
import org.apache.xbean.spring.generator.XmlMetadataGenerator;
import org.apache.xbean.spring.generator.XsdGenerator;

/**
 * @author <a href="gnodet@apache.org">Guillaume Nodet</a>
 * @version $Id: GenerateApplicationXmlMojo.java 314956 2005-10-12 16:27:15Z brett $
 * @goal mapping
 * @description Creates xbean mapping file
 * @phase generate-sources
 * @requiresDependencyResolution compile
 */
public class XBeanMojo extends AbstractMojo implements LogFacade {

    /**
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

    /**
     * Maven ProjectHelper
     *
     * @component
     */
    protected MavenProjectHelper projectHelper;

    /**
     * @parameter
     * @required
     */
    private String namespace;

    /**
     * @parameter expression="${basedir}/src/main/java"
     * @required
     */
    private File srcDir;

    /**
     * @parameter
     */
    private List<String> includes;

    /**
     * @parameter
     */
    private List<String> classPathIncludes;

    /**
     * @parameter
     */
    private String excludedClasses;

    /**
     * @parameter expression="${basedir}/target/xbean/"
     * @required
     */
    private File outputDir;

    /**
     * @parameter
     */
    private File schema;

    /**
     * @parameter expression="org.apache.xbean.spring.context.impl"
     */
    private String propertyEditorPaths;

    /**
     * @parameter schemaAsArtifact
     */
    private boolean schemaAsArtifact = true;
    
    /**
     * @parameter 
     */
    private boolean generateSpringSchemasFile = true;

    /**
     * @parameter 
     */
    private boolean generateSpringHandlersFile = true;

    /**
     * @parameter
     */
    private boolean strictXsdOrder = true;

    /**
     * A list of additional GeneratorPlugins that should get used executed
     * when generating output.
     *
     * @parameter
     */
    private List<GeneratorPlugin> generatorPlugins = Collections.emptyList();

    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().debug( " ======= XBeanMojo settings =======" );
        getLog().debug( "namespace[" + namespace + "]" );
        getLog().debug( "srcDir[" + srcDir + "]" );
        getLog().debug( "schema[" + schema + "]" );
        getLog().debug( "excludedClasses[" + excludedClasses + "]");
        getLog().debug( "outputDir[" + outputDir + "]" );
        getLog().debug( "propertyEditorPaths[" + propertyEditorPaths + "]" );
        getLog().debug( "schemaAsArtifact[" + schemaAsArtifact + "]");
        getLog().debug( "generateSpringSchemasFile[" + generateSpringSchemasFile + "]");
        getLog().debug( "generateSpringHandlersFile[" + generateSpringHandlersFile + "]");
        
        if (schema == null) {
            schema = new File(outputDir, project.getArtifactId() + ".xsd");
        }

        if (propertyEditorPaths != null) {
            List<String> editorSearchPath = new LinkedList<String>(Arrays.asList(PropertyEditorManager.getEditorSearchPath()));
            for (StringTokenizer paths = new StringTokenizer(propertyEditorPaths, " ,"); paths.hasMoreElements(); ) {
                //StringTokenizer implements Enumeration<Object>, not Enumeration<String> !!
                editorSearchPath.add((String) paths.nextElement());
            }
            PropertyEditorManager.setEditorSearchPath( editorSearchPath.toArray(new String[editorSearchPath.size()]));
        }

        ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            schema.getParentFile().mkdirs();

            String[] excludedClasses = null;
            if (this.excludedClasses != null) {
                excludedClasses = this.excludedClasses.split(" *, *");
            }
            Set<Artifact> dependencies = project.getDependencyArtifacts();
            List<File> sourceJars = new ArrayList<File>();
            sourceJars.add(srcDir);
            if( includes !=null ) {
                for (String src : includes) {
                    sourceJars.add(new File(src));
                }
            }
            for (Artifact dependency : dependencies) {
                if ("sources".equals(dependency.getClassifier())) {
                    File file = dependency.getFile();
                    sourceJars.add(file);
                }
            }
            File[] srcJars = sourceJars.toArray(new File[sourceJars.size()]);
            MappingLoader mappingLoader = new QdoxMappingLoader(namespace, srcJars, excludedClasses);
            GeneratorPlugin[] plugins = new GeneratorPlugin[]{
                new XmlMetadataGenerator(outputDir.getAbsolutePath(), schema, generateSpringSchemasFile, generateSpringHandlersFile),
                new DocumentationGenerator(schema),
                new XsdGenerator(schema, strictXsdOrder),
                new WikiDocumentationGenerator(schema),
            };

            // load the mappings
            Thread.currentThread().setContextClassLoader(getClassLoader());
            Set<NamespaceMapping> namespaces = mappingLoader.loadNamespaces();
            if (namespaces.isEmpty()) {
                System.out.println("Warning: no namespaces found!");
            }

            // generate the files
            for (NamespaceMapping namespaceMapping : namespaces) {
                for (GeneratorPlugin plugin : plugins) {
                    plugin.setLog(this);
                    plugin.generate(namespaceMapping);
                }
                for (GeneratorPlugin plugin : generatorPlugins) {
                    plugin.setLog(this);
                    plugin.generate(namespaceMapping);
                }
            }

            // Attach them as artifacts
            if (schemaAsArtifact) {
                projectHelper.attachArtifact(project, "xsd", null, schema);
                projectHelper.attachArtifact(project, "html", "schema", new File(schema.getAbsolutePath() + ".html"));
            }

            Resource res = new Resource();
            res.setDirectory(outputDir.toString());
            project.addResource(res);

            log("...done.");
        } catch (Exception e) {
            throw new BuildException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCL);
        }
    }

    public void log(String message) {
        getLog().info(message);
    }

    public void log(String message, int level) {
        getLog().info(message);
    }

    protected URLClassLoader getClassLoader() throws MojoExecutionException {
        try {
            Set<URL> urls = new HashSet<URL>();

            URL mainClasses = new File(project.getBuild().getOutputDirectory())
                    .toURI().toURL();
            getLog().debug("Adding to classpath : " + mainClasses);
            urls.add(mainClasses);

            URL testClasses = new File(project.getBuild()
                    .getTestOutputDirectory()).toURI().toURL();
            getLog().debug("Adding to classpath : " + testClasses);
            urls.add(testClasses);

            Set<Artifact> dependencies = project.getArtifacts();
            for (Artifact classPathElement : dependencies) {
                getLog().debug(
                        "Adding artifact: " + classPathElement.getFile()
                                + " to classpath");
                urls.add(classPathElement.getFile().toURI().toURL());
            }

            if( classPathIncludes!=null ) {
                for (String include : classPathIncludes) {
                    final URL url = new File(include).toURI().toURL();
                    getLog().debug("Adding to classpath : " + url);
                    urls.add(url);
                }
            }

            return new URLClassLoader(urls.toArray(new URL[urls.size()]),
                    this.getClass().getClassLoader());
        } catch (MalformedURLException e) {
            throw new MojoExecutionException(
                    "Error during setting up classpath", e);
        }
    }

}
