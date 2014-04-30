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
package org.apache.xbean.naming.global;

import org.apache.xbean.naming.context.ContextFlyweight;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.OperationNotSupportedException;
import javax.naming.NoInitialContextException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;

/**
 * The GlobalContextManager contains the static global context object.  JNDI effectively requires a single global static
 * to resolve the root context, and this class manages that static.  This class is also an URLContextFactory and
 * an InitialContextFactory which returns the registered global context.
 *
 * To use this factory simply set the following system property or pass the property in the environment to new InitialContext:
 *
 * java.naming.factory.initial = org.apache.xbean.naming.global.GlobalContextManager
 *
 * @version $Rev$ $Date$
 */
public class GlobalContextManager implements ObjectFactory, InitialContextFactory {
    private static Context DEFAULT_CONTEXT = new DefaultGlobalContext();
    private static Context globalContext;

    /**
     * Gets the global context.  This context is the root of all contexts and will contain entries such as "java:comp".
     * @return the global context
     */
    public static synchronized Context getGlobalContext() {
        if (globalContext == null) return DEFAULT_CONTEXT;
        return globalContext;
    }

    /**
     * Sets the global context. To invoke this method the calling code must have "setFactory" RuntimePermission.
     * @param globalContext the new global context
     */
    public static synchronized void setGlobalContext(Context globalContext) {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkSetFactory();
        }
        GlobalContextManager.globalContext = globalContext;
    }

    /**
     * Returns the Context registered with the GlobalManager. This method is equivalent to:
     *
     * return GlobalContextManager.getGlobalContext();
     *
     * @param obj must be null
     * @param name ignored
     * @param nameCtx ignored
     * @param environment ignored
     * @return GlobalManager.getGlobalContext()
     * @throws javax.naming.OperationNotSupportedException if obj is not null
     */
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable environment) throws Exception {
        if (obj == null) {
            return GlobalContextManager.getGlobalContext();
        } else {
            throw new OperationNotSupportedException();
        }
    }


    /**
     * Returns the Context registered with the GlobalManager. This method is equivalent to:
     *
     * return GlobalContextManager.getGlobalContext();
     *
     * @param environment ignored
     * @return GlobalContextManager.getGlobalContext()
     */
    public Context getInitialContext(Hashtable environment) {
        return GlobalContextManager.getGlobalContext();
    }

    private static class DefaultGlobalContext extends ContextFlyweight {
        protected Context getContext() throws NoInitialContextException {
            synchronized (GlobalContextManager.class) {
                if (globalContext == null) throw new NoInitialContextException("Global context has not been set");
                return globalContext;
            }
        }
    }
}
