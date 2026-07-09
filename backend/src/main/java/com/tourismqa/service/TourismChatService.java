package com.tourismqa.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourismqa.dto.ChatConversationDetailResponse;
import com.tourismqa.dto.ChatConversationItemResponse;
import com.tourismqa.dto.ChatConversationListResponse;
import com.tourismqa.dto.ChatConversationMessageResponse;
import com.tourismqa.dto.ChatMessageDto;
import com.tourismqa.dto.ChatRequest;
import com.tourismqa.dto.ChatResponse;
import com.tourismqa.dto.ChatStreamEvent;
import com.tourismqa.dto.KgContextResponse;
import com.tourismqa.dto.KgSourceReference;
import com.tourismqa.dto.UserLocationDto;
import com.tourismqa.entity.ChatConversation;
import com.tourismqa.entity.ChatMessage;
import com.tourismqa.entity.RoutePlan;
import com.tourismqa.entity.UserAccount;
import com.tourismqa.exception.ApiException;
import com.tourismqa.repository.ChatConversationRepository;
import com.tourismqa.repository.ChatMessageRepository;
import com.tourismqa.repository.RoutePlanRepository;
import com.tourismqa.repository.RoutePointRepository;
import com.tourismqa.repository.UserAccountRepository;
import com.tourismqa.security.UserPrincipal;

/**
 * 旅游问答会话服务。
 * 使用场景：
 * 在聊天接口中承接多轮对话、模型路由、知识图谱增强与消息持久化流程。
 * 核心职责：
 * 1. 校验用户会话权限并管理会话生命周期。
 * 2. 组装系统提示词、历史上下文与知识图谱上下文。
 * 3. 调用模型路由层获取回复，并落库用户与助手消息。
 * 设计说明：
 * 采用事务边界保证“模型回答与消息持久化”一致性，避免会话数据出现单边写入。
 *
 * <p>框架作用：`@Service` 声明业务服务 Bean；`@Transactional` 控制事务语义。</p>
 */
@Service
public class TourismChatService {

    private static final Logger log = LoggerFactory.getLogger(TourismChatService.class);

    private static final String SYSTEM_PROMPT = """
            你是“专业旅游顾问助手”，具备全面的旅游知识与行程规划能力。请严格遵守以下规则：
            [角色与能力]
            1) 仅聚焦旅游相关问题：目的地介绍、景点推荐、交通方式、住宿建议、美食文化、预算拆解、季节天气、签证与入境准备、出行注意事项、亲子/情侣/老人等人群方案。
            2) 优先给“可执行建议”，避免空泛描述。涉及路线时给出先后顺序与时间分配建议。
            3) 当信息不足时，先做“最小可行假设”并明确标注，再补 1-3 个关键澄清问题（如出发地、天数、预算、同行人、出行日期）。
            
            [输出要求]
            4) 默认使用中文回答，表达专业、友好、简洁。优先使用分点，重要信息可用小标题组织。
            5) 回复结构尽量遵循：结论先行 -> 方案细节 -> 注意事项 -> 可选升级建议。
            6) 给预算建议时，尽量分档（经济/舒适/品质）并说明主要开销项（交通/住宿/餐饮/门票）。
            7) 给时间建议时，尽量标注“适合游玩时段/所需时长/是否需预约”。
            8) 涉及价格、政策、开放时间、签证规则等可能变化的信息，提醒用户以官方最新信息为准。
            9) 当用户询问近期出行安排时，主动补充天气变化、节假日客流、预约和景区临时闭馆等风险提醒；若缺少实时数据，明确提示用户核验官方公告。
            10) 若系统额外提供了”实时天气补充”或”景点开放状态补充”，优先采用这些补充信息，不要忽略其中的不确定性提示。

            [行程输出格式]
            当用户在对话中明确要求规划具体行程，你需要给出逐日安排时，必须严格遵循以下模板格式：

            **Day 1（日期或第1天）**
            上午：景点名，安排说明
            中午：景点名，安排说明
            下午：景点名，安排说明
            晚上：景点名，安排说明

            **Day 2（日期或第2天）**
            上午：景点名，安排说明
            ...

            格式约束：
            - 每天以 **Day N** 或 **第N天** 开头（N 从 1 开始）
            - 每个点位独占一行，以时间段标签开头（上午/中午/下午/晚上/早餐/午餐/晚餐 之一），后跟中文冒号
            - 时间段标签后紧跟景点/地点的裸名称，不含动词（如”前往””游览””参观””打卡””逛””去””到”）、序号、括号注释
            - 名称后使用逗号分隔，再接简短安排说明（可选）
            - 景点/地点名称应尽量使用具体的、可在地图上搜索到的名称（如”故宫博物院”、”南锣鼓巷”、”八达岭长城”），避免使用过于模糊的描述（如”老城区”、”市中心”、”商业区”）。如果知识图谱中提供了具体景点名称，优先使用这些名称。
            - 正面示例（正确）：”上午：西湖，环湖漫步约2小时”、”下午：南锣鼓巷，体验胡同文化”
            - 反面示例（禁止）：”上午：前往西湖（白堤入口）游览”、”第一站：逛外滩”、”下午：老城区闲逛”
            - tips 部分另起段落，以”温馨提示：”或”注意事项：”开头
            如果你只是泛泛推荐景点而没有逐日安排，则不需要使用此模板。

            [安全与边界]
            11) 拒绝违法、危险、欺诈、仇恨、色情等不当请求；改为提供安全、合规替代建议。
            12) 对医疗、法律、金融等高风险问题仅给一般信息，不给专业定论，并建议咨询持证专业人士。
            13) 不编造不可验证的事实；不确定时明确说明不确定性，并给出核验路径（官网/使馆/景区公告/航空公司通知）。
            
            [体验增强]
            14) 当用户表达了旅行规划意图（想去某地玩、需要行程建议、询问怎么安排几天等）时，在回答末尾追加一句：
            "如果你愿意，我可以直接生成可视化旅游路线。"并在该行末尾紧接着输出标记 [ROUTE_INTENT]。
            注意：[ROUTE_INTENT] 仅在你判断用户有明确行程规划意图时输出；一般性的景点问答或旅行知识咨询不应输出此标记。
            15) 如果回答引用了系统提供的知识图谱事实或来源清单，请在回答末尾增加"数据来源："小节，列出对应来源名称与链接；禁止编造不存在的来源。
            """;

    private final LlmRouterService llmRouterService;
    private final ModelCatalogService modelCatalogService;
    private final UserAccountRepository userAccountRepository;
    private final ChatConversationRepository chatConversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RoutePlanRepository routePlanRepository;
    private final RoutePointRepository routePointRepository;
    private final KnowledgeGraphService knowledgeGraphService;
    private final RealtimeTravelContextService realtimeTravelContextService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public TourismChatService(LlmRouterService llmRouterService,
                              ModelCatalogService modelCatalogService,
                              UserAccountRepository userAccountRepository,
                              ChatConversationRepository chatConversationRepository,
                              ChatMessageRepository chatMessageRepository,
                              RoutePlanRepository routePlanRepository,
                              RoutePointRepository routePointRepository,
                              KnowledgeGraphService knowledgeGraphService,
                              RealtimeTravelContextService realtimeTravelContextService,
                              ObjectMapper objectMapper,
                              PlatformTransactionManager transactionManager) {
        this.llmRouterService = llmRouterService;
        this.modelCatalogService = modelCatalogService;
        this.userAccountRepository = userAccountRepository;
        this.chatConversationRepository = chatConversationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.routePlanRepository = routePlanRepository;
        this.routePointRepository = routePointRepository;
        this.knowledgeGraphService = knowledgeGraphService;
        this.realtimeTravelContextService = realtimeTravelContextService;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 执行一轮旅游问答。
     *
     * @param request 问答请求，包含用户输入、可选历史和会话编号
     * @param principal 当前认证主体
     * @return 助手回复及路线生成提示元信息
     */
    @Transactional
    public ChatResponse chat(ChatRequest request, UserPrincipal principal) {
        PreparedChat prepared = prepareChat(request, principal);
        String answer;
        if (prepared.directAnswer() != null) {
            answer = prepared.directAnswer();
        } else {
            answer = llmRouterService.chatResolved(
                    prepared.provider(),
                    prepared.model(),
                    prepared.messages(),
                    prepared.usedWeatherContext() ? 0.0 : 0.4
            );
            answer = appendKnowledgeGraphSources(answer, prepared.graphContext());
        }

        // 同一事务内持久化双向消息，保障会话历史的完整可追溯性。
        ChatConversation conversation = chatConversationRepository.findById(prepared.conversationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "会话不存在或无权限"));
        persistMessage(conversation, "user", prepared.userMessage(), prepared.provider(), prepared.model());

        boolean suggestRoute = shouldSuggestRoute(prepared.userMessage(), answer);
        String cleanAnswer = stripRouteIntentMarker(answer);
        persistMessage(conversation, "assistant", cleanAnswer, prepared.provider(), prepared.model());

        String routeHint = suggestRoute ? prepared.userMessage() : null;
        return new ChatResponse(
                cleanAnswer,
                suggestRoute,
                routeHint,
                prepared.conversationId(),
                prepared.usedWeatherContext(),
                prepared.usedAttractionStatusContext()
        );
    }

    public StreamingResponseBody streamChat(ChatRequest request, UserPrincipal principal) {
        PreparedChat prepared = prepareChat(request, principal);
        return outputStream -> {
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
            StringBuilder answerBuilder = new StringBuilder();
            try {
                writeStreamEvent(writer, ChatStreamEvent.meta(prepared.conversationId()), true);
                final int[] deltaCount = {0};
                final boolean[] clientConnected = {true};

                if (prepared.directAnswer() != null) {
                    answerBuilder.append(prepared.directAnswer());
                    clientConnected[0] = tryWriteStreamEvent(writer, ChatStreamEvent.delta(prepared.directAnswer()), true);
                } else {
                    llmRouterService.streamResolved(
                            prepared.provider(),
                            prepared.model(),
                            prepared.messages(),
                            prepared.usedWeatherContext() ? 0.0 : 0.4,
                            delta -> {
                                if (delta == null || delta.isEmpty()) {
                                    return;
                                }
                                answerBuilder.append(delta);
                                if (clientConnected[0]) {
                                    deltaCount[0]++;
                                    boolean shouldFlush = (deltaCount[0] % 5 == 0) || (answerBuilder.length() % 100 < delta.length());
                                    clientConnected[0] = tryWriteStreamEvent(writer, ChatStreamEvent.delta(delta), shouldFlush);
                                }
                            }
                    );
                }

                String rawAnswer = prepared.directAnswer() != null
                        ? prepared.directAnswer()
                        : appendKnowledgeGraphSources(answerBuilder.toString().trim(), prepared.graphContext());
                if (rawAnswer == null || rawAnswer.isBlank()) {
                    if (clientConnected[0]) {
                        writeStreamEvent(writer, ChatStreamEvent.error("没有收到模型回复。"), true);
                    }
                    return;
                }

                boolean suggestRoute = shouldSuggestRoute(prepared.userMessage(), rawAnswer);
                String cleanAnswer = stripRouteIntentMarker(rawAnswer);

                persistChatExchange(
                        prepared.conversationId(),
                        prepared.userMessage(),
                        cleanAnswer,
                        prepared.provider(),
                        prepared.model()
                );

                if (clientConnected[0]) {
                    writeStreamEvent(
                            writer,
                            ChatStreamEvent.done(
                                    prepared.conversationId(),
                                    cleanAnswer,
                                    suggestRoute,
                                    suggestRoute ? prepared.userMessage() : null,
                                    prepared.usedWeatherContext(),
                                    prepared.usedAttractionStatusContext()
                            ),
                            true
                    );
                }
            } catch (UncheckedIOException ex) {
                // 客户端断开连接（用户刷新页面、网络中断、超时等）是正常情况，不记录为错误
                IOException cause = ex.getCause();
                if (cause != null) {
                    log.debug("聊天流已中断（客户端断开）: {}", cause.getMessage());
                } else {
                    log.debug("聊天流已中断（客户端断开）");
                }
                // 【关键修改】即使客户端断开，也要保存部分对话，避免用户刷新后对话丢失
                persistPartialChatExchange(prepared, answerBuilder.toString());
                // 不尝试写入错误事件，因为响应流已关闭
            } catch (ApiException ex) {
                log.warn("聊天流业务异常: {}", ex.getMessage());
                // 保存部分对话
                persistPartialChatExchange(prepared, answerBuilder.toString());
                try {
                    writeStreamEvent(writer, ChatStreamEvent.error(ex.getMessage()), true);
                } catch (UncheckedIOException writeError) {
                    // 响应流已关闭，无法写入错误事件，这是正常情况
                    log.debug("无法写入错误事件（响应流已关闭）");
                }
            } catch (Exception ex) {
                log.error("聊天流处理失败", ex);
                persistPartialChatExchange(prepared, answerBuilder.toString());
                try {
                    writeStreamEvent(writer, ChatStreamEvent.error("流式回答失败: " + ex.getMessage()), true);
                } catch (UncheckedIOException writeError) {
                    // 响应流已关闭，无法写入错误事件，这是正常情况
                    log.debug("无法写入异常事件（响应流已关闭）");
                }
            }
        };
    }

    /**
     * 查询当前用户的会话列表。
     *
     * @param principal 当前认证主体
     * @return 会话列表响应
     */
    @Transactional(readOnly = true)
    public ChatConversationListResponse listConversations(UserPrincipal principal) {
        Long userId = requireUserId(principal, "请先登录后再查看历史会话");
        List<ChatConversationItemResponse> conversations = chatConversationRepository
                .findByUser_IdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(item -> new ChatConversationItemResponse(
                        item.getId(),
                        item.getTitle(),
                        item.getProvider(),
                        item.getModel(),
                        item.getCreatedAt(),
                        item.getUpdatedAt()
                ))
                .toList();
        return new ChatConversationListResponse(conversations);
    }

    /**
     * 查询指定会话的详情与消息序列。
     *
     * @param conversationId 会话主键
     * @param principal 当前认证主体
     * @return 会话详情响应
     */
    @Transactional(readOnly = true)
    public ChatConversationDetailResponse getConversationDetail(Long conversationId, UserPrincipal principal) {
        Long userId = requireUserId(principal, "请先登录后再查看历史消息");
        ChatConversation conversation = chatConversationRepository.findByIdAndUser_Id(conversationId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "会话不存在或无权限"));
        List<ChatConversationMessageResponse> messages = chatMessageRepository
                .findByConversationIdOrderByIdAsc(conversationId)
                .stream()
                .map(item -> new ChatConversationMessageResponse(
                        item.getId(),
                        item.getRole(),
                        item.getContent(),
                        item.getCreatedAt()
                ))
                .toList();
        return new ChatConversationDetailResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getProvider(),
                conversation.getModel(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                messages
        );
    }

    /**
     * 删除当前用户的一条历史会话。
     *
     * @param conversationId 会话主键
     * @param principal 当前认证主体
     */
    @Transactional
    public void deleteConversation(Long conversationId, UserPrincipal principal) {
        Long userId = requireUserId(principal, "请先登录后再删除历史会话");
        ChatConversation conversation = chatConversationRepository.findByIdAndUser_Id(conversationId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "会话不存在或无权限"));

        // Delete associated route plans and their points
        List<RoutePlan> routePlans = routePlanRepository.findByConversation_Id(conversationId);
        for (RoutePlan routePlan : routePlans) {
            routePointRepository.deleteByRoutePlan_Id(routePlan.getId());
        }
        routePlanRepository.deleteAll(routePlans);

        // Delete associated chat messages
        List<ChatMessage> messages = chatMessageRepository.findByConversationIdOrderByIdAsc(conversationId);
        chatMessageRepository.deleteAll(messages);

        chatConversationRepository.delete(conversation);
    }

    /**
     * 保存部分对话（用于流式传输中断后的恢复）。
     * 前端在恢复失败后会调用此方法，确保未完成的对话不会丢失。
     *
     * @param conversationId 会话主键
     * @param userMessage 用户消息
     * @param partialAnswer 部分助手回复
     * @param principal 当前认证主体
     */
    @Transactional
    public void savePartialExchange(Long conversationId,
                                   String userMessage,
                                   String partialAnswer,
                                   UserPrincipal principal) {
        Long userId = requireUserId(principal, "请先登录");
        ChatConversation conversation = chatConversationRepository
                .findByIdAndUser_Id(conversationId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "会话不存在或无权限"));

        // 检查是否已经保存过这条消息（避免重复保存）
        List<ChatMessage> recentMessages = chatMessageRepository
                .findTop2ByConversationIdOrderByIdDesc(conversationId);

        boolean alreadySaved = recentMessages.stream()
                .anyMatch(msg -> msg.getRole().equals("user") && msg.getContent().equals(userMessage));

        if (!alreadySaved) {
            log.info("前端触发保存部分对话，conversationId={}", conversationId);
            persistMessage(conversation, "user", userMessage, conversation.getProvider(), conversation.getModel());
            persistMessage(conversation, "assistant", partialAnswer + "\n\n[回答未完成，已中断]",
                    conversation.getProvider(), conversation.getModel());
        } else {
            log.debug("部分对话已存在，跳过重复保存，conversationId={}", conversationId);
        }
    }

    /**
     * 从认证主体提取用户标识并校验用户存在性。
     *
     * @param principal 当前认证主体
     * @param unauthMessage 未认证时返回的业务提示
     * @return 用户主键
     */
    private Long requireUserId(UserPrincipal principal, String unauthMessage) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED.value(), unauthMessage);
        }
        Long userId = principal.getUserId();
        if (!userAccountRepository.existsById(userId)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED.value(), "用户不存在");
        }
        return userId;
    }

    /**
     * 解析当前请求对应的会话实体。
     *
     * @param request 问答请求
     * @param user 当前用户实体
     * @param provider 实际路由模型提供方
     * @param model 实际路由模型标识
     * @return 已保存的会话实体
     */
    private ChatConversation resolveConversation(ChatRequest request,
                                                 UserAccount user,
                                                 String provider,
                                                 String model) {
        ChatConversation conversation;
        if (request.conversationId() != null) {
            conversation = chatConversationRepository.findByIdAndUser_Id(request.conversationId(), user.getId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "会话不存在或无权限"));
        } else {
            conversation = new ChatConversation();
            conversation.setUser(user);
            conversation.setTitle(buildConversationTitle(request.message()));
        }

        conversation.setProvider(provider);
        conversation.setModel(model);
        return chatConversationRepository.save(conversation);
    }

    /**
     * 构造传入大模型的消息列表。
     *
     * @param request 问答请求
     * @param conversation 当前会话
     * @return 组装后的消息序列
     */
    private List<LlmMessage> buildMessages(ChatRequest request, ChatConversation conversation, UserAccount user) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage("system", SYSTEM_PROMPT));
        appendUserPreferenceContext(messages, user);
        appendUserLocationContext(messages, request.location());

        boolean hasRequestHistory = false;
        List<ChatMessageDto> requestHistory = request.history();
        if (requestHistory != null && "RECENT_ONLY".equalsIgnoreCase(user.getMemoryStrategy()) && requestHistory.size() > 6) {
            requestHistory = requestHistory.subList(requestHistory.size() - 6, requestHistory.size());
        }

        if (requestHistory != null && !"PRIVACY_FIRST".equalsIgnoreCase(user.getMemoryStrategy())) {
            for (ChatMessageDto msg : requestHistory) {
                if (msg.content() == null || msg.content().isBlank()) {
                    continue;
                }
                hasRequestHistory = true;
                String role = normalizeRole(msg.role());
                messages.add(new LlmMessage(role, msg.content().trim()));
            }
        }

        if (!hasRequestHistory
                && request.conversationId() != null
                && "STANDARD".equalsIgnoreCase(user.getMemoryStrategy())) {
            // 未显式传历史时，自动回放最近 20 条数据库消息，兼顾上下文质量与 token 成本。
            List<ChatMessage> dbMessages = chatMessageRepository.findTop20ByConversationIdOrderByIdDesc(conversation.getId());
            for (int i = dbMessages.size() - 1; i >= 0; i--) {
                ChatMessage msg = dbMessages.get(i);
                messages.add(new LlmMessage(normalizeRole(msg.getRole()), msg.getContent()));
            }
        }

        return messages;
    }

    /**
     * 将知识图谱检索结果以系统消息形式注入上下文。
     *
     * @param messages 当前消息序列
     * @param userMessage 用户原始提问
     */
    private void appendKnowledgeGraphContext(List<LlmMessage> messages, KgContextResponse graphContext) {
        if (graphContext == null
                || graphContext.promptContext() == null
                || graphContext.promptContext().isBlank()) {
            return;
        }
        messages.add(new LlmMessage("system", graphContext.promptContext()));
    }

    private void appendRealtimeContext(List<LlmMessage> messages, String realtimeContext) {
        if (realtimeContext == null || realtimeContext.isBlank()) {
            return;
        }
        messages.add(new LlmMessage("system", realtimeContext));
    }

    private void appendUserPreferenceContext(List<LlmMessage> messages, UserAccount user) {
        List<String> parts = new ArrayList<>();
        if (user.getPreferredDeparture() != null && !user.getPreferredDeparture().isBlank()) {
            parts.add("用户常用出发地: " + user.getPreferredDeparture());
        }
        if (user.getBudgetPreference() != null && !user.getBudgetPreference().isBlank()) {
            parts.add("用户预算偏好: " + user.getBudgetPreference());
        }
        if (user.getTravelPreferences() != null && !user.getTravelPreferences().isBlank()) {
            parts.add("用户出行偏好: " + user.getTravelPreferences());
        }
        if (user.getInterestTags() != null && !user.getInterestTags().isBlank()) {
            parts.add("用户兴趣标签: " + user.getInterestTags());
        }
        parts.add("对话记忆策略: " + user.getMemoryStrategy());
        if (!parts.isEmpty()) {
            messages.add(new LlmMessage("system", String.join("\n", parts)));
        }
    }

    private void appendUserLocationContext(List<LlmMessage> messages, UserLocationDto location) {
        if (location == null || location.latitude() == null || location.longitude() == null) {
            return;
        }
        List<String> parts = new ArrayList<>();
        parts.add("用户当前定位: " + defaultText(location.label(), "当前位置"));
        parts.add("纬度: " + String.format(Locale.ROOT, "%.6f", location.latitude()));
        parts.add("经度: " + String.format(Locale.ROOT, "%.6f", location.longitude()));
        if (location.accuracy() != null) {
            parts.add("定位精度约: " + Math.round(location.accuracy()) + " 米");
        }
        if (location.capturedAt() != null && !location.capturedAt().isBlank()) {
            parts.add("定位采集时间: " + location.capturedAt());
        }
        parts.add("当用户询问当前位置、附近、周边、本地天气或就近出行建议时，可优先结合该定位；除非用户明确需要，不要主动暴露精确坐标。");
        messages.add(new LlmMessage("system", String.join("\n", parts)));
    }

    private String appendKnowledgeGraphSources(String answer, KgContextResponse graphContext) {
        if (answer == null || answer.isBlank() || graphContext == null || graphContext.sources().isEmpty()) {
            log.debug("未追加知识图谱来源 - answer为空: {}, graphContext为空: {}, sources为空: {}",
                    answer == null || answer.isBlank(),
                    graphContext == null,
                    graphContext == null || graphContext.sources().isEmpty());
            return answer;
        }
        if (answer.contains("数据来源")) {
            log.debug("回答中已包含数据来源，跳过追加");
            return answer;
        }
        log.debug("追加知识图谱来源，来源数量: {}", graphContext.sources().size());
        StringBuilder sb = new StringBuilder(answer.trim());
        sb.append("\n\n数据来源：\n");
        int sourceLimit = Math.min(5, graphContext.sources().size());
        for (int i = 0; i < sourceLimit; i++) {
            KgSourceReference source = graphContext.sources().get(i);
            sb.append("- ")
                    .append(defaultText(source.title(), "未命名来源"));
            if (source.sourceType() != null && !source.sourceType().isBlank()) {
                sb.append("（").append(source.sourceType()).append("）");
            } else if (source.source() != null && !source.source().isBlank()) {
                sb.append("（").append(source.source()).append("）");
            }
            if (source.url() != null && !source.url().isBlank()) {
                sb.append(": ").append(source.url());
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private String defaultText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    /**
     * 持久化单条会话消息。
     *
     * @param conversation 会话实体
     * @param role 消息角色（user/assistant）
     * @param content 消息内容
     * @param provider 模型提供方
     * @param model 模型标识
     */
    private void persistMessage(ChatConversation conversation,
                                String role,
                                String content,
                                String provider,
                                String model) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setConversation(conversation);
        chatMessage.setRole(role);
        chatMessage.setContent(content);
        chatMessage.setProvider(provider);
        chatMessage.setModel(model);
        chatMessageRepository.save(chatMessage);
    }

    private void persistChatExchange(Long conversationId,
                                     String userMessage,
                                     String answer,
                                     String provider,
                                     String model) {
        transactionTemplate.executeWithoutResult(status -> {
            ChatConversation conversation = chatConversationRepository.findById(conversationId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "会话不存在或无权限"));
            persistMessage(conversation, "user", userMessage, provider, model);
            persistMessage(conversation, "assistant", answer, provider, model);
        });
    }

    private void persistPartialChatExchange(PreparedChat prepared, String partialAnswer) {
        if (partialAnswer == null || partialAnswer.isBlank()) {
            return;
        }

        String cleanAnswer = stripRouteIntentMarker(partialAnswer.trim());
        if (cleanAnswer.isBlank()) {
            return;
        }

        persistChatExchange(
                prepared.conversationId(),
                prepared.userMessage(),
                cleanAnswer,
                prepared.provider(),
                prepared.model()
        );
    }

    private PreparedChat prepareChat(ChatRequest request, UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED.value(), "请先登录后再问答");
        }

        UserAccount user = userAccountRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED.value(), "用户不存在"));

        LlmModelSelection selection = modelCatalogService.resolveModelSelection(request.provider(), request.model());
        String provider = selection.provider();
        String model = selection.modelId();
        ChatConversation conversation = resolveConversation(request, user, provider, model);

        String userMessage = request.message().trim();
        KgContextResponse graphContext = knowledgeGraphService.buildLlmContext(userMessage);
        RealtimeTravelContextService.DirectRealtimeAnswer directAnswer =
                realtimeTravelContextService.tryBuildDirectAnswer(userMessage, graphContext, request.location());
        if (directAnswer != null) {
            return new PreparedChat(
                    conversation.getId(),
                    provider,
                    model,
                    userMessage,
                    List.of(),
                    graphContext,
                    directAnswer.usedWeatherContext(),
                    directAnswer.usedAttractionStatusContext(),
                    directAnswer.answer()
            );
        }
        RealtimeContextPayload realtimeContext = realtimeTravelContextService.buildPromptContext(
                userMessage,
                graphContext,
                request.location()
        );
        List<LlmMessage> messages = buildMessages(request, conversation, user);
        // 在用户消息之前插入图谱上下文，确保模型优先感知结构化旅游知识。
        appendKnowledgeGraphContext(messages, graphContext);
        appendRealtimeContext(messages, realtimeContext == null ? null : realtimeContext.promptContext());
        messages.add(new LlmMessage("user", userMessage));

        return new PreparedChat(
                conversation.getId(),
                provider,
                model,
                userMessage,
                messages,
                graphContext,
                realtimeContext != null && realtimeContext.usedWeatherContext(),
                realtimeContext != null && realtimeContext.usedAttractionStatusContext(),
                null
        );
    }

    private void writeStreamEvent(BufferedWriter writer, ChatStreamEvent event, boolean flush) {
        try {
            writer.write(objectMapper.writeValueAsString(event));
            writer.newLine();
            if (flush) {
                writer.flush();
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private boolean tryWriteStreamEvent(BufferedWriter writer, ChatStreamEvent event, boolean flush) {
        try {
            writeStreamEvent(writer, event, flush);
            return true;
        } catch (UncheckedIOException ex) {
            IOException cause = ex.getCause();
            if (cause != null) {
                log.debug("聊天流已中断（客户端断开）: {}", cause.getMessage());
            } else {
                log.debug("聊天流已中断（客户端断开）");
            }
            return false;
        }
    }

    /**
     * 根据用户首条输入生成会话标题。
     *
     * @param text 用户输入文本
     * @return 规范化后的标题，超过阈值自动截断
     */
    private String buildConversationTitle(String text) {
        String clean = text == null ? "新会话" : text.trim().replaceAll("\\s+", " ");
        if (clean.isBlank()) {
            return "新会话";
        }
        if (clean.length() <= 30) {
            return clean;
        }
        return clean.substring(0, 30) + "...";
    }

    /**
     * 规范化消息角色，过滤非法值。
     *
     * @param role 原始角色文本
     * @return 合法角色标识，非法值回退为 user
     */
    private String normalizeRole(String role) {
        if (role == null) {
            return "user";
        }
        String normalized = role.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "system", "assistant", "user" -> normalized;
            default -> "user";
        };
    }

    /**
     * 判断当前问答是否触发行程规划提示。
     *
     * @param userMessage 用户输入
     * @param answer 助手输出
     * @return 命中关键词时返回 true
     */
    private static final Pattern ROUTE_INTENT_MARKER = Pattern.compile("\\[ROUTE_INTENT]");

    private boolean shouldSuggestRoute(String userMessage, String answer) {
        // Primary: LLM-driven detection via [ROUTE_INTENT] marker in the answer
        if (answer != null && ROUTE_INTENT_MARKER.matcher(answer).find()) {
            return true;
        }
        // Fallback: keyword detection - only check user message for explicit planning intent
        // Avoid checking answer to prevent false positives from general tourism content
        String userText = (userMessage != null ? userMessage : "").toLowerCase(Locale.ROOT);
        String[] keywords = {
                "行程", "路线", "攻略", "怎么玩", "计划", "几天",
                "安排", "规划", "自由行", "周末游", "一日游", "两日游", "三日游",
                "tour", "itinerary", "trip plan", "travel plan"
        };
        for (String keyword : keywords) {
            if (userText.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String stripRouteIntentMarker(String text) {
        if (text == null) return null;
        return ROUTE_INTENT_MARKER.matcher(text).replaceAll("").trim();
    }

    private record PreparedChat(
            Long conversationId,
            String provider,
            String model,
            String userMessage,
            List<LlmMessage> messages,
            KgContextResponse graphContext,
            boolean usedWeatherContext,
            boolean usedAttractionStatusContext,
            String directAnswer
    ) {
    }
}
