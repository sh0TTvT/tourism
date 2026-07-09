package com.tourismqa.service;

import java.util.Locale;

/**
 * 受管外部服务枚举。
 */
public enum ManagedExternalServiceKey {
    MAP("map", "地图服务", "统一管理地图瓦片与地理编码服务，用于路线地图、探索地图与点位坐标解析。"),
    WEATHER("weather", "天气服务", "统一管理天气查询服务，用于实时天气问答增强与天气接口查询。");

    private final String key;
    private final String displayName;
    private final String description;

    ManagedExternalServiceKey(String key, String displayName, String description) {
        this.key = key;
        this.displayName = displayName;
        this.description = description;
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public static ManagedExternalServiceKey fromKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        for (ManagedExternalServiceKey item : values()) {
            if (item.key.equals(normalized)) {
                return item;
            }
        }
        return null;
    }
}
