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

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicReference;
import edu.emory.mathcs.backport.java.util.concurrent.locks.Lock;
import edu.emory.mathcs.backport.java.util.concurrent.locks.ReentrantLock;
import org.apache.xbean.naming.reference.CachingReference;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @version $Rev$ $Date$
 */
public class WritableContext extends AbstractContext {
    private final Lock writeLock = new ReentrantLock();
    private final AtomicReference bindingsRef;
    private final AtomicReference indexRef;
    protected final ContextFederation contextFederation = new ContextFederation(this);

    public WritableContext() throws NamingException {
        this("", Collections.EMPTY_MAP, true);
    }

    public WritableContext(String nameInNamespace) throws NamingException {
        this(nameInNamespace, Collections.EMPTY_MAP, true);
    }

    public WritableContext(Map bindings) throws NamingException {
        this("", bindings, true);
    }

    public WritableContext(Map bindings, boolean cacheReferences) throws NamingException {
        this("", bindings, cacheReferences);
    }

    public WritableContext(String nameInNamespace, Map bindings, boolean cacheReferences) throws NamingException {
        super(nameInNamespace);

        if (cacheReferences) {
            bindings = CachingReference.wrapReferences(bindings);
        }

        Map localBindings = ContextUtil.createBindings(bindings, this);

        this.bindingsRef = new AtomicReference(Collections.unmodifiableMap(localBindings));
        this.indexRef = new AtomicReference(Collections.unmodifiableMap(buildIndex("", localBindings)));
    }

    protected Object faultLookup(String stringName, Name parsedName) {
        Object value = contextFederation.lookup(parsedName);
        if (value != null) {
            return value;
        }
        return super.faultLookup(stringName, parsedName);
    }

    protected NamingEnumeration list() throws NamingException {
        Map bindings = getListBindings();
        return new ContextUtil.ListEnumeration(bindings);
    }

    protected NamingEnumeration listBindings() throws NamingException {
        Map bindings = getListBindings();
        return new ContextUtil.ListBindingEnumeration(bindings);
    }

    protected Map getListBindings() throws NamingException {
        Map bindings = new HashMap();
        bindings.putAll(getBindings());
        bindings.putAll(contextFederation.getFederatedBindings());
        return bindings;
    }

    protected void addBinding(String name, Object value, boolean rebind) throws NamingException {
        addBinding(bindingsRef, name, value);
    }

    protected void addBinding(AtomicReference bindingsRef, String name, Object value) throws NamingException {
        writeLock.lock();
        try {
            Map bindings = (Map) bindingsRef.get();

            if (value instanceof Context && !isNestedSubcontext(value)) {
                Context federatedContext = (Context) value;

                // if we already have a context bound at the specified value
                if (bindings.containsKey(name)) {
                    NestedWritableContext nestedContext = (NestedWritableContext) bindings.get(name);
                    // push new context into all children
                    Map nestedBindings = (Map) nestedContext.bindingsRef.get();
                    addFederatedContext(nestedBindings, federatedContext);
                    return;
                }

                NestedWritableContext nestedContext = (NestedWritableContext) createNestedSubcontext(name, Collections.EMPTY_MAP);
                nestedContext.contextFederation.addContext(federatedContext);
                value = nestedContext;
            }

            Map newBindings = new HashMap(bindings);
            newBindings.put(name,value);
            bindingsRef.set(newBindings);

            Map newIndex = addToIndex(name, value);
            indexRef.set(newIndex);
        } finally {
            writeLock.unlock();
        }
    }

    protected void addFederatedContext(Map bindings, Context federatedContext) {
        for (Iterator iterator = bindings.values().iterator(); iterator.hasNext();) {
            Object value = iterator.next();
            if (value instanceof NestedWritableContext) {
                NestedWritableContext nestedContext = (NestedWritableContext) value;
                nestedContext.contextFederation.addContext(federatedContext);
                Map nestedBindings = (Map) nestedContext.bindingsRef.get();
                addFederatedContext(nestedBindings, federatedContext);
            }
        }
    }

    private Map addToIndex(String name, Object value) {
        Map index = (Map) indexRef.get();
        Map newIndex = new HashMap(index);
        newIndex.put(name, value);
        if (value instanceof NestedWritableContext) {
            NestedWritableContext nestedcontext = (NestedWritableContext) value;
            Map newIndexValues = buildIndex(name, nestedcontext.getBindings());
            newIndex.putAll(newIndexValues);
        }
        return newIndex;
    }

    protected void removeBinding(String name) throws NamingException {
        removeBinding(bindingsRef, name);
    }

    private void removeBinding(AtomicReference bindingsRef, String name) throws NameNotFoundException {
        writeLock.lock();
        try {
            Map bindings = (Map) bindingsRef.get();
            if (!bindings.containsKey(name)) {
                throw new NameNotFoundException(name);
            }

            Map newBindings = new HashMap(bindings);
            newBindings.remove(name);
            bindingsRef.set(newBindings);

            Map newIndex = removeFromIndex(name);
            indexRef.set(newIndex);
        } finally {
            writeLock.unlock();
        }
    }

    private Map removeFromIndex(String name) {
        Map index = (Map) indexRef.get();
        Map newIndex = new HashMap(index);
        Object oldValue = newIndex.remove(name);
        if (oldValue instanceof NestedWritableContext) {
            NestedWritableContext nestedcontext = (NestedWritableContext) oldValue;
            Map removedIndexValues = buildIndex(name, nestedcontext.getBindings());
            for (Iterator iterator = removedIndexValues.keySet().iterator(); iterator.hasNext();) {
                String key = (String) iterator.next();
                newIndex.remove(key);
            }
        }
        return newIndex;
    }

    public boolean isNestedSubcontext(Object value) {
        if (value instanceof NestedWritableContext) {
            NestedWritableContext context = (NestedWritableContext) value;
            return this == context.getUnmodifiableContext();
        }
        return false;
    }

    public Context createNestedSubcontext(String path, Map bindings) throws NamingException {
        return new NestedWritableContext(path,bindings, contextFederation);
    }

    private static Map buildIndex(String nameInNamespace, Map bindings) {
        String path = nameInNamespace;
        if (path.length() > 0 && !path.endsWith("/")) {
            path += "/";
        }

        Map absoluteIndex = new HashMap();
        for (Iterator iterator = bindings.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();
            if (value instanceof NestedWritableContext) {
                NestedWritableContext nestedContext = (NestedWritableContext)value;
                absoluteIndex.putAll(buildIndex(nestedContext.pathWithSlash, nestedContext.getBindings()));
            }
            absoluteIndex.put(path + name, value);
        }
        return absoluteIndex;
    }

    protected Object getDeepBinding(String name) {
        Map index = (Map) indexRef.get();
        return index.get(name);
    }

    protected Map getBindings() {
        Map bindings = (Map) bindingsRef.get();
        return bindings;
    }

    /**
     * Nested context which shares the absolute index map in MapContext.
     */
    public class NestedWritableContext extends AbstractContext {
        private final AtomicReference bindingsRef;
        private final String pathWithSlash;
        protected final ContextFederation contextFederation;

        public NestedWritableContext(String path, Map bindings, ContextFederation parentContextFederation) throws NamingException {
            super(WritableContext.this.getNameInNamespace(path));

            if (!path.endsWith("/")) path += "/";
            this.pathWithSlash = path;

            this.bindingsRef = new AtomicReference(Collections.unmodifiableMap(bindings));

            this.contextFederation = parentContextFederation.createSubcontextFederation(path, this);
        }

        public boolean isNestedSubcontext(Object value) {
            if (value instanceof NestedWritableContext) {
                NestedWritableContext context = (NestedWritableContext) value;
                return getUnmodifiableContext() == context.getUnmodifiableContext();
            }
            return false;
        }

        public Context createNestedSubcontext(String path, Map bindings) throws NamingException {
            return new NestedWritableContext(path, bindings, contextFederation);
        }

        protected Object getDeepBinding(String name) {
            String absoluteName = pathWithSlash + name;
            return WritableContext.this.getDeepBinding(absoluteName);
        }

        protected Map getBindings() {
            Map bindings = (Map) bindingsRef.get();
            return bindings;
        }

        protected Object faultLookup(String stringName, Name parsedName) {
            Object value = contextFederation.lookup(parsedName);
            if (value != null) {
                return value;
            }
            return super.faultLookup(stringName, parsedName);
        }

        protected NamingEnumeration list() throws NamingException {
            Map bindings = getListBindings();
            return new ContextUtil.ListEnumeration(bindings);
        }

        protected NamingEnumeration listBindings() throws NamingException {
            Map bindings = getListBindings();
            return new ContextUtil.ListBindingEnumeration(bindings);
        }

        protected Map getListBindings() throws NamingException {
            Map bindings = new HashMap();
            bindings.putAll(getBindings());
            bindings.putAll(contextFederation.getFederatedBindings());
            return bindings;
        }

        protected void addBinding(String name, Object value, boolean rebind) throws NamingException {
            WritableContext.this.addBinding(bindingsRef, name, value);
        }

        protected void removeBinding(String name) throws NameNotFoundException {
            WritableContext.this.removeBinding(bindingsRef, name);
        }

        private WritableContext getUnmodifiableContext() {
            return WritableContext.this;
        }
    }
}
