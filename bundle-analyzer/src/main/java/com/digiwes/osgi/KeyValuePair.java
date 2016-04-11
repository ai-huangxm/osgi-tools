package com.digiwes.osgi;

/**
 * Created by huangxm on 3/30/2016.
 */
public class KeyValuePair {
    private String key = null;
    private String value = null;

    public KeyValuePair(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return this.key;
    }

    public String getValue() {
        return this.value;
    }
}
