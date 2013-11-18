package org.yinwang.pysonar;

import java.util.HashMap;
import java.util.Map;

public class Stats {
    Map<String, Object> contents = new HashMap<>();

    public void putInt(String key, int value) {
        contents.put(key, value);
    }


    public void inc(String key, int x) {
        Integer old = getInt(key);

        if (old == null) {
            contents.put(key, 1);
        } else {
            contents.put(key, old + x);
        }
    }


    public void inc(String key) {
        inc(key, 1);
    }


    public Integer getInt(String key) {
        Integer ret = (Integer) contents.get(key);
        if (ret == null) {
            return 0;
        } else {
            return ret;
        }
    }


    public String print() {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, Object> e : contents.entrySet()) {
            sb.append("\n- " + e.getKey() + ": " + e.getValue());
        }

        return sb.toString();
    }

}
