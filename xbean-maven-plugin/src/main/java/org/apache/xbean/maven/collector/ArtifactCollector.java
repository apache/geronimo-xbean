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
package org.apache.xbean.maven.collector;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.traversal.DependencyNodeVisitor;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of dependency visitor which collects locations of artifacts on disk for later use.
 */
public class ArtifactCollector implements DependencyNodeVisitor {

    private final List<URL> artifactLocations = new ArrayList<>();

    @Override
    public boolean visit(DependencyNode dependencyNode) {
            Optional.ofNullable(dependencyNode.getArtifact())
                .map(Artifact::getFile)
                .map(File::toURI)
                .map(this::toSafeUrl)
                .ifPresent(artifactLocations::add);
        return true;
    }

    private URL toSafeUrl(URI uri) {
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Could not determine URL for artifact " + uri, e);
        }
    }

    @Override
    public boolean endVisit(DependencyNode dependencyNode) {
        return true;
    }

    public List<URL> getArtifactLocations() {
        return artifactLocations;
    }

}
