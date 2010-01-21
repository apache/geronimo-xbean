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
package org.apache.xbean.naming.context;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NameParser;
import javax.naming.NameClassPair;
import javax.naming.Binding;

import java.util.Hashtable;

/**
 * @version $Rev$ $Date$
 */
public abstract class ContextFlyweight implements Context {
    protected abstract Context getContext() throws NamingException;

    protected Name getName(Name name) throws NamingException {
        return name;
    }

    protected String getName(String name) throws NamingException {
        return name;
    }

    public void close() throws NamingException {
    }

    public String getNameInNamespace() throws NamingException {
        return getContext().getNameInNamespace();
    }

    public Object lookup(Name name) throws NamingException {
        return getContext().lookup(getName(name));
    }

    public Object lookup(String name) throws NamingException {
        return getContext().lookup(getName(name));
    }

    public void bind(Name name, Object obj) throws NamingException {
        getContext().bind(getName(name), obj);
    }

    public void bind(String name, Object obj) throws NamingException {
        getContext().bind(getName(name), obj);
    }

    public void rebind(Name name, Object obj) throws NamingException {
        getContext().rebind(getName(name), obj);
    }

    public void rebind(String name, Object obj) throws NamingException {
        getContext().rebind(getName(name), obj);
    }

    public void unbind(Name name) throws NamingException {
        getContext().unbind(getName(name));
    }

    public void unbind(String name) throws NamingException {
        getContext().unbind(getName(name));
    }

    public void rename(Name oldName, Name newName) throws NamingException {
        getContext().rename(getName(oldName), getName(newName));
    }

    public void rename(String oldName, String newName) throws NamingException {
        getContext().rename(getName(oldName), getName(newName));
    }

    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        return getContext().list(getName(name));
    }

    public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
        return getContext().list(getName(name));
    }

    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        return getContext().listBindings(getName(name));
    }

    public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
        return getContext().listBindings(getName(name));
    }

    public void destroySubcontext(Name name) throws NamingException {
        getContext().destroySubcontext(getName(name));
    }

    public void destroySubcontext(String name) throws NamingException {
        getContext().destroySubcontext(getName(name));
    }

    public Context createSubcontext(Name name) throws NamingException {
        return getContext().createSubcontext(getName(name));
    }

    public Context createSubcontext(String name) throws NamingException {
        return getContext().createSubcontext(getName(name));
    }

    public Object lookupLink(Name name) throws NamingException {
        return getContext().lookupLink(getName(name));
    }

    public Object lookupLink(String name) throws NamingException {
        return getContext().lookupLink(getName(name));
    }

    public NameParser getNameParser(Name name) throws NamingException {
        return getContext().getNameParser(getName(name));
    }

    public NameParser getNameParser(String name) throws NamingException {
        return getContext().getNameParser(getName(name));
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
