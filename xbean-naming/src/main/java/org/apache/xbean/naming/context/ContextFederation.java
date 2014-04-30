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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import javax.naming.OperationNotSupportedException;

/**
 * @version $Rev$ $Date$
 */
public class ContextFederation {
    private final Context actualContext;
    private final AtomicReference<Set<Context>> federatedContextRef = new AtomicReference<Set<Context>>(Collections.<Context>emptySet());
    public static final int MAX_WRITE_ATTEMPTS = 10;

    public ContextFederation(Context actualContext) {
        this.actualContext = actualContext;
    }

    public ContextFederation(Context actualContext, Set<Context> federatedContexts) {
        this.actualContext = actualContext;
        Set<Context> copy = new LinkedHashSet<Context>(federatedContexts);
        federatedContextRef.set(Collections.unmodifiableSet(copy));
    }

    public void addContext(Context context) {
        Set<Context> federatedContext;
        Set<Context> newFederatedContext;
        for (int i = 0; i < MAX_WRITE_ATTEMPTS; i++) {
            federatedContext = getFederatedContexts();

            newFederatedContext = new LinkedHashSet<Context>(federatedContext);
            newFederatedContext.add(context);
            newFederatedContext = Collections.unmodifiableSet(newFederatedContext);
            if (federatedContextRef.compareAndSet(federatedContext, newFederatedContext)) {
                return;
            }
        }
        throw new RuntimeException("Unable to update federatedContextRef within " + MAX_WRITE_ATTEMPTS + " attempts");
    }
    
    public void removeContext(Context context) {
        Set<Context> federatedContext;
        Set<Context> newFederatedContext;
        for (int i = 0; i < MAX_WRITE_ATTEMPTS; i++) {
            federatedContext = getFederatedContexts();

            newFederatedContext = new LinkedHashSet<Context>(federatedContext);
            newFederatedContext.remove(context);
            newFederatedContext = Collections.unmodifiableSet(newFederatedContext);
            if (federatedContextRef.compareAndSet(federatedContext, newFederatedContext)) {
                return;
            }
        }
        throw new RuntimeException("Unable to update federatedContextRef within " + MAX_WRITE_ATTEMPTS + " attempts");
    }

    public Set<Context> getFederatedContexts() {
        return federatedContextRef.get();
    }

    public Object getFederatedBinding(String name) throws NamingException {
        for (Context context : getFederatedContexts()) {

            try {
                Object value = context.lookup(name);
                if (value != null) {
                    return value;
                }
            } catch (NamingException e) {
                // ignore
            }
        }
        return null;
    }

    public Map<String, Object> getFederatedBindings(String name) throws NamingException {
        Map<String, Object> bindings = new HashMap<String, Object>();
        for (Context context : getFederatedContexts()) {

            // list federated context
            try {
                NamingEnumeration namingEnumeration = context.listBindings(name);

                // add to bindings
                while (namingEnumeration.hasMoreElements()) {
                    Binding binding = (Binding) namingEnumeration.nextElement();
                    String bindingName = binding.getName();

                    // don't overwrite existing bindings
                    if (!bindings.containsKey(bindingName)) {
                        try {
                            bindings.put(bindingName, binding.getObject());
                        } catch (RuntimeException e) {
                            // if this is a wrapped NamingException, unwrap and throw the original 
                            Throwable cause = e.getCause(); 
                            if (cause != null && cause instanceof NamingException ) {
                                throw (NamingException)cause; 
                            }
                            // Wrap this into a RuntimeException.  
                            throw (NamingException)new NamingException("Could retrieve bound instance " + name).initCause(e);
                        }
                    }
                }
            } catch (NotContextException e) {
                //this context does not include the supplied name
            }
        }
        return bindings;
    }

    protected boolean addBinding(String name, Object value, boolean rebind) throws NamingException {
        for (Context context : getFederatedContexts()) {

            try {
                if (rebind) {
                    context.rebind(name, value);
                } else {
                    context.bind(name, value);
                }
                return true;
            } catch (OperationNotSupportedException ignored) {
            }
        }
        return false;
    }

    protected boolean removeBinding(String name) throws NamingException {
        for (Context context : getFederatedContexts()) {

            try {
                context.unbind(name);
                return true;
            } catch (OperationNotSupportedException ignored) {
            }
        }
        return false;
    }

    public Object lookup(Name name) {
        for (Context federatedContext : getFederatedContexts()) {
            try {
                Object value = federatedContext.lookup(name);
                if (value instanceof Context) {
                    return new VirtualSubcontext(name, actualContext);
                } else {
                    return value;
                }
            } catch (NamingException ignored) {
            }
        }
        return null;
    }

    public ContextFederation createSubcontextFederation(String subcontextName, Context actualSubcontext) throws NamingException {
        Name parsedSubcontextName = actualContext.getNameParser("").parse(subcontextName);

        ContextFederation subcontextFederation = new ContextFederation(actualSubcontext);
        for (Context federatedContext : getFederatedContexts()) {
            VirtualSubcontext virtualSubcontext = new VirtualSubcontext(parsedSubcontextName, federatedContext);
            subcontextFederation.addContext(virtualSubcontext);
        }
        return subcontextFederation;
    }
}
