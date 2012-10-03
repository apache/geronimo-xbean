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
import javax.naming.NameNotFoundException;
import javax.naming.InitialContext;
import javax.naming.OperationNotSupportedException;
import javax.naming.NameClassPair;
import javax.naming.Binding;

import java.io.Serializable;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;

public abstract class AbstractContext implements Context, NestedContextFactory, Serializable {
    private static final long serialVersionUID = 6481918425692261483L;
    private final String nameInNamespace;
    private final Name parsedNameInNamespace;
    private final ContextAccess contextAccess;
    private final boolean modifiable;
    private final ThreadLocal<Name> inCall = new ThreadLocal<Name>();

    protected AbstractContext(String nameInNamespace) {
        this(nameInNamespace, ContextAccess.MODIFIABLE);
    }

    public AbstractContext(String nameInNamespace, ContextAccess contextAccess) {
        this.nameInNamespace = nameInNamespace;
        try {
            this.parsedNameInNamespace = getNameParser().parse(nameInNamespace);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
        this.contextAccess = contextAccess;
        this.modifiable = contextAccess.isModifiable(getParsedNameInNamespace());
    }

    public void close() throws NamingException {
        //Ignore. Explicitly do not close the context
    }

    protected ContextAccess getContextAccess() {
        return contextAccess;
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
     * Gets the object bound to the name.  The name will not contain slashes.
     * @param name the name
     * @return the object bound to the name, or null if not found
     * @throws javax.naming.NamingException on error
     */
    protected Object getBinding(String name) throws NamingException {
        Map<String, Object> bindings = getBindings();
        return bindings.get(name);
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
            return ContextUtil.resolve(directLookup, stringName, parsedName, this);
        }

        // if the parsed name has no parts, they are asking for the current context
        if (parsedName == null) parsedName = getNameParser().parse(stringName);
        if (parsedName.isEmpty()) {
            return this;
        }

        // we didn't find an entry, pop the first element off the parsed name and attempt to
        // get a context from the bindings and delegate to that context
        Object localValue;
        String firstNameElement = parsedName.get(0);
        if (firstNameElement.length() == 0) {
            // the element is null... this is normally caused by looking up with a trailing '/' character
            localValue = this;
        } else {
            localValue = getBinding(firstNameElement);
        }

        if (localValue != null) {

            // if the name only had one part, we've looked up everything
            if (parsedName.size() == 1) {
                localValue = ContextUtil.resolve(localValue, stringName, parsedName, this);
                return localValue;
            }

            // if we have a link ref, follow it
            if (localValue instanceof LinkRef) {
                LinkRef linkRef = (LinkRef) localValue;
                localValue = lookup(linkRef.getLinkName());
            }

            // we have more to lookup so we better have a context object
            if (!(localValue instanceof Context)) {
                throw new NameNotFoundException(stringName);
            }

            // delegate to the sub-context
            return ((Context) localValue).lookup(parsedName.getSuffix(1));
        }

        // if we didn't find an entry, it may be an absolute name
        Object value = faultLookup(stringName, parsedName);
        if (value != null) {
            return value;
        }
        if (parsedName.size() > 1) {
            throw new NotContextException(stringName);
        } else {
            throw new NameNotFoundException(stringName);
        }
    }

    /**
     * When a value can not be found within this context, this method is called as a last ditch effort befrore
     * thowing a null pointer exception.
     * @param stringName the string version of the name; will not be null
     * @param parsedName the parsed name; will not be null
     * @return the value or null if no fault value could be found
     */
    protected Object faultLookup(String stringName, Name parsedName) {
        if (!stringName.startsWith(nameInNamespace) && stringName.indexOf(':') > 0 && inCall.get() == null) {
            inCall.set(parsedName);
            try {
                Context ctx = new InitialContext();
                return ctx.lookup(parsedName);
            } catch (NamingException ignored) {
                // thrown below
            } finally {
                inCall.set(null);                
            }
        }
        return null;
    }

    protected Context lookupFinalContext(Name name) throws NamingException {
        Object value;
        try {
            value = lookup(name.getPrefix(name.size() - 1));
        } catch (NamingException e) {
            throw new NotContextException("The intermediate context " + name.get(name.size() - 1) + " does not exist");
        }

        if (value == null) {
            throw new NotContextException("The intermediate context " + name.get(name.size() - 1) + " does not exist");
        } else if (!(value instanceof Context)) {
            throw new NotContextException("The intermediate context " + name.get(name.size() - 1) + " is not a context");
        } else {
            return (Context) value;
        }
    }

    //
    //  List Bindings
    //

    /**
     * Gets a map of the bindings for the current node (i.e., no names with slashes).
     * This method must not return null.
     *
     * @return a Map from binding name to binding value
     * @throws NamingException if a problem occurs while getting the bindigns
     */
    protected abstract Map<String, Object> getBindings() throws NamingException;

    //
    //  Add Binding
    //

    protected void addDeepBinding(Name name, Object value, boolean rebind, boolean createIntermediateContexts) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");
        if (value == null) throw new NullPointerException("value is null for name: " + name);

        if (name.isEmpty()) {
            throw new InvalidNameException("Name is empty");
        }

        if (name.size() == 1) {
            addBinding(name.get(0), value, rebind);
            return;
        }

        if (!createIntermediateContexts) {
            Context context = lookupFinalContext(name);

            String lastSegment = name.get(name.size() - 1);
            addBinding(context, lastSegment, value, rebind);
        } else {
            Context currentContext = this;
            for (int i = 0; i < name.size(); i++) {
                String part = name.get(i);

                // empty path parts are not allowed
                if (part.length() == 0) {
                    // this could be supported but it would be tricky
                    throw new InvalidNameException("Name part " + i + " is empty: " + name);
                }

                // Is this the last element in the name?
                if (i == name.size() - 1) {
                    // we're at the end... (re)bind the value into the parent context
                    addBinding(currentContext, part, value, rebind);

                    // all done... this is redundant but makes the code more readable
                    break;
                } else {
                    Object currentValue = getBinding(currentContext, part);
                    if (currentValue == null) {
                        // the next step in the tree is not present, so create everything down
                        // and add it to the current bindings
                        Context subcontext = createSubcontextTree(name.getPrefix(i).toString(), name.getSuffix(i), value);
                        addBinding(currentContext, part, subcontext, rebind);

                        // all done
                        break;
                    } else {
                        // the current value must be a nested subcontext
                        if (!(currentValue instanceof Context)) {
                            throw new NotContextException("Expected an instance of context to be bound at " +
                                    part + " but found an instance of " + currentValue.getClass().getName());
                        }
                        currentContext = (Context) currentValue;
                        // now we recurse into the current context
                    }
                }
            }
        }
    }

    /**
     * Gets the value bound to the specified name within the specified context.  If the specified context is an
     * AbstractContext this method will call the faster getBinding method, otherwise it will call lookup.
     *
     * @param context the context to get the binding from
     * @param name the binding name
     * @return the bound value or null if no value was bound
     */
    private static Object getBinding(Context context, String name) {
        try {
            if (context instanceof AbstractContext) {
                AbstractContext abstractContext = (AbstractContext) context;
                return abstractContext.getBinding(name);
            } else {
                return context.lookup(name);
            }
        } catch (NamingException e) {
            return null;
        }
    }

    /**
     * Binds the specified value to the specified name within the specified context.  If the specified context is an
     * AbstractContext and is a nested subcontext, this method will call the direct addBinding method, otherwise it
     * will call public (re)bind method.
     *
     * @param context the context to add the binding to
     * @param name the binding name
     * @param value the value to bind
     * @param rebind if true, this method will replace any exsiting binding, otherwise a NamingException will be thrown
     * @throws NamingException if a problem occurs while (re)binding
     */
    protected void addBinding(Context context, String name, Object value, boolean rebind) throws NamingException {
        if (context == this || (context instanceof AbstractContext && isNestedSubcontext(context))) {
            AbstractContext abstractContext = (AbstractContext) context;
            abstractContext.addBinding(name, value, rebind);
        } else {
            if (rebind) {
                context.rebind(name, value);
            } else {
                context.bind(name, value);
            }
        }
    }

    protected abstract boolean addBinding(String name, Object value, boolean rebind) throws NamingException;

    /**
     * Creates a context tree which will be rooted at the specified path and contain a single entry located down
     * a path specified by the name.  All necessary intermediate contexts will be created using the createContext method.
     * @param path the path to the context that will contains this context
     * @param name the name under which the value should be bound
     * @param value the value
     * @return a context with the value bound at the specified name
     * @throws NamingException if a problem occurs while creating the subcontext tree
     */
    protected Context createSubcontextTree(String path, Name name, Object value) throws NamingException {
        if (path == null) throw new NullPointerException("path is null");
        if (name == null) throw new NullPointerException("name is null");
        if (name.size() < 2) throw new InvalidNameException("name must have at least 2 parts " + name);

        if (path.length() > 0 && !path.endsWith("/")) path += "/";

        for (int i = name.size() - 1; i > 0; i--) {
            String fullPath = path + name.getPrefix(i);
            String key = name.get(i);
            value = createNestedSubcontext(fullPath, Collections.singletonMap(key, value));
        }
        return (Context) value;
    }


    //
    //  Remove Binding
    //

    /**
     * Removes the binding from the context.  The name will not contain a path and the value will not
     * be a nested context although it may be a foreign context.
     * @param name name under which the value should be bound
     * @param removeNotEmptyContext ??? TODO figure this out
     * @return whether removal was successful
     * @throws NamingException if a problem occurs during the bind such as a value already being bound
     */
    protected abstract boolean removeBinding(String name, boolean removeNotEmptyContext) throws NamingException;

    protected void removeDeepBinding(Name name, boolean pruneEmptyContexts) throws NamingException {
        removeDeepBinding(name, pruneEmptyContexts, false);
    }

    protected void removeDeepBinding(Name name, boolean pruneEmptyContexts, boolean removeNotEmptyContext) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");
        if (name.isEmpty()) {
            throw new InvalidNameException("Name is empty");
        }

        if (name.size() == 1) {
            removeBinding(name.get(0), removeNotEmptyContext);
            return;
        }

        if (!pruneEmptyContexts) {
            Context context = lookupFinalContext(name);
            context.unbind(name.getSuffix(name.size() - 1));
        } else {
            // we serch the tree for a target context and name to remove
            // this is normally the last context in the tree and the final name part, but
            // it may be farther up the path if the intervening nodes are empty
            Context targetContext = this;
            String targetName = name.get(0);

            Context currentContext = this;
            for (int i = 0; i < name.size(); i++) {
                String part = name.get(i);

                // empty path parts are not allowed
                if (part.length() == 0) {
                    throw new InvalidNameException("Name part " + i + " is empty: " + name);
                }

                // update targets
                if (getSize(currentContext) > 1) {
                    targetContext = currentContext;
                    targetName = part;
                }


                // Is this the last element in the name?
                if (i == name.size() - 1) {
                    // we're at the end... unbind value
                    unbind(targetContext, targetName, true);

                    // all done... this is redundant but makes the code more readable
                    break;
                } else {
                    Object currentValue = getBinding(currentContext, part);
                    if (currentValue == null) {
                        // path not found we are done, but first prune the empty contexts
                        if (targetContext != currentContext) {
                            unbind(targetContext, targetName, false);
                        }
                        break;
                    } else {
                        // the current value must be a context
                        if (!(currentValue instanceof Context)) {
                            throw new NotContextException("Expected an instance of context to be bound at " +
                                    part + " but found an instance of " + currentValue.getClass().getName());
                        }
                        currentContext = (Context) currentValue;
                        // now we recurse into the current context
                    }
                }
            }
        }
    }

    protected static boolean isEmpty(Context context) throws NamingException {
        if (context instanceof AbstractContext) {
            AbstractContext abstractContext = (AbstractContext) context;
            Map<String, Object> currentBindings = abstractContext.getBindings();
            return currentBindings.isEmpty();
        } else {
            NamingEnumeration namingEnumeration = context.list("");
            return namingEnumeration.hasMore();
        }
    }

    protected static int getSize(Context context) throws NamingException {
        if (context instanceof AbstractContext) {
            AbstractContext abstractContext = (AbstractContext) context;
            Map<String, Object> currentBindings = abstractContext.getBindings();
            return currentBindings.size();
        } else {
            NamingEnumeration namingEnumeration = context.list("");
            int size = 0;
            while (namingEnumeration.hasMore()) size++;
            return size;
        }
    }

    /**
     * Unbinds any value bound to the specified name within the specified context.  If the specified context is an
     * AbstractContext and is a nested context, this method will call the direct removeBinding method, otherwise it
     * will call public unbind.
     *
     * @param context the context to remove the binding from
     * @param name the binding name
     * @param removeNotEmptyContext ??? TODO figure this out
     * @throws NamingException if a problem occurs while unbinding
     */
    private void unbind(Context context, String name, boolean removeNotEmptyContext) throws NamingException {
        if (context == this || (context instanceof AbstractContext && isNestedSubcontext(context))) {
            AbstractContext abstractContext = (AbstractContext) context;
            abstractContext.removeBinding(name, removeNotEmptyContext);
        } else {
            context.unbind(name);
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
     * Gets the name of this context withing the global namespace.  This method may return null
     * if the location of the node in the global namespace is not known
     * @return the name of this context within the global namespace or null if unknown.
     */
    public String getNameInNamespace() {
        return nameInNamespace;
    }

    /**
     * Gets the name of this context withing the global namespace.  This method may return null
     * if the location of the node in the global namespace is not known
     * @return the name of this context within the global namespace or null if unknown.
     */
    protected Name getParsedNameInNamespace() {
        return parsedNameInNamespace;
    }

    /**
     * Gets the name of a path withing the global namespace context.
     * @param path path to extend
     * @return full path in namespace
     */
    protected String getNameInNamespace(String path) {
        String nameInNamespace = getNameInNamespace();
        if (nameInNamespace == null || nameInNamespace.length() == 0) {
            return path;
        } else {
            return nameInNamespace + "/" + path;
        }
    }

    /**
     * Gets the name of a path withing the global namespace context.
     * @param path path to extend
     * @return full path in namespace
     * @throws javax.naming.NamingException on error
     */
    protected Name getNameInNamespace(Name path) throws NamingException {
        Name nameInNamespace = getParsedNameInNamespace();
        if (nameInNamespace == null || nameInNamespace.size() == 0) {
            return path;
        } else {
            return composeName(nameInNamespace, path);
        }
    }

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
        if (!modifiable) throw new OperationNotSupportedException("Context is read only");
        if (name == null) throw new NullPointerException("name is null");
        if (name.length() == 0) {
            throw new NameAlreadyBoundException("Cannot bind to an empty name (this context)");
        }
        bind(new CompositeName(name), obj);
    }

    public void bind(Name name, Object obj) throws NamingException {
        if (!modifiable) throw new OperationNotSupportedException("Context is read only");
        if (name == null) throw new NullPointerException("name is null");
        if (name.isEmpty()) {
            throw new NameAlreadyBoundException("Cannot bind to an empty name (this context)");
        }
        addDeepBinding(name, obj, false, false);
    }

    public void rebind(String name, Object obj) throws NamingException {
        if (!modifiable) throw new OperationNotSupportedException("Context is read only");
        if (name == null) throw new NullPointerException("name is null");
        rebind(new CompositeName(name), obj);
    }

    public void rebind(Name name, Object obj) throws NamingException {
        if (!modifiable) throw new OperationNotSupportedException("Context is read only");
        if (name == null) throw new NullPointerException("name is null");
        if (name.isEmpty()) {
            throw new NameAlreadyBoundException("Cannot rebind an empty name (this context)");
        }
        addDeepBinding(name, obj, true, false);
    }

    public void rename(String oldName, String newName) throws NamingException {
        if (!modifiable) throw new OperationNotSupportedException("Context is read only");
        if (oldName == null) throw new NullPointerException("oldName is null");
        if (newName == null) throw new NullPointerException("newName is null");
        rename(new CompositeName(oldName), new CompositeName(newName));
    }

    public void rename(Name oldName, Name newName) throws NamingException {
        if (!modifiable) throw new OperationNotSupportedException("Context is read only");
        if (oldName == null || newName == null) {
            throw new NullPointerException("name is null");
        } else if (oldName.isEmpty() || newName.isEmpty()) {
            throw new NameAlreadyBoundException("Name cannot be empty");
        }
        this.bind(newName, this.lookup(oldName));
        this.unbind(oldName);
    }

    public void unbind(String name) throws NamingException {
        if (!modifiable) throw new OperationNotSupportedException("Context is read only");
        if (name == null) throw new NullPointerException("name is null");
        unbind(new CompositeName(name));
    }

    public void unbind(Name name) throws NamingException {
        if (!modifiable) throw new OperationNotSupportedException("Context is read only");
        if (name == null) throw new NullPointerException("name is null");
        if (name.isEmpty()) {
            throw new InvalidNameException("Cannot unbind empty name");
        }
        removeDeepBinding(name, false);
    }

    //
    // List
    //

    protected NamingEnumeration<NameClassPair> list() throws NamingException {
        Map<String, Object> bindings = getBindings();
        return new ContextUtil.ListEnumeration(bindings);
    }

    protected NamingEnumeration<Binding> listBindings() throws NamingException {
        Map<String, Object> bindings = getBindings();
        return new ContextUtil.ListBindingEnumeration(bindings, this);
    }

    public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");

        // if the name is empty, list the current context
        if (name.length() == 0) {
            return list();
        }

        // lookup the target context
        Object target;
        try {
            target = lookup(name);
        } catch (NamingException e) {
            throw new NotContextException(name);
        }

        if (target == this) {
            return list();
        } else if (target instanceof Context) {
            return ((Context) target).list("");
        } else {
            throw new NotContextException("The name " + name + " cannot be listed");
        }
    }

    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");

        // if the name is empty, list the current context
        if (name.isEmpty()) {
            return list();
        }

        // lookup the target context
        Object target;
        try {
            target = lookup(name);
        } catch (NamingException e) {
            throw new NotContextException(name.toString());
        }

        if (target == this) {
            return list();
        } else if (target instanceof Context) {
            return ((Context) target).list("");
        } else {
            throw new NotContextException("The name " + name + " cannot be listed");
        }
    }

    public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");

        // if the name is empty, list the current context
        if (name.length() == 0) {
            return listBindings();
        }

        // lookup the target context
        Object target;
        try {
            target = lookup(name);
        } catch (NamingException e) {
            throw new NotContextException(name);
        }

        if (target == this) {
            return listBindings();
        } else if (target instanceof Context) {
            return ((Context) target).listBindings("");
        } else {
            throw new NotContextException("The name " + name + " cannot be listed");
        }
    }

    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");

        // if the name is empty, list the current context
        if (name.isEmpty()) {
            return listBindings();
        }

        // lookup the target context
        Object target;
        try {
            target = lookup(name);
        } catch (NamingException e) {
            throw new NotContextException(name.toString());
        }

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
        if (!modifiable) throw new OperationNotSupportedException("Context is read only");
        if (name == null) throw new NullPointerException("name is null");
        return createSubcontext(new CompositeName(name));
    }

    public Context createSubcontext(Name name) throws NamingException {
        if (!modifiable) throw new OperationNotSupportedException("Context is read only");
        if (name == null) throw new NullPointerException("name is null");
        if (name.isEmpty()) {
            throw new NameAlreadyBoundException("Cannot create a subcontext if the name is empty");
        }
        Context abstractContext = createNestedSubcontext(name.toString(), Collections.EMPTY_MAP);
        addDeepBinding(name, abstractContext, false, false);
        return abstractContext;
    }

    public void destroySubcontext(String name) throws NamingException {
        if (!modifiable) throw new OperationNotSupportedException("Context is read only");
        if (name == null) throw new NullPointerException("name is null");
        destroySubcontext(new CompositeName(name));
    }

    public void destroySubcontext(Name name) throws NamingException {
        if (!modifiable) throw new OperationNotSupportedException("Context is read only");
        if (name == null) throw new NullPointerException("name is null");
        if (name.isEmpty()) {
            throw new InvalidNameException("Cannot destroy subcontext with empty name");
        }
        unbind(name);
    }
}
