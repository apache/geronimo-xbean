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
