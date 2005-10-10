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
package org.gbean.server.classloader;

import java.util.Map;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;

/**
 * Utility methods for class loader manipulation in a server environment.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public final class ClassLoaderUtil {
    private ClassLoaderUtil() {
    }

    /**
     * Cleans well known class loader leaks in VMs and libraries.  There is a lot of bad code out there and this method
     * will clear up the know problems.  This method should only be called when the class loader will no longer be used.
     * It this method is called two often it can have a serious impact on preformance.
     * @param classLoader the class loader to destroy
     */
    public static void destroy(ClassLoader classLoader) {
        releaseCommonsLoggingCache(classLoader);
        clearSunSoftCache(ObjectInputStream.class, "subclassAudits");
        clearSunSoftCache(ObjectOutputStream.class, "subclassAudits");
        clearSunSoftCache(ObjectStreamClass.class, "localDescs");
        clearSunSoftCache(ObjectStreamClass.class, "reflectors");
    }

    /**
     * Clears the caches maintained by the SunVM object stream implementation.  This method uses reflection and
     * setAccessable to obtain access to the Sun cache.  The cache is locked with a synchronize monitor and cleared.
     * This method completely clears the class loader cache which will impact preformance of object serialization.
     * @param clazz the name of the class containing the cache field
     * @param fieldName the name of the cache field
     */
    public static void clearSunSoftCache(Class clazz, String fieldName) {
        Map cache = null;
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            cache = (Map) field.get(null);
        } catch (Throwable ignored) {
            // there is nothing a user could do about this anyway
        }

        if (cache != null) {
            synchronized (cache) {
                cache.clear();
            }
        }
    }

    /**
     * Releases the specified classloader from the Apache Jakarta Commons Logging class loader cache using reflection.
     * @param classLoader the class loader to release
     */
    public static void releaseCommonsLoggingCache(ClassLoader classLoader) {
        try {
            Class logFactory = classLoader.loadClass("org.apache.commons.logging.LogFactory");
            Method release = logFactory.getMethod("release", new Class[] {ClassLoader.class});
            release.invoke(null, new Object[] {classLoader});
        } catch (Throwable ignored) {
            // there is nothing a user could do about this anyway
        }
    }

}
