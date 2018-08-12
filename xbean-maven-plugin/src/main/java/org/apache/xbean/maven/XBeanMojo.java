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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.artifact.filter.ScopeArtifactFilter;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.xbean.generator.*;
import org.apache.xbean.generator.artifact.HashedArtifactSet;
import org.apache.xbean.maven.collector.ArtifactCollector;

 /**
 * @author <a href="gnodet@apache.org">Guillaume Nodet</a>
 * @version $Id: GenerateApplicationXmlMojo.java 314956 2005-10-12 16:27:15Z brett $
 * goal mapping
 * description Creates xbean mapping file
 * phase generate-sources
 * requiresDependencyResolution compile
 */
@Mojo(
    name = "mapping",
    defaultPhase = LifecyclePhase.GENERATE_RESOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE
)
public class XBeanMojo extends AbstractXBeanMojo implements LogFacade {

     @Parameter
     private List<String> attachArtifacts = Arrays.asList("xsd", "wiki", "html");

     @Override
     protected void processArtifacts(MavenProject project, HashedArtifactSet artifactSet) {
         for (String artifactType : attachArtifacts) {
             Optional<org.apache.xbean.generator.Artifact> artifact = artifactSet.find("type", artifactType);
             artifact.ifPresent(this::attach);
         }
     }

     @Override
     protected void processOutputResources(MavenProject project, Resource resource) {
         project.addResource(resource);
     }

    protected List<URL> createClassLoader(MavenProject project, MavenSession session, DependencyGraphBuilder dependencyGraphBuilder) throws MojoExecutionException, MalformedURLException {
        ProjectBuildingRequest projectBuildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
        projectBuildingRequest.setProject(project);

        File outputDirectory = new File(project.getBuild().getOutputDirectory());
        try {
            DependencyNode compileDependencies = dependencyGraphBuilder.buildDependencyGraph(projectBuildingRequest, new ScopeArtifactFilter("compile"));
            ArtifactCollector artifactCollector = new ArtifactCollector();
            compileDependencies.accept(artifactCollector);
            List<URL> locations = artifactCollector.getArtifactLocations();
            locations.add(outputDirectory.toURI().toURL());
            return locations;
        } catch (DependencyGraphBuilderException e) {
            throw new MojoExecutionException("Could not resolve project dependencies", e);
        }
    }

     private void attach(org.apache.xbean.generator.Artifact artifact) {
         Optional<String> type = artifact.getMeta("type");
         Optional<String> classifier = artifact.getMeta("classifier");
         if (type.isPresent() && classifier.isPresent()) {
             projectHelper.attachArtifact(project, type.get(), classifier.get(), artifact.getLocation());
             return;
         }

         type.ifPresent(artifactType -> projectHelper.attachArtifact(project, artifactType, null, artifact.getLocation()));
     }

}
