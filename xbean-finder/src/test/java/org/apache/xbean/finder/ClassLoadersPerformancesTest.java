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
package org.apache.xbean.finder;

import org.junit.Test;

import java.io.IOException;
import java.net.URLClassLoader;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public class ClassLoadersPerformancesTest {
    private static final int MAX = 1000;

    @Test
    public void perfs() throws IOException {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (!URLClassLoader.class.isInstance(loader) || System.getProperty("surefire.real.class.path") != null) {
            return; // skip test
        }

        final long startURL = System.nanoTime();
        for (int i = 0; i < MAX; i++) {
            ClassLoaders.findUrls(loader).size();
        }
        final long endURL = System.nanoTime();

        final long startFind = System.nanoTime();
        for (int i = 0; i < MAX; i++) {
            ClassLoaders.findUrlFromResources(loader).size();
        }
        final long endFind = System.nanoTime();

        final long urlTime = endURL - startURL;
        final long findTime = endFind - startFind;
        assertTrue(TimeUnit.NANOSECONDS.toMillis(urlTime) + " < " + TimeUnit.NANOSECONDS.toMillis(findTime), urlTime < findTime);
        System.out.println("getURLs => " + TimeUnit.NANOSECONDS.toMillis(urlTime) + "ms - getResources => " + TimeUnit.NANOSECONDS.toMillis(findTime) + "ms");
    }
}
