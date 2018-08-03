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
