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
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

/**
 * @version $Rev$ $Date$
 */
public abstract class AbstractFederatedContext extends AbstractContext {
    private final ContextFederation contextFederation;
    private final ContextAccess contextAccess;
    private final boolean modifiable;
    private final AbstractFederatedContext masterContext;

    public AbstractFederatedContext() {
        this("", ContextAccess.MODIFIABLE);
    }

    public AbstractFederatedContext(String nameInNamespace) {
        this(nameInNamespace, ContextAccess.MODIFIABLE);
    }

    public AbstractFederatedContext(String nameInNamespace, ContextAccess contextAccess) {
        super(nameInNamespace);
        this.masterContext = this;
        this.contextFederation = new ContextFederation(this);
        this.contextAccess = contextAccess;
        this.modifiable = contextAccess.isModifiable(getParsedNameInNamespace());
    }

    public AbstractFederatedContext(AbstractFederatedContext masterContext, String path) throws NamingException {
        super(masterContext.getNameInNamespace(path));
        this.masterContext = masterContext;
        this.contextFederation = this.masterContext.contextFederation.createSubcontextFederation(path, this);
        this.contextAccess = masterContext.contextAccess;
        this.modifiable = masterContext.contextAccess.isModifiable(getParsedNameInNamespace());
    }

    protected Object faultLookup(String stringName, Name parsedName) {
        Object value = contextFederation.lookup(parsedName);
        if (value != null) {
            return value;
        }
        return super.faultLookup(stringName, parsedName);
    }

    protected final Map getBindings() throws NamingException {
        Map bindings = contextFederation.getFederatedBindings();
        bindings.putAll(getWrapperBindings());
        return bindings;
    }

    protected abstract Map getWrapperBindings() throws NamingException;

    protected boolean addBinding(String name, Object value, boolean rebind) throws NamingException {
        if (!(value instanceof Context && !isNestedSubcontext(value))) {
            return contextFederation.addBinding(name, value, rebind);
        } else if (value instanceof Context && !isNestedSubcontext(value)) {
            Context federatedContext = (Context) value;

            // if we already have a context bound at the specified value
            Object existingValue = getBinding(name);
            if (existingValue != null) {
                if (!(existingValue instanceof AbstractFederatedContext)) {
                    throw new NameAlreadyBoundException(name);
                }

                AbstractFederatedContext nestedContext = (AbstractFederatedContext) existingValue;
                addFederatedContext(nestedContext, federatedContext);
                return true;
            } else {
                AbstractFederatedContext nestedContext = (AbstractFederatedContext) createNestedSubcontext(name, Collections.EMPTY_MAP);
                addFederatedContext(nestedContext, federatedContext);

                // call back into this method using the new nested context
                // this gives subclasses a chance to handle the binding
                return addBinding(name, nestedContext, rebind);
            }
        }

        return false;
    }

    protected boolean removeBinding(String name) throws NamingException {
        return contextFederation.removeBinding(name);
    }

    protected static void addFederatedContext(AbstractFederatedContext wrappingContext, Context innerContext) throws NamingException {
        wrappingContext.contextFederation.addContext(innerContext);
        for (Iterator iterator = wrappingContext.getWrapperBindings().entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();
            if (value instanceof AbstractFederatedContext) {
                AbstractFederatedContext nestedContext = (AbstractFederatedContext) value;

                Name parsedName = wrappingContext.getNameParser().parse(name);
                Name nameInNamespace = wrappingContext.getNameInNamespace(parsedName);

                VirtualSubcontext virtualSubcontext = new VirtualSubcontext(nameInNamespace, innerContext);
                addFederatedContext(nestedContext, virtualSubcontext);
            }
        }
    }

    public boolean isNestedSubcontext(Object value) {
        if (value instanceof AbstractFederatedContext) {
            AbstractFederatedContext context = (AbstractFederatedContext) value;
            return getMasterContext() == context.getMasterContext();
        }
        return false;
    }

    protected AbstractFederatedContext getMasterContext() {
        return masterContext;
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
