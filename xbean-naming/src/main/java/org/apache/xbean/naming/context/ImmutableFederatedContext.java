/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.xbean.naming.context;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;

/**
 * @version $Rev$ $Date$
 */
public class ImmutableFederatedContext extends AbstractFederatedContext {

    public ImmutableFederatedContext(String nameInNamespace, Set<Context> federatedContexts) {
        super(nameInNamespace, ContextAccess.UNMODIFIABLE, federatedContexts);
    }

    public void federateContext(Context context) throws NamingException {
        addFederatedContext(this, context);
    }

    public void unfederateContext(Context context) throws NamingException {
        removeFederatedContext(this, context);
    }

    @Override
    protected Map<String, Object> getWrapperBindings() throws NamingException {
        return Collections.emptyMap();
    }

    public Context createNestedSubcontext(String path, Map<String, Object> bindings) throws NamingException {
        return new NestedImmutableFederatedContext(path, bindings);
    }

    /**
      * Nested context which shares the absolute index map in MapContext.
      */
     public class NestedImmutableFederatedContext extends AbstractFederatedContext {
         private final AtomicReference<Map<String, Object>> bindingsRef;
         private final String pathWithSlash;

         public NestedImmutableFederatedContext(String path, Map<String, Object> bindings) throws NamingException {
             super(ImmutableFederatedContext.this, path);

             path = getNameInNamespace();
             if (!path.endsWith("/")) path += "/";
             this.pathWithSlash = path;

             this.bindingsRef = new AtomicReference<Map<String, Object>>(Collections.unmodifiableMap(bindings));
         }

         public Context createNestedSubcontext(String path, Map<String, Object> bindings) throws NamingException {
             return new NestedImmutableFederatedContext(getNameInNamespace(path), bindings);
         }

         protected Object getDeepBinding(String name) {
             String absoluteName = pathWithSlash + name;
             return ImmutableFederatedContext.this.getDeepBinding(absoluteName);
         }

         protected Map<String, Object> getWrapperBindings() throws NamingException {
             return bindingsRef.get();
         }

         protected boolean addBinding(String name, Object value, boolean rebind) throws NamingException {
             throw new OperationNotSupportedException("Context is immutable");
         }

         protected boolean removeBinding(String name, boolean removeNotEmptyContext) throws NamingException {
             throw new OperationNotSupportedException("Context is immutable");
         }
     }

}
