package com.tourismqa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourismqa.config.AppProperties;
import com.tourismqa.dto.AdminExternalServiceSaveRequest;
import com.tourismqa.dto.AdminExternalServiceSaveResponse;
import com.tourismqa.dto.PublicServiceStatusResponse;
import com.tourismqa.entity.ManagedExternalServiceConfig;
import com.tourismqa.repository.ManagedExternalServiceConfigRepository;

class ServiceManagementServiceTest {

    private final Map<String, ManagedExternalServiceConfig> store = new LinkedHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    private ManagedExternalServiceConfigRepository repository;
    private ExternalServiceProbeService probeService;
    private ServiceManagementService serviceManagementService;

    @BeforeEach
    void setUp() {
        store.clear();
        idSequence.set(1);

        repository = org.mockito.Mockito.mock(ManagedExternalServiceConfigRepository.class);
        probeService = org.mockito.Mockito.mock(ExternalServiceProbeService.class);

        when(probeService.probeWeather(any(ServiceManagementService.WeatherRuntimeConfig.class)))
                .thenReturn(ExternalServiceProbeService.ProbeResult.success("天气服务连通正常", 120));
        when(probeService.probeMap(any(ServiceManagementService.MapRuntimeConfig.class)))
                .thenReturn(ExternalServiceProbeService.ProbeResult.success("地图服务连通正常", 180));

        when(repository.findByServiceKey(any())).thenAnswer(invocation ->
                Optional.ofNullable(store.get(invocation.getArgument(0)))
        );
        when(repository.save(any(ManagedExternalServiceConfig.class))).thenAnswer(invocation -> {
            ManagedExternalServiceConfig entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(idSequence.getAndIncrement());
                entity.prePersist();
            } else {
                entity.preUpdate();
            }
            store.put(entity.getServiceKey(), entity);
            return entity;
        });

        AppProperties appProperties = new AppProperties();
        appProperties.getRealtime().setEnabled(true);
        appProperties.getRealtime().getWeather().setEnabled(true);
        appProperties.getRealtime().getWeather().setBaseUrl("https://api.open-meteo.com");
        appProperties.getRealtime().getWeather().setTimezone("Asia/Shanghai");
        appProperties.getRealtime().getWeather().setForecastDays(3);
        appProperties.getRealtime().getWeather().setMaxForecastDays(7);

        serviceManagementService = new ServiceManagementService(
                repository,
                new ObjectMapper(),
                probeService,
                appProperties
        );
    }

    @Test
    void initializeDefaults_shouldCreateWeatherAndMapServices() {
        serviceManagementService.initializeDefaults();

        assertThat(store).hasSize(2);
        assertThat(store).containsKeys("weather", "map");
        assertThat(store.get("weather").getBaseUrl()).isEqualTo("https://api.open-meteo.com");
        assertThat(store.get("map").getBaseUrl()).contains("{z}");
    }

    @Test
    void saveService_shouldPersistConfigAndRecordLastCheck() {
        when(probeService.probeWeather(any(ServiceManagementService.WeatherRuntimeConfig.class)))
                .thenReturn(ExternalServiceProbeService.ProbeResult.success("天气服务连通正常", 128));

        AdminExternalServiceSaveResponse response = serviceManagementService.saveService(
                "weather",
                new AdminExternalServiceSaveRequest(
                        "生产天气服务",
                        true,
                        "https://api.open-meteo.com",
                        Map.of(
                                "timezone", "Asia/Shanghai",
                                "forecastDays", 4,
                                "maxForecastDays", 8
                        )
                )
        );

        assertThat(response.serviceAvailable()).isTrue();
        assertThat(response.message()).contains("通过可用性测试");
        ManagedExternalServiceConfig saved = store.get("weather");
        assertThat(saved.getDisplayName()).isEqualTo("生产天气服务");
        assertThat(saved.getLastCheckPassed()).isTrue();
        assertThat(saved.getLastCheckMessage()).isEqualTo("天气服务连通正常");
        assertThat(saved.getSettingsJson()).contains("\"forecastDays\":4");
    }

    @Test
    void testService_shouldRefreshHeartbeatWhenManualTestSucceeds() {
        serviceManagementService.initializeDefaults();
        ManagedExternalServiceConfig weatherConfig = store.get("weather");
        weatherConfig.setLastHeartbeatAt(Instant.now().minus(Duration.ofHours(2)));
        weatherConfig.setLastHeartbeatPassed(false);
        weatherConfig.setLastHeartbeatMessage("上次天气探测失败");
        repository.save(weatherConfig);

        when(probeService.probeWeather(any(ServiceManagementService.WeatherRuntimeConfig.class)))
                .thenReturn(ExternalServiceProbeService.ProbeResult.success("天气服务连通正常", 96));

        serviceManagementService.testService("weather");
        PublicServiceStatusResponse status = serviceManagementService.getPublicServiceStatus();

        ManagedExternalServiceConfig weather = store.get("weather");
        assertThat(weather.getLastCheckPassed()).isTrue();
        assertThat(weather.getLastHeartbeatPassed()).isTrue();
        assertThat(weather.getLastHeartbeatMessage()).isEqualTo("天气服务连通正常");
        assertThat(weather.getLastHeartbeatLatencyMs()).isEqualTo(96);
        assertThat(status.services())
                .anySatisfy(item -> {
                    if ("weather".equals(item.serviceKey())) {
                        assertThat(item.available()).isTrue();
                    }
                });
    }

    @Test
    void runHeartbeatChecks_shouldMarkDisabledServiceUnavailableInPublicStatus() {
        serviceManagementService.initializeDefaults();
        ManagedExternalServiceConfig mapConfig = store.get("map");
        mapConfig.setEnabled(false);
        repository.save(mapConfig);

        serviceManagementService.runHeartbeatChecks();
        PublicServiceStatusResponse status = serviceManagementService.getPublicServiceStatus();

        assertThat(store.get("map").getLastHeartbeatPassed()).isFalse();
        assertThat(store.get("map").getLastHeartbeatMessage()).isEqualTo("管理员已关闭该服务");
        assertThat(status.services())
                .anySatisfy(item -> {
                    if ("map".equals(item.serviceKey())) {
                        assertThat(item.available()).isFalse();
                        assertThat(item.message()).contains("关闭");
                    }
                });
    }

    @Test
    void runHeartbeatChecks_shouldRetryTransientProbeFailure() {
        serviceManagementService.initializeDefaults();
        when(probeService.probeWeather(any(ServiceManagementService.WeatherRuntimeConfig.class)))
                .thenReturn(
                        ExternalServiceProbeService.ProbeResult.failure("天气服务临时失败", 30),
                        ExternalServiceProbeService.ProbeResult.failure("天气服务临时失败", 40),
                        ExternalServiceProbeService.ProbeResult.success("天气服务连通正常", 120)
                );

        serviceManagementService.runHeartbeatChecks();

        verify(probeService, times(3)).probeWeather(any(ServiceManagementService.WeatherRuntimeConfig.class));
        ManagedExternalServiceConfig weather = store.get("weather");
        assertThat(weather.getLastHeartbeatPassed()).isTrue();
        assertThat(weather.getLastHeartbeatMessage()).isEqualTo("天气服务连通正常");
        assertThat(weather.getLastHeartbeatLatencyMs()).isEqualTo(120);
    }

    @Test
    void runStartupHeartbeatIfMissing_shouldRetryFailedHeartbeatOlderThan24Hours() {
        serviceManagementService.initializeDefaults();
        Instant staleHeartbeat = Instant.now().minus(Duration.ofHours(25));
        ManagedExternalServiceConfig weatherConfig = store.get("weather");
        weatherConfig.setLastHeartbeatAt(staleHeartbeat);
        weatherConfig.setLastHeartbeatPassed(false);
        weatherConfig.setLastHeartbeatMessage("上次天气探测失败");
        repository.save(weatherConfig);

        ManagedExternalServiceConfig mapConfig = store.get("map");
        mapConfig.setLastHeartbeatAt(Instant.now());
        mapConfig.setLastHeartbeatPassed(true);
        mapConfig.setLastHeartbeatMessage("地图服务连通正常");
        repository.save(mapConfig);

        serviceManagementService.runStartupHeartbeatIfMissing();

        ManagedExternalServiceConfig retriedWeather = store.get("weather");
        assertThat(retriedWeather.getLastHeartbeatPassed()).isTrue();
        assertThat(retriedWeather.getLastHeartbeatMessage()).isEqualTo("天气服务连通正常");
        assertThat(retriedWeather.getLastHeartbeatAt()).isAfter(staleHeartbeat);
    }

    @Test
    void runStartupHeartbeatIfMissing_shouldKeepRecentFailedHeartbeatUntilRetryIntervalPasses() {
        serviceManagementService.initializeDefaults();
        Instant recentFailure = Instant.now().minus(Duration.ofHours(23));
        ManagedExternalServiceConfig weatherConfig = store.get("weather");
        weatherConfig.setLastHeartbeatAt(recentFailure);
        weatherConfig.setLastHeartbeatPassed(false);
        weatherConfig.setLastHeartbeatMessage("上次天气探测失败");
        repository.save(weatherConfig);

        Instant recentSuccess = Instant.now();
        ManagedExternalServiceConfig mapConfig = store.get("map");
        mapConfig.setLastHeartbeatAt(recentSuccess);
        mapConfig.setLastHeartbeatPassed(true);
        mapConfig.setLastHeartbeatMessage("地图服务连通正常");
        repository.save(mapConfig);

        serviceManagementService.runStartupHeartbeatIfMissing();

        ManagedExternalServiceConfig weather = store.get("weather");
        assertThat(weather.getLastHeartbeatPassed()).isFalse();
        assertThat(weather.getLastHeartbeatMessage()).isEqualTo("上次天气探测失败");
        assertThat(weather.getLastHeartbeatAt()).isEqualTo(recentFailure);
    }

    @Test
    void getPublicServiceStatus_shouldHideWeatherFailureDetailsFromUsers() {
        serviceManagementService.initializeDefaults();
        ManagedExternalServiceConfig weatherConfig = store.get("weather");
        weatherConfig.setLastHeartbeatPassed(false);
        weatherConfig.setLastHeartbeatMessage(
                "I/O error on GET request for \"https://api.open-meteo.com/v1/forecast\": Remote host terminated the handshake"
        );
        repository.save(weatherConfig);

        PublicServiceStatusResponse status = serviceManagementService.getPublicServiceStatus();

        assertThat(status.services())
                .anySatisfy(item -> {
                    if ("weather".equals(item.serviceKey())) {
                        assertThat(item.available()).isFalse();
                        assertThat(item.message()).isEqualTo("天气服务暂时不可用");
                    }
                });
    }
}
