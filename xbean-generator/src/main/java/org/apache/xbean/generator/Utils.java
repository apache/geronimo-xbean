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
package org.apache.xbean.generator;

import org.apache.xbean.model.Type;

import javax.xml.namespace.QName;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public final class Utils {
    public static final String XBEAN_ANNOTATION = "org.apache.xbean.XBean";
    public static final String PROPERTY_ANNOTATION = "org.apache.xbean.Property";

    private Utils() {
    }

    public static String decapitalise(String value) {
        if (value == null || value.length() == 0) {
            return value;
        }
        return value.substring(0, 1).toLowerCase() + value.substring(1);
    }

    public static boolean isSimpleType(Type type) {
        if (type.isPrimitive()) {
            return true;
        }
        if (type.isCollection()) {
            return false;
        }

        String name = type.getName();
        if (name.equals("java.lang.Class") ||
                name.equals("javax.xml.namespace.QName")) {
            return true;
        }
        return hasPropertyEditor(name);
    }

    private static boolean hasPropertyEditor(String type) {
        Class theClass;
        try {
            theClass = loadClass(type);
            // lets see if we can find a property editor for this type
            PropertyEditor editor = PropertyEditorManager.findEditor(theClass);
            return editor != null;
        } catch (Throwable e) {
            System.out.println("Warning, could not load class: " + type + ": " + e);
            return false;
        }
    }

    /**
     * Attempts to load the class on the current thread context class loader or
     * the class loader which loaded us
     */
    private static Class loadClass(String name) throws ClassNotFoundException {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            try {
                return contextClassLoader.loadClass(name);
            } catch (ClassNotFoundException e) {
            }
        }
        return Utils.class.getClassLoader().loadClass(name);
    }

    public static QName getXsdType(Type type) {
        String name = type.getName();
        QName xsdType = XSD_TYPES.get(name);
        if (xsdType == null) {
            xsdType = XSD_TYPES.get(String.class.getName());
        }
        return xsdType;
    }

    public static final Map<String, QName> XSD_TYPES;

    static {
        // TODO check these XSD types are right...
        String namespace = "http://www.w3.org/2001/XMLSchema";
        Map<String, QName> map = new HashMap<>();
        map.put(String.class.getName(), new QName(namespace, ":string"));
        map.put(Boolean.class.getName(), new QName(namespace, "boolean"));
        map.put(boolean.class.getName(), new QName(namespace, "boolean"));
        map.put(Byte.class.getName(), new QName(namespace, "byte"));
        map.put(byte.class.getName(), new QName(namespace, "byte"));
        map.put(Short.class.getName(), new QName(namespace, "short"));
        map.put(short.class.getName(), new QName(namespace, "short"));
        map.put(Integer.class.getName(), new QName(namespace, "integer"));
        map.put(int.class.getName(), new QName(namespace, "integer"));
        map.put(Long.class.getName(), new QName(namespace, "long"));
        map.put(long.class.getName(), new QName(namespace, "long"));
        map.put(Float.class.getName(), new QName(namespace, "float"));
        map.put(float.class.getName(), new QName(namespace, "float"));
        map.put(Double.class.getName(), new QName(namespace, "double"));
        map.put(double.class.getName(), new QName(namespace, "double"));
        map.put(java.util.Date.class.getName(), new QName(namespace, "date"));
        map.put(java.sql.Date.class.getName(), new QName(namespace, "date"));
        map.put(QName.class.getName(), new QName(namespace, "QName"));
        XSD_TYPES = Collections.unmodifiableMap(map);
    }

}
