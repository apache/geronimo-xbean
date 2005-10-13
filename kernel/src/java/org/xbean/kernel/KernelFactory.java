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
package org.xbean.kernel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import org.xbean.kernel.standard.StandardKernelFactory;

/**
 * The Kernel factory is used to construct and locate Kernels.  This class is loosly based on the SAXParserFactory and
 * the JMX MBeanServerFactory.  To constuct a kernel use the following:
 * <p><blockquote><pre>
 * Kernel kernel = KernelFactory.newInstance().createKernel(name);
 * </pre></blockquote>
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public abstract class KernelFactory {
    /**
     * The name of the system property and META-INF/services used to locate the kernel factory class.
     */
    public static final String KERNEL_FACTORY_KEY = KernelFactory.class.getName();

    private static final ConcurrentHashMap kernels = new ConcurrentHashMap(1);

    /**
     * Gets the kernel registered under the specified name.  If no kernel is registered with the specified name, null
     * is returned.
     *
     * @param name the name of the kernel to return
     * @return the kernel or null if no kernel is registered under the specified name
     */
    public static Kernel getKernel(String name) {
        if (name == null) throw new NullPointerException("name is null");
        return (Kernel) kernels.get(name);
    }

    /**
     * Gets a map of the existing kernels by kernel name.
     *
     * @return the existing kernels by kernel name.
     */
    public static Map getKernels() {
        return new HashMap(kernels);
    }
    
    /**
     * Creates a kernel with the specified name.  This method will attempt to locate a KernelFactory implementation
     * using the following procedure
     * <ul> <li>
     * The org.xbean.kernel.KernelFactory system property.
     * </li> <li>
     * Use the <a href="http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html">Jar Service Specification</a>
     * This method will attempt to get the factory name from the file
     * META-INF/services/org.xbean.kernel.KernelFactory loaded using the thread context class loader.
     * </li>
     * <li>
     * The StandardKernel implementation.
     * </li>
     * </ul>
     * The factory class is loaded and constucted using the thread context class loader, if present, or the class
     * loader of this class.
     *
     * @return the kernel factory implementation
     * @throws KernelFactoryError if the specified kernel factory can not be created
     */
    public static KernelFactory newInstance() throws KernelFactoryError {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = KernelFactory.class.getClassLoader();
        }

        // System property
        try {
            String kernelFactoryName = System.getProperty(KERNEL_FACTORY_KEY);
            if (kernelFactoryName != null) {
                return createKernelFactory(kernelFactoryName, classLoader);
            }
        } catch (SecurityException se) {
        }

        // Jar Service Specification - http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html
        String serviceId = "META-INF/services/" + KERNEL_FACTORY_KEY;
        InputStream inputStream = null;
        try {
            inputStream = classLoader.getResourceAsStream(serviceId);
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                String kernelFactoryName = reader.readLine();
                reader.close();

                if (kernelFactoryName != null && kernelFactoryName.length() > 0) {
                    return createKernelFactory(kernelFactoryName, classLoader);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                }
                inputStream = null;
            }
        }

        // Default is the standard kernel
        return new StandardKernelFactory();
    }

    /**
     * Removes the kernel instance from the internal kernel registry.  This method should only be called by the kernel
     * instance itself during destruction.
     * @param kernel the kernel to destroy
     * @throws KernelFactoryError if the kernel is still running
     */
    public static void destroyInstance(Kernel kernel) throws KernelFactoryError {
        if (kernel.isRunning()) {
            throw new KernelFactoryError("Kernel is running: name" + kernel.getKernelName());
        }

        kernels.remove(kernel.getKernelName(), kernel);
    }

    private static KernelFactory createKernelFactory(String className, ClassLoader classLoader) throws KernelFactoryError {
        try {
            return (KernelFactory) classLoader.loadClass(className).newInstance();
        } catch (ClassCastException e) {
            throw new KernelFactoryError("Kernel factory class does not implement KernelFactory: " + className);
        } catch (ClassNotFoundException e) {
            throw new KernelFactoryError("Kernel factory class not found: " + className);
        } catch (Exception e) {
            throw new KernelFactoryError("Unable to instantiate kernel factory class: " + className, e);
        }
    }

    /**
     * Creates a new kernel instance and registers it with the static KernelFactory registry.  This allows the kernel
     * to be retrieved from the {@link KernelFactory#getKernel(String)} method.
     *
     * @param name the name of the kernel to create
     * @return the new kernel
     * @throws KernelAlreadyExistsException is a kernel already exists with the specified name
     */
    public final Kernel createKernel(String name) throws KernelAlreadyExistsException {
        if (name == null) throw new NullPointerException("name is null");

        // quick check to see if a kernel already registerd wit the name
        if (kernels.containsKey(name)) {
            throw new KernelAlreadyExistsException(name);
        }

        // create the kernel -- this may be an unnecessary construction, but it shouldn't be a big deal
        Kernel kernel = createKernelInternal(name);

        // register the kernel, checking if someone snuck in an registered a kernel while we were creating ours
        if (kernels.putIfAbsent(name, kernel) != null) {
            throw new KernelAlreadyExistsException(name);
        }

        return kernel;
    }

    /**
     * Creates the actual kernel instance which will be registerd in the KernelFactory.
     *
     * @param name the kernel name
     * @return a new kernel instance
     */
    protected abstract Kernel createKernelInternal(String name);
}
