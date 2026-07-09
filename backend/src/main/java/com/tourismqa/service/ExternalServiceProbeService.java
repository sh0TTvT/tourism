package com.tourismqa.service;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * 外部服务探活服务。
 * 使用场景：
 * 提供天气与地图服务的真实 HTTP 探测，供手动测试与定时心跳共用。
 */
@Service
public class ExternalServiceProbeService {

    private final SimpleClientHttpRequestFactory requestFactory;

    public ExternalServiceProbeService(SimpleClientHttpRequestFactory requestFactory) {
        this.requestFactory = requestFactory;
    }

    public ProbeResult probeWeather(ServiceManagementService.WeatherRuntimeConfig config) {
        Instant start = Instant.now();
        try {
            RestClient client = RestClient.builder().requestFactory(requestFactory).baseUrl(config.baseUrl()).build();
            Map<String, Object> response = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/forecast")
                            .queryParam("latitude", 31.2304)
                            .queryParam("longitude", 121.4737)
                            .queryParam("daily", "weather_code,temperature_2m_max,temperature_2m_min")
                            .queryParam("timezone", config.timezone())
                            .queryParam("forecast_days", Math.max(1, Math.min(config.forecastDays(), 3)))
                            .build())
                    .retrieve()
                    .body(Map.class);
            Object daily = response == null ? null : response.get("daily");
            if (!(daily instanceof Map<?, ?> dailyMap) || dailyMap.isEmpty()) {
                return ProbeResult.failure("天气服务返回结构异常", elapsedMillis(start));
            }
            return ProbeResult.success("天气服务连通正常", elapsedMillis(start));
        } catch (Exception ex) {
            return ProbeResult.failure(safeMessage(ex, "天气服务不可用"), elapsedMillis(start));
        }
    }

    public ProbeResult probeMap(ServiceManagementService.MapRuntimeConfig config) {
        Instant start = Instant.now();
        try {
            String tileUrl = resolveProbeTileUrl(config.tileUrlTemplate(), config.subdomains());
            RestClient.builder().requestFactory(requestFactory).build().get()
                    .uri(URI.create(tileUrl))
                    .retrieve()
                    .toBodilessEntity();

            RestClient geocodingClient = RestClient.builder()
                    .requestFactory(requestFactory)
                    .baseUrl(config.geocodingBaseUrl())
                    .build();
            List<Map<String, Object>> geocodingResponse = geocodingClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("format", "json")
                            .queryParam("limit", "1")
                            .queryParam("q", "上海")
                            .build())
                    .header("User-Agent", config.geocodingUserAgent())
                    .retrieve()
                    .body(List.class);
            if (geocodingResponse == null || geocodingResponse.isEmpty()) {
                return ProbeResult.failure("地理编码服务未返回有效结果", elapsedMillis(start));
            }
            return ProbeResult.success("地图瓦片与地理编码服务连通正常", elapsedMillis(start));
        } catch (Exception ex) {
            return ProbeResult.failure(safeMessage(ex, "地图服务不可用"), elapsedMillis(start));
        }
    }

    private long elapsedMillis(Instant start) {
        return Math.max(1L, Duration.between(start, Instant.now()).toMillis());
    }

    private String resolveProbeTileUrl(String template, String subdomains) {
        String subdomain = StringUtils.hasText(subdomains)
                ? String.valueOf(subdomains.trim().charAt(0))
                : "";
        String resolved = template
                .replace("{s}", subdomain)
                .replace("{z}", "0")
                .replace("{x}", "0")
                .replace("{y}", "0")
                .replace("{r}", "");
        return resolved;
    }

    private String safeMessage(Exception ex, String fallback) {
        String message = ex.getMessage();
        if (!StringUtils.hasText(message)) {
            return fallback;
        }
        return message.length() > 255 ? message.substring(0, 255) : message;
    }

    public record ProbeResult(boolean available, String message, long latencyMs) {

        public static ProbeResult success(String message, long latencyMs) {
            return new ProbeResult(true, message, latencyMs);
        }

        public static ProbeResult failure(String message, long latencyMs) {
            return new ProbeResult(false, message, latencyMs);
        }
    }
}
