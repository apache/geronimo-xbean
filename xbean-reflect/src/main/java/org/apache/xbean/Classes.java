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
package org.apache.xbean;

import java.lang.reflect.Array;
import java.util.HashMap;

/**
 * Utility for printing class names
 *
 * @version $Rev: 6685 $
 */
public class Classes {

    /**
     * The returns the name of the class object in
     * method signature format.
     *
     * @param type The class object we convert into name form.
     * @return A string representation of the class name, in method signature
     *         format.
     */
    public static String getClassName(Class type) {
        return getClassName(type, false);
    }

    public static String getClassName(Object instance) {
        return getClassName(instance, false);
    }

    public static String getClassName(Object instance, boolean pretty) {
        if (instance == null) {
            return "null";
        }
        return getClassName(instance.getClass(), pretty);
    }

    public static String getClassName(Class type, boolean pretty) {
        StringBuffer name = new StringBuffer();

        // we test these in reverse order from the resolution steps,
        // first handling arrays, then primitive types, and finally
        // "normal" class objects.

        // First handle arrays.  If a class is an array, the type is
        // element stored at that level.  So, for a 2-dimensional array
        // of ints, the top-level type will be "[I".  We need to loop
        // down the hierarchy until we hit a non-array type.
        while (type.isArray()) {
            // add another array indicator at the front of the name,
            // and continue with the next type.
            name.append('[');
            if (pretty) name.append(']');
            type = type.getComponentType();
        }

        // we're down to the base type.  If this is a primitive, then
        // we poke in the single-character type specifier.
        if (type.isPrimitive()) {
            if (pretty) {
                name.insert(0, type.getName());
            } else {
                if (type.equals((boolean.class))) name.append('Z');
                else if (type.equals((byte.class))) name.append('B');
                else if (type.equals((char.class))) name.append('C');
                else if (type.equals((short.class))) name.append('S');
                else if (type.equals((int.class))) name.append('I');
                else if (type.equals((long.class))) name.append('J');
                else if (type.equals((float.class))) name.append('F');
                else if (type.equals((double.class))) name.append('D');
                else if (type.equals((void.class))) name.append('V');
            }
        }
        // a "normal" class.  This gets expressing using the "Lmy.class.name;" syntax.
        else {
            if (pretty) {
                name.insert(0, type.getName());
            } else {
                name.append('L');
                name.append(type.getName());
                name.append(';');
            }
        }
        return name.toString();
    }
}

