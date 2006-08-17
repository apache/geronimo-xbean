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

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NameParser;
import java.util.Hashtable;

/**
 * @version $Rev$ $Date$
 */
public abstract class ContextFlyweight implements Context {
    protected abstract Context getContext() throws NamingException;

    public void close() throws NamingException {
    }

    public String getNameInNamespace() throws NamingException {
        return getContext().getNameInNamespace();
    }

    public Object lookup(Name name) throws NamingException {
        return getContext().lookup(name);
    }

    public Object lookup(String name) throws NamingException {
        return getContext().lookup(name);
    }

    public void bind(Name name, Object obj) throws NamingException {
        getContext().bind(name, obj);
    }

    public void bind(String name, Object obj) throws NamingException {
        getContext().bind(name, obj);
    }

    public void rebind(Name name, Object obj) throws NamingException {
        getContext().rebind(name, obj);
    }

    public void rebind(String name, Object obj) throws NamingException {
        getContext().rebind(name, obj);
    }

    public void unbind(Name name) throws NamingException {
        getContext().unbind(name);
    }

    public void unbind(String name) throws NamingException {
        getContext().unbind(name);
    }

    public void rename(Name oldName, Name newName) throws NamingException {
        getContext().rename(oldName, newName);
    }

    public void rename(String oldName, String newName) throws NamingException {
        getContext().rename(oldName, newName);
    }

    public NamingEnumeration list(Name name) throws NamingException {
        return getContext().list(name);
    }

    public NamingEnumeration list(String name) throws NamingException {
        return getContext().list(name);
    }

    public NamingEnumeration listBindings(Name name) throws NamingException {
        return getContext().listBindings(name);
    }

    public NamingEnumeration listBindings(String name) throws NamingException {
        return getContext().listBindings(name);
    }

    public void destroySubcontext(Name name) throws NamingException {
        getContext().destroySubcontext(name);
    }

    public void destroySubcontext(String name) throws NamingException {
        getContext().destroySubcontext(name);
    }

    public Context createSubcontext(Name name) throws NamingException {
        return getContext().createSubcontext(name);
    }

    public Context createSubcontext(String name) throws NamingException {
        return getContext().createSubcontext(name);
    }

    public Object lookupLink(Name name) throws NamingException {
        return getContext().lookupLink(name);
    }

    public Object lookupLink(String name) throws NamingException {
        return getContext().lookupLink(name);
    }

    public NameParser getNameParser(Name name) throws NamingException {
        return getContext().getNameParser(name);
    }

    public NameParser getNameParser(String name) throws NamingException {
        return getContext().getNameParser(name);
    }

    public Name composeName(Name name, Name prefix) throws NamingException {
        return getContext().composeName(name, prefix);
    }

    public String composeName(String name, String prefix) throws NamingException {
        return getContext().composeName(name, prefix);
    }

    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        return getContext().addToEnvironment(propName, propVal);
    }

    public Object removeFromEnvironment(String propName) throws NamingException {
        return getContext().removeFromEnvironment(propName);
    }

    public Hashtable getEnvironment() throws NamingException {
        return getContext().getEnvironment();
    }
}
