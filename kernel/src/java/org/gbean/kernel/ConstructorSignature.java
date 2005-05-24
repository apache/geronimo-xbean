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

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @version $Rev: 109957 $ $Date: 2004-12-05 23:52:06 -0800 (Sun, 05 Dec 2004) $
 */
public final class ConstructorSignature {
    private final static String[] NO_TYPES = new String[0];
    private final String[] parameterTypes;

    public ConstructorSignature(Constructor constructor) {
        Class[] parameters = constructor.getParameterTypes();
        parameterTypes = new String[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            parameterTypes[i] = parameters[i].getName();
        }
    }

    public ConstructorSignature(String[] argumentTypes) {
        if (argumentTypes != null) {
            this.parameterTypes = argumentTypes;
        } else {
            this.parameterTypes = NO_TYPES;
        }
    }

    public ConstructorSignature(Class[] argumentTypes) {
        if (argumentTypes != null) {
            this.parameterTypes = new String[argumentTypes.length];
            for (int i = 0; i < argumentTypes.length; i++) {
                this.parameterTypes[i] = argumentTypes[i].getName();
            }
        } else {
            this.parameterTypes = NO_TYPES;
        }
    }

    public ConstructorSignature(List argumentTypes) {
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

    public List getParameterTypes() {
        return Collections.unmodifiableList(Arrays.asList(parameterTypes));
    }

    public boolean equals(Object object) {
        if (!(object instanceof ConstructorSignature)) {
            return false;
        }

        ConstructorSignature constructorSignature = (ConstructorSignature) object;

        // match arg length
        int length = constructorSignature.parameterTypes.length;
        if (length != parameterTypes.length) {
            return false;
        }

        // match each arg
        for (int i = 0; i < length; i++) {
            if (!constructorSignature.parameterTypes[i].equals(parameterTypes[i])) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int result = 17;
        for (int i = 0; i < parameterTypes.length; i++) {
            result = 37 * result + parameterTypes[i].hashCode();
        }
        return result;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer("<init>(");
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) {
                buffer.append(", ");
            }
            buffer.append(parameterTypes[i]);
        }
        return buffer.append(")").toString();
    }
}
