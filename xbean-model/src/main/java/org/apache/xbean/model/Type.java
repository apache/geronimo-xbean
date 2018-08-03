package org.apache.xbean.model;

public interface Type {

    String getName();

    default boolean isCollection() {
        return false;
    }

    default boolean isMap() {
        return false;
    }

    default boolean isPrimitive() {
        return false;
    }

    default boolean isArray() {
        return false;
    }

    default boolean isComplex() {
        return isCollection() || isMap() || isArray();
    }

}
