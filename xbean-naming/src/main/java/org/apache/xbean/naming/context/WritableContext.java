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
import javax.naming.NamingException;
import javax.naming.NameAlreadyBoundException;
import javax.naming.ContextNotEmptyException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @version $Rev$ $Date$
 */
public class WritableContext extends AbstractFederatedContext {
    private final Lock writeLock = new ReentrantLock();
    private final AtomicReference bindingsRef;
    private final AtomicReference indexRef;
    private final boolean cacheReferences;

    public WritableContext() throws NamingException {
        this("", Collections.EMPTY_MAP, ContextAccess.MODIFIABLE, false);
    }

    public WritableContext(String nameInNamespace) throws NamingException {
        this(nameInNamespace, Collections.EMPTY_MAP, ContextAccess.MODIFIABLE, false);
    }

    public WritableContext(String nameInNamespace, Map bindings) throws NamingException {
        this(nameInNamespace, bindings, ContextAccess.MODIFIABLE, false);
    }

    public WritableContext(String nameInNamespace, Map bindings, boolean cacheReferences) throws NamingException {
        this(nameInNamespace, bindings, ContextAccess.MODIFIABLE, cacheReferences);
    }

    public WritableContext(String nameInNamespace, Map bindings, ContextAccess contextAccess) throws NamingException {
        this(nameInNamespace, bindings, contextAccess, false);
    }

    public WritableContext(String nameInNamespace, Map bindings, ContextAccess contextAccess, boolean cacheReferences) throws NamingException {
        super(nameInNamespace, contextAccess);

        this.cacheReferences = cacheReferences;
        if (this.cacheReferences) {
            bindings = CachingReference.wrapReferences(bindings);
        }

        Map localBindings = ContextUtil.createBindings(bindings, this);

        this.bindingsRef = new AtomicReference(Collections.unmodifiableMap(localBindings));
        this.indexRef = new AtomicReference(Collections.unmodifiableMap(buildIndex("", localBindings)));
    }

    protected boolean addBinding(String name, Object value, boolean rebind) throws NamingException {
        if (super.addBinding(name, value, rebind)) {
            return true;
        }

        addBinding(bindingsRef, name, value, rebind);
        return true;
    }

    protected void addBinding(AtomicReference bindingsRef, String name, Object value, boolean rebind) throws NamingException {
        writeLock.lock();
        try {
            Map bindings = (Map) bindingsRef.get();

            if (!rebind && bindings.containsKey(name)) {
                throw new NameAlreadyBoundException(name);
            }
            if (cacheReferences) {
                value = CachingReference.wrapReference(getNameInNamespace(name), value);
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

    private Map addToIndex(String name, Object value) {
        Map index = (Map) indexRef.get();
        Map newIndex = new HashMap(index);
        newIndex.put(name, value);
        if (value instanceof NestedWritableContext) {
            NestedWritableContext nestedcontext = (NestedWritableContext) value;
            Map newIndexValues = buildIndex(name, (Map) nestedcontext.bindingsRef.get());
            newIndex.putAll(newIndexValues);
        }
        return newIndex;
    }

    protected boolean removeBinding(String name, boolean removeNotEmptyContext) throws NamingException {
        if (super.removeBinding(name, removeNotEmptyContext)) {
            return true;
        }
        removeBinding(bindingsRef, name, removeNotEmptyContext);
        return true;
    }

    private boolean removeBinding(AtomicReference bindingsRef, String name, boolean removeNotEmptyContext) throws NamingException {
        writeLock.lock();
        try {
            Map bindings = (Map) bindingsRef.get();
            if (!bindings.containsKey(name)) {
                // remove is idempotent meaning remove succeededs even if there was no value bound
                return false;
            }

            Map newBindings = new HashMap(bindings);
            Object oldValue = newBindings.remove(name);
            if (!removeNotEmptyContext && oldValue instanceof Context && !isEmpty((Context)oldValue)) {
                throw new ContextNotEmptyException(name);
            }
            bindingsRef.set(newBindings);

            Map newIndex = removeFromIndex(name);
            indexRef.set(newIndex);
            return true;
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
            Map removedIndexValues = buildIndex(name, (Map) nestedcontext.bindingsRef.get());
            for (Iterator iterator = removedIndexValues.keySet().iterator(); iterator.hasNext();) {
                String key = (String) iterator.next();
                newIndex.remove(key);
            }
        }
        return newIndex;
    }

    public Context createNestedSubcontext(String path, Map bindings) throws NamingException {
        return new NestedWritableContext(path,bindings);
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
                absoluteIndex.putAll(buildIndex(nestedContext.pathWithSlash, (Map) nestedContext.bindingsRef.get()));
            }
            absoluteIndex.put(path + name, value);
        }
        return absoluteIndex;
    }

    protected Object getDeepBinding(String name) {
        Map index = (Map) indexRef.get();
        return index.get(name);
    }

    protected Map getWrapperBindings() throws NamingException {
        Map bindings = (Map) bindingsRef.get();
        return bindings;
    }

    /**
     * Nested context which shares the absolute index map in MapContext.
     */
    public class NestedWritableContext extends AbstractFederatedContext {
        private final AtomicReference bindingsRef;
        private final String pathWithSlash;

        public NestedWritableContext(String path, Map bindings) throws NamingException {
            super(WritableContext.this, path);

            if (!path.endsWith("/")) path += "/";
            this.pathWithSlash = path;

            this.bindingsRef = new AtomicReference(Collections.unmodifiableMap(bindings));
        }

        public Context createNestedSubcontext(String path, Map bindings) throws NamingException {
            return new NestedWritableContext(path, bindings);
        }

        protected Object getDeepBinding(String name) {
            String absoluteName = pathWithSlash + name;
            return WritableContext.this.getDeepBinding(absoluteName);
        }

        protected Map getWrapperBindings() throws NamingException {
            Map bindings = (Map) bindingsRef.get();
            return bindings;
        }

        protected boolean addBinding(String name, Object value, boolean rebind) throws NamingException {
            if (super.addBinding(name, value, rebind)) {
                return true;
            }

            WritableContext.this.addBinding(bindingsRef, name, value, rebind);
            return true;
        }

        protected boolean removeBinding(String name, boolean removeNotEmptyContext) throws NamingException {
            if (WritableContext.this.removeBinding(bindingsRef, name, false)) {
                return true;
            }
            return super.removeBinding(name, false);
        }
    }
}
