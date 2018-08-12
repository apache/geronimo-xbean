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
package org.apache.xbean.generator.commons;

import java.util.function.Function;

/**
 * Utility function to calculate file name for generated schema.
 */
public class NamespaceFileNameExtractor implements Function<String, String> {

    public String apply(String namespace) {
        if (namespace.startsWith("http://") || namespace.startsWith("https://")) {
            return namespace.substring(namespace.lastIndexOf('/') + 1) + ".xsd";
        }

        if (namespace.contains(":")) {
            // a typical urn:x:y namespace
            return namespace.substring(namespace.lastIndexOf(':') + 1) + ".xsd";
        }

        return namespace.replace("[^a-zA-Z0-9_", "") + ".xsd";
    }

}
