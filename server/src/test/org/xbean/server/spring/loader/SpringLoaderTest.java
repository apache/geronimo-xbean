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
package org.xbean.server.spring.loader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import junit.framework.TestCase;
import net.sf.cglib.core.DefaultGeneratorStrategy;
import net.sf.cglib.core.NamingPolicy;
import net.sf.cglib.core.Predicate;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.NoOp;
import org.xbean.kernel.Kernel;
import org.xbean.kernel.KernelFactory;
import org.xbean.kernel.ServiceName;
import org.xbean.kernel.StringServiceName;
import org.xbean.server.repository.FileSystemRepository;
import org.xbean.server.spring.loader.SpringLoader;
import org.xbean.server.spring.configuration.SpringConfigurationServiceFactory;
import org.xbean.server.spring.context.ClassLoaderXmlPreprocessor;
import org.xbean.server.spring.context.ClassLoaderXmlPreprocessor;
import org.xbean.spring.context.SpringApplicationContext;

/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class SpringLoaderTest extends TestCase {
    private static final String CLASS_NAME = "TestClass";
    private static final String ENTRY_NAME = "foo";
    private static final String ENTRY_VALUE = "bar";
    private File jarFile;

    public void testLoad() throws Exception{
        Kernel kernel = KernelFactory.newInstance().createKernel("test");

        try {
            File baseDir = new File("src/test/org/xbean/server/spring/loader/").getAbsoluteFile();
            System.setProperty("xbean.base.dir", baseDir.getAbsolutePath());

            FileSystemRepository repository = new FileSystemRepository(new File(".").getAbsoluteFile());
            ClassLoaderXmlPreprocessor classLoaderXmlPreprocessor = new ClassLoaderXmlPreprocessor(repository);
            List xmlPreprocessors = Collections.singletonList(classLoaderXmlPreprocessor);

            SpringLoader springLoader = new SpringLoader();
            springLoader.setKernel(kernel);
            springLoader.setBaseDir(baseDir);
            springLoader.setXmlPreprocessors(xmlPreprocessors);
            ServiceName configurationName = springLoader.load("classpath-xbean");

            kernel.startService(configurationName);

            Object testService = kernel.getService(new StringServiceName("test"));
            assertEquals("TestClass", testService.getClass().getName());
            assertTrue(testService instanceof SortedSet);
        } finally {
            kernel.destroy();
        }
    }

    public void testReload() throws Exception{
        Kernel kernel = KernelFactory.newInstance().createKernel("test");

        try {
            File baseDir = new File("src/test/org/xbean/server/spring/loader/").getAbsoluteFile();
            System.setProperty("xbean.base.dir", baseDir.getAbsolutePath());

            FileSystemRepository repository = new FileSystemRepository(new File(".").getAbsoluteFile());
            ClassLoaderXmlPreprocessor classLoaderXmlPreprocessor = new ClassLoaderXmlPreprocessor(repository);
            List xmlPreprocessors = Collections.singletonList(classLoaderXmlPreprocessor);

            SpringLoader springLoader = new SpringLoader();
            springLoader.setKernel(kernel);
            springLoader.setBaseDir(baseDir);
            springLoader.setXmlPreprocessors(xmlPreprocessors);
            ServiceName configurationName = springLoader.load("classpath-xbean");
            SpringConfigurationServiceFactory serviceFactory = (SpringConfigurationServiceFactory) kernel.getServiceFactory(configurationName);
            SpringApplicationContext applicationContext = serviceFactory.getApplicationContext();

            kernel.startService(configurationName);

            Object testService = kernel.getService(new StringServiceName("test"));
            assertEquals("TestClass", testService.getClass().getName());
            assertEquals(applicationContext.getClassLoader(), testService.getClass().getClassLoader());
            assertTrue(testService instanceof SortedSet);

            kernel.stopService(configurationName);
            kernel.startService(configurationName);

            Object newTestService = kernel.getService(new StringServiceName("test"));
            assertEquals("TestClass", newTestService.getClass().getName());
            assertEquals(applicationContext.getClassLoader(), newTestService.getClass().getClassLoader());
            assertTrue(newTestService instanceof SortedSet);

            assertNotSame("test service should be a new instance",
                    testService, newTestService);

            assertNotSame("test service should have been loaded from a different classloader",
                    testService.getClass().getClassLoader(), newTestService.getClass().getClassLoader());
        } finally {
            kernel.destroy();
        }
    }

    protected void setUp() throws Exception {
        super.setUp();
        jarFile = createJarFile();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        jarFile.delete();
    }

    private static File createJarFile() throws IOException {
        File file = new File("target/SpringLoaderTest.jar");

        FileOutputStream out = new FileOutputStream(file);
        JarOutputStream jarOut = new JarOutputStream(out);

        jarOut.putNextEntry(new JarEntry(CLASS_NAME + ".class"));
        jarOut.write(createClass(CLASS_NAME));

        jarOut.putNextEntry(new JarEntry(ENTRY_NAME));
        jarOut.write(ENTRY_VALUE.getBytes());

        jarOut.close();
        out.close();

        return file;
    }

    private static byte[] createClass(final String name) {
        Enhancer enhancer = new Enhancer();
        enhancer.setNamingPolicy(new NamingPolicy() {
            public String getClassName(String prefix, String source, Object key, Predicate names) {
                return name;
            }
        });
        enhancer.setClassLoader(new URLClassLoader(new URL[0]));
        enhancer.setSuperclass(Object.class);
        enhancer.setInterfaces(new Class[]{SortedSet.class});
        enhancer.setCallbackTypes(new Class[]{NoOp.class});
        enhancer.setUseFactory(false);
        ByteCode byteCode = new ByteCode();
        enhancer.setStrategy(byteCode);
        enhancer.createClass();

        return byteCode.getByteCode();
    }

    private static class ByteCode extends DefaultGeneratorStrategy {
        private byte[] byteCode;

        public byte[] transform(byte[] byteCode) {
            this.byteCode = byteCode;
            return byteCode;
        }

        public byte[] getByteCode() {
            return byteCode;
        }
    }
}
