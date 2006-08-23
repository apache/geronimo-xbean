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
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.NameParser;
import java.util.Hashtable;

/**
 * @version $Rev$ $Date$
 */
public class VirtualSubcontext implements Context {
    private final Name nameInContext;
    private final Context context;

    public VirtualSubcontext(Name nameInContext, Context context) throws NamingException {
        if (context instanceof VirtualSubcontext) {
            VirtualSubcontext virtualSubcontext = (VirtualSubcontext) context;
            this.nameInContext = virtualSubcontext.getNameInContext(nameInContext);
            this.context = virtualSubcontext.context;
        } else {
            this.nameInContext = nameInContext;
            this.context = context;
        }
    }

    private Name getNameInContext(Name name) throws NamingException {
        return context.composeName(nameInContext, name);
    }

    private Name getNameInContext(String name) throws NamingException {
        Name parsedName = context.getNameParser("").parse(name);
        return context.composeName(nameInContext, parsedName);
    }

    public Object lookup(Name name) throws NamingException {
        return context.lookup(getNameInContext(name));
    }

    public Object lookup(String name) throws NamingException {
        return context.lookup(getNameInContext(name));
    }

    public void bind(Name name, Object obj) throws NamingException {
        context.bind(getNameInContext(name), obj);
    }

    public void bind(String name, Object obj) throws NamingException {
        context.bind(getNameInContext(name), obj);
    }

    public void rebind(Name name, Object obj) throws NamingException {
        context.rebind(getNameInContext(name), obj);
    }

    public void rebind(String name, Object obj) throws NamingException {
        context.rebind(getNameInContext(name), obj);
    }

    public void unbind(Name name) throws NamingException {
        context.unbind(getNameInContext(name));
    }

    public void unbind(String name) throws NamingException {
        context.unbind(getNameInContext(name));
    }

    public void rename(Name oldName, Name newName) throws NamingException {
        context.rename(getNameInContext(oldName), getNameInContext(newName));
    }

    public void rename(String oldName, String newName) throws NamingException {
        context.rename(getNameInContext(oldName), getNameInContext(newName));
    }

    public NamingEnumeration list(Name name) throws NamingException {
        return context.list(getNameInContext(name));
    }

    public NamingEnumeration list(String name) throws NamingException {
        return context.list(getNameInContext(name));
    }

    public NamingEnumeration listBindings(Name name) throws NamingException {
        return context.listBindings(getNameInContext(name));
    }

    public NamingEnumeration listBindings(String name) throws NamingException {
        return context.listBindings(getNameInContext(name));
    }

    public void destroySubcontext(Name name) throws NamingException {
        context.destroySubcontext(getNameInContext(name));
    }

    public void destroySubcontext(String name) throws NamingException {
        context.destroySubcontext(getNameInContext(name));
    }

    public Context createSubcontext(Name name) throws NamingException {
        return context.createSubcontext(getNameInContext(name));
    }

    public Context createSubcontext(String name) throws NamingException {
        return context.createSubcontext(getNameInContext(name));
    }

    public Object lookupLink(Name name) throws NamingException {
        return context.lookupLink(getNameInContext(name));
    }

    public Object lookupLink(String name) throws NamingException {
        return context.lookupLink(getNameInContext(name));
    }

    public NameParser getNameParser(Name name) throws NamingException {
        return context.getNameParser(getNameInContext(name));
    }

    public NameParser getNameParser(String name) throws NamingException {
        return context.getNameParser(getNameInContext(name));
    }

    public Name composeName(Name name, Name prefix) throws NamingException {
        return context.composeName(name, prefix);
    }

    public String composeName(String name, String prefix) throws NamingException {
        return context.composeName(name, prefix);
    }

    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        return context.addToEnvironment(propName, propVal);
    }

    public Object removeFromEnvironment(String propName) throws NamingException {
        return context.removeFromEnvironment(propName);
    }

    public Hashtable getEnvironment() throws NamingException {
        return context.getEnvironment();
    }

    public void close() throws NamingException {
        context.close();
    }

    public String getNameInNamespace() throws NamingException {
        Name parsedNameInNamespace = context.getNameParser("").parse(context.getNameInNamespace());
        return context.composeName(parsedNameInNamespace, nameInContext).toString();
    }
}
