package com.tourismqa.dto;

/**
 * 用户端地图运行配置。
 */
public record PublicMapConfigResponse(
        boolean enabled,
        String tileUrlTemplate,
        String attribution,
        String subdomains,
        int maxZoom,
        double defaultCenterLatitude,
        double defaultCenterLongitude,
        int defaultZoom
) {
}
