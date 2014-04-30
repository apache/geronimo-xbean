/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.xbean.naming.reference;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.NamingException;
import javax.naming.spi.ObjectFactory;

/**
 * @version $Rev: 355877 $ $Date: 2005-12-10 18:48:27 -0800 (Sat, 10 Dec 2005) $
 */
public abstract class SimpleReference extends Reference {
    private static final Enumeration<RefAddr> EMPTY_ENUMERATION = new Enumeration<RefAddr>() {
        public boolean hasMoreElements() {
            return false;
        }

        public RefAddr nextElement() {
            throw new NoSuchElementException();
        }
    };

    public SimpleReference() {
        super(null);
    }

    /**
     * Gets the actual referenced Object.
     * @return the referenced object
     * @throws javax.naming.NamingException on error
     */
    public abstract Object getContent() throws NamingException;

    /**
     * We will atleast return an Object.  Subclasses may want to provide a more specific class.
     * @return "java.lang.Object"
     */
    public String getClassName() {
        return "java.lang.Object";
    }

    /**
     * If the JNDI context does not understand simple references, this method will be called
     * to obtain the class name of a factory.  This factory in turn understands the simple
     * reference.  This style is much slower because JNDI will use reflection to load and
     * create this class.
     * @return factory class name
     */
    public final String getFactoryClassName() {
        return SimpleObjectFactory.class.getName();
    }

    //
    // Disabled methods that we no longer need
    //
    public final String getFactoryClassLocation() {
        return null;
    }

    public final RefAddr get(String addrType) {
        return null;
    }

    public final RefAddr get(int posn) {
        throw new ArrayIndexOutOfBoundsException(posn);
    }

    public final Enumeration<RefAddr> getAll() {
        return EMPTY_ENUMERATION;
    }

    public final int size() {
        return 0;
    }

    public final void add(RefAddr addr) {
        throw new UnsupportedOperationException("SimpleReference has no addresses so none can be added");
    }

    public final void add(int posn, RefAddr addr) {
        throw new UnsupportedOperationException("SimpleReference has no addresses so none can be added");
    }

    public final Object remove(int posn) {
        throw new ArrayIndexOutOfBoundsException(posn);
    }

    public final void clear() {
    }

    //
    // Reset the java.lang.Object methods back to default implementations
    //
    public boolean equals(Object obj) {
        return this == obj;
    }

    public int hashCode() {
        return System.identityHashCode(this);
    }

    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }

    public Object clone() {
        throw new UnsupportedOperationException("SimpleReference can not be cloned");
    }

    /**
     * Simply calls getContent() on the SimpleReference
     */
    public static final class SimpleObjectFactory implements ObjectFactory {
        public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable environment) throws Exception {
            if (obj instanceof SimpleReference) {
                SimpleReference reference = (SimpleReference) obj;
                return reference.getContent();
            }
            return null;
        }
    }
}
