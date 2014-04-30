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
package org.apache.xbean.blueprint.context.impl;

/**
 * A helper class for turning namespaces into META-INF/services URIs
 * 
 * @version $Revision$
 */
public class NamespaceHelper {
    public static final String META_INF_PREFIX = "META-INF/services/org/apache/xbean/blueprint/";

    /**
     * Converts the namespace and localName into a valid path name we can use on
     * the classpath to discover a text file
     */
    public static String createDiscoveryPathName(String uri, String localName) {
        if (isEmpty(uri)) {
            return localName;
        }
        return createDiscoveryPathName(uri) + "/" + localName;
    }

    /**
     * Converts the namespace and localName into a valid path name we can use on
     * the classpath to discover a text file
     */
    public static String createDiscoveryPathName(String uri) {
        // TODO proper encoding required
        // lets replace any dodgy characters
        return META_INF_PREFIX + uri.replaceAll("://", "/").replace(':', '/').replace(' ', '_');
    }

    public static boolean isEmpty(String uri) {
        return uri == null || uri.length() == 0;
    }

}
