/*
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
package org.apache.xbean.finder;

import org.apache.xbean.finder.archive.Archives;
import org.apache.xbean.finder.archive.JarArchive;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class FileDecodeFinderTest {

    @Test
    public void testSharp() throws Exception {
        doTest(createLib("folder##"));
    }

    @Test
    public void testSpace() throws Exception {
        doTest(createLib("folder with a space"));
    }

    private void doTest(final File lib) throws MalformedURLException {
        final AnnotationFinder all = new AnnotationFinder(new JarArchive(Thread.currentThread().getContextClassLoader(), lib.toURI().toURL()));
        assertEquals(1, all.findAnnotatedClasses(Marker.class).size());
    }

    private File createLib(final String name) throws IOException {
        final File jar = new File("target/" + name + "/lib/dep.jar"); // target is useless with xbean surefire config but better in ide ;)
        jar.getParentFile().mkdirs();
        Archives.jarArchive(jar, Collections.<String, String>emptyMap(), Marker.class, SuperSimpleBean.class);
        return jar;
    }

    @java.lang.annotation.Target(value = {java.lang.annotation.ElementType.TYPE})
    @java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
    public @interface Marker {}

    @Marker public static class SuperSimpleBean {}
}
