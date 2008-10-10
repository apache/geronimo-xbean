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

import static org.apache.xbean.recipe.RecipeHelper.getTypeParameters;
import static org.apache.xbean.recipe.RecipeHelper.*;
import org.apache.xbean.recipe.RecipeHelper;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.SortedSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Type;

/**
 * The property editor manager.  This orchestrates Geronimo usage of
 * property editors, allowing additional search paths to be added and
 * specific editors to be registered.
 *
 * @version $Rev: 6687 $
 */
public class PropertyEditors {
    private static final Map<Class, Converter> registry = Collections.synchronizedMap(new ReferenceIdentityMap());
    private static final Map<Class, Class> PRIMITIVE_TO_WRAPPER;
    private static final Map<Class, Class> WRAPPER_TO_PRIMITIVE;
    private static boolean registerWithVM;

    /**
     * Register all of the built in converters
     */
    static {
        Map<Class, Class> map = new HashMap<Class, Class>();
        map.put(boolean.class, Boolean.class);
        map.put(char.class, Character.class);
        map.put(byte.class, Byte.class);
        map.put(short.class, Short.class);
        map.put(int.class, Integer.class);
        map.put(long.class, Long.class);
        map.put(float.class, Float.class);
        map.put(double.class, Double.class);
        PRIMITIVE_TO_WRAPPER = Collections.unmodifiableMap(map);


        map = new HashMap<Class, Class>();
        map.put(Boolean.class, boolean.class);
        map.put(Character.class, char.class);
        map.put(Byte.class, byte.class);
        map.put(Short.class, short.class);
        map.put(Integer.class, int.class);
        map.put(Long.class, long.class);
        map.put(Float.class, float.class);
        map.put(Double.class, double.class);
        WRAPPER_TO_PRIMITIVE = Collections.unmodifiableMap(map);

        // Explicitly register the types
        registerConverter(new ArrayListEditor());
        registerConverter(new BigDecimalEditor());
        registerConverter(new BigIntegerEditor());
        registerConverter(new BooleanEditor());
        registerConverter(new ByteEditor());
        registerConverter(new CharacterEditor());
        registerConverter(new ClassEditor());
        registerConverter(new DateEditor());
        registerConverter(new DoubleEditor());
        registerConverter(new FileEditor());
        registerConverter(new FloatEditor());
        registerConverter(new HashMapEditor());
        registerConverter(new HashtableEditor());
        registerConverter(new IdentityHashMapEditor());
        registerConverter(new Inet4AddressEditor());
        registerConverter(new Inet6AddressEditor());
        registerConverter(new InetAddressEditor());
        registerConverter(new IntegerEditor());
        registerConverter(new LinkedHashMapEditor());
        registerConverter(new LinkedHashSetEditor());
        registerConverter(new LinkedListEditor());
        registerConverter(new ListEditor());
        registerConverter(new LongEditor());
        registerConverter(new MapEditor());
        registerConverter(new ObjectNameEditor());
        registerConverter(new PropertiesEditor());
        registerConverter(new SetEditor());
        registerConverter(new ShortEditor());
        registerConverter(new SortedMapEditor());
        registerConverter(new SortedSetEditor());
        registerConverter(new StringEditor());
        registerConverter(new TreeMapEditor());
        registerConverter(new TreeSetEditor());
        registerConverter(new URIEditor());
        registerConverter(new URLEditor());
        registerConverter(new LoggerConverter());
        registerConverter(new PatternConverter());
        registerConverter(new JndiConverter());
        registerConverter(new VectorEditor());
        registerConverter(new WeakHashMapEditor());

        try {
            registerConverter(new Log4jConverter());
        } catch (Throwable e) {
        }

        try {
            registerConverter(new CommonsLoggingConverter());
        } catch (Throwable e) {
        }
    }

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

            // register all converters with the VM
            if (registerWithVM) {
                for (Entry<Class, Converter> entry : registry.entrySet()) {
                    Class type = entry.getKey();
                    Converter converter = entry.getValue();
                    PropertyEditorManager.registerEditor(type, converter.getClass());
                }
            }
        }
    }

    public static void registerConverter(Converter converter) {
        if (converter == null) throw new NullPointerException("editor is null");
        Class type = converter.getType();
        registry.put(type, converter);
        if (registerWithVM) {
            PropertyEditorManager.registerEditor(type, converter.getClass());
        }

        if (PRIMITIVE_TO_WRAPPER.containsKey(type)) {
            Class wrapperType = PRIMITIVE_TO_WRAPPER.get(type);
            registry.put(wrapperType, converter);
            if (registerWithVM) {
                PropertyEditorManager.registerEditor(wrapperType, converter.getClass());
            }
        } else if (WRAPPER_TO_PRIMITIVE.containsKey(type)) {
            Class primitiveType = WRAPPER_TO_PRIMITIVE.get(type);
            registry.put(primitiveType, converter);
            if (registerWithVM) {
                PropertyEditorManager.registerEditor(primitiveType, converter.getClass());
            }
        }
    }

    public static boolean canConvert(String type, ClassLoader classLoader) {
        if (type == null) throw new NullPointerException("type is null");
        if (classLoader == null) throw new NullPointerException("classLoader is null");

        // load using the ClassLoading utility, which also manages arrays and primitive classes.
        Class typeClass;
        try {
            typeClass = Class.forName(type, true, classLoader);
        } catch (ClassNotFoundException e) {
            throw new PropertyEditorException("Type class could not be found: " + type);
        }

        return canConvert(typeClass);

    }

    public static boolean canConvert(Class type) {
        PropertyEditor editor = findConverterOrEditor(type);

        return editor != null;
    }

    private static PropertyEditor findConverterOrEditor(Type type){
        Converter converter = findConverter(type);
        if (converter != null) {
            return converter;
        }

        // fall back to a property editor
        PropertyEditor editor = findEditor(type);
        if (editor != null) {
            return editor;
        }

        converter = findBuiltinConverter(type);
        if (converter != null) {
            return converter;
        }

        return null;
    }

    public static String toString(Object value) throws PropertyEditorException {
        if (value == null) throw new NullPointerException("value is null");

        // get an editor for this type
        Class type = value.getClass();

        PropertyEditor editor = findConverterOrEditor(type);

        if (editor instanceof Converter) {
            Converter converter = (Converter) editor;
            return converter.toString(value);
        }

        if (editor == null) {
            throw new PropertyEditorException("Unable to find PropertyEditor for " + type.getSimpleName());
        }

        // create the string value
        editor.setValue(value);
        String textValue;
        try {
            textValue = editor.getAsText();
        } catch (Exception e) {
            throw new PropertyEditorException("Error while converting a \"" + type.getSimpleName() + "\" to text " +
                    " using the property editor " + editor.getClass().getSimpleName(), e);
        }
        return textValue;
    }

    public static Object getValue(String type, String value, ClassLoader classLoader) throws PropertyEditorException {
        if (type == null) throw new NullPointerException("type is null");
        if (value == null) throw new NullPointerException("value is null");
        if (classLoader == null) throw new NullPointerException("classLoader is null");

        // load using the ClassLoading utility, which also manages arrays and primitive classes.
        Class typeClass;
        try {
            typeClass = Class.forName(type, true, classLoader);
        } catch (ClassNotFoundException e) {
            throw new PropertyEditorException("Type class could not be found: " + type);
        }

        return getValue(typeClass, value);

    }

    public static Object getValue(Type type, String value) throws PropertyEditorException {
        if (type == null) throw new NullPointerException("type is null");
        if (value == null) throw new NullPointerException("value is null");

        PropertyEditor editor = findConverterOrEditor(type);

        if (editor instanceof Converter) {
            Converter converter = (Converter) editor;
            return converter.toObject(value);
        }

        Class clazz = toClass(type);

        if (editor == null) {
            throw new PropertyEditorException("Unable to find PropertyEditor for " + clazz.getSimpleName());
        }

        editor.setAsText(value);
        Object objectValue;
        try {
            objectValue = editor.getValue();
        } catch (Exception e) {
            throw new PropertyEditorException("Error while converting \"" + value + "\" to a " + clazz.getSimpleName() +
                    " using the property editor " + editor.getClass().getSimpleName(), e);
        }
        return objectValue;
    }

    private static Converter findBuiltinConverter(Type type) {
        if (type == null) throw new NullPointerException("type is null");

        Class clazz = toClass(type);

        if (Enum.class.isAssignableFrom(clazz)){
            return new EnumConverter(clazz);
        }

        return null;       
    }

    private static Converter findConverter(Type type) {
        if (type == null) throw new NullPointerException("type is null");

        Class clazz = toClass(type);



        // it's possible this was a request for an array class.  We might not
        // recognize the array type directly, but the component type might be
        // resolvable
        if (clazz.isArray() && !clazz.getComponentType().isArray()) {
            // do a recursive lookup on the base type
            PropertyEditor editor = findConverterOrEditor(clazz.getComponentType());
            // if we found a suitable editor for the base component type,
            // wrapper this in an array adaptor for real use
            if (editor != null) {
                return new ArrayConverter(clazz, editor);
            } else {
                return null;
            }
        }

        if (Collection.class.isAssignableFrom(clazz)){
            Type[] types = getTypeParameters(Collection.class, type);

            Type componentType = String.class;
            if (types != null && types.length == 1 && types[0] instanceof Class) {
                componentType = types[0];
            }

            PropertyEditor editor = findConverterOrEditor(componentType);

            if (editor != null){
                if (RecipeHelper.hasDefaultConstructor(clazz)) {
                    return new GenericCollectionConverter(clazz, editor);
                } else if (SortedSet.class.isAssignableFrom(clazz)) {
                    return new GenericCollectionConverter(TreeSet.class, editor);
                } else if (Set.class.isAssignableFrom(clazz)) {
                    return new GenericCollectionConverter(LinkedHashSet.class, editor);
                } else {
                    return new GenericCollectionConverter(ArrayList.class, editor);
                }
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

            PropertyEditor keyConverter = findConverterOrEditor(keyType);
            PropertyEditor valueConverter = findConverterOrEditor(valueType);

            if (keyConverter != null && valueConverter != null){
                if (RecipeHelper.hasDefaultConstructor(clazz)) {
                    return new GenericMapConverter(clazz, keyConverter, valueConverter);
                } else if (SortedMap.class.isAssignableFrom(clazz)) {
                    return new GenericMapConverter(TreeMap.class, keyConverter, valueConverter);
                } else if (ConcurrentMap.class.isAssignableFrom(clazz)) {
                    return new GenericMapConverter(ConcurrentHashMap.class, keyConverter, valueConverter);
                } else {
                    return new GenericMapConverter(LinkedHashMap.class, keyConverter, valueConverter);
                }
            }

            return null;
        }

        Converter converter = registry.get(clazz);

        // we're outta here if we got one.
        if (converter != null) {
            return converter;
        }

        Class[] declaredClasses = clazz.getDeclaredClasses();
        for (Class declaredClass : declaredClasses) {
            if (Converter.class.isAssignableFrom(declaredClass)) {
                try {
                    converter = (Converter) declaredClass.newInstance();
                    registerConverter(converter);

                    // try to get the converter from the registry... the converter
                    // created above may have been for another class
                    converter = registry.get(clazz);
                    if (converter != null) {
                        return converter;
                    }
                } catch (Exception e) {
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
    private static PropertyEditor findEditor(Type type) {
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
}
