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

import org.apache.geronimo.naming.enc.EnterpriseNamingContextNameParser;

import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.LinkRef;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

public class GeronimoGlobalContext implements Context {
    protected Hashtable env;
    protected Map bindings;
    protected GeronimoGlobalContext parent;
    protected Name contextAtomicName;

    public GeronimoGlobalContext() {
        bindings = new HashMap();
        env = new Hashtable();
    }

    protected GeronimoGlobalContext(GeronimoGlobalContext parent, Hashtable environment, Name contextAtomicName) {
        this.env = environment;
        this.parent = parent;
        bindings = new HashMap();
        this.contextAtomicName = contextAtomicName;
    }

    public GeronimoGlobalContext(Hashtable env) {
        if (env == null) {
            this.env = new Hashtable();
        } else {
            this.env = new Hashtable(env);
        }
        bindings = new HashMap();
    }

    protected GeronimoGlobalContext(GeronimoGlobalContext clone, Hashtable env) {
        if (env == null) {
            this.env = new Hashtable();
        } else {
            this.env = new Hashtable(env);
        }
        bindings = clone.bindings;
    }

    public void close() throws NamingException {
        //Ignore. Explicitly do not close the context
    }

    public String getNameInNamespace() throws NamingException {
        GeronimoGlobalContext parentContext = parent;
        if (parentContext == null) {
            return "ROOT CONTEXT";
        }
        Name name = EnterpriseNamingContextNameParser.INSTANCE.parse("");
        name.add(contextAtomicName.toString());

        // Get parent's names
        while (parentContext != null && parentContext.contextAtomicName != null) {
            name.add(0, parentContext.contextAtomicName.toString());
            parentContext = parentContext.parent;
        }
        return name.toString();
    }

    public void destroySubcontext(String name) throws NamingException {
        destroySubcontext(new CompositeName(name));
    }

    public void unbind(String name) throws NamingException {
        unbind(new CompositeName(name));
    }

    public Hashtable getEnvironment() throws NamingException {
        return env;
    }

    public void destroySubcontext(Name name) throws NamingException {
        if (name.isEmpty()) {
            throw new InvalidNameException("Cannot destroy subcontext with empty name");
        }
        unbind(name);
    }

    public void unbind(Name name) throws NamingException {
        if (name.isEmpty()) {
            throw new InvalidNameException("Cannot unbind empty name");
        }
        Map bindings = this.bindings;
        if (name.size() == 1) {
            synchronized (bindings) {
                bindings.remove(name.toString());
            }
        } else {
            String segment = null;
            int lastIndex = name.size() - 1;
            Object terminalContext = null;
            for (int i = 0; i < lastIndex; i++) {
                segment = name.get(i);
                terminalContext = bindings.get(segment);
                if (terminalContext == null) {
                    throw new NamingException("The intermediate context "
                            + segment + " does not exist");
                } else if (!(terminalContext instanceof Context)) {
                    throw new NameAlreadyBoundException(
                            " An object that is not a context is already bound at element "
                                    + segment + "of name " + name);
                } else {
                    bindings = ((GeronimoGlobalContext) terminalContext).bindings;
                }
            }
            segment = name.get(lastIndex);
            ((Context) terminalContext).unbind(segment);
        }
    }

    public Object lookup(String name) throws NamingException {
        return lookup(new CompositeName(name));
    }

    public Object lookupLink(String name) throws NamingException {
        return lookupLink(new CompositeName(name));
    }

    public Object removeFromEnvironment(String propName) throws NamingException {
        if (propName == null) {
            throw new NamingException("The parameter for this method cannot be null");
        }
        Object obj = env.get(propName);
        env.remove(propName);
        return obj;
    }

    public void bind(String name, Object obj) throws NamingException {
        bind(new CompositeName(name), obj);
    }

    public void rebind(String name, Object obj) throws NamingException {
        rebind(new CompositeName(name), obj);
    }

    public Object lookup(Name name) throws NamingException {
        return internalLookup(name, true);
    }

    protected Object internalLookup(Name name, boolean resolveLinks) throws NamingException {
        Object result = null;
        Map bindings = this.bindings;
        Object terminalContext = null;
        if (name.isEmpty()) {
            return new GeronimoGlobalContext(this, this.env);
        }
        int index = name.get(0).indexOf(':');
        if (index != -1) {
            String temp = name.get(0).substring(index + 1);
            name.remove(0);
            name.add(0, temp);
        }
        if (name.size() == 1) {
            result = bindings.get(name.toString());
        } else {
            String segment = null;
            int lastIndex = name.size() - 1;
            for (int i = 0; i < lastIndex; i++) {
                segment = name.get(i);
                terminalContext = bindings.get(segment);
                if (terminalContext == null) {
                    throw new NamingException("The intermediate context "
                            + segment + " does not exist");
                } else if (!(terminalContext instanceof Context)) {
                    throw new NamingException(
                            "One of the intermediate names i.e. "
                                    + segment
                                    + " refer to an object that is not a context");
                } else {
                    bindings = ((GeronimoGlobalContext) terminalContext).bindings;
                }
            }
            segment = name.get(lastIndex);
            result = ((Context) terminalContext).lookup(segment);
        }

        if (result instanceof LinkRef) {
            LinkRef ref = (LinkRef) result;
            result = lookup(ref.getLinkName());
        }

        return result;

    }

    public Object lookupLink(Name name) throws NamingException {
        if (name.isEmpty()) {
            throw new InvalidNameException("Name cannot be empty");
        }
        return internalLookup(name, false);
    }

    /**
     * Binds a name to an object. The logic of this method is as follows If the
     * name consists of only one element, then it is bound directly to the
     * underlying HashMap. If the name consists of many parts, then the object
     * is bound to the target context with the terminal atomic component of the
     * name. If any of the atomic names other than the terminal one is bound to
     * an object that is not a Context a NameAlreadyBoundException is thrown. If
     * the terminal atomic name is already bound to any object then a
     * NameAlreadyBoundException is thrown. If any of the subContexts included
     * in the Name do not exist then a NamingException is thrown.
     *
     * @param name the name to bind; may not be empty
     * @param obj  the object to bind; possibly null
     * @throws NameAlreadyBoundException if name is already bound
     * @throws NamingException           if a naming exception is encountered
     */
    public void bind(Name name, Object obj) throws NamingException {
        if (name.isEmpty()) {
            throw new InvalidNameException("Cannot bind empty name");
        }
        Map bindings = this.bindings;
        if (name.size() == 1) {
            if (bindings.get(name.toString()) == null) {
                synchronized (bindings) {
                    bindings.put(name.toString(), obj);
                }
            } else {
                throw new NameAlreadyBoundException("The name " + name
                        + "is already bound");
            }
        } else {
            String segment = null;
            int lastIndex = name.size() - 1;
            Object terminalContext = null;
            for (int i = 0; i < lastIndex; i++) {
                segment = name.get(i);
                terminalContext = bindings.get(segment);
                if (terminalContext == null) {
                    throw new NamingException("The intermediate context "
                            + segment + " does not exist");
                } else if (!(terminalContext instanceof Context)) {
                    throw new NameAlreadyBoundException(
                            " An object that is not a context is already bound at element "
                                    + segment + "of name " + name);
                } else {
                    bindings = ((GeronimoGlobalContext) terminalContext).bindings;
                }
            }
            segment = name.get(lastIndex);
            ((Context) terminalContext).bind(segment, obj);
        }
    }

    public void rebind(Name name, Object obj) throws NamingException {
        if (name.isEmpty()) {
            throw new InvalidNameException("Cannot bind empty name");
        }
        Map bindings = this.bindings;
        if (name.size() == 1) {
            synchronized (bindings) {
                bindings.put(name.toString(), obj);
            }
        } else {
            String segment = null;
            int lastIndex = name.size() - 1;
            Object terminalContext = null;
            for (int i = 0; i < lastIndex; i++) {
                segment = name.get(i);
                terminalContext = bindings.get(segment);
                if (terminalContext == null) {
                    throw new NamingException("The intermediate context "
                            + segment + " does not exist");
                } else if (!(terminalContext instanceof Context)) {
                    throw new NameAlreadyBoundException(
                            " An object that is not a context is already bound at element "
                                    + segment + "of name " + name);
                } else {
                    bindings = ((GeronimoGlobalContext) terminalContext).bindings;
                }
            }
            segment = name.get(lastIndex);
            ((Context) terminalContext).rebind(segment, obj);
        }
    }

    public synchronized void rename(String oldName, String newName) throws NamingException {
        rename(new CompositeName(oldName), new CompositeName(newName));
    }

    public Context createSubcontext(String name) throws NamingException {
        return createSubcontext(new CompositeName(name));
    }

    public Context createSubcontext(Name name) throws NamingException {
        if (name.isEmpty()) {
            throw new InvalidNameException("Cannot create a subcontext if the name is empty");
        }
        Map bindings = this.bindings;
        GeronimoGlobalContext geronimoGlobalContext = new GeronimoGlobalContext(this, this.env, name);
        if (name.size() == 1) {
            if (bindings.get(name.toString()) == null) {
                synchronized (bindings) {
                    bindings.put(name.toString(), geronimoGlobalContext);
                }
            } else {
                throw new NameAlreadyBoundException("The name " + name + "is already bound");
            }
        } else {
            String segment = null;
            int lastIndex = name.size() - 1;
            Object terminalContext = null;
            for (int i = 0; i < lastIndex; i++) {
                segment = name.get(i);
                terminalContext = bindings.get(segment);
                if (terminalContext == null) {
                    throw new NamingException("The intermediate context " + segment + " does not exist");
                } else if (!(terminalContext instanceof Context)) {
                    throw new NameAlreadyBoundException(" An object that is not a context is already bound at element "
                            + segment + "of name " + name);
                } else {
                    bindings = ((GeronimoGlobalContext) terminalContext).bindings;
                }
            }
            segment = name.get(lastIndex);
            ((Context) terminalContext).bind(segment, geronimoGlobalContext);
        }
        return geronimoGlobalContext;
    }

    public synchronized void rename(Name oldName, Name newName) throws NamingException {
        if (oldName == null || newName == null) {
            throw new InvalidNameException("Name cannot be null");
        } else if (oldName.isEmpty() || newName.isEmpty()) {
            throw new InvalidNameException("Name cannot be empty");
        } else if (this.lookup(newName) != null) {
            throw new NameAlreadyBoundException("The target name is already bound");
        }
        this.bind(newName, this.lookup(oldName));
        this.unbind(oldName);
    }

    public NameParser getNameParser(String name) throws NamingException {
        return getNameParser(new CompositeName(name));
    }

    public NameParser getNameParser(Name name) throws NamingException {
        return EnterpriseNamingContextNameParser.INSTANCE;
    }

    public NamingEnumeration list(String name) throws NamingException {
        return list(new CompositeName(name));
    }

    public NamingEnumeration listBindings(String name) throws NamingException {
        return listBindings(new CompositeName(name));
    }

    public NamingEnumeration list(Name name) throws NamingException {
        if (name.isEmpty()) {
            return new ListEnumeration(bindings);
        }
        Object target = lookup(name);
        if (target instanceof Context) {
            return ((Context) target).list("");
        }
        throw new NotContextException("The name " + name + " cannot be listed");
    }

    public NamingEnumeration listBindings(Name name) throws NamingException {
        if (name.isEmpty()) {
            return new ListBindingEnumeration(bindings);
        }
        Object target = lookup(name);
        if (target instanceof Context) {
            return ((Context) target).listBindings("");
        }
        throw new NotContextException("The name " + name + " cannot be listed");
    }

    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        if (propName == null || propVal == null) {
            throw new NamingException("The parameters for this method cannot be null");
        }
        Object obj = env.get(propName);
        env.put(propName, propVal);
        return obj;
    }

    public String composeName(String name, String prefix) throws NamingException {
        return composeName(new CompositeName(name), new CompositeName(prefix))
                .toString();
    }

    public Name composeName(Name name, Name prefix) throws NamingException {
        if (name == null) {
            throw new NullPointerException("name is null");
        }
        if (prefix == null) {
            throw new NullPointerException("prefix is null");
        }

        Name result = (Name) prefix.clone();
        result.addAll(name);
        return result;
    }

    private final class ListEnumeration implements NamingEnumeration {
        private final Iterator iterator;

        public ListEnumeration(Map localBindings) {
            this.iterator = localBindings.entrySet().iterator();
        }

        public boolean hasMore() {
            return iterator.hasNext();
        }

        public boolean hasMoreElements() {
            return iterator.hasNext();
        }

        public Object next() {
            return nextElement();
        }

        public Object nextElement() {
            Map.Entry entry = (Map.Entry) iterator.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();
            String className = null;
            className = value.getClass().getName();
            return new NameClassPair(name, className);
        }

        public void close() {
        }
    }

    private final class ListBindingEnumeration implements NamingEnumeration {
        private final Iterator iterator;

        public ListBindingEnumeration(Map localBindings) {
            this.iterator = localBindings.entrySet().iterator();
        }

        public boolean hasMore() {
            return iterator.hasNext();
        }

        public boolean hasMoreElements() {
            return iterator.hasNext();
        }

        public Object next() {
            return nextElement();
        }

        public Object nextElement() {
            Map.Entry entry = (Map.Entry) iterator.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();
            return new Binding(name, value);
        }

        public void close() {
        }
    }

    protected void internalBind(String name, Object obj) throws NamingException {
        internalBind(new CompositeName(name), obj);
    }

    /**
     * InternalBind same as bind but creates a new SubContext. Can be used to
     * bind names like a/b/c/d. If the terminal atomic name is already bound to any object then a
     * NameAlreadyBoundException is thrown. If any of the subContexts included
     * in the Name do not exist then a NamingException is thrown.
     *
     * @param name the name to bind; may not be empty
     * @param obj  the object to bind; possibly null
     * @throws NameAlreadyBoundException if name is already bound
     * @throws NamingException           if a naming exception is encountered
     */
    protected void internalBind(Name name, Object obj) throws NamingException {
        if (name.isEmpty()) {
            throw new InvalidNameException("Cannot bind empty name");
        }
        Map bindings = this.bindings;
        if (name.size() == 1) {
            if (bindings.get(name.toString()) == null) {
                synchronized (bindings) {
                    bindings.put(name.toString(), obj);
                }
            } else {
                throw new NameAlreadyBoundException("The name " + name + "is already bound");
            }
        } else {
            String segment = null;
            int lastIndex = name.size() - 1;
            Object terminalContext = null;
            for (int i = 0; i < lastIndex; i++) {
                segment = name.get(i);
                terminalContext = bindings.get(segment);
                if (terminalContext == null) {
                    terminalContext = new GeronimoGlobalContext(this, this.env, name);
                    synchronized (bindings) {
                        bindings.put(segment, terminalContext);
                    }
                } else if (!(terminalContext instanceof Context)) {
                    throw new NameAlreadyBoundException(" An object that is not a context is already bound at element "
                            + segment + "of name " + name);
                }
                bindings = ((GeronimoGlobalContext) terminalContext).bindings;

            }
            segment = name.get(lastIndex);
            ((Context) terminalContext).bind(segment, obj);
        }
    }
}
