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

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;

import org.apache.xbean.naming.reference.CachingReference;

/**
 *
 * @version $Rev: 417891 $ $Date: 2006-06-28 15:45:07 -0700 (Wed, 28 Jun 2006) $
 */
public class ImmutableContext extends AbstractContext {
    private final Map<String, Object> localBindings;
    private final Map<String, Object> absoluteIndex;

    public ImmutableContext(Map<String, Object> bindings) throws NamingException {
        this("", bindings, true);
    }

    public ImmutableContext(Map<String, Object> bindings, boolean cacheReferences) throws NamingException {
        this("", bindings, cacheReferences);
    }

    public ImmutableContext(String nameInNamespace, Map<String, Object> bindings, boolean cacheReferences) throws NamingException {
        super(nameInNamespace, ContextAccess.UNMODIFIABLE);

        if (cacheReferences) {
            bindings = CachingReference.wrapReferences(bindings, this);
        }

        Map<String, Object> localBindings = ContextUtil.createBindings(bindings, this);
        this.localBindings = Collections.unmodifiableMap(localBindings);

        Map<String, Object> globalBindings = ImmutableContext.buildAbsoluteIndex("", localBindings);
        this.absoluteIndex = Collections.unmodifiableMap(globalBindings);
    }

    private static Map<String, Object> buildAbsoluteIndex(String nameInNamespace, Map<String, Object> bindings) {
        String path = nameInNamespace;
        if (path.length() > 0) {
            path += "/";
        }

        Map<String, Object> globalBindings = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : bindings.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof NestedImmutableContext) {
                NestedImmutableContext nestedContext = (NestedImmutableContext) value;
                globalBindings.putAll(ImmutableContext.buildAbsoluteIndex(nestedContext.getNameInNamespace(), nestedContext.localBindings));
            }
            globalBindings.put(path + name, value);
        }
        return globalBindings;
    }

    protected Object getDeepBinding(String name) {
        return absoluteIndex.get(name);
    }

    protected Map<String, Object> getBindings() {
        return localBindings;
    }

    protected final void addDeepBinding(String name, Object value, boolean createIntermediateContexts) throws NamingException {
        throw new OperationNotSupportedException("Context is immutable");
    }

    protected final boolean addBinding(String name, Object value, boolean rebind) throws NamingException {
        throw new OperationNotSupportedException("Context is immutable");
    }

    protected final void removeDeepBinding(Name name, boolean pruneEmptyContexts) throws NamingException {
        throw new OperationNotSupportedException("Context is immutable");
    }

    protected final boolean removeBinding(String name, boolean removeNotEmptyContext) throws NamingException {
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
    public final class NestedImmutableContext extends AbstractContext {
        private final Map<String, Object> localBindings;
        private final String pathWithSlash;

        public NestedImmutableContext(String path, Map<String, Object> bindings) {
            super(ImmutableContext.this.getNameInNamespace(path), ContextAccess.UNMODIFIABLE);

            path = getNameInNamespace();
            if (!path.endsWith("/")) path += "/";
            this.pathWithSlash = path;

            this.localBindings = Collections.unmodifiableMap(bindings);
        }

        protected Object getDeepBinding(String name) {
            String absoluteName = pathWithSlash + name;
            return absoluteIndex.get(absoluteName);
        }

        protected Map<String, Object> getBindings() {
            return localBindings;
        }

        protected final void addDeepBinding(String name, Object value, boolean createIntermediateContexts) throws NamingException {
            throw new OperationNotSupportedException("Context is immutable");
        }

        protected final boolean addBinding(String name, Object value, boolean rebind) throws NamingException {
            throw new OperationNotSupportedException("Context is immutable");
        }

        protected final void removeDeepBinding(Name name, boolean pruneEmptyContexts) throws NamingException {
            throw new OperationNotSupportedException("Context is immutable");
        }

        protected final boolean removeBinding(String name, boolean removeNotEmptyContext) throws NamingException {
            throw new OperationNotSupportedException("Context is immutable");
        }

        public boolean isNestedSubcontext(Object value) {
            if (value instanceof NestedImmutableContext) {
                NestedImmutableContext context = (NestedImmutableContext) value;
                return getImmutableContext() == context.getImmutableContext();
            }
            return false;
        }

        public Context createNestedSubcontext(String path, Map<String, Object> bindings) {
            return new NestedImmutableContext(path, bindings);
        }

        protected ImmutableContext getImmutableContext() {
            return ImmutableContext.this;
        }
    }
}
