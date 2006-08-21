/**
 *
 * Copyright 2005-2006 The Apache Software Foundation or its licensors, as applicable.
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
package org.apache.xbean.server.classloader;

/**
 * DestroyableClassLoader is a mixin interface for a ClassLoader that add a destroy method to propertly cleanup a
 * classloader then dereferenced by the server.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public interface DestroyableClassLoader {
    /**
     * Destroys the clasloader releasing all resources.  After this mehtod is called, the class loader will no longer
     * load any classes or resources.
     */
    void destroy();
}
