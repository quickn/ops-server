package com.cloud.receiver.util;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.Set;

public class CamelCaseUtil {

    public static void underlineToCamelCase(Object json) {
        convert(json, "CamelCase");
    }

    public static void camelCaseToUnderline(Object json) {
        convert(json, "Underline");
    }


    private static void convert(Object json, String type) {
        if (json instanceof JSONArray) {
            JSONArray arr = (JSONArray) json;
            for (Object obj : arr) {
                convert(obj, type);
            }
        } else if (json instanceof JSONObject) {
            JSONObject jo = (JSONObject) json;
            Set<String> keys = jo.keySet();
            String[] array = keys.toArray(new String[0]);
            for (String key : array) {
                Object value = jo.get(key);
                String newKey = null;
                // 手动将下划线转为驼峰
                if (type.equals("CamelCase")) {
                    newKey = toCamelCase(key);
                } else if (type.equals("Underline")) {
                    newKey = toUnderlineCase(key);
                }
                jo.remove(key);
                jo.put(newKey, value);
                if (key.startsWith("is")) {
                    if (value instanceof Integer) {
                        if (Integer.parseInt(value.toString()) == 1) {
                            jo.put(newKey, true);
                        } else {
                            jo.put(newKey, false);
                        }
                    }
                }
                convert(value, type); // 递归处理嵌套对象
            }
        }
    }

    /**
     * 驼峰转下划线
     * 例如: hostName -> host_name, cpuUse -> cpu_use
     */
    private static String toUnderlineCase(String key) {
        if (key == null || key.isEmpty()) {
            return key;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append('_').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // 简单的下划线转驼峰实现
    private static String toCamelCase(String key) {
        String[] parts = key.split("_");
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    sb.append(part.substring(1));
                }
            }
        }
        return sb.toString();
    }
}
