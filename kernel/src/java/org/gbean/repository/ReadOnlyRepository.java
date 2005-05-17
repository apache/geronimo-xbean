/**
 *
 * Copyright 2005 GBean.org
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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * @version $Revision$ $Date$
 */
public class ReadOnlyRepository implements Repository {
    private URI rootUri;

    public ReadOnlyRepository(File root) {
        rootUri = root.toURI();
    }

    public ReadOnlyRepository(URI rootURI) {
        this.rootUri = rootURI;
    }

    public URI getRootUri() {
        return rootUri;
    }

    public boolean containsResource(URI uri) {
        uri = rootUri.resolve(uri);
        if ("file".equals(uri.getScheme())) {
            File file = new File(uri);
            return file.canRead();
        } else {
            try {
                uri.toURL().openStream().close();
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }

    public URL getResource(URI uri) {
        try {
            return rootUri.resolve(uri).toURL();
        } catch (MalformedURLException e) {
            throw (IllegalArgumentException) new IllegalArgumentException("Unable to convert URI to a URL").initCause(e);
        }
    }
}
