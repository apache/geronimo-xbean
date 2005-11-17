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

import org.osgi.framework.Bundle;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class BundleClassLoader extends ClassLoader {
    private final Bundle bundle;

    public BundleClassLoader(Bundle bundle) {
        this.bundle = bundle;
    }

    public Class loadClass(String name) throws ClassNotFoundException {
        return bundle.loadClass(name);
    }

    public URL getResource(String name) {
        return bundle.getResource(name);
    }

    protected Enumeration findResources(String name) throws IOException {
        return bundle.getResources(name);
    }
}
