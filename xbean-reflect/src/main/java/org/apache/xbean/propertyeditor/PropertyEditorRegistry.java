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

import static java.util.Collections.unmodifiableMap;
import static org.apache.xbean.recipe.RecipeHelper.getTypeParameters;
import static org.apache.xbean.recipe.RecipeHelper.toClass;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.io.Closeable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.xbean.recipe.RecipeHelper;

public class PropertyEditorRegistry implements Closeable {
    private final ConcurrentMap<Type, Converter> registry = new ConcurrentHashMap<Type, Converter>();

    public PropertyEditorRegistry registerDefaults() {
        register(new ArrayListEditor());
        register(new BigDecimalEditor());
        register(new BigIntegerEditor());
        register(new BooleanEditor());
        register(new ByteEditor());
        register(new CharacterEditor());
        register(new ClassEditor());
        register(new DateEditor());
        register(new DoubleEditor());
        register(new FileEditor());
        register(new FloatEditor());
        register(new HashMapEditor());
        register(new HashtableEditor());
        register(new IdentityHashMapEditor());
        register(new Inet4AddressEditor());
        register(new Inet6AddressEditor());
        register(new InetAddressEditor());
        register(new IntegerEditor());
        register(new LinkedHashMapEditor());
        register(new LinkedHashSetEditor());
        register(new LinkedListEditor());
        register(new ListEditor());
        register(new LongEditor());
        register(new MapEditor());
        register(new ObjectNameEditor());
        register(new PropertiesEditor());
        register(new SetEditor());
        register(new ShortEditor());
        register(new SortedMapEditor());
        register(new SortedSetEditor());
        register(new StringEditor());
        register(new TreeMapEditor());
        register(new TreeSetEditor());
        register(new URIEditor());
        register(new URLEditor());
        register(new LoggerConverter());
        register(new PatternConverter());
        register(new JndiConverter());
        register(new VectorEditor());
        register(new WeakHashMapEditor());

        try {
            register(new Log4jConverter());
        } catch (final Throwable e) {
            // no-op
        }

        try {
            register(new CommonsLoggingConverter());
        } catch (final Throwable e) {
            // no-op
        }

        return this;
    }

    /**
     * @return a read-only view of the converters.
     */
    public Map<Type, Converter> getRegistry() {
        return unmodifiableMap(registry);
    }

    /**
     * Register a converter in the registry.
     *
     * @param converter the converter to register.
     * @return the previously existing converter for the corresponding type or null.
     */
    public Converter register(final Converter converter) {
        if (converter == null) {
            throw new NullPointerException("converter is null");
        }
        final Class<?> type = converter.getType();
        final Converter existing = registry.put(type, converter);

        final Class<?> sibling = Primitives.findSibling(type);
        if (sibling != null) {
            registry.put(sibling, converter);
        }
        return existing;
    }

    /**
     * Unregister a converter.
     *
     * @param converter the converter to remove from the registry.
     * @return the converter if found, or null.
     */
    public Converter unregister(final Converter converter) {
        if (converter == null) {
            throw new NullPointerException("converter is null");
        }
        return registry.remove(converter.getType());
    }

    public Converter findConverter(final Type type){
        {
            final Converter converter = findInternalConverter(type);
            if (converter != null) {
                if (!registry.containsKey(converter.getType())) {
                    register(converter);
                }
                return converter;
            }
        }

        {
            final Converter converter = createConverterFromEditor(type);
            if (converter != null) {
                register(converter);
                return converter;
            }
        }

        {
            final Converter converter = findStructuralConverter(type);
            if (converter != null) {
                register(converter);
                return converter;
            }
        }

        return null;
    }

    public String toString(final Object value) throws PropertyEditorException {
        if (value == null) {
            throw new NullPointerException("value is null");
        }
        final Class type = unwrapClass(value);
        final Converter converter = findConverter(type);
        if (converter == null) {
            throw new PropertyEditorException("Unable to find PropertyEditor for " + type.getSimpleName());
        }
        return converter.toString(value);
    }

    public Object getValue(final String type, final String value, final ClassLoader classLoader) throws PropertyEditorException {
        if (type == null) {
            throw new NullPointerException("type is null");
        }
        if (value == null) {
            throw new NullPointerException("value is null");
        }
        if (classLoader == null) {
            throw new NullPointerException("classLoader is null");
        }

        try {
            return getValue(Class.forName(type, true, classLoader), value);
        } catch (final ClassNotFoundException e) {
            throw new PropertyEditorException("Type class could not be found: " + type);
        }
    }

    public Object getValue(final Type type, final String value) throws PropertyEditorException {
        if (type == null) {
            throw new NullPointerException("type is null");
        }
        if (value == null) {
            throw new NullPointerException("value is null");
        }

        final Converter converter = findConverter(type);
        if (converter != null) {
            return converter.toObject(value);
        }

        final Class clazz = toClass(type);

        final Converter structuralConverter = findStructuralConverter(clazz);
        if (structuralConverter != null) {
            register(structuralConverter);
            return structuralConverter.toObject(value);
        }

        throw new PropertyEditorException("Unable to find PropertyEditor for " + clazz.getSimpleName());
    }

    protected Class<?> unwrapClass(final Object value) {
        Class<?> aClass = value.getClass();
        while (aClass.getName().contains("$$")) {
            aClass = aClass.getSuperclass();
            if (aClass == null || aClass == Object.class) {
                return value.getClass();
            }
        }
        return aClass;
    }

    protected Converter findStructuralConverter(final Type type) {
        if (type == null) throw new NullPointerException("type is null");

        final Class clazz = toClass(type);

        if (Enum.class.isAssignableFrom(clazz)){
            return new EnumConverter(clazz);
        }

        {
            final ConstructorConverter editor = ConstructorConverter.editor(clazz);
            if (editor != null) {
                return editor;
            }
        }

        {
            final StaticFactoryConverter editor = StaticFactoryConverter.editor(clazz);
            if (editor != null) {
                return editor;
            }
        }

        return null;
    }

    protected Converter createConverterFromEditor(final Type type) {
        if (type == null) {
            throw new NullPointerException("type is null");
        }

        final Class<?> clazz = toClass(type);

        // try to locate this directly from the editor manager first.
        final PropertyEditor editor = PropertyEditorManager.findEditor(clazz);

        // we're outta here if we got one.
        if (editor != null) {
            return new PropertyEditorConverter(clazz);
        }


        // it's possible this was a request for an array class.  We might not
        // recognize the array type directly, but the component type might be
        // resolvable
        if (clazz.isArray() && !clazz.getComponentType().isArray()) {
            // do a recursive lookup on the base type
            final PropertyEditor arrayEditor = findEditor(clazz.getComponentType());
            // if we found a suitable editor for the base component type,
            // wrapper this in an array adaptor for real use
            if (findEditor(clazz.getComponentType()) != null) {
                return new ArrayConverter(clazz, arrayEditor);
            }
        }

        return null;
    }

    protected Converter findInternalConverter(final Type type) {
        if (type == null) {
            throw new NullPointerException("type is null");
        }

        final Class clazz = toClass(type);

        // it's possible this was a request for an array class.  We might not
        // recognize the array type directly, but the component type might be
        // resolvable
        if (clazz.isArray() && !clazz.getComponentType().isArray()) {
            // do a recursive lookup on the base type
            PropertyEditor editor = findConverter(clazz.getComponentType());
            // if we found a suitable editor for the base component type,
            // wrapper this in an array adaptor for real use
            if (editor != null) {
                return new ArrayConverter(clazz, editor);
            }
            return null;
        }

        if (Collection.class.isAssignableFrom(clazz)){
            Type[] types = getTypeParameters(Collection.class, type);

            Type componentType = String.class;
            if (types != null && types.length == 1 && types[0] instanceof Class) {
                componentType = types[0];
            }

            PropertyEditor editor = findConverter(componentType);

            if (editor != null){
                if (RecipeHelper.hasDefaultConstructor(clazz)) {
                    return new GenericCollectionConverter(clazz, editor);
                } else if (SortedSet.class.isAssignableFrom(clazz)) {
                    return new GenericCollectionConverter(TreeSet.class, editor);
                } else if (Set.class.isAssignableFrom(clazz)) {
                    return new GenericCollectionConverter(LinkedHashSet.class, editor);
                }
                return new GenericCollectionConverter(ArrayList.class, editor);
            }

            return null;
        }

        if (Map.class.isAssignableFrom(clazz)){
            Type[] types = getTypeParameters(Map.class, type);

            Type keyType = String.class;
            Type valueType = String.class;
            if (types != null && types.length == 2 && types[0] instanceof Class && types[1] instanceof Class) {
                keyType = types[0];
                valueType = types[1];
            }

            final Converter keyConverter = findConverter(keyType);
            final Converter valueConverter = findConverter(valueType);

            if (keyConverter != null && valueConverter != null){
                if (RecipeHelper.hasDefaultConstructor(clazz)) {
                    return new GenericMapConverter(clazz, keyConverter, valueConverter);
                } else if (SortedMap.class.isAssignableFrom(clazz)) {
                    return new GenericMapConverter(TreeMap.class, keyConverter, valueConverter);
                } else if (ConcurrentMap.class.isAssignableFrom(clazz)) {
                    return new GenericMapConverter(ConcurrentHashMap.class, keyConverter, valueConverter);
                }
                return new GenericMapConverter(LinkedHashMap.class, keyConverter, valueConverter);
            }

            return null;
        }

        Converter converter = registry.get(clazz);

        // we're outta here if we got one.
        if (converter != null) {
            return converter;
        }

        final Class[] declaredClasses = clazz.getDeclaredClasses();
        for (final Class<?> declaredClass : declaredClasses) {
            if (Converter.class.isAssignableFrom(declaredClass)) {
                try {
                    converter = (Converter) declaredClass.newInstance();
                    register(converter);

                    // try to get the converter from the registry... the converter
                    // created above may have been for another class
                    converter = registry.get(clazz);
                    if (converter != null) {
                        return converter;
                    }
                } catch (Exception e) {
                    // no-op
                }

            }
        }

        // nothing found
        return null;
    }

    /**
     * Locate a property editor for qiven class of object.
     *
     * @param type The target object class of the property.
     * @return The resolved editor, if any.  Returns null if a suitable editor
     *         could not be located.
     */
    protected PropertyEditor findEditor(final Type type) {
        if (type == null) throw new NullPointerException("type is null");

        Class clazz = toClass(type);

        // try to locate this directly from the editor manager first.
        PropertyEditor editor = PropertyEditorManager.findEditor(clazz);

        // we're outta here if we got one.
        if (editor != null) {
            return editor;
        }


        // it's possible this was a request for an array class.  We might not
        // recognize the array type directly, but the component type might be
        // resolvable
        if (clazz.isArray() && !clazz.getComponentType().isArray()) {
            // do a recursive lookup on the base type
            editor = findEditor(clazz.getComponentType());
            // if we found a suitable editor for the base component type,
            // wrapper this in an array adaptor for real use
            if (editor != null) {
                return new ArrayConverter(clazz, editor);
            }
        }

        // nothing found
        return null;
    }

    /**
     * Release closeable converters.
     */
    public void close() {
        for (final Converter converter : registry.values()) {
            if (Closeable.class.isInstance(converter)) {
                Closeable.class.cast(converter);
            }
        }
        registry.clear();
    }
}
