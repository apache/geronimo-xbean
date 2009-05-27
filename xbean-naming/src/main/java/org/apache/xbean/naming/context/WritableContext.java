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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.naming.Context;
import javax.naming.ContextNotEmptyException;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;
import javax.naming.Referenceable;
import javax.naming.Reference;
import javax.naming.spi.NamingManager;

import org.apache.xbean.naming.reference.CachingReference;

/**
 * @version $Rev$ $Date$
 */
public class WritableContext extends AbstractFederatedContext {
    private final Lock writeLock = new ReentrantLock();
    private final AtomicReference<Map<String, Object>> bindingsRef;
    private final AtomicReference<Map<String, Object>> indexRef;
    private final boolean cacheReferences;
    private final boolean supportReferenceable;
    private final boolean checkDereferenceDifferent;
    private final boolean assumeDereferenceBound;

    public WritableContext() throws NamingException {
        this("", Collections.<String, Object>emptyMap(), ContextAccess.MODIFIABLE, false);
    }

    public WritableContext(String nameInNamespace) throws NamingException {
        this(nameInNamespace, Collections.<String, Object>emptyMap(), ContextAccess.MODIFIABLE, false);
    }

    public WritableContext(String nameInNamespace, Map<String, Object> bindings) throws NamingException {
        this(nameInNamespace, bindings, ContextAccess.MODIFIABLE, false);
    }

    public WritableContext(String nameInNamespace, Map<String, Object> bindings, boolean cacheReferences) throws NamingException {
        this(nameInNamespace, bindings, ContextAccess.MODIFIABLE, cacheReferences);
    }

    public WritableContext(String nameInNamespace, Map<String, Object> bindings, ContextAccess contextAccess) throws NamingException {
        this(nameInNamespace, bindings, contextAccess, false);
    }

    public WritableContext(String nameInNamespace, Map<String, Object> bindings, ContextAccess contextAccess, boolean cacheReferences) throws NamingException {
        this(nameInNamespace, bindings, contextAccess, cacheReferences, true, true, false);
    }
    public WritableContext(String nameInNamespace,
                           Map<String, Object> bindings,
                           ContextAccess contextAccess,
                           boolean cacheReferences,
                           boolean supportReferenceable,
                           boolean checkDereferenceDifferent,
                           boolean assumeDereferenceBound) throws NamingException {
        super(nameInNamespace, contextAccess);

        this.cacheReferences = cacheReferences;
        if (this.cacheReferences) {
            bindings = CachingReference.wrapReferences(bindings, this);
        }
        this.supportReferenceable = supportReferenceable;
        this.checkDereferenceDifferent = checkDereferenceDifferent;
        this.assumeDereferenceBound = assumeDereferenceBound;

        Map<String, Object> localBindings = ContextUtil.createBindings(bindings, this);

        this.bindingsRef = new AtomicReference<Map<String, Object>>(Collections.unmodifiableMap(localBindings));
        this.indexRef = new AtomicReference<Map<String, Object>>(Collections.unmodifiableMap(buildIndex("", localBindings)));
    }

    protected boolean addBinding(String name, Object value, boolean rebind) throws NamingException {
        if (super.addBinding(name, value, rebind)) {
            return true;
        }

        addBinding(bindingsRef, name, getNameInNamespace(name), value, rebind);
        return true;
    }

    protected void addBinding(AtomicReference<Map<String, Object>> bindingsRef, String name, String nameInNamespace, Object value, boolean rebind) throws NamingException {
        if (supportReferenceable && value instanceof Referenceable) {
            Reference ref = ((Referenceable)value).getReference();
            if (ref != null) {
                if (checkDereferenceDifferent) {
                    try {
                        Object o = NamingManager.getObjectInstance(ref, null, null, new Hashtable());
                        if (!value.equals(o)) {
                            value = ref;
                        }
                    } catch (Exception e) {
                        if (!assumeDereferenceBound) {
                            value = ref;
                        }
                    }
                } else {
                    value = ref;
                }
            }

        }
        if (cacheReferences) {
            value = CachingReference.wrapReference(name, value, this);
        }

        writeLock.lock();
        try {
            Map<String, Object> bindings = bindingsRef.get();

            if (!rebind && bindings.containsKey(name)) {
                throw new NameAlreadyBoundException(name);
            }
            Map<String, Object> newBindings = new HashMap<String, Object>(bindings);
            newBindings.put(name,value);
            bindingsRef.set(newBindings);

            addToIndex(nameInNamespace, value);
        } finally {
            writeLock.unlock();
        }
    }

    private void addToIndex(String name, Object value) {
        Map<String, Object> index = indexRef.get();
        Map<String, Object> newIndex = new HashMap<String, Object>(index);
        newIndex.put(name, value);
        if (value instanceof NestedWritableContext) {
            NestedWritableContext nestedcontext = (NestedWritableContext) value;
            Map<String, Object> newIndexValues = buildIndex(name, nestedcontext.bindingsRef.get());
            newIndex.putAll(newIndexValues);
        }
        indexRef.set(newIndex);
    }

    protected boolean removeBinding(String name, boolean removeNotEmptyContext) throws NamingException {
        if (super.removeBinding(name, removeNotEmptyContext)) {
            return true;
        }
        removeBinding(bindingsRef, name, getNameInNamespace(name), removeNotEmptyContext);
        return true;
    }

    private boolean removeBinding(AtomicReference<Map<String, Object>> bindingsRef, String name, String nameInNamespace, boolean removeNotEmptyContext) throws NamingException {
        writeLock.lock();
        try {
            Map<String, Object> bindings = bindingsRef.get();
            if (!bindings.containsKey(name)) {
                // remove is idempotent meaning remove succeededs even if there was no value bound
                return false;
            }

            Map<String, Object> newBindings = new HashMap<String, Object>(bindings);
            Object oldValue = newBindings.remove(name);
            if (!removeNotEmptyContext && oldValue instanceof Context && !isEmpty((Context)oldValue)) {
                throw new ContextNotEmptyException(name);
            }
            bindingsRef.set(newBindings);

            Map<String, Object> newIndex = removeFromIndex(nameInNamespace);
            indexRef.set(newIndex);
            return true;
        } finally {
            writeLock.unlock();
        }
    }

    private Map<String, Object> removeFromIndex(String name) {
        Map<String, Object> index = indexRef.get();
        Map<String, Object> newIndex = new HashMap<String, Object>(index);
        Object oldValue = newIndex.remove(name);
        if (oldValue instanceof NestedWritableContext) {
            NestedWritableContext nestedcontext = (NestedWritableContext) oldValue;
            Map<String, Object> removedIndexValues = buildIndex(name, nestedcontext.bindingsRef.get());
            for (String key : removedIndexValues.keySet()) {
                newIndex.remove(key);
            }
        }
        return newIndex;
    }

    public Context createNestedSubcontext(String path, Map<String, Object> bindings) throws NamingException {
        if (getNameInNamespace().length() > 0) {
            path = getNameInNamespace() + "/" + path;
        }
        return new NestedWritableContext(path, bindings);
    }

    private static Map<String, Object> buildIndex(String nameInNamespace, Map<String, Object> bindings) {
        String path = nameInNamespace;
        if (path.length() > 0 && !path.endsWith("/")) {
            path += "/";
        }

        Map<String, Object> absoluteIndex = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : bindings.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof NestedWritableContext) {
                NestedWritableContext nestedContext = (NestedWritableContext) value;
                absoluteIndex.putAll(buildIndex(nestedContext.pathWithSlash, nestedContext.bindingsRef.get()));
            }
            absoluteIndex.put(path + name, value);
        }
        return absoluteIndex;
    }

    protected Object getDeepBinding(String name) {
        Map<String, Object> index = indexRef.get();
        return index.get(name);
    }

    protected Map<String, Object> getWrapperBindings() throws NamingException {
        return bindingsRef.get();
    }

    /**
     * Nested context which shares the absolute index map in MapContext.
     */
    public class NestedWritableContext extends AbstractFederatedContext {
        private final AtomicReference<Map<String, Object>> bindingsRef;
        private final String pathWithSlash;

        public NestedWritableContext(String path, Map<String, Object> bindings) throws NamingException {
            super(WritableContext.this, path);
            
            path = getNameInNamespace();
            if (!path.endsWith("/")) path += "/";
            this.pathWithSlash = path;

            this.bindingsRef = new AtomicReference<Map<String, Object>>(Collections.unmodifiableMap(bindings));
        }

        public Context createNestedSubcontext(String path, Map<String, Object> bindings) throws NamingException {
            return new NestedWritableContext(getNameInNamespace(path), bindings);
        }

        protected Object getDeepBinding(String name) {
            String absoluteName = pathWithSlash + name;
            return WritableContext.this.getDeepBinding(absoluteName);
        }

        protected Map<String, Object> getWrapperBindings() throws NamingException {
            return bindingsRef.get();
        }

        protected boolean addBinding(String name, Object value, boolean rebind) throws NamingException {
            if (super.addBinding(name, value, rebind)) {
                return true;
            }

            WritableContext.this.addBinding(bindingsRef, name, getNameInNamespace(name), value, rebind);
            return true;
        }

        protected boolean removeBinding(String name, boolean removeNotEmptyContext) throws NamingException {
            if (WritableContext.this.removeBinding(bindingsRef, name, getNameInNamespace(name), removeNotEmptyContext)) {
                return true;
            }
            return super.removeBinding(name, false);
        }
    }
}
