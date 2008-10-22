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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.naming.Name;
import javax.naming.NamingException;

/**
 * @version $Rev$ $Date$
 */
public class ContextAccessControlList implements ContextAccess {
    private final boolean defaultAllow;
    private final List<Name> allow;
    private final List<Name> deny;

    public ContextAccessControlList(boolean defaultAllow, List allow, List deny) {
        this.defaultAllow = defaultAllow;
        this.allow = toACL(allow);
        this.deny = toACL(deny);
    }

    private List<Name> toACL(List input) {
        if (input == null) return Collections.emptyList();
        
        ArrayList<Name> list = new ArrayList<Name>(input.size());
        for (Object value : input) {
            if (value instanceof Name) {
                list.add((Name) value);
            } else if (value instanceof String) {
                String string = (String) value;
                Name name;
                try {
                    name = ContextUtil.NAME_PARSER.parse(string);
                } catch (NamingException e) {
                    throw new IllegalArgumentException("error while parsing name: " + value);
                }
                list.add(name);
            } else {
                throw new IllegalArgumentException("name is not an instance of Name or String: " + value);
            }
        }
        return Collections.unmodifiableList(list);
    }

    public boolean isModifiable(Name name) {
        if (name == null) throw new NullPointerException("name is null");
        if (defaultAllow) {
            // allow by default, so allow if it wasn't explicitly denied or was explicitly allowed
            return !isDenied(name) || isAllowed(name);
        } else {
            // deny by default, so allow if it was explicitly allowed or wasn't explicitly denied
            return isAllowed(name) && !isDenied(name);
        }
    }

    protected boolean isAllowed(Name name) {
        if (name == null) throw new NullPointerException("name is null");
        for (Name prefix : allow) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    protected boolean isDenied(Name name) {
        if (name == null) throw new NullPointerException("name is null");
        for (Name prefix : deny) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }
}
