/**
 *
 * Copyright 2005 The Apache Software Foundation
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
package org.apache.xbean.classloader;

import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.IOException;
import java.security.cert.Certificate;

/**
 * @version $Rev$ $Date: 2006-06-01 06:35:48 +0200 (Thu, 01 Jun 2006) $
 */
public class JarResourceHandle extends AbstractResourceHandle {
    private final JarFile jarFile;
    private final JarEntry jarEntry;
    private final URL url;
    private final URL codeSource;

    public JarResourceHandle(JarFile jarFile, JarEntry jarEntry, URL codeSource) throws MalformedURLException {
        this.jarFile = jarFile;
        this.jarEntry = jarEntry;
        this.url = JarFileUrlStreamHandler.createUrl(jarFile, jarEntry, codeSource);
        this.codeSource = codeSource;
    }

    public String getName() {
        return jarEntry.getName();
    }

    public URL getUrl() {
        return url;
    }

    public URL getCodeSourceUrl() {
        return codeSource;
    }

    public boolean isDirectory() {
        return jarEntry.isDirectory();
    }

    public InputStream getInputStream() throws IOException {
        return jarFile.getInputStream(jarEntry);
    }

    public int getContentLength() {
        return (int) jarEntry.getSize();
    }

    public Manifest getManifest() throws IOException {
        return jarFile.getManifest();
    }

    public Attributes getAttributes() throws IOException {
        return jarEntry.getAttributes();
    }

    public Certificate[] getCertificates() {
        return jarEntry.getCertificates();
    }
}
