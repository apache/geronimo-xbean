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
