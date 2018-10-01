/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.xbean.finder.archive;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.RETURN;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.xbean.finder.AnnotationFinder;
import org.apache.xbean.finder.ClassLoaders;
import org.apache.xbean.finder.util.IOUtil;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * TODO: setup toolchain to be able to test with j9?
 */
public class MJarJarArchiveTest {

    private static File jar;

    @ClassRule
    public static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder() {

        @Override
        protected void before() throws Throwable {
            super.before();

            final String version = getVersion();

            jar = File.createTempFile(MJarJarArchiveTest.class.getName(), ".jar");
            JarOutputStream jarOS = null;
            try {
                final Manifest manifest = new Manifest();
                manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
                manifest.getMainAttributes().put(new Attributes.Name("Multi-Release"), "true");
                jarOS = new JarOutputStream(new FileOutputStream(jar), manifest);
                {
                    jarOS.putNextEntry(new JarEntry("org/test/Foo.class"));

                    final ClassWriter writer = new ClassWriter(COMPUTE_FRAMES);
                    writer.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, "org.test.Foo", null, Type.getInternalName(Object.class), new String[0]);

                    final MethodVisitor constructor = writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
                    constructor.visitCode();
                    constructor.visitVarInsn(ALOAD, 0);
                    constructor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
                    constructor.visitInsn(RETURN);
                    constructor.visitMaxs(1, 1);
                    constructor.visitEnd();

                    // invalid bytecode since we don't call writer.visitEnd();, intended to make the test failing

                    jarOS.write(writer.toByteArray());
                    jarOS.closeEntry();
                }
                {
                    jarOS.putNextEntry(new JarEntry("META-INF/"));
                    jarOS.closeEntry();
                }
                {
                    jarOS.putNextEntry(new JarEntry("META-INF/versions/"));
                    jarOS.closeEntry();
                }
                {
                    jarOS.putNextEntry(new JarEntry("META-INF/versions/" + version + "/"));
                    jarOS.closeEntry();
                }
                {
                    jarOS.putNextEntry(new JarEntry("META-INF/versions/9/"));
                    jarOS.closeEntry();
                }
                {
                    jarOS.putNextEntry(new JarEntry("META-INF/versions/" + version + "/org/test/Foo.class"));
                    jarOS.write(createFooClazz().toByteArray());
                    jarOS.closeEntry();
                }
                {
                    jarOS.putNextEntry(new JarEntry("META-INF/versions/9/org/test/Foo.class"));
                    jarOS.write(createFooClazz().toByteArray());
                    jarOS.closeEntry();
                }
            } finally {
                if (jarOS != null) {
                    jarOS.close();
                }
            }
        }
    };

    private static String getVersion() {
        String version = System.getProperty("java.version", "8");
        final int sep = version.indexOf('.');
        if (sep > 0) {
            version = version.substring(0, sep);
        }
        return version;
    }

    @Test
    public void testGetBytecode() throws Exception {
        ensureJava9OrLater();
        final URLClassLoader loader = newMJarClassLoader();
        final JarArchive archive = new JarArchive(loader, jar.toURI().toURL());
        final AnnotationFinder finder = new AnnotationFinder(archive, true);
        assertEquals(1, finder.findAnnotatedMethods(Marker.class).size());
        if (Closeable.class.isInstance(loader)) {
            Closeable.class.cast(loader).close();
        }
    }

    @Test
    public void classLoaderScanningOneUrl() throws Exception {
        ensureJava9OrLater();
        final URLClassLoader loader = newMJarClassLoader();
        final Set<URL> urls = ClassLoaders.findUrlFromResources(loader);
        final Collection<String> testUrls = new ArrayList<String>();
        for (final URL u : urls) {
            final String str = u.toExternalForm();
            if (str.contains("org.apache.xbean.finder.archive.MJarJarArchiveTest")) {
                testUrls.add(str);
            }
        }
        assertEquals(1, testUrls.size());
        if (Closeable.class.isInstance(loader)) {
            Closeable.class.cast(loader).close();
        }
    }

    private void ensureJava9OrLater() {
        assumeTrue(!System.getProperty("java.version", "1").startsWith("1."));
    }

    private static ClassWriter createFooClazz() {
        final ClassWriter writer = new ClassWriter(COMPUTE_FRAMES);
        writer.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, "org/test/Foo", null, Type.getInternalName(Object.class), new String[0]);

        final MethodVisitor constructor = writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(ALOAD, 0);
        constructor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(RETURN);
        constructor.visitMaxs(1, 1);
        constructor.visitEnd();

        final MethodVisitor run = writer.visitMethod(ACC_PUBLIC, "run", "()V", null, null);
        run.visitAnnotation(Type.getDescriptor(Marker.class), true).visitEnd();
        run.visitCode();
        run.visitInsn(RETURN);
        run.visitMaxs(-1, -1);
        run.visitEnd();

        writer.visitEnd();
        return writer;
    }

    private URLClassLoader newMJarClassLoader() throws MalformedURLException {
        return new URLClassLoader(new URL[]{jar.toURI().toURL()}, Thread.currentThread().getContextClassLoader()) {

            @Override
            protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
                if ("org.test.Foo".equals(name)) { // fake the multi version impl until we run for >= j9
                    Class<?> val = super.findLoadedClass(name);
                    if (val == null) {
                        final URL url = super.getResource("META-INF/versions/" + getVersion() + "/org/test/Foo.class");
                        assertNotNull(url);
                        try {
                            final InputStream stream = url.openStream();
                            final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                            IOUtil.copy(stream, buffer);
                            final byte[] loaded = buffer.toByteArray();
                            stream.close();
                            val = super.defineClass(name, loaded, 0, loaded.length);
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }

                    }
                    if (resolve) {
                        resolveClass(val);
                    }
                    return val;
                }
                return super.loadClass(name, resolve);
            }
        };
    }

    @Retention(RUNTIME)
    @Target(METHOD)
    public @interface Marker {}
}