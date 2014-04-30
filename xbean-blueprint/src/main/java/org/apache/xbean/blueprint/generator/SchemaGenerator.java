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
package org.apache.xbean.blueprint.generator;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

/**
 * @version $Revision$
 */
public class SchemaGenerator {
    private final MappingLoader mappingLoader;
    private final GeneratorPlugin[] plugins;
    private final LogFacade log;

    public SchemaGenerator(LogFacade log, MappingLoader mappingLoader, GeneratorPlugin[] plugins) {
        this.log = log;
        this.mappingLoader = mappingLoader;
        this.plugins = plugins;
    }

    public void generate() throws IOException {
        Set namespaces = mappingLoader.loadNamespaces();
        if (namespaces.isEmpty()) {
            log.log("Warning: no namespaces found!");
        }

        for (Iterator iterator = namespaces.iterator(); iterator.hasNext();) {
            NamespaceMapping namespaceMapping = (NamespaceMapping) iterator.next();
            for (int i = 0; i < plugins.length; i++) {
                GeneratorPlugin plugin = plugins[i];
                plugin.generate(namespaceMapping);
            }
        }
    }
}
