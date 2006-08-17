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
package org.apache.xbean.naming.reference;

import org.apache.xbean.naming.context.ContextUtil;

import javax.naming.Reference;
import javax.naming.NamingException;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Iterator;

/**
 * @version $Rev: 355877 $ $Date: 2005-12-10 18:48:27 -0800 (Sat, 10 Dec 2005) $
 */
public class CachingReference extends SimpleReference {
    public static Map wrapReferences(Map bindings) {
        LinkedHashMap newBindings = new LinkedHashMap(bindings);
        for (Iterator iterator = bindings.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Reference && !(value instanceof CachingReference)) {
                newBindings.put(name, new CachingReference(name, (Reference)value));
            }
        }
        return newBindings;
    }

    private final Object lock = new Object();
    private final String fullName;
    private final Reference reference;
    private final String className;
    private Object value;

    public CachingReference(String fullName, Reference reference) {
        this.fullName = fullName;
        this.reference = reference;
        className = reference.getClassName();
    }

    public Object getContent() throws NamingException {
        synchronized(lock) {
            if (value == null) {
                value = ContextUtil.resolve(fullName, reference);
            }
            return value;
        }
    }

    public String getClassName() {
        return className;
    }
}
