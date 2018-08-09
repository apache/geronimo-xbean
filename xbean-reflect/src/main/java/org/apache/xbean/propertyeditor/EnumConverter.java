/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.xbean.propertyeditor;

import java.lang.reflect.Method;

/**
 * @version $Rev$ $Date$
 */
public class EnumConverter extends AbstractConverter {

    public EnumConverter(Class type) {
        super(type);
    }

    @Override
    protected String toStringImpl(final Object value) {
        return Enum.class.cast(value).name();
    }

    @Override
    protected Object toObjectImpl(String text) {
        Class type = getType();

        try {
            return Enum.valueOf(type, text);
        } catch (Exception cause) {
            try {
                final int index = Integer.parseInt(text);
                final Method method = type.getMethod("values");
                final Object[] values = (Object[]) method.invoke(null);
                return values[index];
            } catch (final NumberFormatException e) {
                // no-op
            } catch (final Exception e) {
                cause = e;
            }

            throw new PropertyEditorException("Value \"" + text + "\" cannot be converted to enum type " + type.getName(), cause);
        }
    }
}
