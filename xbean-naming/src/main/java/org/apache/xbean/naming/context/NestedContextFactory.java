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
import javax.naming.NamingException;
import java.util.Map;

/**
 * @version $Rev$ $Date$
 */
public interface NestedContextFactory {
    /**
     * Is the specified value an instance of a nested context
     * @param value the value to inspect
     * @return true if the specified value an instance of a nested context; false otherwise
     */
    boolean isNestedSubcontext(Object value);

    /**
     * Creates a nested subcontext instance.  This does not cause the nested context to be bound.
     * @param path the path to the new nested context
     * @param bindings the initial bindings for the context
     * @return the new nested context
     * @throws javax.naming.NamingException on error
     */
    Context createNestedSubcontext(String path, Map<String, Object> bindings) throws NamingException;
}
