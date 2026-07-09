package com.tourismqa.service;

import java.util.List;
import java.util.Map;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * 地理编码服务。
 * 使用场景：
 * 在路线规划中将地点名称解析为经纬度，用于地图可视化渲染。
 * 核心职责：
 * 1. 调用 Nominatim 搜索接口进行地理编码。
 * 2. 对外部响应进行结构校验并返回标准经纬度数组。
 * 3. 在异常或无结果场景下返回空值，避免中断主流程。
 *
 * <p>框架作用：`@Service` 声明服务 Bean，默认单例作用域。</p>
 */
@Service
public class GeocodingService {

    private final SimpleClientHttpRequestFactory requestFactory;
    private final ServiceManagementService serviceManagementService;

    public GeocodingService(SimpleClientHttpRequestFactory requestFactory,
                            ServiceManagementService serviceManagementService) {
        this.requestFactory = requestFactory;
        this.serviceManagementService = serviceManagementService;
    }

    /**
     * 将自然语言地点文本解析为经纬度。
     *
     * @param query 地点查询文本
     * @return `[latitude, longitude]`；解析失败时返回 `null`
     */
    public double[] geocode(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }

        try {
            ServiceManagementService.MapRuntimeConfig config = serviceManagementService.getMapRuntimeConfig();
            if (!config.enabled()) {
                return null;
            }
            List<Map<String, Object>> list = RestClient.builder()
                    .requestFactory(requestFactory)
                    .baseUrl(config.geocodingBaseUrl())
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("format", "json")
                            .queryParam("limit", "1")
                            .queryParam("q", query)
                            .build())
                    .header("User-Agent", config.geocodingUserAgent())
                    .retrieve()
                    .body(List.class);

            // 仅取首条命中结果，降低外部调用开销并保持返回结构稳定。
            if (list == null || list.isEmpty()) {
                return null;
            }
            Object first = list.get(0);
            if (!(first instanceof Map<?, ?> map)) {
                return null;
            }
            Object latObj = map.get("lat");
            Object lonObj = map.get("lon");
            if (latObj == null || lonObj == null) {
                return null;
            }
            return new double[] {Double.parseDouble(latObj.toString()), Double.parseDouble(lonObj.toString())};
        } catch (Exception ex) {
            // 地理编码失败不阻断路线生成流程，由上层以空坐标兜底。
            return null;
        }
    }
}
