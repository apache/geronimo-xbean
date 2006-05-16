/**
 *
 * Copyright 2005 The Apache Software Foundation
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
package org.apache.xbean.recipe;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;

import org.apache.xbean.ClassLoading;

/**
 * @version $Rev$ $Date$
 */
class CloseMatchException extends ConstructionException {
    private final int level;
    private final Method method;
    private final Constructor constructor;

    public static CloseMatchException greater(CloseMatchException m1, CloseMatchException m2) {
        if (m1 == null) return m2;
        if (m2 == null) return m1;

        if (m1.level < m2.level) {
            return m2;
        } else {
            return m1;
        }
    }

    public static CloseMatchException setterNoParameters(Method method) {
        return new CloseMatchException(1,
                "Setter takes no parameters",
                method);
    }

    public static CloseMatchException setterMultipleParameters(Method method) {
        return new CloseMatchException(1,
                "Setter takes more then one parameter",
                method);
    }

    public static CloseMatchException setterWithReturn(Method method) {
        return new CloseMatchException(2,
                "Setter returns a value",
                method);
    }

    public static CloseMatchException setterIsAbstract(Method method) {
        return new CloseMatchException(3,
                "Setter is abstract",
                method);
    }

    public static CloseMatchException setterIsNotPublic(Method method) {
        return new CloseMatchException(4,
                "Setter is not publict",
                method);
    }

    public static CloseMatchException setterIsStatic(Method method) {
        return new CloseMatchException(4,
                "Setter is static",
                method);
    }

    public static CloseMatchException typeMismatch(Method method, Object propertyValue) {
        Class methodParameterType = method.getParameterTypes()[0];
        return new CloseMatchException(5,
                ClassLoading.getClassName(propertyValue, true) + " can not be assigned or converted to " +
                    ClassLoading.getClassName(methodParameterType, true),
                method);
    }

    public static CloseMatchException assignNullToPrimitive(Method method) {
        Class methodParameterType = method.getParameterTypes()[0];
        return new CloseMatchException(6,
                "Null can not be assigned to primitive type " + ClassLoading.getClassName(methodParameterType, true),
                method);
    }

    public static CloseMatchException factoryMethodIsNotPublic(Method method) {
        return new CloseMatchException(20,
                "Factory method is not public",
                method);
    }

    public static CloseMatchException factoryMethodIsNotStatic(Method method) {
        return new CloseMatchException(20,
                "Factory method is not static",
                method);
    }

    public static CloseMatchException factoryMethodWithNoReturn(Method method) {
        return new CloseMatchException(20,
                "Factory method does not return anything",
                method);
    }

    public static CloseMatchException factoryMethodReturnsPrimitive(Method method) {
        return new CloseMatchException(20,
                "Factory method returns a primitive type",
                method);
    }

    public static CloseMatchException typeMismatch(Constructor consturctor, String[] parameterNames, Class[] propertyTypes) {
        Class[] parameterTypes = consturctor.getParameterTypes();
        String message = createTypeMismatchMessage(parameterTypes, propertyTypes, parameterNames);

        return new CloseMatchException(10,
                message.toString(),
                consturctor);
    }

    public static CloseMatchException typeMismatch(Method method, String[] parameterNames, Class[] propertyTypes) {
        Class[] parameterTypes = method.getParameterTypes();
        String message = createTypeMismatchMessage(parameterTypes, propertyTypes, parameterNames);

        return new CloseMatchException(10,
                message.toString(),
                method);
    }

    private static String createTypeMismatchMessage(Class[] expectedTypes, Class[] actualTypes, String[] parameterNames) {
        List badParameters = new ArrayList();
        for (int i = 0; i < expectedTypes.length; i++) {
            Class parameterType = expectedTypes[i];
            if (!ObjectRecipe.isAssignableFrom(parameterType,  actualTypes[i])) {
                badParameters.add(parameterNames[i] + " (" + i + ") to " + ClassLoading.getClassName(parameterType, true));
            }
        }

        StringBuffer message = new StringBuffer();
        if (badParameters.size() == 1) {
            message.append("Unable to covert parameter ");
            message.append(badParameters.get(0));
        } else {
            message.append("Unable to covert parameters ");
            for (ListIterator iterator = badParameters.listIterator(); iterator.hasNext();) {
                String s = (String) iterator.next();
                int index = iterator.previousIndex();
                if (index == badParameters.size() - 1) {
                    message.append(" and ");
                }
                if (index > 0) {
                    message.append(", ");
                }
                message.append(s);
            }
        }
        return message.toString();
    }

    public CloseMatchException(int level, String message, Method method) {
        super(message);
        this.level = level;
        this.method = method;
        this.constructor = null;
    }

    public CloseMatchException(int level, String message, Constructor constructor) {
        super(message);
        this.level = level;
        this.method = null;
        this.constructor = constructor;
    }

    public int getLevel() {
        return level;
    }

    public Method getMethod() {
        return method;
    }

    public Constructor getConstructor() {
        return constructor;
    }
}
