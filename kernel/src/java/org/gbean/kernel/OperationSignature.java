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

package org.gbean.kernel;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;

/**
 * @version $Rev: 109957 $ $Date: 2004-12-05 23:52:06 -0800 (Sun, 05 Dec 2004) $
 */
public final class OperationSignature {
    private final static String[] NO_TYPES = new String[0];
    private final String name;
    private final String[] parameterTypes;

    public OperationSignature(Method method) {
        name = method.getName();
        Class[] parameters = method.getParameterTypes();
        parameterTypes = new String[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            parameterTypes[i] = parameters[i].getName();
        }
    }

    public OperationSignature(String name, String[] argumentTypes) {
        this.name = name;
        if (argumentTypes != null) {
            this.parameterTypes = argumentTypes;
        } else {
            this.parameterTypes = NO_TYPES;
        }
    }

    public OperationSignature(String name, Class[] argumentTypes) {
        this.name = name;
        if (argumentTypes != null) {
            this.parameterTypes = new String[argumentTypes.length];
            for (int i = 0; i < argumentTypes.length; i++) {
                this.parameterTypes[i] = argumentTypes[i].getName();
            }
        } else {
            this.parameterTypes = NO_TYPES;
        }
    }

    public OperationSignature(String name, List argumentTypes) {
        this.name = name;
        if (argumentTypes != null) {
            this.parameterTypes = new String[argumentTypes.size()];
            for (int i = 0; i < argumentTypes.size(); i++) {
                Object argumentType = argumentTypes.get(i);
                if (argumentType instanceof Class) {
                    this.parameterTypes[i] = ((Class) argumentType).getName();
                } else if (argumentType instanceof String) {
                    this.parameterTypes[i] = (String) argumentType;
                } else {
                    throw new IllegalArgumentException("Argument type must be a String or a Class: index=" + i + ", type=" + argumentType.getClass());
                }
            }
        } else {
            this.parameterTypes = NO_TYPES;
        }
    }

    public String getName() {
        return name;
    }

    public List getParameterTypes() {
        return Collections.unmodifiableList(Arrays.asList(parameterTypes));
    }

    public boolean equals(Object object) {
        if (!(object instanceof OperationSignature)) {
            return false;
        }

        // match names
        OperationSignature methodKey = (OperationSignature) object;
        if (!methodKey.name.equals(name)) {
            return false;
        }

        // match arg length
        int length = methodKey.parameterTypes.length;
        if (length != parameterTypes.length) {
            return false;
        }

        // match each arg
        for (int i = 0; i < length; i++) {
            if (!methodKey.parameterTypes[i].equals(parameterTypes[i])) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int result = 17;
        result = 37 * result + name.hashCode();
        for (int i = 0; i < parameterTypes.length; i++) {
            result = 37 * result + parameterTypes[i].hashCode();
        }
        return result;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer(name).append("(");
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) {
                buffer.append(", ");
            }
            buffer.append(parameterTypes[i]);
        }
        return buffer.append(")").toString();
    }
}
