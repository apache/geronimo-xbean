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
package org.gbean.metadata;

import java.util.Map;
import java.util.List;
import java.lang.reflect.Method;

import org.gbean.kernel.OperationSignature;

/**
 * @version $Revision$ $Date$
 */
public interface MethodMetadata {
    OperationSignature getSignature();

    Method getMethod();

    List getParameters();

    ParameterMetadata getParameter(int index);

    Map getProperties();

    Object get(Object key);

    Object put(Object key, Object value);

    Object remove(Object key);
}
