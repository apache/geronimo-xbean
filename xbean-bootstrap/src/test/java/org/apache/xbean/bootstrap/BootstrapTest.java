/**
 *
 * Copyright 2006 The Apache Software Foundation
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
package org.apache.xbean.bootstrap;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.5-colossus
 */
public class BootstrapTest extends TestCase {
    private static final String basedir = System.getProperties().getProperty("basedir", ".");

    private static boolean bootCalled;
    private static String[] actualArgs;
    private static Properties actualProperties;
    private static ClassLoader actualClassLoader;

    private Bootstrap bootstrap;

    public void testSetDirectly() throws Exception{
        bootstrap.setBootstrapDirectory(basedir);
        bootstrap.setHomeDirectory("target");
        bootstrap.initialize(Collections.<String>emptyList());
        assertBootable(bootstrap);
    }

    public void testArgs() throws Exception{
        bootstrap.initialize(Arrays.asList(new String[] {
                "-D" + Bootstrap.BOOTSTRAP_DIR + "=" + basedir,
                "-D" + Bootstrap.HOME_DIR + "=target",
        }));
        assertBootable(bootstrap);
    }

    private static void assertBootable(Bootstrap bootstrap) {
        assertEquals(basedir, bootstrap.getBootstrapDirectory());
        try {
            bootstrap.boot();
        } catch (RuntimeException e) {
            e.printStackTrace(System.err);
            throw e;
        } catch (Error e) {
            e.printStackTrace(System.err);
            throw e;
        }

        assertTrue(bootCalled);
        assertNotNull(actualArgs);
        assertNotNull(actualProperties);
        assertNotNull(actualClassLoader);
        assertEquals(basedir, actualProperties.getProperty(Bootstrap.BOOTSTRAP_DIR));
    }

    protected void setUp() throws Exception {
        super.setUp();

        bootCalled = false;
        actualArgs = null;
        actualProperties = null;
        actualClassLoader = null;

        bootstrap = new Bootstrap();
        bootstrap.setBootstrapLoader(TestBootstrapLoader.class.getName());
    }

    public static class TestBootstrapLoader implements BootstrapLoader {
        public void boot(String[] args, Properties properties, ClassLoader classLoader) {
            bootCalled = true;
            actualArgs = args;
            actualProperties = properties;
            actualClassLoader = classLoader;
        }
    }
}
