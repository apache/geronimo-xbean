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

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClassLoadersTest {
    @Test
    public void testNative() throws MalformedURLException {
        final String base = "file:/usr/lib/x86_64-linux-gnu/jni/libatk-wrapper.so.0.0.18";

        assertTrue(ClassLoaders.isNative(new URL(base)));
        assertFalse(ClassLoaders.isNative(new URL(base + ".jar")));
        assertTrue(ClassLoaders.isNative(new URL("jar:" + base + "!/")));
        assertFalse(ClassLoaders.isNative(new URL("jar:" + base + ".jar!/")));
    }
}
