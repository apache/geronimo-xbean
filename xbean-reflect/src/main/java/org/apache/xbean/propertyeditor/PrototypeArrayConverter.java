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
import java.lang.reflect.Array;
import java.util.List;
import java.util.ListIterator;

/**
 * Adapter for editing array types.
 *
 * @version $Rev: 6687 $ $Date: 2005-12-28T21:08:56.733437Z $
 */
public final class PrototypeArrayConverter extends AbstractCollectionConverter {
    private static final PropertyEditor MOCK_EDITOR = new StringEditor();

    public PrototypeArrayConverter(Class type) {
        super(type, MOCK_EDITOR);
        if (!type.isArray()) {
            throw new IllegalArgumentException("type is not an array " + type.getSimpleName());
        }
        if (type.getComponentType().isArray()) {
            throw new IllegalArgumentException("type is a multi-dimensional array " + type.getSimpleName());
        }
    }

    @Override
    protected PropertyEditor getEditor() {
        return PropertyEditorManager.findEditor(getType());
    }

    protected Object createCollection(final List list) {
        final Object array = Array.newInstance(getType().getComponentType(), list.size());
        for (final ListIterator iterator = list.listIterator(); iterator.hasNext();) {
            final Object item = iterator.next();
            int index = iterator.previousIndex();
            Array.set(array, index, item);
        }
        return array;
    }
}
