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

 import org.apache.maven.artifact.Artifact;
 import org.apache.maven.execution.MavenSession;
 import org.apache.maven.model.Resource;
 import org.apache.maven.plugin.AbstractMojo;
 import org.apache.maven.plugin.MojoExecutionException;
 import org.apache.maven.plugin.MojoFailureException;
 import org.apache.maven.plugins.annotations.*;
 import org.apache.maven.project.MavenProject;
 import org.apache.maven.project.MavenProjectHelper;
 import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
 import org.apache.xbean.generator.*;
 import org.apache.xbean.generator.artifact.HashedArtifactSet;
 import org.apache.xbean.generator.qdox.QdoxMappingLoader;
 import org.apache.xbean.model.mapping.NamespaceMapping;

 import java.beans.PropertyEditorManager;
 import java.io.File;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.net.URLClassLoader;
 import java.util.*;
 import java.util.stream.Collectors;

 /**
  * This type is super class for specific mojo's which are responsible for execution of their work in their scopes.
  *
  * Main responsibility of this mojo is plugging generators into work. Generators are looked up via classpath thanks to
  * plexus/sisu injections so there is no direct dependency on any specific injection framework (either Spring or Blueprint).
  */
 public abstract class AbstractXBeanMojo extends AbstractMojo implements LogFacade {

     /**
      * Maven ProjectHelper.
      */
     @Component
     protected MavenProjectHelper projectHelper;

     /**
      * Maven project information.
      */
     @Parameter(required = true, defaultValue = "${project}")
     protected MavenProject project;

     /**
      * Maven build session.
      */
     @Parameter(required = true, defaultValue = "${session}")
     private MavenSession session;

     /**
      */
     @Parameter(required = true)
     private String namespace;

     /**
      */
     @Parameter(required = true, defaultValue = "${basedir}/src/main/java")
     private File srcDir;

     /**
      * Included elements
      */
     @Parameter
     private List<String> includes;

     /**
      * ???
      */
     @Parameter
     private List<String> classPathIncludes;

     /**
      */
     @Parameter
     private String excludedClasses;

     /**
      * Identifiers of generators which should be excluded even if they are available. By default it is empty.
      */
     @Parameter
     private List<String> excludedGenerators = Collections.emptyList();

     /**
      */
     @Parameter(required = true, defaultValue = "${basedir}/target/xbean/")
     private File outputDir;

     /**
      * Settings and properties which should be passed to underlying generators.
      *
      * Because each and every generator might have different configuration options it is not possible to support them in
      * reliable and portable way at maven plugin level. For this reason a flat property map is used as a universal and
      * not challenging medium to deliver settings from user calling this mojo to generators.
      */
     @Parameter
     private Map<String, String> properties = Collections.emptyMap();

     /**
      * @parameter
      */
     private File schema;

     /**
      *
      */
     @Parameter(required = true, defaultValue = "org.apache.xbean.propertyeditors")
     private String propertyEditorPaths;

     /**
      * Plugin scanned from plugin classpath.
      */
     @Component(role = GeneratorPluginFactory.class)
     private Map<String, GeneratorPluginFactory> plugins;

     @Component(role = DependencyGraphBuilder.class, hint = "maven31")
     private DependencyGraphBuilder dependencyGraphBuilder;

     public void execute() throws MojoExecutionException, MojoFailureException {

         getLog().debug( " ======= XBeanMojo settings =======" );
         getLog().debug( "namespace[" + namespace + "]" );
         getLog().debug( "srcDir[" + srcDir + "]" );
         getLog().debug( "schema[" + schema + "]" );
         getLog().debug( "excludedClasses[" + excludedClasses + "]");
         getLog().debug( "outputDir[" + outputDir + "]" );
         getLog().debug( "propertyEditorPaths[" + propertyEditorPaths + "]" );

 //        getLog().debug( "schemaAsArtifact[" + schemaAsArtifact + "]");
 //        getLog().debug( "generateSpringSchemasFile[" + generateSpringSchemasFile + "]");
 //        getLog().debug( "generateSpringHandlersFile[" + generateSpringHandlersFile + "]");

         if (schema == null) {
             schema = new File(outputDir, project.getArtifactId() + ".xsd");
         }

         if (propertyEditorPaths != null) {
             List<String> editorSearchPath = new LinkedList<String>(Arrays.asList(PropertyEditorManager.getEditorSearchPath()));
             for (StringTokenizer paths = new StringTokenizer(propertyEditorPaths, " ,"); paths.hasMoreElements(); ) {
                 //StringTokenizer implements Enumeration<Object>, not Enumeration<String> !!
                 editorSearchPath.add((String) paths.nextElement());
             }
             PropertyEditorManager.setEditorSearchPath(editorSearchPath.toArray(new String[editorSearchPath.size()]));
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
             List<File> sourceJars = new ArrayList<>();
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
             // TODO discover these from classpath

             // load the mappings
             Thread.currentThread().setContextClassLoader(getClassLoader());
             Set<NamespaceMapping> namespaces = mappingLoader.loadNamespaces();
             if (namespaces.isEmpty()) {
                 getLog().warn("Warning: no namespaces found!");
             }

             HashedArtifactSet artifactSet = new HashedArtifactSet();

             List<GeneratorPluginFactory> pluginFactories = plugins.values().stream()
                 .sorted(Comparator.comparingInt(GeneratorPluginFactory::position))
                 .collect(Collectors.toList());

             Map<String, String> settings = Collections.unmodifiableMap(properties);

             // generate the files
             for (NamespaceMapping namespaceMapping : namespaces) {
                 for (GeneratorPluginFactory factory : pluginFactories) {
                     if (excludedGenerators.contains(factory.id())) {
                         getLog().info("Skipping execution of generator " + factory.name() + " (" + factory.id() + ") because it is excluded from execution chain");
                         continue;
                     }
                     GeneratorPlugin plugin = factory.createPlugin(outputDir, artifactSet, this);
                     plugin.generate(namespaceMapping, settings);
                 }
             }

             // Attach them as artifacts
             /*
             if (schemaAsArtifact) {
                 projectHelper.attachArtifact(project, "xsd", null, schema);
                 projectHelper.attachArtifact(project, "html", "schema", new File(schema.getAbsolutePath() + ".html"));
             }
             */

             Resource resource = new Resource();
             resource.setDirectory(outputDir.toString());
             processOutputResources(project, resource);
             processArtifacts(project, artifactSet);

             log("...done.");
         } catch (Exception e) {
             throw new MojoFailureException("Failed to execute xbean generator", e);
         } finally {
             Thread.currentThread().setContextClassLoader(oldCL);
         }
     }

     protected abstract void processArtifacts(MavenProject project, HashedArtifactSet artifactSet);

     protected abstract void processOutputResources(MavenProject project, Resource resource);

     public void log(String message) {
         getLog().info(message);
     }

     public void log(String message, int level) {
         getLog().info(message);
     }

     protected URLClassLoader getClassLoader() throws MojoExecutionException {
         try {
             List<URL> artifactLocations = createClassLoader(project, session, dependencyGraphBuilder);
             return new URLClassLoader(artifactLocations.toArray(new URL[artifactLocations.size()]));
         } catch (MalformedURLException e) {
             throw new MojoExecutionException("Error during setting up classpath", e);
         }
     }

     protected abstract List<URL> createClassLoader(MavenProject project, MavenSession session, DependencyGraphBuilder dependencyGraphBuilder) throws MalformedURLException, MojoExecutionException;

 }
