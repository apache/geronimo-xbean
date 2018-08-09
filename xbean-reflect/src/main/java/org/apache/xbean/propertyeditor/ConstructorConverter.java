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

import java.lang.reflect.Constructor;

public class ConstructorConverter extends AbstractConverter {
    private final Constructor constructor;

    public ConstructorConverter(final Class type, final Constructor constructor) {
        super(type);
        this.constructor = constructor;
    }

    @Override
    protected Object toObjectImpl(final String text) {
        try {
            return constructor.newInstance(text);
        } catch (final Exception e) {
            final String message = String.format("Cannot convert string '%s' to %s.", text, super.getType());
            throw new PropertyEditorException(message, e);
        }
    }

    public static ConstructorConverter editor(final Class type) {
        try {
            final Constructor<?> constructor = type.getConstructor(String.class);
            return new ConstructorConverter(type, constructor);
        } catch (final NoSuchMethodException e) {
            // fine
        }
        return null;
    }
}
