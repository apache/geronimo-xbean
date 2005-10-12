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
package org.xbean.server.annotation;

/**
 * ParameterNames is an annotation to provide the names of constructor and method names to the server.  Parameter names
 * can be used during constructor injection instead of the standard index based approach.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public interface ParameterNames {
    /**
     * The names of the constructor or method parameters.
     * @return names of the constructor or method parameters. 
     */
    String[] names();
}
