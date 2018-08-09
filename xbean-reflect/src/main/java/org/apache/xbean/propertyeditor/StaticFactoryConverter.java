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
package org.apache.xbean.propertyeditor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Of the javax and java packages in the Java 8 JVM, there are roughly
 * 10 static factory patterns in use.
 *
 * Here they are listed in the order they are preferred by this library
 *
 *   64 valueOf
 *    7 new
 *    6 decode
 *    5 for
 *    4 of
 *    1 parse
 *    1 from
 *    1 create
 *    1 compile
 *   40 get
 *
 * Though get* has the second most usage in the JVM, it is also the least
 * consistent and in classes that have multiple factories, it is the least
 * preferred.
 *
 * For each of these prefixes there is a sub order of preference, using
 * "create" as an example, this is the preferred usage:
 *
 *  - create
 *  - create<Type>
 *  - create*
 *
 */
public class StaticFactoryConverter extends AbstractConverter {

    private final Method method;

    public StaticFactoryConverter(final Class type, final Method method) {
        super(type);
        this.method = method;
    }

    @Override
    protected Object toObjectImpl(final String text) {
        try {
            return method.invoke(null, text);
        } catch (final Exception e) {
            final String message = String.format("Cannot convert string '%s' to %s.", text, super.getType());
            throw new PropertyEditorException(message, e);
        }
    }

    public static StaticFactoryConverter editor(final Class type) {
        final List<Method> candidates = getCandidates(type);

        if (candidates.size() == 0) return null;

        final Method method = select(candidates);

        return new StaticFactoryConverter(type, method);
    }

    static List<Method> getCandidates(final Class type) {
        final List<Method> candidates = new ArrayList<Method>();

        for (final Method method : type.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) continue;
            if (!Modifier.isPublic(method.getModifiers())) continue;
            if (!method.getReturnType().equals(type)) continue;
            if (method.getParameterTypes().length != 1) continue;
            if (!method.getParameterTypes()[0].equals(String.class)) continue;

            candidates.add(method);
        }

        return candidates;
    }

    /**
     * We want the selection to be stable and not dependent on
     * VM reflection ordering.
     */
    static Method select(final List<Method> candidates) {
        sort(candidates);

        return candidates.get(0);
    }

    static void sort(final List<Method> candidates) {
        Collections.sort(candidates, new Comparator<Method>() {
            public int compare(final Method a, final Method b) {
                int av = grade(a);
                int bv = grade(b);
                return (a.getName().compareTo(b.getName()) + (av - bv));
            }
        });
    }

    private static int grade(final Method a) {
        final String type = a.getReturnType().getSimpleName();
        final String name = a.getName();

        // valueOf beats all
        if (name.equals("valueOf"))        return -990000;
        if (name.equals("valueOf" + type)) return -980000;
        if (name.startsWith("valueOf"))    return -970000;

        // new*
        if (name.equals("new" + type))    return -890000;
        if (name.equals("newInstance"))   return -880000;
        if (name.startsWith("new"))       return -870000;

        // decode*
        if (name.equals("decode"))        return -790000;
        if (name.equals("decode" + type)) return -780000;
        if (name.startsWith("decode"))    return -770000;

        // for*
        if (name.equals("for" + type))    return -690000;
        if (name.startsWith("for"))       return -680000;

        // of*
        if (name.equals("of"))            return -590000;
        if (name.equals("of" + type))     return -580000;
        if (name.startsWith("of"))        return -570000;

        // parse*
        if (name.equals("parse"))         return -490000;
        if (name.equals("parse" + type))  return -480000;
        if (name.startsWith("parse"))     return -470000;

        // from*
        if (name.equals("from"))          return -390000;
        if (name.equals("fromString"))    return -380000;
        if (name.startsWith("from"))      return -370000;

        // create*
        if (name.equals("create"))        return -290000;
        if (name.equals("create" + type)) return -280000;
        if (name.startsWith("create"))    return -270000;

        // compile*
        if (name.equals("compile"))       return -190000;
        if (name.equals("compile" + type))return -180000;
        if (name.startsWith("compile"))   return -170000;

        // get*
        if (name.equals("get"))           return 1000;
        if (name.equals("get" + type))    return 1200;
        if (name.equals("getInstance"))   return 1200;
        if (name.startsWith("get"))       return 1300;

        return 0;
    }
}
