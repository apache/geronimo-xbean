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

import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;
import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.OperationNotSupportedException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @version $Rev: 417891 $ $Date: 2006-06-28 15:45:07 -0700 (Wed, 28 Jun 2006) $
 */
public class UnmodifiableContext extends AbstractUnmodifiableContext {
    private final Lock writeLock = new ReentrantLock();
    private final AtomicReference bindingsRef;
    private final AtomicReference indexRef;
    public static final int MAX_WRITE_ATTEMPTS = 3;

    public UnmodifiableContext(Map bindings) throws NamingException {
        this("", bindings, true);
    }

    public UnmodifiableContext(Map bindings, boolean cacheReferences) throws NamingException {
        this("", bindings, cacheReferences);
    }

    public UnmodifiableContext(String nameInNamespace, Map bindings, boolean cacheReferences) throws NamingException {
        super(nameInNamespace);

        if (cacheReferences) {
            bindings = CachingReference.wrapReferences(bindings);
        }

        Map localBindings = ContextUtil.createBindings(bindings, this);

        this.bindingsRef = new AtomicReference(Collections.unmodifiableMap(localBindings));
        this.indexRef = new AtomicReference(Collections.unmodifiableMap(buildIndex("", localBindings)));
    }

    protected void addBinding(String name, Object value, boolean rebind) throws NamingException {
        if (rebind) {
            throw new OperationNotSupportedException("This conext does not support rebind");
        }

        writeLock.lock();
        try {
            Map bindings = (Map) bindingsRef.get();
            if (bindings.containsKey(name)) {
                throw new NameAlreadyBoundException(name);
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
        if (value instanceof NestedMapContext) {
            NestedMapContext nestedcontext = (NestedMapContext) value;
            Map newIndexValues = buildIndex(name, nestedcontext.getBindings());
            newIndex.putAll(newIndexValues);
        }
        return newIndex;
    }

    protected void removeBinding(String name) throws NamingException {
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
        if (oldValue instanceof NestedMapContext) {
            NestedMapContext nestedcontext = (NestedMapContext) oldValue;
            Map removedIndexValues = buildIndex(name, nestedcontext.getBindings());
            for (Iterator iterator = removedIndexValues.keySet().iterator(); iterator.hasNext();) {
                String key = (String) iterator.next();
                newIndex.remove(key);
            }
        }
        return newIndex;
    }

    public Context createContext(String path, Map bindings) {
        return new NestedMapContext(path,bindings);
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
            if (value instanceof UnmodifiableContext.NestedMapContext) {
                UnmodifiableContext.NestedMapContext nestedContext = (UnmodifiableContext.NestedMapContext)value;
                absoluteIndex.putAll(UnmodifiableContext.buildIndex(nestedContext.pathWithSlash, nestedContext.getBindings()));
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
    public final class NestedMapContext extends AbstractUnmodifiableContext {
        private final AtomicReference bindingsRef;
        private final String pathWithSlash;

        public NestedMapContext(String path, String key, Object value) {
            this(path, Collections.singletonMap(key, value));
        }

        public NestedMapContext(String path, Map bindings) {
            super(UnmodifiableContext.this.getNameInNamespace(path));

            if (!path.endsWith("/")) path += "/";
            this.pathWithSlash = path;

            this.bindingsRef = new AtomicReference(Collections.unmodifiableMap(bindings));
        }

        public Context createContext(String path, Map bindings) {
            return new NestedMapContext(path, bindings);
        }

        protected Object getDeepBinding(String name) {
            String absoluteName = pathWithSlash + name;
            return UnmodifiableContext.this.getDeepBinding(absoluteName);
        }

        protected Map getBindings() {
            Map bindings = (Map) bindingsRef.get();
            return bindings;
        }

        protected void addBinding(String name, Object value, boolean rebind) throws NamingException {
            if (rebind) {
                throw new OperationNotSupportedException("This conext does not support rebind");
            }

            writeLock.lock();
            try {
                Map currentBindings = (Map) bindingsRef.get();
                Map newBindings = new HashMap(currentBindings);
                newBindings.put(name, value);
                newBindings = Collections.unmodifiableMap(newBindings);
                bindingsRef.set(newBindings);

                Map newIndex = addToIndex(name, value);
                indexRef.set(newIndex);
            } finally {
                writeLock.unlock();
            }
        }

        protected void removeBinding(String name) {
            writeLock.lock();
            try {
                Map currentBindings = (Map) bindingsRef.get();
                Map newBindings = new HashMap(currentBindings);
                newBindings.remove(name);
                newBindings = Collections.unmodifiableMap(newBindings);
                bindingsRef.set(newBindings);

                Map newIndex = removeFromIndex(name);
                indexRef.set(newIndex);
            } finally {
                writeLock.unlock();
            }
        }
    }
}
