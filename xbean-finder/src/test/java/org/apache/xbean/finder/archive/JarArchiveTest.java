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

import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.acme.foo.Blue;
import org.acme.foo.Green;
import org.acme.foo.Red;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @version $Rev$ $Date$
 */
public class JarArchiveTest {

    private static final Class[] classes = {Blue.class, Blue.Navy.class, Blue.Sky.class, Green.class, Green.Emerald.class, Red.class, Red.CandyApple.class, Red.Pink.class};
    private static File classpath;
    private JarArchive archive;

    @Rule
    public TemporaryFolder testTmpDir = new TemporaryFolder();

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
            Assert.fail("ClassNotFoundException should have been thrown");
        } catch (ClassNotFoundException e) {
            // pass
        }
    }

    @Test
    public void testLoadClass() throws Exception {
        for (Class clazz : classes) {
            Assert.assertEquals(clazz.getName(), clazz, archive.loadClass(clazz.getName()));
        }

        try {
            archive.loadClass("Fake");
            Assert.fail("ClassNotFoundException should have been thrown");
        } catch (ClassNotFoundException e) {
            // pass
        }
    }

    @Test
    public void testIterator() throws Exception {
        List<String> actual = new ArrayList<String>();
        for (Archive.Entry entry : archive) {
            actual.add(entry.getName());
        }

        Assert.assertFalse(0 == actual.size());

        for (Class clazz : classes) {
            Assert.assertTrue(clazz.getName(), actual.contains(clazz.getName()));
        }

        Assert.assertEquals(JarArchiveTest.classes.length, actual.size());
    }

    @Test
    public void testXBEAN337() throws Exception {


        // Virtual path

        String path = "/this!/!file!/does!/!not/exist.jar";
        URL[] urls = {new URL("jar:file:" + path + "!/some!/!inner!/.jar!/file.jar")};

        try(JarArchive jar = new JarArchive(new URLClassLoader(urls), urls[0])){

        }catch(Exception ex){
            Assert.assertTrue(String.format(
                    "Muzz never fail on '/this', but try full path with exclamations('%s') instead",
                    path),
                    ex.getCause().getMessage().contains("exist.jar"));
        }


        // Real file

        File tmpDir = testTmpDir.newFolder("!" + JarArchiveTest.class.getSimpleName() + "!-temp!");

        File exclamated = Files.copy(JarArchiveTest.classpath.toPath(),
                tmpDir.toPath().resolve(
                        JarArchiveTest.classpath.getName()))
                .toFile();

        urls[0] = new URL("jar:" + exclamated.toURI().toURL() + "!/");

        try(JarArchive jar = new JarArchive(new URLClassLoader(urls), urls[0])){

            Assert.assertEquals(String.format("Muzz successfully open '%s'", exclamated.getAbsolutePath()),
                    this.archive.iterator().hasNext(),
                    jar.iterator().hasNext());
        }


        // Unsupported protocols stack

        urls[0] = new URL("http:ftp:jar:" + exclamated.toURI().toURL() + "!/");

        try(JarArchive jar = new JarArchive(new URLClassLoader(urls), urls[0])){
            Assert.fail(String.format("Muzz eat only local file URLs:"
                    + " 'file:/...' or 'jar:file:/...!/' but not '%s'",
                    urls[0]));
        }catch(UnsupportedOperationException ex){

        }
    }
}