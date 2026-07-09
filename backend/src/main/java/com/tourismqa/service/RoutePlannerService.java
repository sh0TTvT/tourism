package com.tourismqa.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourismqa.dto.ChatMessageDto;
import com.tourismqa.dto.CreateRoutePlanRequest;
import com.tourismqa.dto.ExtractRouteDraftRequest;
import com.tourismqa.dto.ExtractRouteDraftResponse;
import com.tourismqa.dto.ExtractedPointDto;
import com.tourismqa.dto.GeocodeRoutePointsRequest;
import com.tourismqa.dto.KgNodeResponse;
import com.tourismqa.dto.RoutePlanItemResponse;
import com.tourismqa.dto.RoutePlanRequest;
import com.tourismqa.dto.RoutePlanResponse;
import com.tourismqa.dto.RoutePointDto;
import com.tourismqa.dto.RoutePointSaveRequest;
import com.tourismqa.dto.UpdateRoutePlanRequest;
import com.tourismqa.entity.ChatConversation;
import com.tourismqa.entity.RoutePlan;
import com.tourismqa.entity.RoutePoint;
import com.tourismqa.entity.UserAccount;
import com.tourismqa.entity.UserRole;
import com.tourismqa.exception.ApiException;
import com.tourismqa.repository.ChatConversationRepository;
import com.tourismqa.repository.RoutePlanRepository;
import com.tourismqa.repository.RoutePointRepository;
import com.tourismqa.repository.UserAccountRepository;
import com.tourismqa.security.UserPrincipal;

/**
 * 旅游路线规划服务。
 * 使用场景：
 * 负责路线生成、路线历史查询与路线编辑保存。
 * 核心职责：
 * 1. 调用大模型生成初始路线并持久化。
 * 2. 查询当前用户的路线历史与详情。
 * 3. 接收用户编辑后的路线并同步更新点位数据。
 */
@Service
public class RoutePlannerService {

    private static final Logger log = LoggerFactory.getLogger(RoutePlannerService.class);

    private static final String ROUTE_SYSTEM_PROMPT = """
            你是专业旅游路线规划师。请输出严格 JSON，不要输出 markdown，不要输出任何额外解释。
            JSON 结构：
            {
              "title": "字符串",
              "summary": "字符串",
              "points": [
                {"day":1,"order":1,"name":"景点/地点","description":"安排说明"}
              ],
              "tips": ["字符串"]
            }

            【重要】points 中的 name 字段要求：
            1. 必须使用具体的、可在地图上搜索到的地点名称，如"故宫博物院"、"天坛公园"、"南锣鼓巷"、"八达岭长城"
            2. 禁止使用模糊的区域描述，如"老城区"、"市中心"、"美食街"、"商业区"、"附近"、"周边"
            3. 优先使用知识图谱中提供的景点名称
            4. 使用官方全称或通用名称，确保可以被地理编码服务识别
            5. 示例对比：
               ✓ 正确："故宫博物院"、"八达岭长城"、"南锣鼓巷"、"三里屯太古里"
               ✗ 错误："老城区"、"市中心"、"长城"（太宽泛）、"美食街"

            points 需要按多日行程覆盖，每天 3-5 个点，描述简短可执行。
            tips 至少给 3 条，优先覆盖天气变化、节假日拥堵、预约和临时闭馆等提醒；不确定时明确提示用户核验官方最新公告。
            """;

    private final LlmRouterService llmRouterService;
    private final ModelCatalogService modelCatalogService;
    private final GeocodingService geocodingService;
    private final ObjectMapper objectMapper;
    private final UserAccountRepository userAccountRepository;
    private final ChatConversationRepository chatConversationRepository;
    private final RoutePlanRepository routePlanRepository;
    private final RoutePointRepository routePointRepository;
    private final KnowledgeGraphService knowledgeGraphService;

    private record TravelWindow(LocalDate startDate, LocalDate endDate, int days) {
    }

    private record DestinationContext(String raw, String queryPrefix) {
    }

    public RoutePlannerService(LlmRouterService llmRouterService,
                               ModelCatalogService modelCatalogService,
                               GeocodingService geocodingService,
                               ObjectMapper objectMapper,
                               UserAccountRepository userAccountRepository,
                               ChatConversationRepository chatConversationRepository,
                               RoutePlanRepository routePlanRepository,
                               RoutePointRepository routePointRepository,
                               KnowledgeGraphService knowledgeGraphService) {
        this.llmRouterService = llmRouterService;
        this.modelCatalogService = modelCatalogService;
        this.geocodingService = geocodingService;
        this.objectMapper = objectMapper;
        this.userAccountRepository = userAccountRepository;
        this.chatConversationRepository = chatConversationRepository;
        this.routePlanRepository = routePlanRepository;
        this.routePointRepository = routePointRepository;
        this.knowledgeGraphService = knowledgeGraphService;
    }

    @Transactional
    public RoutePlanResponse plan(RoutePlanRequest request, UserPrincipal principal) {
        UserAccount user = requireUser(principal);
        ChatConversation conversation = resolveConversation(request.conversationId(), user.getId());

        TravelWindow travelWindow = resolveTravelWindow(request.startDate(), request.endDate(), request.days());
        int days = travelWindow.days();
        String interests = firstNonBlank(request.interests(), user.getInterestTags());
        String budget = firstNonBlank(request.budget(), user.getBudgetPreference());
        String departure = firstNonBlank(request.departure(), user.getPreferredDeparture());

        LlmModelSelection selection = modelCatalogService.resolveModelSelection(request.provider(), request.model());
        String provider = selection.provider();
        String model = selection.modelId();

        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage("system", ROUTE_SYSTEM_PROMPT));

        String graphContext = knowledgeGraphService.buildLlmContextPrompt(
                request.destination() + " " + blankAsDefault(interests, "")
        );
        if (graphContext != null && !graphContext.isBlank()) {
            messages.add(new LlmMessage("system", graphContext));
        }

        String preferencePrompt = buildPreferencePrompt(user, interests, budget, departure);
        if (!preferencePrompt.isBlank()) {
            messages.add(new LlmMessage("system", preferencePrompt));
        }

        messages.add(new LlmMessage("user", buildPrompt(
                request.destination(),
                travelWindow,
                interests,
                budget,
                departure
        )));

        String raw;
        try {
            raw = llmRouterService.chatResolved(provider, model, messages, 0.3);
        } catch (ApiException ex) {
            raw = "{}";
        }

        Map<String, Object> root = parseJsonOrFallback(raw, request.destination(), days);
        String title = valueOrDefault(root.get("title"), request.destination() + " 旅行路线");
        String summary = valueOrDefault(root.get("summary"), "已为你生成行程路线，可在地图中查看。");
        List<RoutePointDto> points = extractPoints(root.get("points"), request.destination(), days);
        List<String> tips = extractTips(root.get("tips"));

        RoutePlan routePlan = new RoutePlan();
        routePlan.setUser(user);
        routePlan.setConversation(conversation);
        routePlan.setDestination(request.destination().trim());
        routePlan.setDays(days);
        routePlan.setStartDate(travelWindow.startDate());
        routePlan.setEndDate(travelWindow.endDate());
        routePlan.setInterests(blankToNull(interests));
        routePlan.setBudget(blankToNull(budget));
        routePlan.setDeparture(blankToNull(departure));
        routePlan.setProvider(provider);
        routePlan.setModel(model);
        routePlan.setTitle(title);
        routePlan.setSummary(summary);
        routePlan.setRawLlmOutput(raw);
        routePlan.setTipsJson(serializeTips(tips));
        RoutePlan savedPlan = routePlanRepository.save(routePlan);
        syncRoutePoints(savedPlan, points);
        return toResponse(savedPlan, points, tips);
    }

    @Transactional
    public RoutePlanResponse createPlan(CreateRoutePlanRequest request, UserPrincipal principal) {
        UserAccount user = requireUser(principal);
        ChatConversation conversation = request.conversationId() == null
                ? null
                : chatConversationRepository.findByIdAndUser_Id(request.conversationId(), user.getId())
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "会话不存在或无权限"));

        TravelWindow travelWindow = resolveTravelWindow(request.startDate(), request.endDate(), request.days());
        int days = travelWindow.days();

        LlmModelSelection selection = modelCatalogService.resolveModelSelection(null, null);

        RoutePlan routePlan = new RoutePlan();
        routePlan.setUser(user);
        routePlan.setConversation(conversation);
        routePlan.setDestination(request.destination().trim());
        routePlan.setDays(days);
        routePlan.setStartDate(travelWindow.startDate());
        routePlan.setEndDate(travelWindow.endDate());
        routePlan.setInterests(blankToNull(request.interests()));
        routePlan.setBudget(blankToNull(request.budget()));
        routePlan.setDeparture(blankToNull(request.departure()));
        routePlan.setProvider(selection.provider());
        routePlan.setModel(selection.modelId());
        routePlan.setTitle(request.title().trim());
        routePlan.setSummary(request.summary().trim());
        routePlan.setTipsJson(serializeTips(normalizeTips(request.tips())));

        RoutePlan savedPlan = routePlanRepository.save(routePlan);

        List<RoutePointDto> points = normalizePointsFromRequest(request.points(), request.destination());
        syncRoutePoints(savedPlan, points);
        return toResponse(savedPlan, points, deserializeTips(savedPlan.getTipsJson()));
    }

    private static final String EXTRACT_DRAFT_SYSTEM_PROMPT = """
            你是旅游意图提取助手。根据对话内容提取路线规划参数，输出严格JSON：
            {
              "destination": "目的地名称",
              "days": 天数整数,
              "startDate": "yyyy-mm-dd格式日期或null",
              "endDate": "yyyy-mm-dd格式日期或null",
              "interests": "兴趣标签",
              "budget": "经济型/舒适型/品质型",
              "departure": "出发地城市名称",
              "title": "路线标题",
              "summary": "路线摘要",
              "points": [
                {"day": 1, "order": 1, "name": "景点/地点名称", "description": "安排说明"}
              ],
              "tips": ["提示1", "提示2"]
            }
            规则：
            - 如果对话中助手已给出具体的逐日行程安排，提取完整 points、title、summary 和 tips
            - 如果对话仅有目的地和意向但未展开具体行程，points、title、summary 设为空数组/空字符串
            - 无法提取的字段设为 null（数字）、""（字符串）或 []（数组）
            仅输出 JSON，不要输出任何额外内容。
            """;

    @Transactional(readOnly = true)
    public ExtractRouteDraftResponse extractDraft(ExtractRouteDraftRequest request) {
        LlmModelSelection selection = modelCatalogService.resolveModelSelection(null, null);

        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage("system", EXTRACT_DRAFT_SYSTEM_PROMPT));

        StringBuilder conversationText = new StringBuilder("对话历史：\n");
        for (ChatMessageDto msg : request.messages()) {
            String roleLabel = "user".equals(msg.role()) ? "用户" : "助手";
            conversationText.append(roleLabel).append("：").append(msg.content()).append("\n");
        }
        messages.add(new LlmMessage("user", conversationText.toString()));

        String raw;
        try {
            raw = llmRouterService.chatResolved(
                    selection.provider(), selection.modelId(), messages, 0.1);
        } catch (ApiException ex) {
            return emptyExtractDraft();
        }

        Map<String, Object> parsed;
        try {
            String json = extractJson(raw);
            parsed = objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return emptyExtractDraft();
        }

        String destination = valueOrDefault(parsed.get("destination"), "");
        if (destination.length() > 18) {
            destination = "";
        }

        Integer days = null;
        Object daysObj = parsed.get("days");
        if (daysObj instanceof Number n) {
            days = Math.max(1, Math.min(n.intValue(), 14));
        }

        LocalDate startDate = null;
        Object startObj = parsed.get("startDate");
        if (startObj instanceof String s && !s.isBlank() && !"null".equals(s)) {
            try {
                startDate = LocalDate.parse(s);
            } catch (Exception ignored) {
            }
        }

        LocalDate endDate = null;
        Object endObj = parsed.get("endDate");
        if (endObj instanceof String s && !s.isBlank() && !"null".equals(s)) {
            try {
                endDate = LocalDate.parse(s);
            } catch (Exception ignored) {
            }
        }

        String interests = valueOrDefault(parsed.get("interests"), "");
        String budget = valueOrDefault(parsed.get("budget"), "");
        String departure = valueOrDefault(parsed.get("departure"), "");
        String title = valueOrDefault(parsed.get("title"), "");
        String summary = valueOrDefault(parsed.get("summary"), "");

        List<ExtractedPointDto> points = parseExtractedPoints(parsed.get("points"), destination);
        List<String> tips = extractTipsAsStrings(parsed.get("tips"));

        return new ExtractRouteDraftResponse(
                destination, days, startDate, endDate, interests, budget, departure,
                title, summary, points, tips);
    }

    private ExtractRouteDraftResponse emptyExtractDraft() {
        return new ExtractRouteDraftResponse(
                "", null, null, null, "", "", "", "", "", List.of(), List.of());
    }

    public List<RoutePointDto> geocodePoints(GeocodeRoutePointsRequest request) {
        return normalizePointsFromRequest(request.points(), request.destination());
    }

    private List<ExtractedPointDto> parseExtractedPoints(Object pointObj, String destination) {
        List<ExtractedPointDto> points = new ArrayList<>();
        if (!(pointObj instanceof List<?> arr)) {
            return points;
        }
        for (Object item : arr) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            int day = toInt(map.get("day"), 1);
            int order = toInt(map.get("order"), points.size() + 1);
            String name = normalizeRoutePointName(valueOrDefault(map.get("name"), ""));
            String desc = valueOrDefault(map.get("description"), "");
            if (!name.isBlank()) {
                double[] latLng = geocodeIfNeeded(name, destination, null, null);
                Double lat = latLng == null ? null : latLng[0];
                Double lon = latLng == null ? null : latLng[1];
                points.add(new ExtractedPointDto(day, order, name, desc, lat, lon));
            }
        }
        points.sort(Comparator.comparingInt(ExtractedPointDto::day)
                .thenComparingInt(ExtractedPointDto::order));
        return points;
    }

    private List<String> extractTipsAsStrings(Object tipsObj) {
        List<String> tips = new ArrayList<>();
        if (!(tipsObj instanceof List<?> arr)) {
            return tips;
        }
        for (Object tip : arr) {
            if (tip != null && !tip.toString().isBlank()) {
                tips.add(tip.toString().trim());
            }
        }
        return tips;
    }

    @Transactional(readOnly = true)
    public List<RoutePlanItemResponse> listPlans(UserPrincipal principal) {
        UserAccount user = requireUser(principal);
        return routePlanRepository.findByUser_Id(user.getId())
                .stream()
                .sorted(Comparator.comparing(this::sortTime).reversed())
                .map(routePlan -> new RoutePlanItemResponse(
                        routePlan.getId(),
                        routePlan.getConversation() == null ? null : routePlan.getConversation().getId(),
                        routePlan.getTitle(),
                        routePlan.getDestination(),
                        routePlan.getDays(),
                        routePlan.getStartDate(),
                        routePlan.getEndDate(),
                        routePlan.getSummary(),
                        routePlan.getCreatedAt(),
                        sortTime(routePlan)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public RoutePlanResponse getPlan(Long routePlanId, UserPrincipal principal) {
        UserAccount user = requireUser(principal);
        RoutePlan routePlan = routePlanRepository.findByIdAndUser_Id(routePlanId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "路线不存在或无权限"));
        List<RoutePointDto> points = loadPointDtos(routePlan.getId());
        return toResponse(routePlan, points, deserializeTips(routePlan.getTipsJson()));
    }

    @Transactional
    public RoutePlanResponse updatePlan(Long routePlanId,
                                        UpdateRoutePlanRequest request,
                                        UserPrincipal principal) {
        UserAccount user = requireUser(principal);
        RoutePlan routePlan = routePlanRepository.findByIdAndUser_Id(routePlanId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "路线不存在或无权限"));

        routePlan.setTitle(request.title().trim());
        routePlan.setSummary(request.summary().trim());
        routePlan.setDestination(request.destination().trim());
        TravelWindow travelWindow = resolveTravelWindow(request.startDate(), request.endDate(), request.days());
        routePlan.setDays(travelWindow.days());
        routePlan.setStartDate(travelWindow.startDate());
        routePlan.setEndDate(travelWindow.endDate());
        routePlan.setInterests(blankToNull(request.interests()));
        routePlan.setBudget(blankToNull(request.budget()));
        routePlan.setDeparture(blankToNull(request.departure()));
        routePlan.setTipsJson(serializeTips(normalizeTips(request.tips())));
        RoutePlan savedPlan = routePlanRepository.save(routePlan);

        List<RoutePointDto> points = normalizePointsFromRequest(request.points(), request.destination());
        syncRoutePoints(savedPlan, points);
        return toResponse(savedPlan, points, deserializeTips(savedPlan.getTipsJson()));
    }

    @Transactional
    public void deletePlan(Long routePlanId, UserPrincipal principal) {
        UserAccount user = requireUser(principal);
        RoutePlan routePlan = requireRoutePlan(routePlanId);
        ensureCanDeleteRoutePlan(routePlan, user);
        routePointRepository.deleteByRoutePlan_Id(routePlanId);
        routePlanRepository.delete(routePlan);
    }

    private UserAccount requireUser(UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED.value(), "请先登录后再使用路线功能");
        }
        return userAccountRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED.value(), "用户不存在"));
    }

    private RoutePlan requireRoutePlan(Long routePlanId) {
        return routePlanRepository.findById(routePlanId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "路线不存在"));
    }

    private void ensureCanDeleteRoutePlan(RoutePlan routePlan, UserAccount user) {
        if (user.getRole() == UserRole.ADMIN || user.getId().equals(routePlan.getUser().getId())) {
            return;
        }
        throw new ApiException(HttpStatus.FORBIDDEN.value(), "无权限删除该路线");
    }

    private RoutePlanResponse toResponse(RoutePlan routePlan,
                                         List<RoutePointDto> points,
                                         List<String> tips) {
        return new RoutePlanResponse(
                routePlan.getId(),
                routePlan.getConversation() == null ? null : routePlan.getConversation().getId(),
                routePlan.getTitle(),
                routePlan.getSummary(),
                routePlan.getDestination(),
                routePlan.getDays(),
                routePlan.getStartDate(),
                routePlan.getEndDate(),
                routePlan.getInterests(),
                routePlan.getBudget(),
                routePlan.getDeparture(),
                points,
                tips,
                routePlan.getCreatedAt(),
                routePlan.getUpdatedAt()
        );
    }

    private List<RoutePointDto> loadPointDtos(Long routePlanId) {
        return routePointRepository.findByRoutePlan_IdOrderByDayNoAscPointOrderAsc(routePlanId)
                .stream()
                .map(point -> new RoutePointDto(
                        point.getDayNo(),
                        point.getPointOrder(),
                        point.getName(),
                        point.getDescription(),
                        point.getLatitude(),
                        point.getLongitude()
                ))
                .toList();
    }

    private void syncRoutePoints(RoutePlan routePlan, List<RoutePointDto> points) {
        routePointRepository.deleteByRoutePlan_Id(routePlan.getId());
        List<RoutePoint> entities = new ArrayList<>();
        for (RoutePointDto point : points) {
            RoutePoint entity = new RoutePoint();
            entity.setRoutePlan(routePlan);
            entity.setDayNo(point.day());
            entity.setPointOrder(point.order());
            entity.setName(point.name());
            entity.setDescription(point.description());
            entity.setLatitude(point.latitude());
            entity.setLongitude(point.longitude());
            entities.add(entity);
        }
        routePointRepository.saveAll(entities);
    }

    private List<RoutePointDto> extractPoints(Object pointObj, String destination, int days) {
        List<RoutePointDto> points = new ArrayList<>();
        if (pointObj instanceof List<?> arr) {
            for (Object item : arr) {
                if (!(item instanceof Map<?, ?> map)) {
                    continue;
                }
                int day = toInt(map.get("day"), 1);
                int order = toInt(map.get("order"), points.size() + 1);
                String name = normalizeRoutePointName(valueOrDefault(map.get("name"), "未命名地点"));
                String desc = valueOrDefault(map.get("description"), "建议停留与打卡");
                if (name.isBlank()) {
                    continue;
                }

                double[] latLng = geocodeIfNeeded(name, destination, null, null);
                Double lat = latLng == null ? null : latLng[0];
                Double lon = latLng == null ? null : latLng[1];
                points.add(new RoutePointDto(day, order, name, desc, lat, lon));
            }
        }

        points.sort(Comparator.comparingInt(RoutePointDto::day).thenComparingInt(RoutePointDto::order));
        return points.isEmpty() ? fallbackPoints(destination, days) : points;
    }

    private List<RoutePointDto> normalizePointsFromRequest(List<RoutePointSaveRequest> requests, String destination) {
        List<RoutePointDto> points = new ArrayList<>();
        for (RoutePointSaveRequest item : requests) {
            String pointName = normalizeRoutePointName(item.name());
            if (pointName.isBlank()) {
                pointName = item.name().trim();
            }
            Double lat = item.latitude();
            Double lon = item.longitude();
            if (lat == null || lon == null) {
                double[] latLng = geocodeIfNeeded(pointName, destination, lat, lon);
                lat = latLng == null ? null : latLng[0];
                lon = latLng == null ? null : latLng[1];
            }
            points.add(new RoutePointDto(
                    item.day(),
                    item.order(),
                    pointName,
                    blankToNull(item.description()),
                    lat,
                    lon
            ));
        }
        return points.stream()
                .sorted(Comparator.comparingInt(RoutePointDto::day).thenComparingInt(RoutePointDto::order))
                .toList();
    }

    private double[] geocodeIfNeeded(String name, String destination, Double latitude, Double longitude) {
        log.info("[DEBUG] geocodeIfNeeded - name: {}, destination: {}", name, destination);

        if (latitude != null && longitude != null) {
            log.info("[DEBUG] 已有坐标，跳过地理编码");
            return new double[]{latitude, longitude};
        }

        String pointName = normalizeRoutePointName(name);
        log.info("[DEBUG] 规范化后的点位名称: {} -> {}", name, pointName);

        if (pointName.isBlank()) {
            pointName = normalizeGeocodeText(name);
        }
        if (pointName.isBlank()) {
            log.warn("[DEBUG] 点位名称为空，无法地理编码");
            return null;
        }

        double[] kgCoordinates = resolveCoordinatesFromKnowledgeGraph(pointName, destination);
        if (kgCoordinates != null) {
            log.info("[DEBUG] 从知识图谱获取坐标成功: [{}, {}]", kgCoordinates[0], kgCoordinates[1]);
            return kgCoordinates;
        }

        String query = buildGeocodeQuery(pointName, destination);
        log.info("[DEBUG] 调用 Nominatim API, query: {}", query);

        double[] result = geocodingService.geocode(query);
        if (result != null) {
            log.info("[DEBUG] Nominatim API 返回坐标: [{}, {}]", result[0], result[1]);
        } else {
            log.warn("[DEBUG] Nominatim API 返回 null");
        }

        return result;
    }

    private String normalizeRoutePointName(String value) {
        String text = value == null ? "" : value;
        text = text.replaceAll("\\[([^\\]]+)]\\([^)]+\\)", "$1")
                .replaceAll("[*_`~#>]", "")
                .replaceAll("（[^）]{0,30}）|\\([^)]{0,30}\\)", "")
                .replaceAll("^\\s*[-*•\\d.)、]+", "")
                .trim();
        text = text.replaceAll("^(用户|助手)\\s*[：:]", "")
                .replaceAll("^(上午|中午|下午|晚上|早上|傍晚|夜间|夜游|早餐|午餐|晚餐|第[一二两三四五六七八九十0-9]+站)\\s*[：:、-]?", "")
                .replaceAll("^(可以|可|建议|推荐|适合|优先|不妨|最好|还能|还可以)?\\s*(前往|抵达|游览|参观|打卡|逛|去|到|安排|体验|品尝|入住)\\s*", "")
                .replaceAll("^(可以|可|建议|推荐|适合|优先|不妨|最好|还能|还可以)\\s*", "")
                .trim();
        text = text.split("[，,。；;：:]")[0].trim();
        text = text.replaceAll("\\s+(附近|周边|区域|片区).*", "")
                .replaceAll("(进行)?(游览|参观|打卡|拍照|用餐|午餐|晚餐|早餐|自由活动|附近|周边|游玩|散步|漫步|闲逛|逛吃|吃美食|美食|看夜景|夜景|赏夜景|看展|购物|休息|停留).*$", "")
                .replaceAll("^(可以|可|建议|推荐|适合|前往|去|到)\\s*", "")
                .trim();
        if (text.matches("^(上午|中午|下午|晚上|早上|傍晚|夜间|自由活动|酒店|住宿|交通|高铁|飞机|地铁|公交|景点|地点|地方)$")) {
            return "";
        }
        return text.length() > 30 ? "" : text;
    }

    private double[] resolveCoordinatesFromKnowledgeGraph(String name, String destination) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            List<KgNodeResponse> nodes = knowledgeGraphService.searchNodes(name.trim(), 8);
            String destinationText = destination == null ? "" : destination.trim();
            for (KgNodeResponse node : nodes) {
                if (!isLikelySamePlace(node, name, destinationText)) {
                    continue;
                }
                Double lat = readCoordinate(node.attributes(), "latitude", "lat");
                Double lon = readCoordinate(node.attributes(), "longitude", "lon", "lng");
                if (lat != null && lon != null) {
                    return new double[]{lat, lon};
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private boolean isLikelySamePlace(KgNodeResponse node, String name, String destination) {
        String nodeName = node.name() == null ? "" : node.name();
        String normalizedName = name == null ? "" : name.trim();
        if (!nodeName.contains(normalizedName) && !normalizedName.contains(nodeName)) {
            boolean aliasMatched = node.aliases() != null && node.aliases().stream()
                    .anyMatch(alias -> alias != null
                            && (alias.contains(normalizedName) || normalizedName.contains(alias)));
            if (!aliasMatched) {
                return false;
            }
        }
        if (destination == null || destination.isBlank()) {
            return true;
        }
        String haystack = String.join(" ",
                nodeName,
                node.description() == null ? "" : node.description(),
                node.category() == null ? "" : node.category(),
                node.tags() == null ? "" : String.join(" ", node.tags()),
                node.attributes() == null ? "" : node.attributes().toString());
        return haystack.contains(destination) || destination.contains(nodeName) || destination.contains(normalizedName);
    }

    private Double readCoordinate(Map<String, Object> attributes, String... keys) {
        if (attributes == null) {
            return null;
        }
        for (String key : keys) {
            Object value = attributes.get(key);
            if (value == null) {
                continue;
            }
            try {
                return Double.parseDouble(value.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private String buildGeocodeQuery(String name, String destination) {
        String pointName = normalizeGeocodeText(name);
        DestinationContext context = buildDestinationContext(destination);
        if (pointName.isBlank()) {
            return context.queryPrefix();
        }
        if (context.queryPrefix().isBlank() || containsAdministrativeContext(pointName)) {
            return pointName;
        }
        if (pointName.contains(context.raw()) || context.raw().contains(pointName)) {
            return normalizeGeocodeText(context.queryPrefix() + " " + pointName);
        }
        return normalizeGeocodeText(context.queryPrefix() + " " + pointName);
    }

    private DestinationContext buildDestinationContext(String destination) {
        String raw = normalizeGeocodeText(destination);
        if (raw.isBlank()) {
            return new DestinationContext("", "");
        }
        String knownContext = knownDestinationContext(raw);
        if (!knownContext.isBlank()) {
            return new DestinationContext(raw, knownContext);
        }
        if (raw.contains("中国")) {
            return new DestinationContext(raw, raw);
        }
        if (containsChinese(raw)) {
            String suffix = containsAdministrativeContext(raw) ? raw : raw + "市";
            return new DestinationContext(raw, "中国 " + suffix);
        }
        return new DestinationContext(raw, raw);
    }

    private String knownDestinationContext(String raw) {
        String city = raw.replaceAll("(中国|省|市|特别行政区|自治区|壮族自治区|回族自治区|维吾尔自治区)", "");
        return switch (city) {
            case "北京" -> "中国 北京市";
            case "上海" -> "中国 上海市";
            case "天津" -> "中国 天津市";
            case "重庆" -> "中国 重庆市";
            case "杭州" -> "中国 浙江省 杭州市";
            case "南京" -> "中国 江苏省 南京市";
            case "苏州" -> "中国 江苏省 苏州市";
            case "广州" -> "中国 广东省 广州市";
            case "深圳" -> "中国 广东省 深圳市";
            case "成都" -> "中国 四川省 成都市";
            case "西安" -> "中国 陕西省 西安市";
            case "武汉" -> "中国 湖北省 武汉市";
            case "长沙" -> "中国 湖南省 长沙市";
            case "厦门" -> "中国 福建省 厦门市";
            case "青岛" -> "中国 山东省 青岛市";
            case "济南" -> "中国 山东省 济南市";
            case "大理" -> "中国 云南省 大理白族自治州";
            case "丽江" -> "中国 云南省 丽江市";
            case "桂林" -> "中国 广西壮族自治区 桂林市";
            case "三亚" -> "中国 海南省 三亚市";
            case "香港" -> "中国 香港特别行政区";
            case "澳门" -> "中国 澳门特别行政区";
            default -> "";
        };
    }

    private boolean containsAdministrativeContext(String text) {
        if (text == null) {
            return false;
        }
        // 排除"商业区"、"景区"、"园区"、"开发区"、"历史文化街区"等非行政区划的"区"和"街区"
        if (text.matches(".*(商业区|景区|园区|开发区|工业区|保税区|示范区|度假区|历史文化街区|商业街区|步行街|美食街|湿地公园|森林公园|地质公园|遗址公园).*")) {
            return false;
        }
        return text.matches(".*(中国|省|市|区|县|州|镇|乡|街道|特别行政区|自治区).*");
    }

    private boolean containsChinese(String text) {
        return text != null && text.matches(".*[\\u4e00-\\u9fa5].*");
    }

    private String normalizeGeocodeText(String text) {
        return text == null ? "" : text.trim().replaceAll("\\s+", " ");
    }

    private ChatConversation resolveConversation(Long conversationId, Long userId) {
        if (conversationId == null) {
            return null;
        }
        return chatConversationRepository.findByIdAndUser_Id(conversationId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "会话不存在或无权限"));
    }

    private String buildPrompt(String destination,
                               TravelWindow travelWindow,
                               String interests,
                               String budget,
                               String departure) {
        return "目的地: " + destination + "\n"
                + "日期: " + travelWindow.startDate() + " 至 " + travelWindow.endDate() + "\n"
                + "天数: " + travelWindow.days() + "\n"
                + "兴趣偏好: " + blankAsDefault(interests, "综合游览") + "\n"
                + "预算: " + blankAsDefault(budget, "中等预算") + "\n"
                + "出发地: " + blankAsDefault(departure, "未提供") + "\n"
                + "请生成可执行的逐日旅游路线。";
    }

    private String buildPreferencePrompt(UserAccount user,
                                         String interests,
                                         String budget,
                                         String departure) {
        List<String> parts = new ArrayList<>();
        if (user.getTravelPreferences() != null && !user.getTravelPreferences().isBlank()) {
            parts.add("用户出行偏好: " + user.getTravelPreferences());
        }
        if (interests != null && !interests.isBlank()) {
            parts.add("用户兴趣标签: " + interests);
        }
        if (budget != null && !budget.isBlank()) {
            parts.add("用户预算偏好: " + budget);
        }
        if (departure != null && !departure.isBlank()) {
            parts.add("用户常用出发地: " + departure);
        }
        return String.join("\n", parts);
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }

    private TravelWindow resolveTravelWindow(LocalDate requestedStartDate,
                                             LocalDate requestedEndDate,
                                             Integer requestedDays) {
        int fallbackDays = Math.max(1, Math.min(requestedDays == null ? 3 : requestedDays, 14));
        LocalDate startDate = requestedStartDate == null ? LocalDate.now() : requestedStartDate;
        LocalDate endDate = requestedEndDate == null ? startDate.plusDays(fallbackDays - 1L) : requestedEndDate;
        long rawDays = ChronoUnit.DAYS.between(startDate, endDate) + 1L;
        int days = (int) Math.max(1, Math.min(rawDays, 14));
        if (rawDays != days) {
            endDate = startDate.plusDays(days - 1L);
        }
        return new TravelWindow(startDate, endDate, days);
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String blankAsDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private Map<String, Object> parseJsonOrFallback(String raw, String destination, int days) {
        String json = extractJson(raw);
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("title", destination + " " + days + "日路线");
            fallback.put("summary", "根据目的地自动生成的基础路线，可继续调整。");
            fallback.put("points", List.of(
                    Map.of("day", 1, "order", 1, "name", destination + " 市中心", "description", "抵达并熟悉周边"),
                    Map.of("day", Math.min(days, 2), "order", 1, "name", destination + " 代表景点", "description", "核心景点游览"),
                    Map.of("day", days, "order", 1, "name", destination + " 美食街", "description", "体验当地美食")
            ));
            fallback.put("tips", List.of("出行前确认天气和景区公告", "节假日高峰建议提前预约", "保留机动时间应对交通和闭馆变化"));
            return fallback;
        }
    }

    private String extractJson(String text) {
        if (text == null) {
            return "{}";
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("```") && trimmed.contains("{")) {
            int start = trimmed.indexOf('{');
            int end = trimmed.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return trimmed.substring(start, end + 1);
            }
        }
        return trimmed;
    }

    private List<String> extractTips(Object tipsObj) {
        List<String> tips = new ArrayList<>();
        if (tipsObj instanceof List<?> arr) {
            for (Object tip : arr) {
                if (tip != null && !tip.toString().isBlank()) {
                    tips.add(tip.toString().trim());
                }
            }
        }
        return normalizeTips(tips);
    }

    private List<String> normalizeTips(List<String> tips) {
        List<String> normalized = new ArrayList<>();
        if (tips != null) {
            for (String tip : tips) {
                if (tip == null) {
                    continue;
                }
                String item = tip.trim();
                if (!item.isBlank() && normalized.stream().noneMatch(item::equals)) {
                    normalized.add(item);
                }
            }
        }
        if (!normalized.isEmpty()) {
            return normalized;
        }
        return List.of("出行前确认天气与景点开放时间", "热门景点建议提前预约", "注意查看官方公告，预留机动时间");
    }

    private String serializeTips(List<String> tips) {
        try {
            return objectMapper.writeValueAsString(normalizeTips(tips));
        } catch (Exception ex) {
            return "[]";
        }
    }

    private List<String> deserializeTips(String tipsJson) {
        if (tipsJson == null || tipsJson.isBlank()) {
            return normalizeTips(List.of());
        }
        try {
            return normalizeTips(objectMapper.readValue(tipsJson, new TypeReference<>() {
            }));
        } catch (Exception ex) {
            return normalizeTips(List.of());
        }
    }

    private int toInt(Object value, int defaultValue) {
        try {
            return value == null ? defaultValue : Integer.parseInt(value.toString());
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private String valueOrDefault(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String str = value.toString().trim();
        return str.isEmpty() ? defaultValue : str;
    }

    private List<RoutePointDto> fallbackPoints(String destination, int days) {
        List<RoutePointDto> points = new ArrayList<>();
        for (int i = 1; i <= Math.max(1, Math.min(days, 5)); i++) {
            points.add(new RoutePointDto(i, 1,
                    destination + " Day " + i + " 推荐点", "可根据兴趣补充景点", null, null));
        }
        return points;
    }

    private Instant sortTime(RoutePlan routePlan) {
        return routePlan.getUpdatedAt() == null ? routePlan.getCreatedAt() : routePlan.getUpdatedAt();
    }
}
