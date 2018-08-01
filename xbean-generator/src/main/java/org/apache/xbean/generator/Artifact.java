package org.apache.xbean.generator;

import java.io.File;
import java.util.Optional;

public interface Artifact {

    File getLocation();

    boolean isAttachable();

    Optional<String> getMeta(String key);

}
