package com.digiwes.osgi;

import java.util.*;

/**
 * Created by huangxm on 3/30/2016.
 */
public class JarManifest {
    private List<KeyValuePair> list = null;
    private Map<String, String> map = null;

    public JarManifest(List<KeyValuePair> list, Map<String, String> map) {
        this.list = list;
        this.map = map;
    }

    public String getValue(String key) {
        return map.get(key);
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        Iterator<KeyValuePair> iterator = list.iterator();
        while (iterator.hasNext()) {
            KeyValuePair keyValuePair = iterator.next();
            stringBuffer.append(keyValuePair.getKey() + ": " + keyValuePair.getValue() + "\n");
        }

        return stringBuffer.toString();
    }
}
