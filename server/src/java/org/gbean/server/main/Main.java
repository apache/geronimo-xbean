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
package org.gbean.server.main;

/**
 * Main is centeral entry point for the server.  This should used instead of public static void Main(String[] args).
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public interface Main {
    /**
     * The main entry point method.
     * @param args arguments to the main entry point
     */
    void main(String[]  args);
}
