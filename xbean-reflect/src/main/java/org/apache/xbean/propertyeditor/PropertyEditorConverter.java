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

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;

public class PropertyEditorConverter extends AbstractConverter {
    public PropertyEditorConverter(final Class<?> type) {
        super(type);
    }

    public String toStringImpl(final Object value) throws PropertyEditorException {
        final PropertyEditor editor = PropertyEditorManager.findEditor(getType());
        editor.setValue(value);
        try {
            return editor.getAsText();
        } catch (final Exception e) {
            throw new PropertyEditorException("Error while converting a \"" + getType().getSimpleName() + "\" to text " +
                    " using the property editor " + editor.getClass().getSimpleName(), e);
        }
    }

    public Object toObjectImpl(final String text) throws PropertyEditorException {
        final PropertyEditor editor = PropertyEditorManager.findEditor(getType());
        editor.setAsText(text);
        try {
            return editor.getValue();
        } catch (final Exception e) {
            throw new PropertyEditorException("Error while converting \"" + text + "\" to a " + getType().getSimpleName() +
                    " using the property editor " + editor.getClass().getSimpleName(), e);
        }
    }
}
