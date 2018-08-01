package org.apache.xbean.generator.commons;

import java.util.function.Function;

public class NamespaceFileNameExtractor implements Function<String, String> {

    public String apply(String namespace) {
        if (namespace.startsWith("http://") || namespace.startsWith("https://")) {
            return namespace.substring(namespace.lastIndexOf('/') + 1) + ".xsd";
        }

        if (namespace.contains(":")) {
            // a typical urn:x:y namespace
            return namespace.substring(namespace.lastIndexOf(':') + 1) + ".xsd";
        }

        return namespace.replace("[^a-zA-Z0-9_", "") + ".xsd";
    }

}
