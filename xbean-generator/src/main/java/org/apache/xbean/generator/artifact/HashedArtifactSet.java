package org.apache.xbean.generator.artifact;

import org.apache.xbean.generator.Artifact;
import org.apache.xbean.generator.ArtifactSet;

import java.util.HashSet;
import java.util.Optional;

public class HashedArtifactSet extends HashSet<Artifact> implements ArtifactSet {

    @Override
    public void register(Artifact artifact) {
        if (!add(artifact)) {
            throw new IllegalArgumentException("Artifact is already registered");
        }
    }

    @Override
    public Optional<Artifact> find(String metaKey, String metaValue) {
        return stream().filter(artifact -> matches(artifact, metaKey, metaValue))
            .findFirst();
    }

    private boolean matches(Artifact artifact, String metaKey, String metaValue) {
        Optional<String> value = artifact.getMeta(metaKey);
        return value.isPresent() && metaValue.equals(value.get());
    }

}
