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
package org.apache.xbean.naming.context;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import javax.naming.LinkRef;
import java.io.Serializable;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;

public abstract class AbstractContext implements Context, ContextFactory, Serializable {
    private static final long serialVersionUID = 6481918425692261483L;
    private final String nameInNamespace;

    protected AbstractContext(String nameInNamespace) {
        this.nameInNamespace = nameInNamespace;
    }


    public void close() throws NamingException {
        //Ignore. Explicitly do not close the context
    }

    protected abstract void removeBindings(Name name) throws NamingException;
    protected abstract Object lookup(String stringName, Name parsedName) throws NamingException;
    protected abstract void addBinding(Name name, Object obj, boolean rebind) throws NamingException;
    protected abstract Map getBindings() throws NamingException;

    /**
     * Gets the name of this context withing the global namespace.  This method may return null
     * if the location of the node in the global namespace is not known
     * @return the name of this context within the global namespace or null if unknown.
     */
    public String getNameInNamespace() {
        return nameInNamespace;
    }

    /**
     * Gets the name of a path withing the global namespace context.
     */
    protected String getNameInNamespace(String path) {
        String nameInNamespace = getNameInNamespace();
        if (nameInNamespace == null || nameInNamespace.length() == 0) {
            return path;
        } else {
            return nameInNamespace + "/" + path;
        }
    }

    //
    // Environment
    //

    /**
     * Always returns a new (empty) Hashtable.
     * @return a new (empty) Hashtable
     */
    public Hashtable getEnvironment() {
        return new Hashtable();
    }

    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        if (propName == null) throw new NullPointerException("propName is null");
        if (propVal == null) throw new NullPointerException("propVal is null");

        Map env = getEnvironment();
        return env.put(propName, propVal);
    }

    public Object removeFromEnvironment(String propName) throws NamingException {
        if (propName == null) throw new NullPointerException("propName is null");

        Map env = getEnvironment();
        return env.remove(propName);
    }

    //
    // Name handling
    //

    /**
     * A parser that can turn Strings into javax.naming.Name objects.
     * @return ContextUtil.NAME_PARSER
     */
    protected NameParser getNameParser() {
        return ContextUtil.NAME_PARSER;
    }

    public NameParser getNameParser(Name name) {
        return getNameParser();
    }

    public NameParser getNameParser(String name) {
        return getNameParser();
    }

    public Name composeName(Name name, Name prefix) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");
        if (prefix == null) throw new NullPointerException("prefix is null");

        Name result = (Name) prefix.clone();
        result.addAll(name);
        return result;
    }

    public String composeName(String name, String prefix) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");
        if (prefix == null) throw new NullPointerException("prefix is null");

        CompositeName result = new CompositeName(prefix);
        result.addAll(new CompositeName(name));
        return result.toString();
    }

    //
    // Lookup
    //

    public Object lookup(String name) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");

        Object value = lookup(name, null);

        // if we got a link back we need to resolve it
        if (value instanceof LinkRef) {
            LinkRef linkRef = (LinkRef) value;
            value = lookup(linkRef.getLinkName());
        }

        return value;
    }

    public Object lookup(Name name) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");

        Object value = lookup(null, name);


        // if we got a link back we need to resolve it
        if (value instanceof LinkRef) {
            LinkRef linkRef = (LinkRef) value;
            value = lookup(linkRef.getLinkName());
        }

        return value;
    }

    public Object lookupLink(String name) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");
        return lookup(name, null);
    }

    public Object lookupLink(Name name) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");
        return lookup(null, name);
    }

    //
    // Bind, rebind, rename and unbind
    //

    public void bind(String name, Object obj) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");
        if (name.length() == 0) {
            throw new NameAlreadyBoundException("Cannot bind to an empty name (this context)");
        }
        bind(new CompositeName(name), obj);
    }

    public void bind(Name name, Object obj) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");
        if (name.isEmpty()) {
            throw new NameAlreadyBoundException("Cannot bind to an empty name (this context)");
        }
        addBinding(name, obj, false);
    }

    public void rebind(String name, Object obj) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");
        rebind(new CompositeName(name), obj);
    }

    public void rebind(Name name, Object obj) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");
        if (name.isEmpty()) {
            throw new NameAlreadyBoundException("Cannot rebind an empty name (this context)");
        }
        addBinding(name, obj, true);
    }

    public void rename(String oldName, String newName) throws NamingException {
        if (oldName == null) throw new NullPointerException("oldName is null");
        if (newName == null) throw new NullPointerException("newName is null");
        rename(new CompositeName(oldName), new CompositeName(newName));
    }

    public void rename(Name oldName, Name newName) throws NamingException {
        if (oldName == null || newName == null) {
            throw new NullPointerException("name is null");
        } else if (oldName.isEmpty() || newName.isEmpty()) {
            throw new InvalidNameException("Name cannot be empty");
        } else if (this.lookup(newName) != null) {
            throw new NameAlreadyBoundException("The target name is already bound");
        }
        this.bind(newName, this.lookup(oldName));
        this.unbind(oldName);
    }

    public void unbind(String name) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");
        unbind(new CompositeName(name));
    }

    public void unbind(Name name) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");
        if (name.isEmpty()) {
            throw new InvalidNameException("Cannot unbind empty name");
        }
        removeBindings(name);
    }

    //
    // List
    //

    protected NamingEnumeration list() throws NamingException {
        Map bindings = getBindings();
        return new ContextUtil.ListEnumeration(bindings);
    }

    protected NamingEnumeration listBindings() throws NamingException {
        Map bindings = getBindings();
        return new ContextUtil.ListBindingEnumeration(bindings);
    }

    public NamingEnumeration list(String name) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");

        // if the name is empty, list the current context
        if (name.length() == 0) {
            return list();
        }

        // lookup the target context
        Object target = lookup(name);

        if (target == this) {
            return list();
        } else if (target instanceof Context) {
            return ((Context) target).list("");
        } else {
            throw new NotContextException("The name " + name + " cannot be listed");
        }
    }

    public NamingEnumeration list(Name name) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");

        // if the name is empty, list the current context
        if (name.isEmpty()) {
            return list();
        }

        // lookup the target context
        Object target = lookup(name);

        if (target == this) {
            return list();
        } else if (target instanceof Context) {
            return ((Context) target).list("");
        } else {
            throw new NotContextException("The name " + name + " cannot be listed");
        }
    }

    public NamingEnumeration listBindings(String name) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");

        // if the name is empty, list the current context
        if (name.length() == 0) {
            return listBindings();
        }

        // lookup the target context
        Object target = lookup(name);

        if (target == this) {
            return listBindings();
        } else if (target instanceof Context) {
            return ((Context) target).listBindings("");
        } else {
            throw new NotContextException("The name " + name + " cannot be listed");
        }
    }

    public NamingEnumeration listBindings(Name name) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");

        // if the name is empty, list the current context
        if (name.isEmpty()) {
            return listBindings();
        }

        // lookup the target context
        Object target = lookup(name);

        if (target == this) {
            return listBindings();
        } else if (target instanceof Context) {
            return ((Context) target).listBindings("");
        } else {
            throw new NotContextException("The name " + name + " cannot be listed");
        }
    }

    //
    // Subcontexts
    //

    public Context createSubcontext(String name) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");
        return createSubcontext(new CompositeName(name));
    }

    public Context createSubcontext(Name name) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");
        if (name.isEmpty()) {
            throw new InvalidNameException("Cannot create a subcontext if the name is empty");
        }
        Context abstractContext = createContext(name.toString(), Collections.EMPTY_MAP);
        addBinding(name, abstractContext, false);
        return abstractContext;
    }

    public void destroySubcontext(String name) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");
        destroySubcontext(new CompositeName(name));
    }

    public void destroySubcontext(Name name) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");
        if (name.isEmpty()) {
            throw new InvalidNameException("Cannot destroy subcontext with empty name");
        }
        unbind(name);
    }
}
