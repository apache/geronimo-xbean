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
package org.gbean.classloader;

import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;

import junit.framework.TestCase;

/**
 * @version $Revision$ $Date$
 */
public class JarFileClassLoaderTest extends TestCase {
    private static final String ENTRY_NAME = "foo";
    private static final String ENTRY_VALUE = "bar";
    private File file;
    private static final String NON_EXISTANT_RESOURCE = "non-existant-resource";

    // this stuff doesn't work on windows
    public void testNothing() {
    }

    public void XtestReadEntry() throws Exception {
        JarFile jarFile = new JarFile(file);
        JarEntry jarEntry = jarFile.getJarEntry(ENTRY_NAME);
        String urlString = "jar:" + file.toURL() + "!/" + ENTRY_NAME;
        URL url = new URL(file.toURL(), urlString);
        assertStreamContains(ENTRY_VALUE, url.openStream());
        jarFile.close();
    }

    public void XtestGetResourceAsStream() throws Exception {
        JarFileClassLoader classLoader = new JarFileClassLoader("test", Collections.singletonList(file.toURL()));
        InputStream in = classLoader.getResourceAsStream(ENTRY_NAME);
        assertStreamContains(ENTRY_VALUE, in);

        classLoader.destroy();
    }

    public void XtestGetNonExistantResourceAsStream() throws Exception {
        JarFileClassLoader classLoader = new JarFileClassLoader("test", Collections.singletonList(file.toURL()));
        InputStream in = classLoader.getResourceAsStream(NON_EXISTANT_RESOURCE);
        assertNull(in);

        classLoader.destroy();
    }

    public void XtestGetResource() throws Exception {
        JarFileClassLoader classLoader = new JarFileClassLoader("test", Collections.singletonList(file.toURL()));
        URL resource = classLoader.getResource(ENTRY_NAME);
        assertNotNull(resource);

        InputStream in = resource.openStream();
        assertStreamContains(ENTRY_VALUE, in);

        classLoader.destroy();
    }

    public void XtestGetNonExistantResource() throws Exception {
        JarFileClassLoader classLoader = new JarFileClassLoader("test", Collections.singletonList(file.toURL()));
        URL resource = classLoader.getResource(NON_EXISTANT_RESOURCE);
        assertNull(resource);

        classLoader.destroy();
    }

    public void XtestGetResources() throws Exception {
        JarFileClassLoader classLoader = new JarFileClassLoader("test", Collections.singletonList(file.toURL()));
        Enumeration resources = classLoader.getResources(ENTRY_NAME);
        assertNotNull(resources);
        assertTrue(resources.hasMoreElements());

        URL resource = (URL) resources.nextElement();
        assertNotNull(resource);

        InputStream in = resource.openStream();
        assertStreamContains(ENTRY_VALUE, in);

        classLoader.destroy();
    }

    public void XtestGetNonExistantResources() throws Exception {
        JarFileClassLoader classLoader = new JarFileClassLoader("test", Collections.singletonList(file.toURL()));
        Enumeration resources = classLoader.getResources(NON_EXISTANT_RESOURCE);
        assertNotNull(resources);
        assertFalse(resources.hasMoreElements());

        classLoader.destroy();
    }

    private void assertStreamContains(String expectedValue, InputStream in) throws IOException {
        String entryValue;
        try {
            StringBuffer stringBuffer = new StringBuffer();
            byte[] bytes = new byte[4000];
            for (int count = in.read(bytes); count != -1; count = in.read(bytes)) {
                stringBuffer.append(new String(bytes, 0, count));
            }
            entryValue = stringBuffer.toString();
        } finally {
            in.close();
        }
        assertEquals(expectedValue, entryValue);
    }

    private static void assertFileExists(File file) {
        assertTrue("File should exist: " + file, file.canRead());
    }

    private static void assertFileNotExists(File file) {
        assertTrue("File should not exist: " + file, !file.canRead());
    }

    protected void setUp() throws Exception {
        file = new File("test.jar");
        file.delete();

        assertFileNotExists(file);

        FileOutputStream out = new FileOutputStream(file);
        JarOutputStream jarOut = new JarOutputStream(out);
        jarOut.putNextEntry(new JarEntry(ENTRY_NAME));
        jarOut.write(ENTRY_VALUE.getBytes());
        jarOut.close();
        out.close();

        assertFileExists(file);
    }

    protected void tearDown() throws Exception {
        file.delete();
        assertFileNotExists(file);
    }
}
