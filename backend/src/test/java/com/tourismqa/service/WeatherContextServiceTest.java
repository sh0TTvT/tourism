package com.tourismqa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import com.tourismqa.dto.KgContextResponse;
import com.tourismqa.dto.KgNodeResponse;
import com.tourismqa.dto.UserLocationDto;

@ExtendWith(MockitoExtension.class)
class WeatherContextServiceTest {

    @Mock
    private GeocodingService geocodingService;

    @Mock
    private ServiceManagementService serviceManagementService;

    private WeatherContextService weatherContextService;

    @BeforeEach
    void setUp() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(200);
        requestFactory.setReadTimeout(200);
        weatherContextService = new WeatherContextService(requestFactory, geocodingService, serviceManagementService);
    }

    @Test
    void tryBuildDirectAnswer_shouldReturnUnavailableNoticeWhenServiceDisabled() {
        when(serviceManagementService.getWeatherRuntimeConfig()).thenReturn(
                new ServiceManagementService.WeatherRuntimeConfig(
                        false,
                        "https://api.open-meteo.com",
                        "Asia/Shanghai",
                        3,
                        7
                )
        );

        WeatherContextService.DirectWeatherAnswer answer = weatherContextService.tryBuildDirectAnswer(
                "北京天气怎么样",
                emptyGraphContext()
        );

        assertThat(answer).isNotNull();
        assertThat(answer.answer()).contains("天气服务暂时不可用");
        assertThat(answer.answer()).contains("不能编造天气");
    }

    @Test
    void tryBuildDirectAnswer_shouldReturnUnavailableNoticeWhenForecastRequestFails() {
        when(serviceManagementService.getWeatherRuntimeConfig()).thenReturn(unavailableWeatherConfig());

        WeatherContextService.DirectWeatherAnswer answer = weatherContextService.tryBuildDirectAnswer(
                "北京天气怎么样",
                graphContextWithLocation()
        );

        assertThat(answer).isNotNull();
        assertThat(answer.answer()).contains("天气服务暂时不可用");
        assertThat(answer.answer()).contains("不能编造天气");
    }

    @Test
    void buildPromptContext_shouldReturnStrictUnavailablePromptWhenForecastRequestFails() {
        when(serviceManagementService.getWeatherRuntimeConfig()).thenReturn(unavailableWeatherConfig());

        String promptContext = weatherContextService.buildPromptContext(
                "北京周末怎么玩",
                graphContextWithLocation()
        );

        assertThat(promptContext).contains("天气服务暂时不可用");
        assertThat(promptContext).contains("不能编造任何天气、温度、降雨概率或天气趋势");
    }

    @Test
    void tryBuildDirectAnswer_shouldPreferQuestionLocationOverUserLocation() {
        when(serviceManagementService.getWeatherRuntimeConfig()).thenReturn(unavailableWeatherConfig());
        when(geocodingService.geocode("北京市")).thenReturn(new double[] {39.9042d, 116.4074d});

        WeatherContextService.DirectWeatherAnswer answer = weatherContextService.tryBuildDirectAnswer(
                "北京市的天气怎么样",
                emptyGraphContext(),
                new UserLocationDto(30.2741d, 120.1551d, null, "杭州", null)
        );

        assertThat(answer).isNotNull();
        verify(geocodingService).geocode("北京市");
        verify(geocodingService, never()).geocode("北京市的");
    }

    private ServiceManagementService.WeatherRuntimeConfig unavailableWeatherConfig() {
        return new ServiceManagementService.WeatherRuntimeConfig(
                true,
                "http://127.0.0.1:1",
                "Asia/Shanghai",
                3,
                7
        );
    }

    private KgContextResponse emptyGraphContext() {
        return new KgContextResponse(null, List.of(), List.of(), List.of());
    }

    private KgContextResponse graphContextWithLocation() {
        return new KgContextResponse(
                null,
                List.of(new KgNodeResponse(
                        1L,
                        "北京",
                        "城市",
                        null,
                        List.of(),
                        List.of(),
                        Map.of("latitude", 39.9042d, "longitude", 116.4074d),
                        null,
                        null
                )),
                List.of(),
                List.of()
        );
    }
}
