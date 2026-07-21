/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.xbean.classloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.SortedSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import junit.framework.TestCase;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Tests the MultiParentClassLoader including classloading and resource loading.
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class MultiParentClassLoaderTest extends TestCase {
    private static final String CLASS_NAME = "TestClass";
    private static final String ENTRY_NAME = "foo";
    private static final String ENTRY_VALUE = "bar";
    private File[] files;
    private static final String NON_EXISTANT_RESOURCE = "non-existant-resource";
    private static final String NON_EXISTANT_CLASS = "NonExistent.class";
    private URLClassLoader[] parents;
    private File myFile;
    private MultiParentClassLoader classLoader;
    private static final String NAME = "my test class loader";

    /**
     * Verify that the test jars are valid.
     * @throws Exception if a problem occurs
     */
    public void testValidJars() throws Exception {
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            JarFile jarFile = new JarFile(files[i]);
            String urlString = "jar:" + file.toURI().toURL() + "!/" + ENTRY_NAME;
            URL url = new URL(file.toURI().toURL(), urlString);
            assertStreamContains(ENTRY_VALUE + i, url.openStream());
            jarFile.close();

            URLClassLoader urlClassLoader = 
                new URLClassLoader(new URL[]{file.toURI().toURL()});
            // class shared by all
            Class clazz = urlClassLoader.loadClass(CLASS_NAME);
            assertNotNull(clazz);
            assertTrue(SortedSet.class.isAssignableFrom(clazz));

            // class specific to this jar
            clazz = urlClassLoader.loadClass(CLASS_NAME + i);
            assertNotNull(clazz);
            assertTrue(SortedSet.class.isAssignableFrom(clazz));

            // resource shared by all jars
            InputStream in = urlClassLoader.getResourceAsStream(ENTRY_NAME);
            assertStreamContains("Should have found value from parent " + i,
                                 ENTRY_VALUE + i, in);

            // resource specific to this jar
            in = urlClassLoader.getResourceAsStream(ENTRY_NAME + i);
            assertStreamContains("Should have found value from parent " + i,
                                 ENTRY_VALUE + i + ENTRY_VALUE, in);
        }
    }

    /**
     * Verify the get name method returns the name provided to the constructor.
     */
    public void testGetName() {
        assertEquals(NAME, classLoader.getName());
    }

    /**
     * Verify that the getParents method returns a different array from the one 
     * passed to the constructor and that the parents are in the same order.
     */
    public void testGetParents() {
        ClassLoader[] actualParents = classLoader.getParents();
        assertNotSame(parents, actualParents);
        assertEquals(parents.length, actualParents.length);
        for (int i = 0; i < actualParents.length; i++) {
            assertEquals(parents[i], actualParents[i]);
        }
    }

    /**
     * Test loadClass loads in preference of the parents, in order, and then 
     * the local urls.
     * @throws Exception if a problem occurs
     */
    public void testLoadClass() throws Exception {
        // load class specific to my class loader
        Class clazz = classLoader.loadClass(CLASS_NAME + 33);
        assertNotNull(clazz);
        assertTrue(SortedSet.class.isAssignableFrom(clazz));
        assertEquals(classLoader, clazz.getClassLoader());

        // load class specific to each parent class loader
        for (int i = 0; i < parents.length; i++) {
            URLClassLoader parent = parents[i];
            clazz = classLoader.loadClass(CLASS_NAME + i);
            assertNotNull(clazz);
            assertTrue(SortedSet.class.isAssignableFrom(clazz));
            assertEquals(parent, clazz.getClassLoader());
        }

        // class shared by all class loaders
        clazz = classLoader.loadClass(CLASS_NAME);
        assertNotNull(clazz);
        assertTrue(SortedSet.class.isAssignableFrom(clazz));
        assertEquals(parents[0], clazz.getClassLoader());
    }

    /**
     * Test that an attempt to load a non-existent class causes a 
     * ClassNotFoundException.
     */
    public void testLoadNonExistentClass() {
        try {
            classLoader.loadClass(NON_EXISTANT_CLASS);
            fail("loadClass should have thrown a ClassNotFoundException");
        } catch (ClassNotFoundException e) {
            // expected
        }
    }

    /**
     * Test getResourceAsStream loads in preference of the parents, in order, 
     * and then the local urls.
     * @throws Exception if a problem occurs
     */
    public void testGetResourceAsStream() throws Exception {
        InputStream in = classLoader.getResourceAsStream(ENTRY_NAME + 33);
        assertStreamContains("Should have found value from my file",
                             ENTRY_VALUE + 33 + ENTRY_VALUE, in);

        for (int i = 0; i < parents.length; i++) {
            in = classLoader.getResourceAsStream(ENTRY_NAME + i);
            assertStreamContains("Should have found value from parent " + i,
                                 ENTRY_VALUE + i + ENTRY_VALUE, in);
        }

        in = classLoader.getResourceAsStream(ENTRY_NAME);
        assertStreamContains("Should have found value from first parent",
                             ENTRY_VALUE + 0, in);
    }

    /**
     * Test getResourceAsStream returns null when attempt is made to load a 
     * non-existent resource.
     * @throws Exception if a problem occurs
     */
    public void testGetNonExistentResourceAsStream() throws Exception {
        InputStream in = classLoader.getResourceAsStream(NON_EXISTANT_RESOURCE);
        assertNull(in);
    }

    /**
     * Test getResource loads in preference of the parents, in order, and 
     * then the local urls.
     * @throws Exception if a problem occurs
     */
    public void testGetResource() throws Exception {
        URL resource = classLoader.getResource(ENTRY_NAME + 33);
        assertURLContains("Should have found value from my file",
                          ENTRY_VALUE + 33 + ENTRY_VALUE, resource);

        for (int i = 0; i < parents.length; i++) {
            resource = classLoader.getResource(ENTRY_NAME + i);
            assertURLContains("Should have found value from parent " + i,
                              ENTRY_VALUE + i + ENTRY_VALUE, resource);
        }

        resource = classLoader.getResource(ENTRY_NAME);
        assertURLContains("Should have found value from first parent",
                          ENTRY_VALUE + 0, resource);
    }

    /**
     * Test getResource returns null when attempt is made to load a 
     * non-existent resource.
     * @throws Exception if a problem occurs
     */
    public void testGetNonExistentResource() throws Exception {
        URL resource = classLoader.getResource(NON_EXISTANT_RESOURCE);
        assertNull(resource);
    }

    /**
     * Test resources returns an enumeration in preference of the parents, 
     * in order, and then the local urls.
     * @throws Exception if a problem occurs
     */
    public void testGetResources() throws Exception {
        Enumeration resources = classLoader.getResources(ENTRY_NAME);
        assertNotNull(resources);
        assertTrue(resources.hasMoreElements());

        // there should be one entry for each parent
        for (int i = 0; i < parents.length; i++) {
            URL resource = (URL) resources.nextElement();
            assertURLContains("Should have found value from parent " + i,
                              ENTRY_VALUE + i, resource);
        }

        // and one entry from my url
        assertTrue(resources.hasMoreElements());
        URL resource = (URL) resources.nextElement();
        assertURLContains("Should have found value from my file",
                          ENTRY_VALUE + 33, resource);
    }

    /**
     * Test getResources returns an empty enumeration when attempt is made 
     * to load a non-existent resource.
     * @throws Exception if a problem occurs
     */
    public void testGetNonExistentResources() throws Exception {
        Enumeration resources = classLoader.getResources(NON_EXISTANT_RESOURCE);
        assertNotNull(resources);
        assertFalse(resources.hasMoreElements());
    }

    /**
     * Creates the class loader to test.
     * @param name the name of the classloader
     * @param urls the urls to load classes and resources from
     * @param parents the parents of the class loader
     * @return the class loader to test
     */
    protected MultiParentClassLoader createClassLoader(String name, 
                                                         URL[] urls, 
                                                         ClassLoader[] parents) {
        return new MultiParentClassLoader(name, urls, parents);
    }

    /**
     * Creates a test jar file with the given index.
     */
    private static File createJarFile(int i) throws IOException {
        File file = File.createTempFile("test-" + i + "-", ".jar");

        FileOutputStream out = new FileOutputStream(file);
        JarOutputStream jarOut = new JarOutputStream(out);

        // class shared by everyone
        jarOut.putNextEntry(new JarEntry(CLASS_NAME + ".class"));
        jarOut.write(createTestClassBytes(CLASS_NAME));

        // class only available in this jar  
        jarOut.putNextEntry(new JarEntry(CLASS_NAME + i + ".class"));
        jarOut.write(createTestClassBytes(CLASS_NAME + i));

        // common resource shared by everyone
        jarOut.putNextEntry(new JarEntry(ENTRY_NAME));
        jarOut.write((ENTRY_VALUE + i).getBytes());

        // resource only available in this jar
        jarOut.putNextEntry(new JarEntry(ENTRY_NAME + i));
        jarOut.write((ENTRY_VALUE + i + ENTRY_VALUE).getBytes());

        jarOut.close();
        out.close();

        assertFileExists(file);
        return file;
    }

    private static byte[] createTestClassBytes(final String name) {
        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | 
                                                 ClassWriter.COMPUTE_MAXS);

        // Visit class: implements SortedSet, extends Object (implicit)
        cw.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC, name, null,
                 "java/lang/Object",
                 new String[]{"java/util/SortedSet"});

        // Constructor: public TestClass() { super(); }
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", 
                                                 null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object",
                             "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        // --- SortedSet methods (abstract) ---
        addEmptyRefMethod(cw, "comparator", 
                           "()Ljava/util/Comparator;");
        addEmptyRefMethod(cw, "subSet", 
                           "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/SortedSet;");
        addEmptyRefMethod(cw, "headSet", 
                           "(Ljava/lang/Object;)Ljava/util/SortedSet;");
        addEmptyRefMethod(cw, "tailSet", 
                           "()Ljava/util/SetsortedSet;");
        addEmptyRefMethod(cw, "first", "()Ljava/lang/Object;");
        addEmptyRefMethod(cw, "last",   "()Ljava/lang/Object;");

        // --- Set/Collection abstract methods of interface methods ---
        addEmptyRefMethod(cw, "iterator", 
                           "()Ljava/util/Iterator;");
        addEmptyRefMethod(cw, "toArray", 
                           "([Ljava/lang/Object;)[Ljava/lang/Object;");

        addEmptyBoolMethod(cw, "add", "(Ljava/lang/Object;)Z");
        addEmptyBoolMethod(cw, "remove", "(Ljava/lang/Object;)Z");
        addEmptyIntMethod(cw, "size");
        addEmptyBoolMethod(cw, "isEmpty");
        addEmptyRefMethod(cw, "toArray", "()Ljava/lang/Object;");  
        addEmptyBoolMethod(cw, "contains", "(Ljava/lang/Object;)Z");
        addEmptyBoolMethod(cw, "addAll", 
                            "(Ljava/util/Collection;)Z");
        addEmptyBoolMethod(cw, "removeAll", 
                            "(Ljava/util/Collection;)Z");
        addEmptyBoolMethod(cw, "retainAll", 
                            "(Ljava/util/Collection;)Z");
        addEmptyBoolMethod(cw, "containsAll", 
                            "(Ljava/util/Collection;)Z");

        // --- Object methods stubs ---
        addEmptyRefMethod(cw, "toString", "()Ljava/lang/String;");  
        addEmptyIntMethod(cw, "hashCode");


        // clear(): void - empty method
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, 
                             "clear", "()V", null, null);
        mv.visitCode();
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 1);
        mv.visitEnd();

        return cw.toByteArray();
    }

    private static void addEmptyRefMethod(ClassWriter cw, String name, 
                                           String descriptor) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, name, descriptor, 
                                                    null, null);
        mv.visitCode();  
        mv.visitInsn(Opcodes.ACONST_NULL); // Return null as default
        mv.visitEnd();
    }

    private static void addEmptyIntMethod(ClassWriter cw, String name) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, name, "()I", 
                                                    null, null);
        mv.visitCode();  
        mv.visitInsn(Opcodes.ICONST_M1); // Return -1 as default   
        mv.visitEnd();
    }

    private static void addEmptyBoolMethod(ClassWriter cw, String name) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, name, "()Z", 
                                                    null, null);  
        mv.visitCode();  
        mv.visitInsn(3); // Return false as default (ICONST_FALSE)
        mv.visitEnd();
    }

    private static void addEmptyBoolMethod(ClassWriter cw, String name, 
                                            String descriptor) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, name, descriptor, 
                                                    null, null);
        mv.visitCode();  
        mv.visitInsn(3); // Return false as default (ICONST_FALSE for method with params)  
        mv.visitEnd();
    }

    private void assertStreamContains(String expectedValue, InputStream in)  
                                    throws IOException {
        assertStreamContains(null, expectedValue, in);
    }

    private void assertStreamContains(String message, String expectedValue, 
                                      InputStream in) throws IOException {
        String entryValue;
        try {
            StringBuffer stringBuffer = new StringBuffer();
            byte[] bytes = new byte[4096];
            for (int count = in.read(bytes); count != -1; 
                                     count = in.read(bytes)) {
                 stringBuffer.append(new String(bytes, 0, count));
               }
            entryValue = stringBuffer.toString();
          } finally {
             in.close();
           }
         assertEquals(message, expectedValue, entryValue);
        }

    private void assertURLContains(String message, String expectedValue, 
                                   URL resource) throws IOException {
        InputStream in;
        assertNotNull(resource);
        in = resource.openStream();
        assertStreamContains(message, expectedValue, in);
    }

    private static void assertFileExists(File file) {
        assertTrue("File should exist: " + file, file.canRead());
    }

    private static void assertFileNotExists(File file) {
        assertTrue("File should not exist: " + file, !file.canRead());
    }

    protected void setUp() throws Exception {
        super.setUp();
        files = new File[3];
        for (int i = 0; i < files.length; i++) {
            files[i] = createJarFile(i);
         }

        parents = new URLClassLoader[3];
        for (int i = 0; i < parents.length; i++) {
            parents[i] = new URLClassLoader(
                          new URL[]{files[i].toURI().toURL()});
           }

          myFile = createJarFile(33);
         classLoader = createClassLoader(NAME, 
                        new URL[]{myFile.toURI().toURL()}, parents);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        for (int i = 0; i < files.length; i++) {
           files[i].delete();
            }
       }
}
