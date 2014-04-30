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

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.NameParser;
import javax.naming.Binding;
import javax.naming.NameClassPair;

import java.util.Hashtable;

/**
 * @version $Rev$ $Date$
 */
public class VirtualSubcontext extends ContextFlyweight {
    private final Name nameInContext;
    private final Context context;

    public VirtualSubcontext(Name nameInContext, Context context) throws NamingException {
        if (context instanceof VirtualSubcontext) {
            VirtualSubcontext virtualSubcontext = (VirtualSubcontext) context;
            this.nameInContext = virtualSubcontext.getName(nameInContext);
            this.context = virtualSubcontext.context;
        } else {
            this.nameInContext = nameInContext;
            this.context = context;
        }
    }

    @Override
    protected Context getContext() throws NamingException {
        return context;
    }

    @Override
    protected Name getName(Name name) throws NamingException {
        return context.composeName(nameInContext, name);
    }

    @Override
    protected String getName(String name) throws NamingException {
        Name parsedName = context.getNameParser("").parse(name);
        return context.composeName(nameInContext, parsedName).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VirtualSubcontext that = (VirtualSubcontext) o;

        if (context != null ? !context.equals(that.context) : that.context != null) return false;
        if (nameInContext != null ? !nameInContext.equals(that.nameInContext) : that.nameInContext != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = nameInContext != null ? nameInContext.hashCode() : 0;
        result = 31 * result + (context != null ? context.hashCode() : 0);
        return result;
    }

    public void close() throws NamingException {
        context.close();
    }

    public String getNameInNamespace() throws NamingException {
        Name parsedNameInNamespace = context.getNameParser("").parse(context.getNameInNamespace());
        return context.composeName(parsedNameInNamespace, nameInContext).toString();
    }
}
