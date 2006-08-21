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
import javax.naming.OperationNotSupportedException;
import java.util.Map;

/**
 * @version $Rev: 417891 $ $Date: 2006-06-28 15:45:07 -0700 (Wed, 28 Jun 2006) $
 */
public class UnmodifiableContext extends WritableContext {
    public UnmodifiableContext() throws NamingException {
    }

    public UnmodifiableContext(String nameInNamespace) throws NamingException {
        super(nameInNamespace);
    }

    public UnmodifiableContext(Map bindings) throws NamingException {
        super(bindings);
    }

    public UnmodifiableContext(Map bindings, boolean cacheReferences) throws NamingException {
        super(bindings, cacheReferences);
    }

    public UnmodifiableContext(String nameInNamespace, Map bindings, boolean cacheReferences) throws NamingException {
        super(nameInNamespace, bindings, cacheReferences);
    }

    public boolean isNestedSubcontext(Object value) {
        if (value instanceof NestedUnmodifiableContext) {
            NestedUnmodifiableContext context = (NestedUnmodifiableContext) value;
            return this == context.getUnmodifiableContext();
        }
        return false;
    }

    public Context createNestedSubcontext(String path, Map bindings) {
        return new NestedUnmodifiableContext(path,bindings);
    }

    public final void bind(Name name, Object obj) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final void bind(String name, Object obj) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final void close() throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final Context createSubcontext(Name name) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final Context createSubcontext(String name) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final void destroySubcontext(Name name) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final void destroySubcontext(String name) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final void rebind(Name name, Object obj) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final void rebind(String name, Object obj) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final void rename(Name oldName, Name newName) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final void rename(String oldName, String newName) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final void unbind(Name name) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final void unbind(String name) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    /**
     * Nested context which shares the absolute index map in MapContext.
     */
    public class NestedUnmodifiableContext extends NestedWritableContext {
        public NestedUnmodifiableContext(String path, String key, Object value) {
            super(path, key, value);
        }

        public NestedUnmodifiableContext(String path, Map bindings) {
            super(path, bindings);
        }

        public boolean isNestedSubcontext(Object value) {
            if (value instanceof NestedUnmodifiableContext) {
                NestedUnmodifiableContext context = (NestedUnmodifiableContext) value;
                return getUnmodifiableContext() == context.getUnmodifiableContext();
            }
            return false;
        }

        public Context createNestedSubcontext(String path, Map bindings) {
            return new NestedUnmodifiableContext(path, bindings);
        }

        protected UnmodifiableContext getUnmodifiableContext() {
            return UnmodifiableContext.this;
        }
        public final void bind(Name name, Object obj) throws NamingException {
            throw new OperationNotSupportedException("Context is read only");
        }

        public final void bind(String name, Object obj) throws NamingException {
            throw new OperationNotSupportedException("Context is read only");
        }

        public final void close() throws NamingException {
            throw new OperationNotSupportedException("Context is read only");
        }

        public final Context createSubcontext(Name name) throws NamingException {
            throw new OperationNotSupportedException("Context is read only");
        }

        public final Context createSubcontext(String name) throws NamingException {
            throw new OperationNotSupportedException("Context is read only");
        }

        public final void destroySubcontext(Name name) throws NamingException {
            throw new OperationNotSupportedException("Context is read only");
        }

        public final void destroySubcontext(String name) throws NamingException {
            throw new OperationNotSupportedException("Context is read only");
        }

        public final void rebind(Name name, Object obj) throws NamingException {
            throw new OperationNotSupportedException("Context is read only");
        }

        public final void rebind(String name, Object obj) throws NamingException {
            throw new OperationNotSupportedException("Context is read only");
        }

        public final void rename(Name oldName, Name newName) throws NamingException {
            throw new OperationNotSupportedException("Context is read only");
        }

        public final void rename(String oldName, String newName) throws NamingException {
            throw new OperationNotSupportedException("Context is read only");
        }

        public final void unbind(Name name) throws NamingException {
            throw new OperationNotSupportedException("Context is read only");
        }

        public final void unbind(String name) throws NamingException {
            throw new OperationNotSupportedException("Context is read only");
        }

    }
}
