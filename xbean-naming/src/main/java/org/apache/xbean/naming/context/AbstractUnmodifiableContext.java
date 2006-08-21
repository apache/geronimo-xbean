/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
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

package org.apache.xbean.naming.context;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import java.io.Serializable;
import java.util.Map;

/**
 * @version $Rev: 355877 $ $Date: 2005-12-10 18:48:27 -0800 (Sat, 10 Dec 2005) $
 */
public abstract class AbstractUnmodifiableContext extends AbstractContext implements Context, NestedContextFactory, Serializable {
    private static final long serialVersionUID = 3808693663629444493L;

    protected AbstractUnmodifiableContext(String nameInNamespace) {
        super(nameInNamespace);
    }

    protected final void addBinding(Name name, Object obj, boolean rebind) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    //
    //  Lookup Binding
    //

    /**
     * Gets the object bound to the name.  The name may contain slashes.
     * @param name the name
     * @return the object bound to the name, or null if not found
     */
    protected Object getDeepBinding(String name) {
        return null;
    }

    /**
     * Finds the specified entry.  Normally there is no need to override this method; instead you should
     * simply implement the getDeepBindings(String) and getBindings(String) method.
     *
     * This method will follow links except for the final element which is always just returned without
     * inspection.  This means this method can be used to implement lookupLink.
     *
     * @param stringName the string version of the name; maybe null
     * @param parsedName the parsed name; may be null
     * @return the value bound to the name
     * @throws NamingException if no value is bound to that name or if a problem occurs during the lookup
     */
    protected Object lookup(String stringName, Name parsedName) throws NamingException {
        if (stringName == null && parsedName == null) {
            throw new IllegalArgumentException("Both stringName and parsedName are null");
        }
        if (stringName == null) stringName = parsedName.toString();

        // try to look up the name directly (this is the fastest path)
        Object directLookup = getDeepBinding(stringName);
        if (directLookup != null) {
            return ContextUtil.resolve(stringName, directLookup);
        }

        Object value = super.lookup(stringName, parsedName);
        return value;
    }

    //
    //  List Bindings
    //

    /**
     * Gets a map of the bindings for the current node (i.e., no names with slashes).
     * This method must not return null.
     */
    protected Map getBindings() throws NamingException {
        throw new OperationNotSupportedException("This context is not listable");
    }

    //
    //  Remove Binding
    //

    /**
     * Removes the binding from the context.  The name will not contain a path and the value will not
     * be a nested context although it may be a foreign context.
     * @param name name under which the value should be bound
     * @throws NamingException if a problem occurs during the bind such as a value already being bound
     */
    protected void removeBinding(String name) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    // ==================================================================================
    // =================== Final Methods (see methods above) ============================
    // ==================================================================================

    //
    //  Unsupported Operations
    //

    public final void bind(Name name, Object obj) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final void bind(String name, Object obj) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final void close() throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final Context createSubcontext(Name name) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final Context createSubcontext(String name) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final void destroySubcontext(Name name) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final void destroySubcontext(String name) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final void rebind(Name name, Object obj) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final void rebind(String name, Object obj) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final void rename(Name oldName, Name newName) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final void rename(String oldName, String newName) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final void unbind(Name name) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final void unbind(String name) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }
}
