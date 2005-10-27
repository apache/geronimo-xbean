/**
 *
 * Copyright 2005 the original author or authors.
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
package org.xbean.spring.generator;

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
    public static final String XBEAN_ANNOTATION = "org.xbean.XBean";
    public static final String PROPERTY_ANNOTATION = "org.xbean.Property";

    private Utils() {
    }

    public static String decapitalise(String value) {
        if (value == null || value.length() == 0) {
            return value;
        }
        return value.substring(0, 1).toLowerCase() + value.substring(1);
    }

    public static boolean isSimpleType(AttributeMapping attributeMapping) {
        if (attributeMapping.isPrimitive()) {
            return true;
        }
        String type = attributeMapping.getType();
        if (type.equals("javax.xml.namespace.QName")) {
            return true;
        }
        if (attributeMapping.isArray()) {
            return false;
        }
        return hasPropertyEditor(type);
    }

    private static boolean hasPropertyEditor(String type) {
        Class theClass;
        try {
            theClass = loadClass(type);
        } catch (ClassNotFoundException e) {
            System.out.println("Warning, could not load class: " + type);
            return false;
        }
        // lets see if we can find a property editor for this type
        PropertyEditor editor = PropertyEditorManager.findEditor(theClass);
        return editor != null;
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

    public static String getXsdType(AttributeMapping attributeMapping) {
        String typeName = attributeMapping.getType();
        String xsdType = (String) XSD_TYPES.get(typeName);
        if (xsdType == null) {
            xsdType = "xsd:string";
        }
        return xsdType;
    }

    public static final Map XSD_TYPES;

    static {
        // TODO check these XSD types are right...
        Map map = new HashMap();
        map.put(String.class.getName(), "xs:string");
        map.put(Boolean.class.getName(), "xs:boolean");
        map.put(boolean.class.getName(), "xs:boolean");
        map.put(Byte.class.getName(), "xs:byte");
        map.put(byte.class.getName(), "xs:byte");
        map.put(Short.class.getName(), "xs:short");
        map.put(short.class.getName(), "xs:short");
        map.put(Integer.class.getName(), "xs:integer");
        map.put(int.class.getName(), "xs:integer");
        map.put(Long.class.getName(), "xs:long");
        map.put(long.class.getName(), "xs:long");
        map.put(Float.class.getName(), "xs:float");
        map.put(float.class.getName(), "xs:float");
        map.put(Double.class.getName(), "xs:double");
        map.put(double.class.getName(), "xs:double");
        map.put(java.util.Date.class.getName(), "xs:date");
        map.put(java.sql.Date.class.getName(), "xs:date");
        map.put("javax.xml.namespace.QName", "xs:QName");
        XSD_TYPES = Collections.unmodifiableMap(map);
    }
}
