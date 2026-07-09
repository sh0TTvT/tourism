package com.tourismqa.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.tourismqa.dto.KgContextResponse;
import com.tourismqa.dto.KgNodeResponse;
import com.tourismqa.dto.UserLocationDto;
import com.tourismqa.dto.WeatherForecastItemResponse;
import com.tourismqa.dto.WeatherForecastResponse;
import com.tourismqa.exception.ApiException;

/**
 * 天气上下文增强服务。
 * 使用场景：
 * 在旅游问答中补充目的地近期天气，帮助模型给出更可执行的出行建议。
 * 核心职责：
 * 1. 从用户问题与图谱实体中识别目的地与日期。
 * 2. 调用天气预报接口获取近几日天气摘要。
 * 3. 将结果整理为可注入大模型的系统上下文文本。
 */
@Service
public class WeatherContextService {

    private static final Pattern FULL_DATE_PATTERN =
            Pattern.compile("(20\\d{2})[-/.年](\\d{1,2})[-/.月](\\d{1,2})日?");
    private static final Pattern MONTH_DAY_PATTERN =
            Pattern.compile("(\\d{1,2})月(\\d{1,2})日");
    private static final Pattern EXPLICIT_WEATHER_LOCATION_PATTERN =
            Pattern.compile("([\\p{IsHan}A-Za-z]{2,30}?)(?:的)?(?:今天|今日|现在|当前|目前|明天|后天|大后天|周末|这周|本周|未来几天|未来一周)?(?:天气|气温|温度|会不会下雨|会下雨|下雨|降雨|冷不冷|热不热|穿什么)");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String WEATHER_SOURCE = "Open-Meteo Forecast API";
    private static final String WEATHER_UNAVAILABLE_PROMPT =
            "实时天气补充：天气服务暂时不可用，本轮无法查询真实天气。请明确告知用户当前拿不到实时天气数据，"
                    + "不能编造任何天气、温度、降雨概率或天气趋势；仅可建议用户稍后重试或查看官方天气服务。";
    private static final String WEATHER_UNAVAILABLE_DIRECT_ANSWER = """
            当前天气服务暂时不可用，我不能提供真实天气数据，也不能编造天气、温度、降雨概率或天气趋势。
            建议你稍后重试，或直接查看官方天气服务获取最新信息。
            """.trim();

    private final SimpleClientHttpRequestFactory requestFactory;
    private final GeocodingService geocodingService;
    private final ServiceManagementService serviceManagementService;

    public WeatherContextService(SimpleClientHttpRequestFactory requestFactory,
                                 GeocodingService geocodingService,
                                 ServiceManagementService serviceManagementService) {
        this.requestFactory = requestFactory;
        this.geocodingService = geocodingService;
        this.serviceManagementService = serviceManagementService;
    }

    public WeatherForecastResponse queryForecast(String locationName, LocalDate targetDate, Integer requestedDays) {
        ServiceManagementService.WeatherRuntimeConfig config = weatherConfig();
        if (!config.enabled()) {
            throw new ApiException(503, "天气查询功能当前未启用");
        }
        if (!StringUtils.hasText(locationName)) {
            throw new ApiException(400, "地点不能为空");
        }

        ResolvedLocation location = geocodeText(locationName);
        if (location == null) {
            throw new ApiException(404, "未找到地点对应的经纬度，请换一个更具体的城市或景点名称");
        }

        return queryForecast(config, location, targetDate, requestedDays);
    }

    public WeatherForecastResponse queryForecastByCoordinates(String locationName,
                                                              Double latitude,
                                                              Double longitude,
                                                              LocalDate targetDate,
                                                              Integer requestedDays) {
        ServiceManagementService.WeatherRuntimeConfig config = weatherConfig();
        if (!config.enabled()) {
            throw new ApiException(503, "天气查询功能当前未启用");
        }
        ResolvedLocation location = resolveUserLocation(new UserLocationDto(
                latitude,
                longitude,
                null,
                StringUtils.hasText(locationName) ? locationName.trim() : "当前位置",
                null
        ));
        if (location == null) {
            throw new ApiException(400, "定位坐标无效");
        }
        return queryForecast(config, location, targetDate, requestedDays);
    }

    private WeatherForecastResponse queryForecast(ServiceManagementService.WeatherRuntimeConfig config,
                                                  ResolvedLocation location,
                                                  LocalDate targetDate,
                                                  Integer requestedDays) {
        int forecastDays = resolveForecastDays(config, targetDate, requestedDays);
        try {
            Map<String, Object> response = fetchForecast(config, location, forecastDays);
            List<WeatherForecastItemResponse> forecasts = buildForecastItems(response, targetDate);
            if (forecasts.isEmpty()) {
                throw new ApiException(502, "天气服务未返回有效预报数据");
            }
            return new WeatherForecastResponse(
                    location.name(),
                    location.latitude(),
                    location.longitude(),
                    config.timezone(),
                    targetDate == null ? forecastDays : forecasts.size(),
                    targetDate,
                    "°C",
                    "%",
                    WEATHER_SOURCE,
                    forecasts
            );
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(503, "天气服务暂时不可用，请稍后重试");
        }
    }

    public DirectWeatherAnswer tryBuildDirectAnswer(String question, KgContextResponse graphContext) {
        return tryBuildDirectAnswer(question, graphContext, null);
    }

    public DirectWeatherAnswer tryBuildDirectAnswer(String question,
                                                    KgContextResponse graphContext,
                                                    UserLocationDto userLocation) {
        if (!isDirectWeatherQuestion(question)) {
            return null;
        }
        ServiceManagementService.WeatherRuntimeConfig config = weatherConfig();
        if (!config.enabled()) {
            return weatherUnavailableDirectAnswer();
        }

        ResolvedLocation location = resolveLocation(question, graphContext, userLocation);
        if (location == null) {
            return new DirectWeatherAnswer(
                    """
                    当前是实时天气查询，但我还不能稳定识别地点。
                    请直接提供更具体的城市或景点名称，例如“北京明天天气”或“上海迪士尼后天天气”。
                    """.trim()
            );
        }

        LocalDate targetDate = resolveTargetDate(question);
        try {
            WeatherForecastResponse forecast = queryForecast(config, location, targetDate, null);
            return new DirectWeatherAnswer(formatDirectWeatherAnswer(forecast));
        } catch (ApiException ex) {
            return weatherUnavailableDirectAnswer();
        }
    }

    public String buildPromptContext(String question, KgContextResponse graphContext) {
        return buildPromptContext(question, graphContext, null);
    }

    public String buildPromptContext(String question, KgContextResponse graphContext, UserLocationDto userLocation) {
        if (!StringUtils.hasText(question)) {
            return null;
        }

        boolean strongWeatherIntent = hasWeatherIntent(question);
        boolean tripPlanningIntent = hasTripPlanningIntent(question);
        if (!strongWeatherIntent && !tripPlanningIntent) {
            return null;
        }
        ServiceManagementService.WeatherRuntimeConfig config = weatherConfig();
        if (!config.enabled()) {
            return weatherUnavailablePromptContext();
        }

        ResolvedLocation location = resolveLocation(question, graphContext, userLocation);
        if (location == null) {
            return strongWeatherIntent
                    ? "实时天气补充：当前未能稳定识别目的地，请在回答中提醒用户补充城市或景点名称。"
                    : null;
        }

        LocalDate targetDate = resolveTargetDate(question);
        WeatherWindow window = resolveWindow(config, targetDate);
        if (window == null) {
            return "实时天气补充：用户询问日期超出当前天气预测窗口，请提示仅能参考近 "
                    + config.maxForecastDays()
                    + " 天天气，并建议临近出行时再次核验。";
        }

        try {
            Map<String, Object> response = fetchForecast(config, location, window.forecastDays());
            return buildPromptFromResponse(
                    config,
                    location.name(),
                    response,
                    window.targetDate(),
                    strongWeatherIntent || tripPlanningIntent
            );
        } catch (Exception ex) {
            return weatherUnavailablePromptContext();
        }
    }

    private Map<String, Object> fetchForecast(ServiceManagementService.WeatherRuntimeConfig config,
                                              ResolvedLocation location,
                                              int forecastDays) {
        return clientFor(config.baseUrl()).get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/forecast")
                        .queryParam("latitude", location.latitude())
                        .queryParam("longitude", location.longitude())
                        .queryParam("daily",
                                "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max")
                        .queryParam("timezone", config.timezone())
                        .queryParam("forecast_days", forecastDays)
                        .build())
                .retrieve()
                .body(Map.class);
    }

    private List<WeatherForecastItemResponse> buildForecastItems(Map<String, Object> response, LocalDate targetDate) {
        if (!(response.get("daily") instanceof Map<?, ?> daily)) {
            throw new ApiException(502, "天气服务返回结构异常");
        }

        List<String> dates = asStringList(daily.get("time"));
        List<Double> maxTemps = asDoubleList(daily.get("temperature_2m_max"));
        List<Double> minTemps = asDoubleList(daily.get("temperature_2m_min"));
        List<Double> rainProbabilities = asDoubleList(daily.get("precipitation_probability_max"));
        List<Integer> weatherCodes = asIntegerList(daily.get("weather_code"));
        if (dates.isEmpty()) {
            throw new ApiException(502, "天气服务未返回有效预报日期");
        }

        List<WeatherForecastItemResponse> forecasts = new ArrayList<>();
        for (int i = 0; i < dates.size(); i++) {
            LocalDate date = parseIsoDate(dates.get(i));
            if (date == null) {
                continue;
            }
            if (targetDate != null && !date.equals(targetDate)) {
                continue;
            }
            Integer weatherCode = getInt(weatherCodes, i);
            forecasts.add(new WeatherForecastItemResponse(
                    date,
                    weatherCode,
                    describeWeatherCode(weatherCode),
                    getDouble(minTemps, i),
                    getDouble(maxTemps, i),
                    getDouble(rainProbabilities, i)
            ));
        }

        if (targetDate != null && forecasts.isEmpty()) {
            throw new ApiException(502, "天气服务未返回 " + targetDate + " 的单日预报");
        }
        return forecasts;
    }

    private String buildPromptFromResponse(ServiceManagementService.WeatherRuntimeConfig config,
                                           String locationName,
                                           Map<String, Object> response,
                                           LocalDate targetDate,
                                           boolean weatherRelevantIntent) {
        if (!(response.get("daily") instanceof Map<?, ?> daily)) {
            return weatherRelevantIntent
                    ? weatherUnavailablePromptContext()
                    : null;
        }

        List<String> dates = asStringList(daily.get("time"));
        List<Double> maxTemps = asDoubleList(daily.get("temperature_2m_max"));
        List<Double> minTemps = asDoubleList(daily.get("temperature_2m_min"));
        List<Double> rainProbabilities = asDoubleList(daily.get("precipitation_probability_max"));
        List<Integer> weatherCodes = asIntegerList(daily.get("weather_code"));
        if (dates.isEmpty()) {
            return weatherRelevantIntent
                    ? weatherUnavailablePromptContext()
                    : null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("以下为系统补充的实时天气预报，仅在与用户问题直接相关时使用；天气属于预测，不要表述为绝对确定事实。\n");
        sb.append("目的地：").append(locationName).append("\n");
        sb.append("天气摘要：\n");

        boolean matchedTarget = false;
        int limit = targetDate == null
                ? Math.min(dates.size(), config.forecastDays())
                : dates.size();
        for (int i = 0; i < limit; i++) {
            LocalDate date = parseIsoDate(dates.get(i));
            if (date == null) {
                continue;
            }
            if (targetDate != null && !date.equals(targetDate)) {
                continue;
            }
            matchedTarget = true;
            sb.append("- ").append(date).append("：")
                    .append(describeWeatherCode(getInt(weatherCodes, i)))
                    .append("，")
                    .append(formatTemperature(getDouble(minTemps, i), getDouble(maxTemps, i)));
            Double rainProbability = getDouble(rainProbabilities, i);
            if (rainProbability != null) {
                sb.append("，降水概率约 ").append(Math.round(rainProbability)).append("%");
            }
            sb.append("\n");
        }

        if (targetDate != null && !matchedTarget) {
            sb.append("- 未命中 ").append(targetDate)
                    .append(" 的单日预报，请提示用户当前仅能参考近几日天气。\n");
        }
        sb.append("来源：Open-Meteo Forecast API。若用户需要精确到小时的安排，请提醒其临近出行前再次核验。");
        return sb.toString().trim();
    }

    private ResolvedLocation resolveLocation(String question, KgContextResponse graphContext) {
        return resolveLocation(question, graphContext, null);
    }

    private ResolvedLocation resolveLocation(String question,
                                             KgContextResponse graphContext,
                                             UserLocationDto userLocation) {
        String explicitLocation = extractExplicitWeatherLocation(question);
        if (StringUtils.hasText(explicitLocation)) {
            ResolvedLocation location = geocodeText(explicitLocation);
            if (location != null) {
                return location;
            }
        }

        ResolvedLocation graphLocation = resolveLocationFromGraph(graphContext);
        if (graphLocation != null) {
            return graphLocation;
        }

        String extracted = extractLocationByKeyword(question);
        if (StringUtils.hasText(extracted)) {
            return geocodeText(extracted);
        }
        if (hasCurrentLocationIntent(question) || hasWeatherIntent(question)) {
            return resolveUserLocation(userLocation);
        }
        return null;
    }

    private String extractExplicitWeatherLocation(String question) {
        if (!StringUtils.hasText(question)) {
            return null;
        }
        Matcher matcher = EXPLICIT_WEATHER_LOCATION_PATTERN.matcher(question.trim());
        while (matcher.find()) {
            String candidate = normalizeExplicitLocationCandidate(matcher.group(1));
            if (StringUtils.hasText(candidate) && !hasCurrentLocationIntent(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private String normalizeExplicitLocationCandidate(String candidate) {
        if (!StringUtils.hasText(candidate)) {
            return null;
        }
        String normalized = candidate.trim();
        normalized = normalized.replaceFirst("^(请问|麻烦|帮我|帮忙|查一下|查查|查询|看一下|看看|看下|我想知道|想知道|问一下|问问|想问)+", "");
        normalized = normalized.replaceFirst("^(今天|今日|现在|当前|目前|明天|后天|大后天|周末|这周|本周|未来几天|未来一周)+", "");
        normalized = normalized.replaceFirst("(今天|今日|现在|当前|目前|明天|后天|大后天|周末|这周|本周|未来几天|未来一周|的)+$", "");
        return normalized.trim();
    }

    private ResolvedLocation resolveUserLocation(UserLocationDto userLocation) {
        if (userLocation == null || userLocation.latitude() == null || userLocation.longitude() == null) {
            return null;
        }
        double latitude = userLocation.latitude();
        double longitude = userLocation.longitude();
        if (latitude < -90D || latitude > 90D || longitude < -180D || longitude > 180D) {
            return null;
        }
        String name = StringUtils.hasText(userLocation.label()) ? userLocation.label().trim() : "当前位置";
        return new ResolvedLocation(name, latitude, longitude);
    }

    private ResolvedLocation resolveLocationFromGraph(KgContextResponse graphContext) {
        if (graphContext == null || graphContext.nodes().isEmpty()) {
            return null;
        }
        for (KgNodeResponse node : graphContext.nodes()) {
            if (isSourceNode(node)) {
                continue;
            }
            Double latitude = readDouble(node.attributes().get("latitude"));
            Double longitude = readDouble(node.attributes().get("longitude"));
            if (latitude != null && longitude != null) {
                return new ResolvedLocation(node.name(), latitude, longitude);
            }
        }
        for (KgNodeResponse node : graphContext.nodes()) {
            if (isSourceNode(node)) {
                continue;
            }
            ResolvedLocation location = geocodeText(node.name());
            if (location != null) {
                return location;
            }
        }
        return null;
    }

    private boolean isSourceNode(KgNodeResponse node) {
        String entityType = asText(node.attributes().get("entityType"));
        if ("web_source".equalsIgnoreCase(entityType)) {
            return true;
        }
        return StringUtils.hasText(node.category()) && node.category().contains("来源");
    }

    private ResolvedLocation geocodeText(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        double[] coordinates = geocodingService.geocode(text.trim());
        if (coordinates == null || coordinates.length < 2) {
            return null;
        }
        return new ResolvedLocation(text.trim(), coordinates[0], coordinates[1]);
    }

    private String extractLocationByKeyword(String question) {
        String[] markers = {"去", "到", "在", "飞", "玩", "旅行", "旅游", "出发去"};
        for (String marker : markers) {
            int index = question.indexOf(marker);
            if (index >= 0) {
                String tail = question.substring(index + marker.length()).trim();
                String candidate = readLeadingPlaceText(tail);
                if (StringUtils.hasText(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private String readLeadingPlaceText(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isWhitespace(ch) || "，。！？,.?!；;的了呢吗呀和或".indexOf(ch) >= 0) {
                break;
            }
            sb.append(ch);
            if (sb.length() >= 20) {
                break;
            }
        }
        return sb.toString().trim();
    }

    private WeatherWindow resolveWindow(ServiceManagementService.WeatherRuntimeConfig config, LocalDate targetDate) {
        int defaultDays = Math.max(1, config.forecastDays());
        int maxDays = Math.max(defaultDays, config.maxForecastDays());
        if (targetDate == null) {
            return new WeatherWindow(null, defaultDays);
        }
        long daysBetween = ChronoUnit.DAYS.between(currentDate(config), targetDate);
        if (daysBetween < 0 || daysBetween >= maxDays) {
            return null;
        }
        return new WeatherWindow(targetDate, (int) daysBetween + 1);
    }

    private int resolveForecastDays(ServiceManagementService.WeatherRuntimeConfig config,
                                    LocalDate targetDate,
                                    Integer requestedDays) {
        int defaultDays = Math.max(1, config.forecastDays());
        int maxDays = Math.max(defaultDays, config.maxForecastDays());
        LocalDate today = LocalDate.now(resolveZoneId(config));
        if (targetDate != null) {
            long daysBetween = ChronoUnit.DAYS.between(today, targetDate);
            if (daysBetween < 0 || daysBetween >= maxDays) {
                LocalDate latestSupportedDate = today.plusDays(maxDays - 1L);
                throw new ApiException(
                        400,
                        "仅支持查询 " + today + " 到 " + latestSupportedDate + " 之间的天气，不能查询 " + targetDate
                );
            }
            return (int) daysBetween + 1;
        }

        int days = requestedDays == null ? defaultDays : requestedDays;
        if (days > maxDays) {
            throw new ApiException(400, "查询天数不能超过 " + maxDays + " 天");
        }
        return days;
    }

    private LocalDate resolveTargetDate(String question) {
        LocalDate today = LocalDate.now(resolveZoneId(weatherConfig()));
        if (question.contains("大后天")) {
            return today.plusDays(3);
        }
        if (question.contains("后天")) {
            return today.plusDays(2);
        }
        if (question.contains("明天")) {
            return today.plusDays(1);
        }
        if (question.contains("今天") || question.contains("今日")) {
            return today;
        }

        Matcher fullDateMatcher = FULL_DATE_PATTERN.matcher(question);
        if (fullDateMatcher.find()) {
            try {
                return LocalDate.of(
                        Integer.parseInt(fullDateMatcher.group(1)),
                        Integer.parseInt(fullDateMatcher.group(2)),
                        Integer.parseInt(fullDateMatcher.group(3))
                );
            } catch (Exception ignored) {
                return null;
            }
        }

        Matcher monthDayMatcher = MONTH_DAY_PATTERN.matcher(question);
        if (monthDayMatcher.find()) {
            try {
                LocalDate candidate = LocalDate.of(
                        today.getYear(),
                        Integer.parseInt(monthDayMatcher.group(1)),
                        Integer.parseInt(monthDayMatcher.group(2))
                );
                if (candidate.isBefore(today)) {
                    return candidate.plusYears(1);
                }
                return candidate;
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private boolean hasWeatherIntent(String question) {
        String lower = question.toLowerCase(Locale.ROOT);
        String[] keywords = {"天气", "气温", "温度", "下雨", "降雨", "穿什么", "冷不冷", "热不热"};
        for (String keyword : keywords) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCurrentLocationIntent(String question) {
        String lower = question.toLowerCase(Locale.ROOT);
        String[] keywords = {"附近", "周边", "当前位置", "我这里", "这边", "本地", "当地", "nearby", "around me"};
        for (String keyword : keywords) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDirectWeatherQuestion(String question) {
        if (!StringUtils.hasText(question) || !hasWeatherIntent(question)) {
            return false;
        }
        String lower = question.toLowerCase(Locale.ROOT);
        String[] complexKeywords = {
                "路线", "行程", "攻略", "景点", "酒店", "住宿", "美食", "预算", "签证",
                "机票", "高铁", "火车", "自驾", "安排", "推荐", "规划", "怎么玩"
        };
        for (String keyword : complexKeywords) {
            if (lower.contains(keyword)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasTripPlanningIntent(String question) {
        String lower = question.toLowerCase(Locale.ROOT);
        String[] keywords = {"行程", "路线", "怎么玩", "安排", "攻略", "出发", "旅行", "旅游", "几天"};
        String[] timeKeywords = {"今天", "明天", "后天", "这周", "周末", "月", "日", "号"};
        boolean hasPlanning = false;
        for (String keyword : keywords) {
            if (lower.contains(keyword)) {
                hasPlanning = true;
                break;
            }
        }
        if (!hasPlanning) {
            return false;
        }
        for (String keyword : timeKeywords) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String describeWeatherCode(Integer code) {
        if (code == null) {
            return "天气未知";
        }
        return switch (code) {
            case 0 -> "晴";
            case 1, 2 -> "晴到多云";
            case 3 -> "阴";
            case 45, 48 -> "雾";
            case 51, 53, 55 -> "毛毛雨";
            case 56, 57 -> "冻毛毛雨";
            case 61, 63, 65 -> "雨";
            case 66, 67 -> "冻雨";
            case 71, 73, 75, 77 -> "雪";
            case 80, 81, 82 -> "阵雨";
            case 85, 86 -> "阵雪";
            case 95 -> "雷暴";
            case 96, 99 -> "强雷暴";
            default -> "天气代码 " + code;
        };
    }

    private String formatTemperature(Double minTemp, Double maxTemp) {
        if (minTemp == null && maxTemp == null) {
            return "气温未知";
        }
        if (minTemp == null) {
            return "最高约 " + Math.round(maxTemp) + "°C";
        }
        if (maxTemp == null) {
            return "最低约 " + Math.round(minTemp) + "°C";
        }
        return Math.round(minTemp) + "-" + Math.round(maxTemp) + "°C";
    }

    private List<String> asStringList(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
        }
        return result;
    }

    private List<Double> asDoubleList(Object value) {
        List<Double> result = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                Double parsed = readDouble(item);
                result.add(parsed);
            }
        }
        return result;
    }

    private List<Integer> asIntegerList(Object value) {
        List<Integer> result = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                result.add(readInteger(item));
            }
        }
        return result;
    }

    private Double readDouble(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer readInteger(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Double getDouble(List<Double> values, int index) {
        return index >= 0 && index < values.size() ? values.get(index) : null;
    }

    private Integer getInt(List<Integer> values, int index) {
        return index >= 0 && index < values.size() ? values.get(index) : null;
    }

    private LocalDate parseIsoDate(String value) {
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String asText(Object value) {
        return value == null ? null : value.toString();
    }

    private ZoneId resolveZoneId(ServiceManagementService.WeatherRuntimeConfig config) {
        try {
            return ZoneId.of(config.timezone());
        } catch (Exception ex) {
            return ZoneId.systemDefault();
        }
    }

    private RestClient clientFor(String baseUrl) {
        return RestClient.builder().requestFactory(requestFactory).baseUrl(baseUrl).build();
    }

    private ServiceManagementService.WeatherRuntimeConfig weatherConfig() {
        return serviceManagementService.getWeatherRuntimeConfig();
    }

    private String weatherUnavailablePromptContext() {
        return WEATHER_UNAVAILABLE_PROMPT;
    }

    private DirectWeatherAnswer weatherUnavailableDirectAnswer() {
        return new DirectWeatherAnswer(WEATHER_UNAVAILABLE_DIRECT_ANSWER);
    }

    private LocalDate currentDate(ServiceManagementService.WeatherRuntimeConfig config) {
        return LocalDate.now(resolveZoneId(config));
    }

    private String formatDirectWeatherAnswer(WeatherForecastResponse forecast) {
        StringBuilder sb = new StringBuilder();
        sb.append("以下结果来自真实天气接口，不是模型臆测。\n");
        sb.append("地点：").append(forecast.location())
                .append("（").append(roundCoordinate(forecast.latitude()))
                .append(", ").append(roundCoordinate(forecast.longitude()))
                .append("）\n");
        sb.append("时区：").append(forecast.timezone()).append("\n");
        sb.append("数据源：").append(forecast.source()).append("\n");
        sb.append("说明：天气属于预报，数据可能随上游更新而变化，请以临近出行时的最新结果为准。\n");
        sb.append("预报：\n");
        for (WeatherForecastItemResponse item : forecast.forecasts()) {
            sb.append("- ").append(item.date()).append("：")
                    .append(item.weatherDescription()).append("，")
                    .append(formatTemperature(item.minTemperature(), item.maxTemperature()));
            if (item.precipitationProbabilityMax() != null) {
                sb.append("，降水概率约 ").append(Math.round(item.precipitationProbabilityMax())).append("%");
            }
            sb.append("\n");
        }
        String advisory = buildWeatherAdvisory(forecast.forecasts());
        if (StringUtils.hasText(advisory)) {
            sb.append("建议：").append(advisory).append("\n");
        }
        sb.append("如果你需要，我可以继续基于这份真实天气结果给出穿衣或出行建议。");
        return sb.toString().trim();
    }

    private String buildWeatherAdvisory(List<WeatherForecastItemResponse> forecasts) {
        if (forecasts == null || forecasts.isEmpty()) {
            return null;
        }
        double minMinTemp = Double.POSITIVE_INFINITY;
        double maxMaxTemp = Double.NEGATIVE_INFINITY;
        double maxRainProbability = 0D;
        for (WeatherForecastItemResponse item : forecasts) {
            if (item.minTemperature() != null) {
                minMinTemp = Math.min(minMinTemp, item.minTemperature());
            }
            if (item.maxTemperature() != null) {
                maxMaxTemp = Math.max(maxMaxTemp, item.maxTemperature());
            }
            if (item.precipitationProbabilityMax() != null) {
                maxRainProbability = Math.max(maxRainProbability, item.precipitationProbabilityMax());
            }
        }

        List<String> tips = new ArrayList<>();
        if (maxRainProbability >= 60) {
            tips.add("降水概率偏高，建议带伞");
        }
        if (minMinTemp != Double.POSITIVE_INFINITY && maxMaxTemp != Double.NEGATIVE_INFINITY
                && Math.round(maxMaxTemp - minMinTemp) >= 10) {
            tips.add("昼夜温差较大，建议分层穿搭");
        }
        if (maxMaxTemp != Double.NEGATIVE_INFINITY && maxMaxTemp >= 30) {
            tips.add("白天偏热，注意防晒和补水");
        }
        if (minMinTemp != Double.POSITIVE_INFINITY && minMinTemp <= 10) {
            tips.add("早晚偏凉，注意保暖");
        }
        return tips.isEmpty() ? null : String.join("；", tips) + "。";
    }

    private String roundCoordinate(Double value) {
        if (value == null) {
            return "?";
        }
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private record ResolvedLocation(String name, double latitude, double longitude) {
    }

    private record WeatherWindow(LocalDate targetDate, int forecastDays) {
    }

    public record DirectWeatherAnswer(String answer) {
    }
}
