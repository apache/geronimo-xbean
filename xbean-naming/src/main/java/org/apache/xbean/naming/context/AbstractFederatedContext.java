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
import java.util.Map;
import java.util.Set;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;

/**
 * @version $Rev$ $Date$
 */
public abstract class AbstractFederatedContext extends AbstractContext {
    private final ContextFederation contextFederation;
    private final AbstractFederatedContext masterContext;

    public AbstractFederatedContext() {
        this("", ContextAccess.MODIFIABLE);
    }

    public AbstractFederatedContext(String nameInNamespace) {
        this(nameInNamespace, ContextAccess.MODIFIABLE);
    }

    public AbstractFederatedContext(String nameInNamespace, ContextAccess contextAccess) {
        super(nameInNamespace, contextAccess);
        this.masterContext = this;
        this.contextFederation = new ContextFederation(this);
    }

    public AbstractFederatedContext(String nameInNamespace, ContextAccess contextAccess, Set<Context> federatedContexts) {
        super(nameInNamespace, contextAccess);
        this.masterContext = this;
        this.contextFederation = new ContextFederation(this, federatedContexts);
    }

    public AbstractFederatedContext(AbstractFederatedContext masterContext, String nameInNamespace) throws NamingException {
        super(nameInNamespace, masterContext.getContextAccess());
        this.masterContext = masterContext;
        this.contextFederation = this.masterContext.contextFederation.createSubcontextFederation(nameInNamespace, this);
    }

    protected Object faultLookup(String stringName, Name parsedName) {
        Object value = contextFederation.lookup(parsedName);
        if (value != null) {
            return value;
        }
        return super.faultLookup(stringName, parsedName);
    }

    @Override
    protected Object getDeepBinding(String name) {
        try {
            Object value = contextFederation.getFederatedBinding(name);
            if (value instanceof Context) {
                return null;
            }
            return value;
        } catch (NamingException e) {
            return null;
        }
    }

    @Override
    protected Object getBinding(String name) throws NamingException {
        Object value = contextFederation.getFederatedBinding(name);
        if (value instanceof Context) {
            return createNestedSubcontext(name, getBindings(name));
        }
        if (value == null) {
            value = getWrapperBindings().get(name);
        }
        return value;
    }

    protected final Map<String, Object> getBindings() throws NamingException {
        return getBindings("");
    }

    protected final Map<String, Object> getBindings(String name) throws NamingException {
        Map<String, Object> bindings = contextFederation.getFederatedBindings(name);
        bindings.putAll(getWrapperBindings());
        return bindings;
    }

    protected abstract Map<String, Object> getWrapperBindings() throws NamingException;

    protected boolean addBinding(String name, Object value, boolean rebind) throws NamingException {
        if (!(value instanceof Context && !isNestedSubcontext(value))) {
            return contextFederation.addBinding(name, value, rebind);
        } else if (!isNestedSubcontext(value)) {
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
                AbstractFederatedContext nestedContext = (AbstractFederatedContext) createNestedSubcontext(name, Collections.<String, Object>emptyMap());
                addFederatedContext(nestedContext, federatedContext);

                // call back into this method using the new nested context
                // this gives subclasses a chance to handle the binding
                return addBinding(name, nestedContext, rebind);
            }
        }

        return false;
    }

    protected boolean removeBinding(String name, boolean removeNotEmptyContext) throws NamingException {
        return contextFederation.removeBinding(name);
    }

    protected static void addFederatedContext(AbstractFederatedContext wrappingContext, Context innerContext) throws NamingException {
        wrappingContext.contextFederation.addContext(innerContext);
        for (Map.Entry<String, Object> entry : wrappingContext.getWrapperBindings().entrySet()) {
            String name = entry.getKey();
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
    
    protected static void removeFederatedContext(AbstractFederatedContext wrappingContext, Context innerContext) throws NamingException {
        wrappingContext.contextFederation.removeContext(innerContext);
        for (Map.Entry<String, Object> entry : wrappingContext.getWrapperBindings().entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof AbstractFederatedContext) {
                AbstractFederatedContext nestedContext = (AbstractFederatedContext) value;

                Name parsedName = wrappingContext.getNameParser().parse(name);
                Name nameInNamespace = wrappingContext.getNameInNamespace(parsedName);

                VirtualSubcontext virtualSubcontext = new VirtualSubcontext(nameInNamespace, innerContext);
                removeFederatedContext(nestedContext, virtualSubcontext);
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
}
