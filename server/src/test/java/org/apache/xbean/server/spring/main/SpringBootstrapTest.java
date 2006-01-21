/**
 *
 * Copyright 2005-2006 The Apache Software Foundation or its licensors, as applicable.
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
package org.apache.xbean.server.spring.main;

import junit.framework.TestCase;
import org.apache.xbean.kernel.Kernel;
import org.apache.xbean.kernel.KernelFactory;
import org.apache.xbean.server.main.KernelMain;

/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class SpringBootstrapTest extends TestCase {
    private static final String basedir = System.getProperties().getProperty("basedir", ".");
    private SpringBootstrap springBootstrap;

    public void testClasspathBootstrap() throws Exception{
        springBootstrap.setConfigurationFile("META-INF/xbean-bootstrap.xml");
        springBootstrap.setServerBaseDirectory(basedir);
        assertBootable(springBootstrap);
    }

    public void testFileBootstrapWithAbsoluteConfigPath() throws Exception {
        springBootstrap.setConfigurationFile(basedir + "/src/main/resources/META-INF/xbean-bootstrap.xml");
        springBootstrap.setServerBaseDirectory(basedir);
        assertBootable(springBootstrap);
    }

    public void testFileBootstrapWithRelativeConfigPath() throws Exception {
        springBootstrap.setConfigurationFile("src/main/resources/META-INF/xbean-bootstrap.xml");
        springBootstrap.setServerBaseDirectory(basedir);
        assertBootable(springBootstrap);
    }

    private static void assertBootable(SpringBootstrap springBootstrap) {
        // load the main instance
        KernelMain main = (KernelMain) springBootstrap.loadMain();

        // we don't want to start a daemon
        main.setDaemon(false);

        // verify a kernel was registered
        Kernel kernel = (Kernel) KernelFactory.getKernels().values().iterator().next();
        assertEquals(kernel, KernelFactory.getKernels().values().iterator().next());
        assertEquals(kernel, KernelFactory.getKernel(kernel.getKernelName()));

        // now boot the main instance
        main.main(new String[0]);

        // verify the kernel is destroyed (the kernel main destroys the kernel on exit)
        assertFalse(kernel.isRunning());

        // verify a kernel was unregistered
        assertFalse(KernelFactory.getKernels().values().iterator().hasNext());
        assertNull(KernelFactory.getKernel(kernel.getKernelName()));
    }

    protected void setUp() throws Exception {
        super.setUp();
        springBootstrap = new SpringBootstrap();
    }
}
