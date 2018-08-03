package org.apache.xbean.model.type;

import org.apache.xbean.model.Type;

import java.util.*;

public class Types {

    static final Map<String, PrimitiveType> primitives;

    static {
        Map<String, PrimitiveType> primitiveMap = new TreeMap<>();
        primitiveMap.put("boolean", new PrimitiveType("boolean"));
        primitiveMap.put("byte", new PrimitiveType("byte"));
        primitiveMap.put("char", new PrimitiveType("char"));
        primitiveMap.put("short", new PrimitiveType("short"));
        primitiveMap.put("int", new PrimitiveType("int"));
        primitiveMap.put("long", new PrimitiveType("long"));
        primitiveMap.put("float", new PrimitiveType("float"));
        primitiveMap.put("double", new PrimitiveType("double"));
        primitives = Collections.unmodifiableMap(primitiveMap);
    }

    public static Type newSimpleType(String name) {
        Objects.requireNonNull(name, "SimpleType name must be specified");
        if (name.contains("[") || name.contains("]")) {
            throw new IllegalArgumentException("Name can not contain '[' or ']' " + name);
        }

        if (primitives.containsKey(name)) {
            return primitives.get(name);
        }

        return new SimpleType(name);
    }

    public static Type newArrayType(String type, int dimensions) {
        Objects.requireNonNull(type, "Array Type  must be specified");
        if (dimensions < 1) throw new IllegalArgumentException("dimensions must be at least one");
        StringBuffer buf = new StringBuffer(type.length() + (dimensions * 2));
        buf.append(type);
        for (int index = 0; index < dimensions; index ++) {
            buf.append("[]");
        }
        return new ArrayType(buf.toString(), newSimpleType(type));
    }

    public static Type newCollectionType(String collectionType, Type elementType) {
        if (collectionType == null) throw new NullPointerException("collectionType");
        if (elementType == null) throw new NullPointerException("elementType");
        return new CollectionType(collectionType, elementType);
    }

    public static Type newMapType(String type, Type key, Type value) {
        return new MapType(type, key, value);
    }
}
