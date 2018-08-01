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

 import org.apache.maven.execution.MavenSession;
 import org.apache.maven.model.Resource;
 import org.apache.maven.plugin.MojoExecutionException;
 import org.apache.maven.plugins.annotations.LifecyclePhase;
 import org.apache.maven.plugins.annotations.Mojo;
 import org.apache.maven.plugins.annotations.ResolutionScope;
 import org.apache.maven.project.DefaultProjectBuildingRequest;
 import org.apache.maven.project.MavenProject;
 import org.apache.maven.project.ProjectBuildingRequest;
 import org.apache.maven.shared.artifact.filter.ScopeArtifactFilter;
 import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
 import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
 import org.apache.maven.shared.dependency.graph.DependencyNode;
 import org.apache.xbean.generator.LogFacade;
 import org.apache.xbean.generator.artifact.HashedArtifactSet;
 import org.apache.xbean.maven.collector.ArtifactCollector;

 import java.io.File;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.util.List;

 /**
  * Mojo dedicated to creation of test resources.
  */
 @Mojo(
     name = "test-mapping",
     defaultPhase = LifecyclePhase.GENERATE_TEST_RESOURCES,
     requiresDependencyResolution = ResolutionScope.TEST
 )
 public class XBeanTestMojo extends AbstractXBeanMojo implements LogFacade {

     @Override
     protected void processArtifacts(MavenProject project, HashedArtifactSet artifactSet) {
         
     }

     @Override
     protected void processOutputResources(MavenProject project, Resource resource) {
         project.addTestResource(resource);
     }

     protected List<URL> createClassLoader(MavenProject project, MavenSession session, DependencyGraphBuilder dependencyGraphBuilder) throws MojoExecutionException, MalformedURLException {
         ProjectBuildingRequest projectBuildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
         projectBuildingRequest.setProject(project);

         File outputDirectory = new File(project.getBuild().getOutputDirectory());
         File testOutputDirectory = new File(project.getBuild().getTestOutputDirectory());
         try {
             DependencyNode testDependencies = dependencyGraphBuilder.buildDependencyGraph(projectBuildingRequest, new ScopeArtifactFilter("test"));
             ArtifactCollector artifactCollector = new ArtifactCollector();
             testDependencies.accept(artifactCollector);

             List<URL> locations = artifactCollector.getArtifactLocations();
             locations.add(outputDirectory.toURI().toURL());
             locations.add(testOutputDirectory.toURI().toURL());
             return locations;
         } catch (DependencyGraphBuilderException e) {
             throw new MojoExecutionException("Could not resolve project dependencies", e);
         }
     }

 }
