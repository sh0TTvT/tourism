package com.tourismqa.service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourismqa.config.AppProperties;
import com.tourismqa.dto.AdminExternalServiceItemResponse;
import com.tourismqa.dto.AdminExternalServiceSaveRequest;
import com.tourismqa.dto.AdminExternalServiceSaveResponse;
import com.tourismqa.dto.AdminExternalServiceTestResponse;
import com.tourismqa.dto.PublicExternalServiceStatusItemResponse;
import com.tourismqa.dto.PublicMapConfigResponse;
import com.tourismqa.dto.PublicServiceStatusResponse;
import com.tourismqa.entity.ManagedExternalServiceConfig;
import com.tourismqa.exception.ApiException;
import com.tourismqa.repository.ManagedExternalServiceConfigRepository;

/**
 * 服务管理核心服务。
 * 使用场景：
 * 统一管理天气服务与地图服务的配置、人工测试、公开状态输出和心跳检测结果。
 */
@Service
public class ServiceManagementService {

    private static final Duration FAILED_HEARTBEAT_RETRY_INTERVAL = Duration.ofHours(24);
    private static final int HEARTBEAT_PROBE_MAX_ATTEMPTS = 3;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final ManagedExternalServiceConfigRepository repository;
    private final ObjectMapper objectMapper;
    private final ExternalServiceProbeService probeService;
    private final EnumMap<ManagedExternalServiceKey, ServiceDefaults> defaultsMap =
            new EnumMap<>(ManagedExternalServiceKey.class);

    public ServiceManagementService(ManagedExternalServiceConfigRepository repository,
                                    ObjectMapper objectMapper,
                                    ExternalServiceProbeService probeService,
                                    AppProperties appProperties) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.probeService = probeService;
        defaultsMap.put(ManagedExternalServiceKey.WEATHER, buildWeatherDefaults(appProperties));
        defaultsMap.put(ManagedExternalServiceKey.MAP, buildMapDefaults());
    }

    @Transactional(readOnly = true)
    public List<AdminExternalServiceItemResponse> listServicesForAdmin() {
        List<AdminExternalServiceItemResponse> results = new ArrayList<>();
        for (ManagedExternalServiceKey key : ManagedExternalServiceKey.values()) {
            results.add(toAdminResponse(loadOrDefaultEntity(key)));
        }
        return results;
    }

    @Transactional
    public void initializeDefaults() {
        ensureDefaultsLoaded();
    }

    @Transactional
    public AdminExternalServiceSaveResponse saveService(String key, AdminExternalServiceSaveRequest request) {
        ManagedExternalServiceKey serviceKey = requireServiceKey(key);
        ManagedExternalServiceConfig entity = loadOrCreateEntity(serviceKey);
        NormalizedServiceConfig normalized = normalizeRequest(serviceKey, request);

        entity.setDisplayName(normalized.displayName());
        entity.setEnabled(normalized.enabled());
        entity.setBaseUrl(normalized.baseUrl());
        entity.setSettingsJson(writeSettings(normalized.settings()));

        ExternalServiceProbeService.ProbeResult result = probe(serviceKey, entity);
        entity.setLastCheckedAt(Instant.now());
        entity.setLastCheckPassed(result.available());
        entity.setLastCheckMessage(limitMessage(result.message()));

        ManagedExternalServiceConfig saved = repository.save(entity);
        String message = result.available()
                ? "服务配置已保存并通过可用性测试。"
                : "服务配置已保存，但测试未通过：" + limitMessage(result.message());
        return new AdminExternalServiceSaveResponse(toAdminResponse(saved), result.available(), message);
    }

    @Transactional
    public AdminExternalServiceTestResponse testService(String key) {
        ManagedExternalServiceKey serviceKey = requireServiceKey(key);
        ManagedExternalServiceConfig entity = loadOrCreateEntity(serviceKey);
        ExternalServiceProbeService.ProbeResult result = probe(serviceKey, entity);
        Instant now = Instant.now();
        entity.setLastCheckedAt(now);
        entity.setLastCheckPassed(result.available());
        entity.setLastCheckMessage(limitMessage(result.message()));
        if (result.available()) {
            entity.setLastHeartbeatAt(now);
            entity.setLastHeartbeatPassed(true);
            entity.setLastHeartbeatMessage(limitMessage(result.message()));
            entity.setLastHeartbeatLatencyMs(result.latencyMs());
        }
        repository.save(entity);
        return new AdminExternalServiceTestResponse(
                serviceKey.key(),
                result.available(),
                limitMessage(result.message()),
                now,
                result.latencyMs()
        );
    }

    @Transactional
    public void runHeartbeatChecks() {
        ensureDefaultsLoaded();
        for (ManagedExternalServiceKey key : ManagedExternalServiceKey.values()) {
            ManagedExternalServiceConfig entity = loadOrCreateEntity(key);
            Instant now = Instant.now();
            entity.setLastHeartbeatAt(now);
            if (!entity.isEnabled()) {
                entity.setLastHeartbeatPassed(false);
                entity.setLastHeartbeatMessage("管理员已关闭该服务");
                entity.setLastHeartbeatLatencyMs(null);
                repository.save(entity);
                continue;
            }
            ExternalServiceProbeService.ProbeResult result = probeHeartbeatWithRetries(key, entity);
            entity.setLastHeartbeatPassed(result.available());
            entity.setLastHeartbeatMessage(limitMessage(result.message()));
            entity.setLastHeartbeatLatencyMs(result.latencyMs());
            repository.save(entity);
        }
    }

    @Transactional
    public void runStartupHeartbeatIfMissing() {
        ensureDefaultsLoaded();
        Instant retryBefore = Instant.now().minus(FAILED_HEARTBEAT_RETRY_INTERVAL);
        boolean shouldRun = false;
        for (ManagedExternalServiceKey key : ManagedExternalServiceKey.values()) {
            ManagedExternalServiceConfig entity = loadOrCreateEntity(key);
            if (entity.getLastHeartbeatAt() == null || shouldRetryFailedHeartbeat(entity, retryBefore)) {
                shouldRun = true;
                break;
            }
        }
        if (shouldRun) {
            runHeartbeatChecks();
        }
    }

    private boolean shouldRetryFailedHeartbeat(ManagedExternalServiceConfig entity, Instant retryBefore) {
        return Boolean.FALSE.equals(entity.getLastHeartbeatPassed())
                && entity.getLastHeartbeatAt() != null
                && entity.getLastHeartbeatAt().isBefore(retryBefore);
    }

    private ExternalServiceProbeService.ProbeResult probeHeartbeatWithRetries(ManagedExternalServiceKey key,
                                                                              ManagedExternalServiceConfig entity) {
        ExternalServiceProbeService.ProbeResult result = null;
        for (int attempt = 0; attempt < HEARTBEAT_PROBE_MAX_ATTEMPTS; attempt++) {
            result = probe(key, entity);
            if (result.available()) {
                return result;
            }
        }
        return result == null
                ? ExternalServiceProbeService.ProbeResult.failure("服务心跳检测失败", 0)
                : result;
    }

    @Transactional(readOnly = true)
    public PublicServiceStatusResponse getPublicServiceStatus() {
        List<PublicExternalServiceStatusItemResponse> services = new ArrayList<>();
        for (ManagedExternalServiceKey key : ManagedExternalServiceKey.values()) {
            ManagedExternalServiceConfig entity = loadOrDefaultEntity(key);
            AvailabilitySnapshot availability = resolveAvailability(entity);
            services.add(new PublicExternalServiceStatusItemResponse(
                    key.key(),
                    entity.getDisplayName(),
                    entity.isEnabled(),
                    availability.available(),
                    publicAvailabilityMessage(key, entity, availability),
                    entity.getLastHeartbeatAt()
            ));
        }
        MapRuntimeConfig mapConfig = getMapRuntimeConfig();
        return new PublicServiceStatusResponse(
                services,
                new PublicMapConfigResponse(
                        mapConfig.enabled(),
                        mapConfig.tileUrlTemplate(),
                        mapConfig.attribution(),
                        mapConfig.subdomains(),
                        mapConfig.maxZoom(),
                        mapConfig.defaultCenterLatitude(),
                        mapConfig.defaultCenterLongitude(),
                        mapConfig.defaultZoom()
                )
        );
    }

    @Transactional(readOnly = true)
    public WeatherRuntimeConfig getWeatherRuntimeConfig() {
        ManagedExternalServiceConfig entity = loadOrDefaultEntity(ManagedExternalServiceKey.WEATHER);
        Map<String, Object> settings = readSettings(entity.getSettingsJson());
        ServiceDefaults defaults = defaultsMap.get(ManagedExternalServiceKey.WEATHER);
        return new WeatherRuntimeConfig(
                entity.isEnabled(),
                normalizeBaseUrl(entity.getBaseUrl()),
                stringValue(settings.get("timezone"), String.valueOf(defaults.settings().get("timezone"))),
                intValue(settings.get("forecastDays"), (Integer) defaults.settings().get("forecastDays")),
                intValue(settings.get("maxForecastDays"), (Integer) defaults.settings().get("maxForecastDays"))
        );
    }

    @Transactional(readOnly = true)
    public MapRuntimeConfig getMapRuntimeConfig() {
        ManagedExternalServiceConfig entity = loadOrDefaultEntity(ManagedExternalServiceKey.MAP);
        Map<String, Object> settings = readSettings(entity.getSettingsJson());
        ServiceDefaults defaults = defaultsMap.get(ManagedExternalServiceKey.MAP);
        return new MapRuntimeConfig(
                entity.isEnabled(),
                entity.getBaseUrl(),
                stringValue(settings.get("attribution"), String.valueOf(defaults.settings().get("attribution"))),
                stringValue(settings.get("subdomains"), String.valueOf(defaults.settings().get("subdomains"))),
                intValue(settings.get("maxZoom"), (Integer) defaults.settings().get("maxZoom")),
                doubleValue(settings.get("defaultCenterLatitude"), (Double) defaults.settings().get("defaultCenterLatitude")),
                doubleValue(settings.get("defaultCenterLongitude"), (Double) defaults.settings().get("defaultCenterLongitude")),
                intValue(settings.get("defaultZoom"), (Integer) defaults.settings().get("defaultZoom")),
                stringValue(settings.get("geocodingBaseUrl"), String.valueOf(defaults.settings().get("geocodingBaseUrl"))),
                stringValue(settings.get("geocodingUserAgent"), String.valueOf(defaults.settings().get("geocodingUserAgent")))
        );
    }

    @Transactional(readOnly = true)
    public boolean isWeatherAvailableForPrompt() {
        ManagedExternalServiceConfig entity = loadOrDefaultEntity(ManagedExternalServiceKey.WEATHER);
        return entity.isEnabled();
    }

    private void ensureDefaultsLoaded() {
        for (ManagedExternalServiceKey key : ManagedExternalServiceKey.values()) {
            loadOrCreateEntity(key);
        }
    }

    private ManagedExternalServiceConfig loadOrDefaultEntity(ManagedExternalServiceKey key) {
        return repository.findByServiceKey(key.key()).orElseGet(() -> buildDefaultEntity(key));
    }

    private ManagedExternalServiceConfig loadOrCreateEntity(ManagedExternalServiceKey key) {
        return repository.findByServiceKey(key.key()).orElseGet(() -> repository.save(buildDefaultEntity(key)));
    }

    private ManagedExternalServiceConfig buildDefaultEntity(ManagedExternalServiceKey key) {
        ServiceDefaults defaults = defaultsMap.get(key);
        ManagedExternalServiceConfig entity = new ManagedExternalServiceConfig();
        entity.setServiceKey(key.key());
        entity.setDisplayName(defaults.displayName());
        entity.setEnabled(defaults.enabled());
        entity.setBaseUrl(defaults.baseUrl());
        entity.setSettingsJson(writeSettings(defaults.settings()));
        return entity;
    }

    private ServiceDefaults buildWeatherDefaults(AppProperties appProperties) {
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("timezone", appProperties.getRealtime().getWeather().getTimezone());
        settings.put("forecastDays", appProperties.getRealtime().getWeather().getForecastDays());
        settings.put("maxForecastDays", appProperties.getRealtime().getWeather().getMaxForecastDays());
        return new ServiceDefaults(
                ManagedExternalServiceKey.WEATHER.displayName(),
                appProperties.getRealtime().isEnabled() && appProperties.getRealtime().getWeather().isEnabled(),
                normalizeBaseUrl(appProperties.getRealtime().getWeather().getBaseUrl()),
                settings
        );
    }

    private ServiceDefaults buildMapDefaults() {
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("attribution", "&copy; OpenStreetMap contributors");
        settings.put("subdomains", "abc");
        settings.put("maxZoom", 19);
        settings.put("defaultCenterLatitude", 31.2304d);
        settings.put("defaultCenterLongitude", 121.4737d);
        settings.put("defaultZoom", 4);
        settings.put("geocodingBaseUrl", "https://nominatim.openstreetmap.org");
        settings.put("geocodingUserAgent", "tourism-qa/1.0");
        return new ServiceDefaults(
                ManagedExternalServiceKey.MAP.displayName(),
                true,
                "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
                settings
        );
    }

    private ManagedExternalServiceKey requireServiceKey(String key) {
        ManagedExternalServiceKey serviceKey = ManagedExternalServiceKey.fromKey(key);
        if (serviceKey == null) {
            throw new ApiException(HttpStatus.NOT_FOUND.value(), "未找到服务类型: " + key);
        }
        return serviceKey;
    }

    private NormalizedServiceConfig normalizeRequest(ManagedExternalServiceKey serviceKey,
                                                     AdminExternalServiceSaveRequest request) {
        String displayName = requireText(request.displayName(), "服务名称不能为空");
        String baseUrl = requireText(request.baseUrl(), "服务地址不能为空");
        boolean enabled = Boolean.TRUE.equals(request.enabled());
        Map<String, Object> settings = request.settings() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(request.settings());

        if (serviceKey == ManagedExternalServiceKey.WEATHER) {
            baseUrl = normalizeHttpUrl(baseUrl, "天气服务地址必须是合法的 http/https URL");
            String timezone = requireText(stringValue(settings.get("timezone"), ""), "天气时区不能为空");
            try {
                ZoneId.of(timezone);
            } catch (Exception ex) {
                throw new ApiException(HttpStatus.BAD_REQUEST.value(), "天气时区无效: " + timezone);
            }
            int forecastDays = intValue(settings.get("forecastDays"), 3);
            int maxForecastDays = intValue(settings.get("maxForecastDays"), 7);
            if (forecastDays < 1 || forecastDays > 16) {
                throw new ApiException(HttpStatus.BAD_REQUEST.value(), "默认天气天数必须在 1 到 16 之间");
            }
            if (maxForecastDays < forecastDays || maxForecastDays > 16) {
                throw new ApiException(HttpStatus.BAD_REQUEST.value(), "最大天气天数必须在默认天数到 16 之间");
            }
            Map<String, Object> normalizedSettings = new LinkedHashMap<>();
            normalizedSettings.put("timezone", timezone);
            normalizedSettings.put("forecastDays", forecastDays);
            normalizedSettings.put("maxForecastDays", maxForecastDays);
            return new NormalizedServiceConfig(displayName, enabled, baseUrl, normalizedSettings);
        }

        String tileUrlTemplate = requireText(baseUrl, "地图瓦片地址不能为空");
        String probeUrl = tileUrlTemplate
                .replace("{s}", "a")
                .replace("{z}", "0")
                .replace("{x}", "0")
                .replace("{y}", "0")
                .replace("{r}", "");
        normalizeHttpUrl(probeUrl, "地图瓦片模板必须能解析为合法的 http/https 地址");

        String geocodingBaseUrl = normalizeHttpUrl(
                stringValue(settings.get("geocodingBaseUrl"), ""),
                "地理编码服务地址必须是合法的 http/https URL"
        );
        String geocodingUserAgent = requireText(
                stringValue(settings.get("geocodingUserAgent"), ""),
                "地理编码 User-Agent 不能为空"
        );
        int maxZoom = intValue(settings.get("maxZoom"), 19);
        int defaultZoom = intValue(settings.get("defaultZoom"), 4);
        if (maxZoom < 1 || maxZoom > 22) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "地图最大缩放级别必须在 1 到 22 之间");
        }
        if (defaultZoom < 1 || defaultZoom > maxZoom) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "地图默认缩放级别必须在 1 到最大缩放级别之间");
        }
        Map<String, Object> normalizedSettings = new LinkedHashMap<>();
        normalizedSettings.put("attribution", requireText(stringValue(settings.get("attribution"), ""), "地图归属说明不能为空"));
        normalizedSettings.put("subdomains", stringValue(settings.get("subdomains"), "abc"));
        normalizedSettings.put("maxZoom", maxZoom);
        normalizedSettings.put("defaultCenterLatitude", doubleValue(settings.get("defaultCenterLatitude"), 31.2304d));
        normalizedSettings.put("defaultCenterLongitude", doubleValue(settings.get("defaultCenterLongitude"), 121.4737d));
        normalizedSettings.put("defaultZoom", defaultZoom);
        normalizedSettings.put("geocodingBaseUrl", geocodingBaseUrl);
        normalizedSettings.put("geocodingUserAgent", geocodingUserAgent);
        return new NormalizedServiceConfig(displayName, enabled, tileUrlTemplate, normalizedSettings);
    }

    private ExternalServiceProbeService.ProbeResult probe(ManagedExternalServiceKey key, ManagedExternalServiceConfig entity) {
        if (!entity.isEnabled()) {
            return ExternalServiceProbeService.ProbeResult.failure("管理员已关闭该服务", 0);
        }
        return switch (key) {
            case WEATHER -> probeService.probeWeather(getWeatherRuntimeConfig(entity));
            case MAP -> probeService.probeMap(getMapRuntimeConfig(entity));
        };
    }

    private WeatherRuntimeConfig getWeatherRuntimeConfig(ManagedExternalServiceConfig entity) {
        Map<String, Object> settings = readSettings(entity.getSettingsJson());
        return new WeatherRuntimeConfig(
                entity.isEnabled(),
                normalizeBaseUrl(entity.getBaseUrl()),
                stringValue(settings.get("timezone"), "Asia/Shanghai"),
                intValue(settings.get("forecastDays"), 3),
                intValue(settings.get("maxForecastDays"), 7)
        );
    }

    private MapRuntimeConfig getMapRuntimeConfig(ManagedExternalServiceConfig entity) {
        Map<String, Object> settings = readSettings(entity.getSettingsJson());
        return new MapRuntimeConfig(
                entity.isEnabled(),
                entity.getBaseUrl(),
                stringValue(settings.get("attribution"), "&copy; OpenStreetMap contributors"),
                stringValue(settings.get("subdomains"), "abc"),
                intValue(settings.get("maxZoom"), 19),
                doubleValue(settings.get("defaultCenterLatitude"), 31.2304d),
                doubleValue(settings.get("defaultCenterLongitude"), 121.4737d),
                intValue(settings.get("defaultZoom"), 4),
                stringValue(settings.get("geocodingBaseUrl"), "https://nominatim.openstreetmap.org"),
                stringValue(settings.get("geocodingUserAgent"), "tourism-qa/1.0")
        );
    }

    private AdminExternalServiceItemResponse toAdminResponse(ManagedExternalServiceConfig entity) {
        ManagedExternalServiceKey key = requireServiceKey(entity.getServiceKey());
        AvailabilitySnapshot availability = resolveAvailability(entity);
        return new AdminExternalServiceItemResponse(
                entity.getId(),
                entity.getServiceKey(),
                entity.getDisplayName(),
                key.description(),
                entity.isEnabled(),
                entity.getBaseUrl(),
                readSettings(entity.getSettingsJson()),
                availability.available(),
                availability.message(),
                entity.getLastCheckedAt(),
                entity.getLastCheckPassed(),
                entity.getLastCheckMessage(),
                entity.getLastHeartbeatAt(),
                entity.getLastHeartbeatPassed(),
                entity.getLastHeartbeatMessage(),
                entity.getLastHeartbeatLatencyMs(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private AvailabilitySnapshot resolveAvailability(ManagedExternalServiceConfig entity) {
        if (!entity.isEnabled()) {
            return new AvailabilitySnapshot(false, "管理员已关闭该服务");
        }
        if (entity.getLastHeartbeatPassed() != null) {
            return new AvailabilitySnapshot(
                    entity.getLastHeartbeatPassed(),
                    defaultMessage(entity.getLastHeartbeatMessage(), entity.getLastHeartbeatPassed())
            );
        }
        if (entity.getLastCheckPassed() != null) {
            return new AvailabilitySnapshot(
                    entity.getLastCheckPassed(),
                    defaultMessage(entity.getLastCheckMessage(), entity.getLastCheckPassed())
            );
        }
        return new AvailabilitySnapshot(true, "服务尚未完成首次心跳检测，系统会自动继续检测");
    }

    private String defaultMessage(String value, boolean passed) {
        if (StringUtils.hasText(value)) {
            return value;
        }
        return passed ? "最近一次检测通过" : "最近一次检测失败";
    }

    private String publicAvailabilityMessage(ManagedExternalServiceKey key,
                                             ManagedExternalServiceConfig entity,
                                             AvailabilitySnapshot availability) {
        if (key == ManagedExternalServiceKey.WEATHER && entity.isEnabled() && !availability.available()) {
            return "天气服务暂时不可用";
        }
        return availability.message();
    }

    private Map<String, Object> readSettings(String json) {
        try {
            if (!StringUtils.hasText(json)) {
                return new LinkedHashMap<>();
            }
            Map<String, Object> map = objectMapper.readValue(json, MAP_TYPE);
            return map == null ? new LinkedHashMap<>() : new LinkedHashMap<>(map);
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
    }

    private String writeSettings(Map<String, Object> settings) {
        try {
            return objectMapper.writeValueAsString(settings == null ? Map.of() : settings);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务配置序列化失败");
        }
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), message);
        }
        return value.trim();
    }

    private String stringValue(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }

    private int intValue(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ex) {
            return fallback;
        }
    }

    private double doubleValue(Object value, double fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (Exception ex) {
            return fallback;
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizeHttpUrl(String value, String errorMessage) {
        String normalized = requireText(value, errorMessage);
        try {
            String scheme = java.net.URI.create(normalized).getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException();
            }
            return normalized;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), errorMessage);
        }
    }

    private String limitMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "";
        }
        String normalized = message.trim();
        return normalized.length() > 255 ? normalized.substring(0, 255) : normalized;
    }

    private record ServiceDefaults(String displayName,
                                   boolean enabled,
                                   String baseUrl,
                                   Map<String, Object> settings) {
    }

    private record NormalizedServiceConfig(String displayName,
                                           boolean enabled,
                                           String baseUrl,
                                           Map<String, Object> settings) {
    }

    private record AvailabilitySnapshot(boolean available, String message) {
    }

    public record WeatherRuntimeConfig(boolean enabled,
                                       String baseUrl,
                                       String timezone,
                                       int forecastDays,
                                       int maxForecastDays) {
    }

    public record MapRuntimeConfig(boolean enabled,
                                   String tileUrlTemplate,
                                   String attribution,
                                   String subdomains,
                                   int maxZoom,
                                   double defaultCenterLatitude,
                                   double defaultCenterLongitude,
                                   int defaultZoom,
                                   String geocodingBaseUrl,
                                   String geocodingUserAgent) {
    }
}
