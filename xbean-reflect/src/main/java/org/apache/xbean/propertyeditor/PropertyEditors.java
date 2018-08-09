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

import java.beans.PropertyEditorManager;
import java.lang.reflect.Type;

import com.sun.org.apache.regexp.internal.RE;

/**
 * The property editor manager.  This orchestrates Geronimo usage of
 * property editors, allowing additional search paths to be added and
 * specific editors to be registered.
 *
 * @version $Rev: 6687 $
 */
@Deprecated // this is all static and leaks, use PropertyEditorRegistry
public class PropertyEditors {
    private static boolean registerWithVM;
    private static final PropertyEditorRegistry REGISTRY = new PropertyEditorRegistry() {
        {
            registerDefaults();
        }

        @Override
        public Converter register(final Converter converter) {
            final Converter register = super.register(converter);
            if (registerWithVM) {
                PropertyEditorManager.registerEditor(converter.getType(), converter.getClass());
                final Class<?> sibling = Primitives.findSibling(converter.getType());
                if (sibling != null) {
                    PropertyEditorManager.registerEditor(sibling, converter.getClass());
                }
            }
            return register;
        }
    };

    /**
     * Are converters registered with the VM PropertyEditorManager.  By default
     * converters are not registered with the VM as this creates problems for
     * IDE and Spring because they rely in their specific converters being
     * registered to function properly. 
     */
    public static boolean isRegisterWithVM() {
        return registerWithVM;
    }

    /**
     * Sets if converters registered with the VM PropertyEditorManager.
     * If the new value is true, all currently registered converters are
     * immediately registered with the VM.
     */
    public static void setRegisterWithVM(boolean registerWithVM) {
        if (PropertyEditors.registerWithVM != registerWithVM) {
            PropertyEditors.registerWithVM = registerWithVM;
            if (registerWithVM) {
                for (final Converter converter : REGISTRY.getRegistry().values()) {
                    PropertyEditorManager.registerEditor(converter.getType(), converter.getClass());
                }
            }
        }
    }

    public static void registerConverter(Converter converter) {
        REGISTRY.register(converter);
    }

    public static boolean canConvert(final String type, final ClassLoader classLoader) {
        if (type == null) {
            throw new NullPointerException("type is null");
        }
        if (classLoader == null) {
            throw new NullPointerException("classLoader is null");
        }

        try {
            return REGISTRY.findConverter(Class.forName(type, true, classLoader)) != null;
        } catch (ClassNotFoundException e) {
            throw new PropertyEditorException("Type class could not be found: " + type);
        }
    }

    public static boolean canConvert(final Class<?> type) {
        return REGISTRY.findConverter(type) != null;
    }

    public static String toString(final Object value) throws PropertyEditorException {
        return REGISTRY.toString(value);
    }

    public static Object getValue(final String type, final String value, final ClassLoader classLoader) throws PropertyEditorException {
        return REGISTRY.getValue(type, value, classLoader);

    }

    public static Object getValue(final Type type, final String value) throws PropertyEditorException {
        return REGISTRY.getValue(type, value);
    }

    public static PropertyEditorRegistry registry() {
        return REGISTRY;
    }
}
