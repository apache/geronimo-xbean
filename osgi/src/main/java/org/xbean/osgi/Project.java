/**
 *
 * Copyright 2005 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.xbean.osgi;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Collections;

/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class Project implements Artifact {
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String type;
    private final String jar;
    private final Set dependencies;

    public Project(String groupId, String artifactId, String version, String type, Set dependencies) {
        this(groupId, artifactId, version, type, artifactId + "-" + version + "." + type, dependencies);
    }

    public Project(String groupId, String artifactId, String version, String type, String jar, Set dependencies) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = type;
        this.jar = jar;

        Set deps = new HashSet();
        for (Iterator iterator = dependencies.iterator(); iterator.hasNext();) {
            Artifact dependency = (Artifact) iterator.next();
            deps.add(dependency);
        }
        this.dependencies = Collections.unmodifiableSet(deps);
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getType() {
        return type;
    }

    public String getJar() {
        return jar;
    }

    public Set getDependencies() {
        return dependencies;
    }
}
