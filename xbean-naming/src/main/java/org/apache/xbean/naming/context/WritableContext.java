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
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;
import java.util.HashMap;
import java.util.Map;

/**
 * @version $Rev$ $Date$
 */
public class WritableContext extends AbstractContext {
    protected final Map bindings;

    public WritableContext() {
        super("");
        bindings = new HashMap();
    }

    public WritableContext(String nameInNamespace) {
        super(nameInNamespace);
        bindings = new HashMap();
    }

    public Context createContext(String path, Map bindings) {
        return new WritableContext(getNameInNamespace(path));
    }

    protected Map getBindings() {
        return bindings;
    }

    protected void addBinding(String name, Object value, boolean rebind) throws NamingException {
        synchronized (bindings) {
            if (rebind || !bindings.containsKey(name)) {
                bindings.put(name, value);
            } else {
                throw new NameAlreadyBoundException("The name " + name + " is already bound");
            }
        }
    }

    protected void removeBinding(String name) throws NamingException {
        synchronized (bindings) {
            bindings.remove(name);
        }
    }
}
