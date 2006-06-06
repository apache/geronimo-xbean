package org.apache.xbean.spring.generator;

public class MapMapping {
    private String entryName;
    private String keyName;

    public MapMapping(String entryName, String keyName) {
        this.entryName = entryName;
        this.keyName = keyName;
    }

    public String getEntryName() {
        return entryName;
    }

    public String getKeyName() {
        return keyName;
    }
}
