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
package org.apache.xbean.server.repository;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * FileSystemRepository maps resource ids to a directory on the local file system.
 *
 * @org.apache.xbean.XBean namespace="http://xbean.apache.org/schemas/server" element="file-system-repository"
 *     description="Maps resource ids to a directory on the local file system."
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class FileSystemRepository implements Repository {
    private File root;

    /**
     * Creates a new repository without a specified root directory.  This repository is not usable until the root
     * directory is specified.
     */
    public FileSystemRepository() {
    }

    /**
     * Creates a new repository using the specified root directory.
     * @param root the root directory from which resources are resolved
     */
    public FileSystemRepository(File root) {
        this.root = root;
    }

    /**
     * Gets the root directory from which resources are resolved.
     * @return the root directory of this repository
     */
    public File getRoot() {
        return root;
    }

    /**
     * Sets the root directory of this repository.
     * Note: the setting of the root directory is not synchronized and is expected to be called immediately after the
     * default constructor in the same thread.
     * @param root the new root directory from which resources are to be resolved
     */
    public void setRoot(File root) {
        this.root = root;
    }

    /**
     * Gets location of the resource realitive to the root directory.  This method simply resolves the location against
     * the root directory using root.toURI().resolve(location).
     * @param location the location of the resource
     * @return the absolute location of the resource or null if the root directory does not contain a readable file at
     * the specified location
     */
    public URL getResource(String location) {
        File root = this.root;
        if (root == null) {
            throw new NullPointerException("root directory is null");
        }

        URI uri = root.toURI().resolve(location);
        File file = new File(uri);

        if (!file.canRead()) {
            return null;
        }

        try {
            return file.toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Malformed resource " + uri);
        }
    }
}
