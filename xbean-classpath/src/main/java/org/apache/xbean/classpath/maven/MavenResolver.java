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
package org.apache.xbean.classpath.maven;

import java.net.URL;

/**
 * Transitively resolves dependencies using Maven.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class MavenResolver {
//    private final ArtifactFactory artifactFactory;
//    private final Set artifacts = new HashSet();

    /**
     * Creates a new resolver.
     */
    public MavenResolver() {
//        artifactFactory = new DefaultArtifactFactory();
    }

    /**
     * Add an artifact to resolve.
     * @param groupId the id of the group to which the dependency belongs
     * @param artifactId the id of the artifact to resolve
     * @param version the version of the artifact
     */
    public void addArtifact(String groupId, String artifactId, String version) {
        addArtifact(groupId, artifactId, version, "jar");
    }

    /**
     * Add an artifact to resolve.
     * @param groupId the id of the group to which the dependency belongs
     * @param artifactId the id of the artifact to resolve
     * @param version the version of the artifact
     * @param type the type of the artifact
     */
    public void addArtifact(String groupId, String artifactId, String version, String type) {
//        Artifact artifact = artifactFactory.createArtifact( groupId, artifactId, version, "runtime", type );
//        artifacts.add(artifact);
    }

    /**
     * Downloads all of the artificts, all of the artificts they depend on and so on.
     * @return the URLs to all artifacts
     */
    public URL[] resolve() {
        return null;
    }
}
