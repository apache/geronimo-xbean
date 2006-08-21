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

import org.apache.xbean.naming.reference.SimpleReference;

import javax.naming.Binding;
import javax.naming.CompoundName;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Context;
import javax.naming.spi.NamingManager;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.HashMap;
import java.util.Enumeration;

/**
 * @version $Rev$ $Date$
 */
public final class ContextUtil {
    private ContextUtil() {
    }

    public final static NameParser NAME_PARSER = new SimpleNameParser();

    public static Name parseName(String name) throws NamingException {
        return NAME_PARSER.parse(name);
    }

    public static Object resolve(String name, Object value) throws NamingException {
        if (!(value instanceof Reference)) {
            return value;
        }

        Reference reference = (Reference) value;

        // for SimpleReference we can just call the getContext method
        if (reference instanceof SimpleReference) {
            try {
                return ((SimpleReference) reference).getContent();
            } catch (NamingException e) {
                throw e;
            } catch (Exception e) {
                throw (NamingException) new NamingException("Could not look up : " + name).initCause(e);
            }
        }

        if (reference instanceof org.apache.geronimo.naming.reference.SimpleReference) {
            try {
                return ((org.apache.geronimo.naming.reference.SimpleReference) reference).getContent();
            } catch (NamingException e) {
                throw e;
            } catch (Exception e) {
                throw (NamingException) new NamingException("Could not look up : " + name).initCause(e);
            }
        }

        // for normal References we have to do it the slow way
        try {
            return NamingManager.getObjectInstance(reference, null, null, new Hashtable());
        } catch (NamingException e) {
            throw e;
        } catch (Exception e) {
            throw (NamingException) new NamingException("Could not look up : " + name).initCause(e);
        }
    }

    public static final class ListEnumeration implements NamingEnumeration {
        private final Iterator iterator;

        public ListEnumeration(Map localBindings) {
            this.iterator = localBindings.entrySet().iterator();
        }

        public boolean hasMore() {
            return iterator.hasNext();
        }

        public boolean hasMoreElements() {
            return iterator.hasNext();
        }

        public Object next() {
            return nextElement();
        }

        public Object nextElement() {
            Map.Entry entry = (Map.Entry) iterator.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();
            String className = null;
            if (value instanceof Reference) {
                Reference reference = (Reference) value;
                className = reference.getClassName();
            } else {
                className = value.getClass().getName();
            }
            return new NameClassPair(name, className);
        }

        public void close() {
        }
    }

    public static final class ListBindingEnumeration implements NamingEnumeration {
        private final Iterator iterator;

        public ListBindingEnumeration(Map localBindings) {
            this.iterator = localBindings.entrySet().iterator();
        }

        public boolean hasMore() {
            return iterator.hasNext();
        }

        public boolean hasMoreElements() {
            return iterator.hasNext();
        }

        public Object next() {
            return nextElement();
        }

        public Object nextElement() {
            Map.Entry entry = (Map.Entry) iterator.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();
            return new ReadOnlyBinding(name, value);
        }

        public void close() {
        }
    }

    public static final class ReadOnlyBinding extends Binding {
        private final Object value;

        public ReadOnlyBinding(String name, Object value) {
            super(name, value);
            this.value = value;
        }

        public void setName(String name) {
            throw new UnsupportedOperationException("Context is read only");
        }

        public String getClassName() {
            if (value instanceof Reference) {
                Reference reference = (Reference) value;
                return reference.getClassName();
            }
            return value.getClass().getName();
        }

        public void setClassName(String name) {
            throw new UnsupportedOperationException("Context is read only");
        }

        public Object getObject() {
            try {
                return resolve(getName(), value);
            } catch (NamingException e) {
                throw new RuntimeException(e);
            }
        }

        public void setObject(Object obj) {
            throw new UnsupportedOperationException("Context is read only");
        }

        public boolean isRelative() {
            return false;
        }

        public void setRelative(boolean r) {
            throw new UnsupportedOperationException("Context is read only");
        }
    }


    private static final class SimpleNameParser implements NameParser {
        private static final Properties PARSER_PROPERTIES = new Properties();

        static {
            PARSER_PROPERTIES.put("jndi.syntax.direction", "left_to_right");
            PARSER_PROPERTIES.put("jndi.syntax.separator", "/");
        }


        private SimpleNameParser() {
        }

        public Name parse(String name) throws NamingException {
            return new CompoundName(name, PARSER_PROPERTIES);
        }
    }

    public static Map createBindings(Map absoluteBindings, NestedContextFactory factory) throws NamingException {
        // create a tree of Nodes using the absolute bindings
        Node node = buildMapTree(absoluteBindings);

        // convert the node tree into a tree of context objects
        Map localBindings = ContextUtil.createBindings(null, node, factory);

        return localBindings;
    }

    private static Map createBindings(String nameInNameSpace, Node node, NestedContextFactory factory) {
        Map bindings = new HashMap(node.size());
        for (Iterator iterator = node.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();

            // if this is a nested node we need to create a context for the node
            if (value instanceof Node) {
                Node nestedNode = (Node) value;

                // recursive call create bindings to cause building the context depth first
                String path = nameInNameSpace == null ? name : nameInNameSpace + "/" + name;

                Map nestedBindings = createBindings(path, nestedNode, factory);
                Context nestedContext = factory.createNestedSubcontext(path, nestedBindings);
                bindings.put(name, nestedContext);
            } else {
                bindings.put(name, value);
            }
        }
        return bindings;
    }


    /**
     * Do nothing subclass of hashmap used to differentiate between a Map in the tree an a nested element during tree building
     */
    public static final class Node extends HashMap {
    }

    public static Node buildMapTree(Map absoluteBindings) throws NamingException {
        Node rootContext = new Node();

        for (Iterator iterator = absoluteBindings.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();

            Node parentContext = rootContext;

            Name compoundName = ContextUtil.parseName(name);
            for (Enumeration parts = compoundName.getAll(); parts.hasMoreElements(); ) {
                String part = (String) parts.nextElement();
                // the last element in the path is the name of the value
                if (parts.hasMoreElements()) {
                    // nest node into parent
                    Node bindings = (Node) parentContext.get(part);
                    if (bindings == null) {
                        bindings = new Node();
                        parentContext.put(part, bindings);
                    }

                    parentContext = bindings;
                }
            }

            parentContext.put(compoundName.get(compoundName.size() - 1), value);
        }
        return rootContext;
    }
}
