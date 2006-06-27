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
package org.gbean.classloader;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.Map;

import org.apache.commons.logging.LogFactory;

/**
 * @version $Revision$ $Date$
 */
public class NamedClassLoader extends URLClassLoader implements DestroyableClassLoader {
    private final String name;

    public NamedClassLoader(String name, URL[] urls) {
        super(urls);
        this.name = name;
    }

    public NamedClassLoader(String name, URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.name = name;
    }

    public NamedClassLoader(String name, URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
        this.name = name;
    }

    public void destroy() {
        LogFactory.release(this);
        clearSoftCache(ObjectInputStream.class, "subclassAudits");
        clearSoftCache(ObjectOutputStream.class, "subclassAudits");
        clearSoftCache(ObjectStreamClass.class, "localDescs");
        clearSoftCache(ObjectStreamClass.class, "reflectors");
    }

    public String getName() {
        return name;
    }

    private static Object clearSoftCacheLock = new Object();
    private static boolean clearSoftCacheFailed = false;
    private void clearSoftCache(Class clazz, String fieldName) {
        Map cache = null;
        try {
            Field f = clazz.getDeclaredField(fieldName);
            f.setAccessible(true);
            cache = (Map) f.get(null);
        } catch (Throwable e) {
            synchronized (clearSoftCacheLock) {
                // only print the failed message once per vm
                if (!clearSoftCacheFailed) {
                    clearSoftCacheFailed = true;
                    LogFactory.getLog(JarFileClassLoader.class).error("Unable to clear SoftCache field " + fieldName + " in class " + clazz);
                }
            }
        }

        if (cache != null) {
            synchronized (cache) {
                cache.clear();
            }
        }
    }
}
