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
package org.gbean.server.repository;

import java.net.URL;

/**
 * Repository is used to locate resources by id.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public interface Repository {
    /**
     * Gets a URL to the specified resource.
     * @param id the id of the resource
     * @return the location of the resource or null if the repository does not contain the resource
     */
    URL getResource(String id);
}
