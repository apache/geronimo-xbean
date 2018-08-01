package org.apache.xbean.generator.artifact;

import org.apache.xbean.generator.Artifact;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class SimpleArtifact implements Artifact {

    private final File location;
    private final boolean attachable;
    private final Map<String, String> metadata;

    public SimpleArtifact(File location, boolean attachable, Map<String, String> metadata) {
        this.location = location;
        this.attachable = attachable;
        this.metadata = metadata;
    }

    @Override
    public File getLocation() {
        return location;
    }

    @Override
    public boolean isAttachable() {
        return attachable;
    }

    @Override
    public Optional<String> getMeta(String key) {
        return Optional.ofNullable(metadata.get(key));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SimpleArtifact)) return false;
        SimpleArtifact that = (SimpleArtifact) o;
        return isAttachable() == that.isAttachable() &&
            Objects.equals(getLocation(), that.getLocation()) &&
            Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLocation(), isAttachable(), metadata);
    }

}
