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
package org.xbean.server.spring.main;

import junit.framework.TestCase;
import org.xbean.kernel.Kernel;
import org.xbean.kernel.KernelFactory;
import org.xbean.server.main.KernelMain;
import org.xbean.server.spring.main.SpringBootstrap;

/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class SpringBootstrapTest extends TestCase {
    private SpringBootstrap springBootstrap;

    public void testClasspathBootstrap() throws Exception{
        springBootstrap.setConfigurationFile("META-INF/xbean-bootstrap.xml");
        springBootstrap.setServerBaseDirectory(".");
        assertBootable(springBootstrap);
    }

    public void testFileBootstrap() throws Exception{
        springBootstrap.setConfigurationFile("src/resources/META-INF/xbean-bootstrap.xml");
        springBootstrap.setServerBaseDirectory(".");
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
