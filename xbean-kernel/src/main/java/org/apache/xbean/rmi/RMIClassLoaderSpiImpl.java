/**
 *
 * Copyright 2005-2006 The Apache Software Foundation or its licensors, as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.xbean.rmi;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.server.RMIClassLoader;
import java.rmi.server.RMIClassLoaderSpi;
import java.util.StringTokenizer;


/**
 * An implementation of {@link java.rmi.server.RMIClassLoaderSpi} which provides normilzation
 * of codebase URLs and delegates to the default {@link RMIClassLoaderSpi}.
 *
 * @version $Rev: 209990 $ $Date: 2005-07-09 21:24:52 -0700 (Sat, 09 Jul 2005) $
 */
public class RMIClassLoaderSpiImpl extends RMIClassLoaderSpi {

    private static final RMIClassLoaderSpi delegate = RMIClassLoader.getDefaultProviderInstance();

    public Class loadClass(String codebase, String name, ClassLoader defaultLoader)
            throws MalformedURLException, ClassNotFoundException
    {
        if (codebase != null) {
            codebase = normalizeCodebase(codebase);
        }

        return delegate.loadClass(codebase, name, defaultLoader);
    }

    public Class loadProxyClass(String codebase, String[] interfaces, ClassLoader defaultLoader)
            throws MalformedURLException, ClassNotFoundException
    {
        if (codebase != null) {
            codebase = normalizeCodebase(codebase);
        }

        return delegate.loadProxyClass(codebase, interfaces, defaultLoader);
    }

    public ClassLoader getClassLoader(String codebase) throws MalformedURLException {

        if (codebase != null) {
            codebase = normalizeCodebase(codebase);
        }

        return delegate.getClassLoader(codebase);
    }

    public String getClassAnnotation(Class type) {

        Object obj = type.getClassLoader();
        if (obj instanceof ClassLoaderServerAware) {
            ClassLoaderServerAware classLoader = (ClassLoaderServerAware) obj;
            URL urls[] = classLoader.getClassLoaderServerURLs();
            if (null == urls) {
                return delegate.getClassAnnotation(type);
            }
            StringBuffer codebase = new StringBuffer();
            for (int i = 0; i < urls.length; i++) {
                URL url = normalizeURL(urls[i]);
                if (codebase.length() != 0) {
                    codebase.append(' ');
                }
                codebase.append(url);
            }
            return codebase.toString();
        }

        return delegate.getClassAnnotation(type);
    }

    static String normalizeCodebase(String input) throws MalformedURLException {

        StringBuffer codebase = new StringBuffer();
        StringBuffer working = new StringBuffer();
        StringTokenizer stok = new StringTokenizer(input, " \t\n\r\f", true);

        while (stok.hasMoreTokens()) {
            String item = stok.nextToken();
            try {
                // If we got this far then item is a valid url, so commit the current
                // buffer and start collecting any trailing bits from where we are now

                updateCodebase(working, codebase);
            }
            catch (MalformedURLException ignore) {
                // just keep going & append to the working buffer
            }

            working.append(item);
        }

        // Handle trailing elements
        updateCodebase(working, codebase);

        return codebase.toString();
    }

    private static void updateCodebase(final StringBuffer working, final StringBuffer codebase) throws MalformedURLException {

        if (working.length() != 0) {
            // Normalize the URL
            URL url = normalizeURL(new URL(working.toString()));

            // Put spaces back in for URL delims
            if (codebase.length() != 0) {
                codebase.append(" ");
            }
            codebase.append(url);

            // Reset the working buffer
            working.setLength(0);
        }
    }

    static URL normalizeURL(URL url) {

        if (url.getProtocol().equals("file")) {
            String filename = url.getFile().replace('/', File.separatorChar);
            File file = new File(filename);
            try {
                url = file.toURI().toURL();
            }
            catch (MalformedURLException ignore) {
            }
        }

        return url;
    }

    public interface ClassLoaderServerAware {
        public URL[] getClassLoaderServerURLs();
    }
}

