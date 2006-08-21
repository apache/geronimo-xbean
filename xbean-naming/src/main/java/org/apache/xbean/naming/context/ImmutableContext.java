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

import org.apache.xbean.naming.reference.CachingReference;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.Name;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @version $Rev: 417891 $ $Date: 2006-06-28 15:45:07 -0700 (Wed, 28 Jun 2006) $
 */
public class ImmutableContext extends AbstractUnmodifiableContext {
    private final Map localBindings;
    private final Map absoluteIndex;

    public ImmutableContext(Map bindings) throws NamingException {
        this("", bindings, true);
    }

    public ImmutableContext(Map bindings, boolean cacheReferences) throws NamingException {
        this("", bindings, cacheReferences);
    }

    public ImmutableContext(String nameInNamespace, Map bindings, boolean cacheReferences) throws NamingException {
        super(nameInNamespace);

        if (cacheReferences) {
            bindings = CachingReference.wrapReferences(bindings);
        }

        Map localBindings = ContextUtil.createBindings(bindings, this);
        this.localBindings = Collections.unmodifiableMap(localBindings);

        Map globalBindings = ImmutableContext.buildAbsoluteIndex("", localBindings);
        this.absoluteIndex = Collections.unmodifiableMap(globalBindings);
    }

    private static Map buildAbsoluteIndex(String nameInNamespace, Map bindings) {
        String path = nameInNamespace;
        if (path.length() > 0) {
            path += "/";
        }

        Map globalBindings = new HashMap();
        for (Iterator iterator = bindings.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();
            if (value instanceof ImmutableContext.NestedImmutableContext) {
                ImmutableContext.NestedImmutableContext nestedContext = (ImmutableContext.NestedImmutableContext)value;
                globalBindings.putAll(ImmutableContext.buildAbsoluteIndex(nestedContext.getNameInNamespace(), nestedContext.localBindings));
            }
            globalBindings.put(path + name, value);
        }
        return globalBindings;
    }

    protected Object getDeepBinding(String name) {
        return absoluteIndex.get(name);
    }

    protected Map getBindings() {
        return localBindings;
    }

    protected void addDeepBinding(String name, Object value, boolean createIntermediateContexts) throws NamingException {
        throw new OperationNotSupportedException("Context is immutable");
    }

    protected void addBinding(String name, Object value, boolean rebind) throws NamingException {
        throw new OperationNotSupportedException("Context is immutable");
    }

    protected void removeDeepBinding(Name name, boolean pruneEmptyContexts) throws NamingException {
        throw new OperationNotSupportedException("Context is immutable");
    }

    protected void removeBinding(String name) throws NamingException {
        throw new OperationNotSupportedException("Context is immutable");
    }

    public boolean isNestedSubcontext(Object value) {
        if (value instanceof NestedImmutableContext) {
            NestedImmutableContext context = (NestedImmutableContext) value;
            return this == context.getImmutableContext();
        }
        return false;
    }

    public Context createNestedSubcontext(String path, Map bindings) {
        return new NestedImmutableContext(path, bindings);
    }

    /**
     * Nested context which shares the absolute index map in MapContext.
     */
    public final class NestedImmutableContext extends AbstractUnmodifiableContext {
        private final Map localBindings;
        private final String pathWithSlash;

        public NestedImmutableContext(String path, Map bindings) {
            super(ImmutableContext.this.getNameInNamespace(path));

            if (!path.endsWith("/")) path += "/";
            this.pathWithSlash = path;

            this.localBindings = Collections.unmodifiableMap(bindings);
        }

        protected Object getDeepBinding(String name) {
            String absoluteName = pathWithSlash + name;
            return absoluteIndex.get(absoluteName);
        }

        protected Map getBindings() {
            return localBindings;
        }

        protected void addDeepBinding(String name, Object value, boolean createIntermediateContexts) throws NamingException {
            throw new OperationNotSupportedException("Context is immutable");
        }

        protected void addBinding(String name, Object value, boolean rebind) throws NamingException {
            throw new OperationNotSupportedException("Context is immutable");
        }

        protected void removeDeepBinding(Name name, boolean pruneEmptyContexts) throws NamingException {
            throw new OperationNotSupportedException("Context is immutable");
        }

        protected void removeBinding(String name) throws NamingException {
            throw new OperationNotSupportedException("Context is immutable");
        }

        public boolean isNestedSubcontext(Object value) {
            if (value instanceof NestedImmutableContext) {
                NestedImmutableContext context = (NestedImmutableContext) value;
                return getImmutableContext() == context.getImmutableContext();
            }
            return false;
        }

        public Context createNestedSubcontext(String path, Map bindings) {
            return new NestedImmutableContext(path, bindings);
        }

        private ImmutableContext getImmutableContext() {
            return ImmutableContext.this;
        }
    }
}
