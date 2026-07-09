package com.tourismqa.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tourismqa.dto.WeatherForecastResponse;
import com.tourismqa.exception.ApiException;
import com.tourismqa.service.WeatherContextService;

import jakarta.validation.constraints.Min;

/**
 * 天气查询控制器。
 * 使用场景：
 * 为前端提供目的地天气查询接口，支持按地点获取未来几日预报或指定日期预报。
 * 核心职责：
 * 1. 接收天气查询参数并完成基础校验。
 * 2. 调用天气服务获取结构化天气结果。
 * 3. 输出稳定的 JSON 响应供前端展示。
 */
@Validated
@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherContextService weatherContextService;

    public WeatherController(WeatherContextService weatherContextService) {
        this.weatherContextService = weatherContextService;
    }

    @GetMapping("/forecast")
    public WeatherForecastResponse forecast(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @Min(value = 1, message = "查询天数至少为1") Integer days) {
        if (latitude != null && longitude != null) {
            return weatherContextService.queryForecastByCoordinates(location, latitude, longitude, date, days);
        }
        if (location == null || location.isBlank()) {
            throw new ApiException(400, "地点不能为空");
        }
        return weatherContextService.queryForecast(location, date, days);
    }

    @GetMapping("/forecast/coordinates")
    public WeatherForecastResponse forecastByCoordinates(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(required = false, defaultValue = "当前位置") String location,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @Min(value = 1, message = "查询天数至少为1") Integer days) {
        return weatherContextService.queryForecastByCoordinates(location, latitude, longitude, date, days);
    }
}
