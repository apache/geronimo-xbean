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
package org.apache.xbean.finder.archive;

import org.acme.foo.Blue;
import org.acme.foo.Green;
import org.acme.foo.Red;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static java.util.Collections.singletonList;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.apache.xbean.finder.archive.Archives.putClasses;

/**
 * @version $Rev$ $Date$
 */
public class JarArchiveTest {

    private static final Class[] classes = {Blue.class, Blue.Navy.class, Blue.Sky.class, Green.class, Green.Emerald.class, Red.class, Red.CandyApple.class, Red.Pink.class};
    private static File classpath;
    private JarArchive archive;

    @Rule
    public final TemporaryFolder tmp = new TemporaryFolder();

    @BeforeClass
    public static void classSetUp() throws Exception {

        classpath = Archives.jarArchive(classes);
    }

    @Before
    public void setUp() throws Exception {

        URL[] urls = {new URL("jar:" + classpath.toURI().toURL() + "!/")};

        archive = new JarArchive(new URLClassLoader(urls), urls[0]);
    }


    @Test
    public void testGetBytecode() throws Exception {

        for (Class clazz : classes) {
            assertNotNull(clazz.getName(), archive.getBytecode(clazz.getName()));
        }

        try {
            archive.getBytecode("Fake");
            fail("ClassNotFoundException should have been thrown");
        } catch (ClassNotFoundException e) {
            // pass
        }
    }

    @Test
    public void testLoadClass() throws Exception {
        for (Class clazz : classes) {
            assertEquals(clazz.getName(), clazz, archive.loadClass(clazz.getName()));
        }

        try {
            archive.loadClass("Fake");
            fail("ClassNotFoundException should have been thrown");
        } catch (ClassNotFoundException e) {
            // pass
        }
    }

    @Test
    public void testIterator() throws Exception {
        List<String> actual = list(archive);

        assertFalse(0 == actual.size());

        for (Class clazz : classes) {
            assertTrue(clazz.getName(), actual.contains(clazz.getName()));
        }

        assertEquals(classes.length, actual.size());
    }

    private List<String> list(final JarArchive archive) {
        final List<String> actual = new ArrayList<>();
        for (final Archive.Entry entry : archive) {
            actual.add(entry.getName());
        }
        return actual;
    }

    @Test
    @Ignore("PR-34")
    public void exclamationMarkInFilename() throws Exception {
        createAndAssertBlueArchive(tmp.newFile("foo!bar.jar"));
    }

    @Test
    @Ignore("PR-34")
    public void exclamationMarkAsResourceSep() throws Exception {
        createAndAssertBlueArchive(new File(tmp.newFolder("foo!"), "the-file.jar"));
    }

    @Test
    @Ignore("PR-34")
    public void exclamationMarkInFilePath() throws Exception {
        createAndAssertBlueArchive(new File(tmp.newFolder("foo!bar"), "the-file.jar"));
    }

    @Test
    public void exclamationMarkInResource() throws Exception {
        final File file = tmp.newFile("file.jar");
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (final JarOutputStream out = new JarOutputStream(Files.newOutputStream(file.toPath()))) {
            putClasses(loader, out, new Class<?>[]{Blue.class});
            out.putNextEntry(new JarEntry("foo!/bar.marker"));
            out.write("Just a marker to find the file for ex".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }

        try (
                final URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{file.toURI().toURL()});
                final JarArchive jar = new JarArchive(
                        loader, urlClassLoader.getResource("foo!/bar.marker"))) {
            assertEquals(singletonList("org.acme.foo.Blue"), list(jar));
        }
    }

    private void createAndAssertBlueArchive(final File file) throws Exception {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (final JarOutputStream out = new JarOutputStream(Files.newOutputStream(file.toPath()))) {
            putClasses(loader, out, new Class<?>[]{Blue.class});
        }

        final URL url = file.toURI().toURL();
        // file:...
        try (final JarArchive jar = new JarArchive(loader, url)) {
            assertEquals(singletonList("org.acme.foo.Blue"), list(jar));
        }
        // file:... with marker with exclamation mark - this one is highly unlikely
        try (final JarArchive jar = new JarArchive(loader, url)) {
            assertEquals(singletonList("org.acme.foo.Blue"), list(jar));
        }
        // no resource
        try (final JarArchive jar = new JarArchive(loader, new URL("jar", null, -1, url.toExternalForm()))) {
            assertEquals(singletonList("org.acme.foo.Blue"), list(jar));
        }
        // resource
        try (final JarArchive jar = new JarArchive(loader, new URL("jar", null, -1, url.toExternalForm() + "!/whatever"))) {
            assertEquals(singletonList("org.acme.foo.Blue"), list(jar));
        }
        // resource with exclamation mark
        try (final JarArchive jar = new JarArchive(loader, new URL("jar", null, -1, url.toExternalForm() + "!/whatever/is!/the!/marker"))) {
            assertEquals(singletonList("org.acme.foo.Blue"), list(jar));
        }
    }
}