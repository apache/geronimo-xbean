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

import javax.naming.NamingException;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.Context;
import javax.naming.OperationNotSupportedException;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @version $Rev$ $Date$
 */
public abstract class AbstractFederatedContext extends AbstractContext {
    private final ContextFederation contextFederation = new ContextFederation(this);
    private final ContextAccess contextAccess;
    private final boolean modifiable;

    public AbstractFederatedContext() {
        this("", ContextAccess.MODIFIABLE);
    }

    public AbstractFederatedContext(String nameInNamespace) {
        this(nameInNamespace, ContextAccess.MODIFIABLE);
    }

    protected AbstractFederatedContext(String nameInNamespace, ContextAccess contextAccess) {
        super(nameInNamespace);
        this.contextAccess = contextAccess;
        modifiable = contextAccess.isModifiable(getParsedNameInNamespace());
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

    protected void addFederatedContext(Context federatedContext) throws NamingException {
        contextFederation.addContext(federatedContext);
        for (Iterator iterator = getBindings().values().iterator(); iterator.hasNext();) {
            Object value = iterator.next();
            if (value instanceof AbstractNestedFederatedContext) {
                AbstractNestedFederatedContext nestedContext = (AbstractNestedFederatedContext) value;
                nestedContext.addFederatedContext(federatedContext);
            }
        }
    }

    public boolean isNestedSubcontext(Object value) {
        if (value instanceof AbstractNestedFederatedContext) {
            AbstractFederatedContext.AbstractNestedFederatedContext context = (AbstractNestedFederatedContext) value;
            return this == context.getOuterContext();
        }
        return false;
    }

    public void bind(String name, Object obj) throws NamingException {
        if (!modifiable) {
            throw new OperationNotSupportedException("Context is read only");
        }
        super.bind(name, obj);
    }

    public void bind(Name name, Object obj) throws NamingException {
        if (!modifiable) {
            throw new OperationNotSupportedException("Context is read only");
        }
        super.bind(name, obj);
    }

    public void rebind(String name, Object obj) throws NamingException {
        if (!modifiable) {
            throw new OperationNotSupportedException("Context is read only");
        }
        super.rebind(name, obj);
    }

    public void rebind(Name name, Object obj) throws NamingException {
        if (!modifiable) {
            throw new OperationNotSupportedException("Context is read only");
        }
        super.rebind(name, obj);
    }

    public void rename(String oldName, String newName) throws NamingException {
        if (!modifiable) {
            throw new OperationNotSupportedException("Context is read only");
        }
        super.rename(oldName, newName);
    }

    public void rename(Name oldName, Name newName) throws NamingException {
        if (!modifiable) {
            throw new OperationNotSupportedException("Context is read only");
        }
        super.rename(oldName, newName);
    }

    public void unbind(String name) throws NamingException {
        if (!modifiable) {
            throw new OperationNotSupportedException("Context is read only");
        }
        super.unbind(name);
    }

    public void unbind(Name name) throws NamingException {
        if (!modifiable) {
            throw new OperationNotSupportedException("Context is read only");
        }
        super.unbind(name);
    }

    public Context createSubcontext(String name) throws NamingException {
        if (!modifiable) {
            throw new OperationNotSupportedException("Context is read only");
        }
        return super.createSubcontext(name);
    }

    public Context createSubcontext(Name name) throws NamingException {
        if (!modifiable) {
            throw new OperationNotSupportedException("Context is read only");
        }
        return super.createSubcontext(name);
    }

    public void destroySubcontext(String name) throws NamingException {
        if (!modifiable) {
            throw new OperationNotSupportedException("Context is read only");
        }
        super.destroySubcontext(name);
    }

    public void destroySubcontext(Name name) throws NamingException {
        if (!modifiable) {
            throw new OperationNotSupportedException("Context is read only");
        }
        super.destroySubcontext(name);
    }

    public abstract class AbstractNestedFederatedContext extends AbstractContext {
        private final ContextFederation contextFederation;
        private final boolean modifiable;

        public AbstractNestedFederatedContext(String path) throws NamingException {
            super(AbstractFederatedContext.this.getNameInNamespace(path));

            AbstractFederatedContext outerContext = getOuterContext();
            this.contextFederation = outerContext.contextFederation.createSubcontextFederation(path, this);
            this.modifiable = outerContext.contextAccess.isModifiable(getParsedNameInNamespace());
        }

        public boolean isNestedSubcontext(Object value) {
            if (value instanceof AbstractNestedFederatedContext) {
                AbstractNestedFederatedContext context = (AbstractNestedFederatedContext) value;
                return getOuterContext() == context.getOuterContext();
            }
            return false;
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

        protected AbstractFederatedContext getOuterContext() {
            return AbstractFederatedContext.this;
        }

        protected void addFederatedContext(Context federatedContext) throws NamingException {
            contextFederation.addContext(federatedContext);
            for (Iterator iterator = getBindings().values().iterator(); iterator.hasNext();) {
                Object value = iterator.next();
                if (value instanceof AbstractNestedFederatedContext) {
                    AbstractNestedFederatedContext nestedContext = (AbstractNestedFederatedContext) value;
                    nestedContext.addFederatedContext(federatedContext);
                }
            }
        }

        public void bind(String name, Object obj) throws NamingException {
            if (!modifiable) {
                throw new OperationNotSupportedException("Context is read only");
            }
            super.bind(name, obj);
        }

        public void bind(Name name, Object obj) throws NamingException {
            if (!modifiable) {
                throw new OperationNotSupportedException("Context is read only");
            }
            super.bind(name, obj);
        }

        public void rebind(String name, Object obj) throws NamingException {
            if (!modifiable) {
                throw new OperationNotSupportedException("Context is read only");
            }
            super.rebind(name, obj);
        }

        public void rebind(Name name, Object obj) throws NamingException {
            if (!modifiable) {
                throw new OperationNotSupportedException("Context is read only");
            }
            super.rebind(name, obj);
        }

        public void rename(String oldName, String newName) throws NamingException {
            if (!modifiable) {
                throw new OperationNotSupportedException("Context is read only");
            }
            super.rename(oldName, newName);
        }

        public void rename(Name oldName, Name newName) throws NamingException {
            if (!modifiable) {
                throw new OperationNotSupportedException("Context is read only");
            }
            super.rename(oldName, newName);
        }

        public void unbind(String name) throws NamingException {
            if (!modifiable) {
                throw new OperationNotSupportedException("Context is read only");
            }
            super.unbind(name);
        }

        public void unbind(Name name) throws NamingException {
            if (!modifiable) {
                throw new OperationNotSupportedException("Context is read only");
            }
            super.unbind(name);
        }

        public Context createSubcontext(String name) throws NamingException {
            if (!modifiable) {
                throw new OperationNotSupportedException("Context is read only");
            }
            return super.createSubcontext(name);
        }

        public Context createSubcontext(Name name) throws NamingException {
            if (!modifiable) {
                throw new OperationNotSupportedException("Context is read only");
            }
            return super.createSubcontext(name);
        }

        public void destroySubcontext(String name) throws NamingException {
            if (!modifiable) {
                throw new OperationNotSupportedException("Context is read only");
            }
            super.destroySubcontext(name);
        }

        public void destroySubcontext(Name name) throws NamingException {
            if (!modifiable) {
                throw new OperationNotSupportedException("Context is read only");
            }
            super.destroySubcontext(name);
        }
    }
}
