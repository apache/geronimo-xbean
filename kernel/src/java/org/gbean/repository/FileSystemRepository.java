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
package org.gbean.repository;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * @version $Revision$ $Date$
 */
public class FileSystemRepository implements Repository {
    private File root;

    public FileSystemRepository() {
    }

    public FileSystemRepository(File root) {
        this.root = root;
    }

    public File getRoot() {
        return root;
    }

    public void setRoot(File root) {
        this.root = root;
    }

    public boolean containsResource(URI uri) {
        uri = root.toURI().resolve(uri);
        File file = new File(uri);
        return file.canRead();
    }

    public URL getResource(URI uri) {
        uri = root.toURI().resolve(uri);
        File file = new File(uri);
        try {
            return file.toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Malformed resource " + uri);
        }
    }
}
