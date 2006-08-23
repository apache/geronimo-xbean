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

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

/**
 * @version $Rev$ $Date$
 */
public class ContextFederation {
    private final Context actualContext;
    private final AtomicReference federatedContextRef = new AtomicReference(Collections.EMPTY_SET);
    public static final int MAX_WRITE_ATTEMPTS = 10;

    public ContextFederation(Context actualContext) {
        this.actualContext = actualContext;
    }

    public void addContext(Context context) {
        Set federatedContext;
        Set newFederatedContext;
        for (int i = 0; i < MAX_WRITE_ATTEMPTS; i++) {
            federatedContext = getFederatedContexts();

            newFederatedContext = new LinkedHashSet(federatedContext);
            newFederatedContext.add(context);
            newFederatedContext = Collections.unmodifiableSet(newFederatedContext);
            if (federatedContextRef.compareAndSet(federatedContext, newFederatedContext)) {
                return;
            }
        }
        throw new RuntimeException("Unable to update federatedContextRef within " + MAX_WRITE_ATTEMPTS + " attempts");
    }

    public Set getFederatedContexts() {
        return (Set) federatedContextRef.get();
    }

    public Map getFederatedBindings() throws NamingException {
        Map bindings = new HashMap();
        for (Iterator iterator = getFederatedContexts().iterator(); iterator.hasNext();) {
            Context context = (Context) iterator.next();

            // list federated context
            NamingEnumeration namingEnumeration = context.listBindings("");

            // add to bindings
            Map map = ContextUtil.listBindingsToMap(namingEnumeration);
            bindings.putAll(map);
        }
        return bindings;
    }

    public Object lookup(Name name) {
        for (Iterator iterator = getFederatedContexts().iterator(); iterator.hasNext();) {
            try {
                Context federatedContext = (Context) iterator.next();
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
        for (Iterator iterator = getFederatedContexts().iterator(); iterator.hasNext();) {
            Context federatedContext = (Context) iterator.next();
            VirtualSubcontext virtualSubcontext = new VirtualSubcontext(parsedSubcontextName, federatedContext);
            subcontextFederation.addContext(virtualSubcontext);
        }
        return subcontextFederation;
    }
}
