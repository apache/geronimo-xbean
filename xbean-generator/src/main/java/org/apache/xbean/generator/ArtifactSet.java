package org.apache.xbean.generator;

import java.util.Optional;

public interface ArtifactSet {

    void register(Artifact artifact);

    Optional<Artifact> find(String metaKey, String metaValue);

}
