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
import javax.naming.NameAlreadyBoundException;
import javax.naming.LinkRef;
import java.util.Map;
import java.util.HashMap;

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

    protected void removeBindings(Name name) throws NamingException {
        Map bindings;
        synchronized (this.bindings) {
            bindings = this.bindings;
        }
        if (name.size() == 1) {
            synchronized (bindings) {
                bindings.remove(name.toString());
            }
        } else {
            String segment = null;
            int lastIndex = name.size() - 1;
            Object terminalContext = null;
            for (int i = 0; i < lastIndex; i++) {
                segment = name.get(i);
                terminalContext = bindings.get(segment);
                if (terminalContext == null) {
                    throw new NamingException("The intermediate context "
                            + segment + " does not exist");
                } else if (!(terminalContext instanceof Context)) {
                    throw new NameAlreadyBoundException(
                            " An object that is not a context is already bound at element "
                                    + segment + "of name " + name);
                } else {
                    WritableContext writableContext = ((WritableContext) terminalContext);
                    synchronized (writableContext.bindings) {
                        bindings = writableContext.bindings;
                    }
                }
            }
            segment = name.get(lastIndex);
            ((Context) terminalContext).unbind(segment);
        }
    }

    protected void addBinding(String name, Object value, boolean rebind) throws NamingException {
        synchronized (bindings) {
            if (rebind || bindings.get(name.toString()) == null) {
                bindings.put(name, value);
            } else {
                throw new NameAlreadyBoundException("The name " + name
                        + "is already bound");
            }
        }
    }

    protected Map getBindings() {
        return bindings;
    }
}
