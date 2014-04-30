/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.xbean.finder.util;

import org.apache.xbean.finder.archive.FileArchive;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public final class Files {
    public static File toFile(final URL url) {
        if ("jar".equals(url.getProtocol())) {
            try {
                final String spec = url.getFile();
                final int separator = spec.indexOf('!');
                if (separator == -1) {
                    return null;
                }
                return toFile(new URL(spec.substring(0, separator + 1)));
            } catch (final MalformedURLException e) {
                return null;
            }
        } else if ("file".equals(url.getProtocol())) {
            String path = FileArchive.decode(url.getFile());
            if (path.endsWith("!")) {
                path = path.substring(0, path.length() - 1);
            }
            return new File(path);
        }
        return null;
    }

    private Files() {
        // no-op
    }
}
