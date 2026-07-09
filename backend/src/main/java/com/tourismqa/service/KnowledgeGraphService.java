package com.tourismqa.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.data.neo4j.core.Neo4jClient;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.JiebaSegmenter.SegMode;
import com.huaban.analysis.jieba.SegToken;

import com.tourismqa.config.AppProperties;
import com.tourismqa.dto.KgChangeLogResponse;
import com.tourismqa.dto.KgContextRequest;
import com.tourismqa.dto.KgContextResponse;
import com.tourismqa.dto.KgNodeCreateRequest;
import com.tourismqa.dto.KgNodeResponse;
import com.tourismqa.dto.KgSourceReference;
import com.tourismqa.dto.KgNodeUpdateRequest;
import com.tourismqa.dto.KgRelationshipCreateRequest;
import com.tourismqa.dto.KgRelationshipResponse;
import com.tourismqa.dto.KgRelationshipUpdateRequest;
import com.tourismqa.entity.KgChangeLog;
import com.tourismqa.exception.ApiException;
import com.tourismqa.repository.KgChangeLogRepository;
import com.tourismqa.security.UserPrincipal;

/**
 * 旅游知识图谱服务。
 * 使用场景：
 * 为后台管理接口提供节点/关系维护能力，并为聊天与路线规划提供图谱上下文增强。
 * 核心职责：
 * 1. 管理图谱节点和关系的增删改查。
 * 2. 执行关键词检索并生成上下文提示文本。
 * 3. 对图谱属性键和值执行白名单与规范化处理。
 *
 * <p>框架作用：`@Service` 声明图谱领域服务 Bean。</p>
 */
@Service
public class KnowledgeGraphService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeGraphService.class);
    private static final Set<String> RESERVED_NODE_KEYS = Set.of(
            "name", "category", "description", "aliases", "tags", "createdAt", "updatedAt",
            "createdByUserId", "createdByUsername", "createdByDisplayName",
            "updatedByUserId", "updatedByUsername", "updatedByDisplayName"
    );
    private static final Set<String> RESERVED_RELATIONSHIP_KEYS = Set.of(
            "predicate", "description", "weight", "createdAt", "updatedAt",
            "createdByUserId", "createdByUsername", "createdByDisplayName",
            "updatedByUserId", "updatedByUsername", "updatedByDisplayName"
    );
    private static final Pattern ATTRIBUTE_KEY_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,63}");
    // jieba 分词器，字典加载后线程安全
    private static final JiebaSegmenter SEGMENTER = new JiebaSegmenter();
    // 旅游问答停用词：过滤后只保留对图谱检索有意义的实体词
    private static final Set<String> STOP_WORDS = Set.of(
            "的", "地", "得", "了", "着", "过", "吗", "呢", "啊", "吧", "嗯", "哦",
            "什么", "怎么", "哪里", "哪些", "哪个", "哪座", "哪条", "哪种", "怎样", "如何",
            "有没有", "是什么", "在哪", "在哪里", "哪儿",
            "我", "你", "他", "她", "我们", "你们", "他们", "自己",
            "这", "那", "这个", "那个", "这些", "那些", "这里", "那里",
            "是", "在", "有", "和", "与", "或", "也", "都", "就", "还", "又",
            "不", "没", "很", "非常", "比较", "最", "更", "太",
            "可以", "能", "会", "要", "想", "需要", "应该", "必须",
            "因为", "所以", "但是", "而且", "然后", "另外", "同时",
            "一个", "一些", "很多", "一点", "几个", "多少"
    );

    private final Neo4jClient neo4jClient;
    private final AppProperties appProperties;
    private final KgChangeLogRepository kgChangeLogRepository;

    public KnowledgeGraphService(ObjectProvider<Neo4jClient> neo4jClientProvider,
                                 AppProperties appProperties,
                                 KgChangeLogRepository kgChangeLogRepository) {
        this.neo4jClient = neo4jClientProvider.getIfAvailable();
        this.appProperties = appProperties;
        this.kgChangeLogRepository = kgChangeLogRepository;
    }

    /**
     * 创建图谱节点。
     *
     * @param request 节点创建请求
     * @return 新建节点响应
     */
    public KgNodeResponse createNode(KgNodeCreateRequest request, UserPrincipal principal) {
        ensureEnabled();
        String now = Instant.now().toString();
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("name", normalizeRequiredText(request.name(), "实体名称"));
        putOptionalText(props, "category", request.category());
        putOptionalText(props, "description", request.description());
        putOptionalStringList(props, "aliases", request.aliases());
        putOptionalStringList(props, "tags", request.tags());
        props.put("createdAt", now);
        props.put("updatedAt", now);
        putCreatedAndUpdatedOperatorProps(props, principal);
        props.putAll(sanitizeAttributes(request.attributes(), RESERVED_NODE_KEYS));

        String cypher = """
                CREATE (n:KgNode)
                SET n += $props
                RETURN id(n) AS id,
                       n.name AS name,
                       n.category AS category,
                       n.description AS description,
                       coalesce(n.aliases, []) AS aliases,
                       coalesce(n.tags, []) AS tags,
                       n.createdAt AS createdAt,
                       n.updatedAt AS updatedAt,
                       properties(n) AS properties
                """;
        Map<String, Object> row = neo4jClient.query(cypher)
                .bind(props).to("props")
                .fetch().one()
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "创建实体失败"));
        KgNodeResponse response = mapNodeRow(row);
        recordKgChange("NODE", response.id(), "CREATE", response.name(), principal);
        return response;
    }

    /**
     * 查询单个节点详情。
     *
     * @param nodeId 节点主键
     * @return 节点响应
     */
    public KgNodeResponse getNode(Long nodeId) {
        ensureEnabled();
        Map<String, Object> row = fetchNodeRow(nodeId);
        if (row == null) {
            throw notFound("实体不存在: " + nodeId);
        }
        return mapNodeRow(row);
    }

    /**
     * 按关键字检索节点列表。
     *
     * @param keyword 关键字
     * @param limit 返回条数上限
     * @return 节点列表
     */
    public List<KgNodeResponse> searchNodes(String keyword, Integer limit) {
        ensureEnabled();
        int safeLimit = normalizeLimit(limit, 20, 1, 200);
        String kw = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        String cypher = """
                WITH $keyword AS kw
                MATCH (n:KgNode)
                WHERE kw = ''
                   OR toLower(coalesce(n.name, '')) CONTAINS kw
                   OR toLower(coalesce(n.category, '')) CONTAINS kw
                   OR toLower(coalesce(n.description, '')) CONTAINS kw
                   OR any(alias IN coalesce(n.aliases, []) WHERE toLower(alias) CONTAINS kw)
                   OR any(tag IN coalesce(n.tags, []) WHERE toLower(tag) CONTAINS kw)
                RETURN id(n) AS id,
                       n.name AS name,
                       n.category AS category,
                       n.description AS description,
                       coalesce(n.aliases, []) AS aliases,
                       coalesce(n.tags, []) AS tags,
                       n.createdAt AS createdAt,
                       n.updatedAt AS updatedAt,
                       properties(n) AS properties
                ORDER BY coalesce(n.updatedAt, n.createdAt) DESC, id(n) DESC
                LIMIT $limit
                """;
        return neo4jClient.query(cypher)
                .bind(kw).to("keyword")
                .bind(safeLimit).to("limit")
                .fetch().all()
                .stream()
                .map(this::mapNodeRow)
                .toList();
    }

    /**
     * 更新节点属性。
     *
     * @param nodeId 节点主键
     * @param request 节点更新请求
     * @return 更新后的节点响应
     */
    public KgNodeResponse updateNode(Long nodeId, KgNodeUpdateRequest request, UserPrincipal principal) {
        ensureEnabled();
        Map<String, Object> props = new LinkedHashMap<>();

        if (request.name() != null && request.name().isBlank()) {
            throw badRequest("实体名称不能为空");
        }
        putOptionalText(props, "name", request.name());
        putOptionalTextAllowNull(props, "category", request.category());
        putOptionalTextAllowNull(props, "description", request.description());
        if (request.aliases() != null) {
            props.put("aliases", normalizeStringList(request.aliases()));
        }
        if (request.tags() != null) {
            props.put("tags", normalizeStringList(request.tags()));
        }
        props.putAll(sanitizeAttributes(request.attributes(), RESERVED_NODE_KEYS));
        props.put("updatedAt", Instant.now().toString());
        putUpdatedOperatorProps(props, principal);

        String cypher = """
                MATCH (n:KgNode)
                WHERE id(n) = $id
                SET n += $props
                RETURN id(n) AS id,
                       n.name AS name,
                       n.category AS category,
                       n.description AS description,
                       coalesce(n.aliases, []) AS aliases,
                       coalesce(n.tags, []) AS tags,
                       n.createdAt AS createdAt,
                       n.updatedAt AS updatedAt,
                       properties(n) AS properties
                """;
        Map<String, Object> row = neo4jClient.query(cypher)
                .bind(nodeId).to("id")
                .bind(props).to("props")
                .fetch().one()
                .orElseThrow(() -> notFound("实体不存在: " + nodeId));
        KgNodeResponse response = mapNodeRow(row);
        recordKgChange("NODE", response.id(), "UPDATE", response.name(), principal);
        return response;
    }

    /**
     * 删除指定节点及其关联关系。
     *
     * @param nodeId 节点主键
     */
    public void deleteNode(Long nodeId, UserPrincipal principal) {
        ensureEnabled();
        String cypher = """
                MATCH (n:KgNode)
                WHERE id(n) = $id
                WITH n, id(n) AS deletedId, n.name AS targetLabel
                DETACH DELETE n
                RETURN deletedId AS id, targetLabel AS targetLabel
                """;
        Map<String, Object> row = neo4jClient.query(cypher)
                .bind(nodeId).to("id")
                .fetch().one()
                .orElse(null);
        if (row == null) {
            throw notFound("实体不存在: " + nodeId);
        }
        recordKgChange("NODE", toLong(row.get("id")), "DELETE", asText(row.get("targetLabel")), principal);
    }

    /**
     * 获取所有市级节点。
     * 保留供旧接口（用户端前端）调用，内部委托 getNodesByCategory。
     *
     * @return 市级节点列表
     */
    public List<KgNodeResponse> getCityNodes() {
        return getNodesByCategory("城市");
    }

    /**
     * 按 category 获取节点。
     *
     * @param category 类别名，如 国家/省份/城市/区县/景点 等
     * @return 节点列表（按名称排序）
     */
    public List<KgNodeResponse> getNodesByCategory(String category) {
        ensureEnabled();
        if (!StringUtils.hasText(category)) {
            throw badRequest("category 不能为空");
        }
        String cypher = """
                MATCH (n:KgNode)
                WHERE n.category = $category
                RETURN id(n) AS id,
                       n.name AS name,
                       n.category AS category,
                       n.description AS description,
                       coalesce(n.aliases, []) AS aliases,
                       coalesce(n.tags, []) AS tags,
                       n.createdAt AS createdAt,
                       n.updatedAt AS updatedAt,
                       properties(n) AS properties
                ORDER BY n.name
                """;
        return neo4jClient.query(cypher)
                .bind(category).to("category")
                .fetch().all()
                .stream()
                .map(this::mapNodeRow)
                .toList();
    }

    /**
     * 获取城市之间的关系。
     * 保留供旧接口调用，内部委托 getRelationshipsByCategory。
     *
     * @return 城市间关系列表
     */
    public List<KgRelationshipResponse> getCityRelationships() {
        return getRelationshipsByCategory("城市");
    }

    /**
     * 获取同类别节点之间的关系（起点和终点 category 均匹配）。
     *
     * @param category 类别名
     * @return 同层级节点间的关系列表
     */
    public List<KgRelationshipResponse> getRelationshipsByCategory(String category) {
        ensureEnabled();
        if (!StringUtils.hasText(category)) {
            throw badRequest("category 不能为空");
        }
        String cypher = """
                MATCH (a:KgNode)-[r:KG_REL]->(b:KgNode)
                WHERE a.category = $category AND b.category = $category
                RETURN id(r) AS id,
                       id(a) AS fromNodeId,
                       id(b) AS toNodeId,
                       r.predicate AS predicate,
                       r.description AS description,
                       r.weight AS weight,
                       r.createdAt AS createdAt,
                       r.updatedAt AS updatedAt,
                       properties(r) AS properties
                """;
        return neo4jClient.query(cypher)
                .bind(category).to("category")
                .fetch().all()
                .stream()
                .map(this::mapRelationshipRow)
                .toList();
    }

    /**
     * 获取指定市节点的子节点和关系。
     * 保留供旧接口调用，要求节点 category='城市'；通用展开请使用 expandNode。
     *
     * @param cityId 市节点 ID
     * @return 包含子节点和关系的 Map
     */
    public Map<String, Object> expandCityNode(Long cityId) {
        ensureEnabled();
        Map<String, Object> cityRow = fetchNodeRow(cityId);
        if (cityRow == null) {
            throw notFound("城市节点不存在: " + cityId);
        }
        String category = asText(cityRow.get("category"));
        if (!"城市".equals(category)) {
            throw badRequest("节点不是城市节点: " + cityId);
        }
        return expandNode(cityId);
    }

    /**
     * 通用节点展开：返回当前节点的所有下级节点和对应关系。
     * 不校验 category，可用于任意层级（国家→省份→城市→区县→景点/酒店/美食/名人）。
     *
     * <p>方向语义按 predicate 判定：</p>
     * <ul>
     *   <li>出边 + 谓词以 "包含" 或 "相关" 开头 → 对端是子</li>
     *   <li>入边 + 谓词以 "所属" 开头 → 对端是子（数据里 "所属*" 为子→父方向）</li>
     * </ul>
     *
     * @param nodeId 节点 ID
     * @return 包含子节点和关系的 Map
     */
    public Map<String, Object> expandNode(Long nodeId) {
        ensureEnabled();

        Map<String, Object> nodeRow = fetchNodeRow(nodeId);
        if (nodeRow == null) {
            throw notFound("节点不存在: " + nodeId);
        }

        String cypher = """
                MATCH (self:KgNode)-[r:KG_REL]-(child:KgNode)
                WHERE id(self) = $nodeId
                  AND (
                    (startNode(r) = self AND (r.predicate STARTS WITH '包含' OR r.predicate STARTS WITH '相关'))
                    OR
                    (endNode(r) = self AND r.predicate STARTS WITH '所属')
                  )
                RETURN id(child) AS id,
                       child.name AS name,
                       child.category AS category,
                       child.description AS description,
                       coalesce(child.aliases, []) AS aliases,
                       coalesce(child.tags, []) AS tags,
                       child.createdAt AS createdAt,
                       child.updatedAt AS updatedAt,
                       properties(child) AS properties,
                       id(r) AS relId,
                       id(startNode(r)) AS fromNodeId,
                       id(endNode(r)) AS toNodeId,
                       r.predicate AS predicate,
                       r.description AS relDescription,
                       r.weight AS weight,
                       r.createdAt AS relCreatedAt,
                       r.updatedAt AS relUpdatedAt,
                       properties(r) AS relProperties
                """;

        List<Map<String, Object>> rows = neo4jClient.query(cypher)
                .bind(nodeId).to("nodeId")
                .fetch().all()
                .stream()
                .collect(Collectors.toList());

        List<KgNodeResponse> nodes = new ArrayList<>();
        List<KgRelationshipResponse> edges = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            Map<String, Object> childRow = new LinkedHashMap<>();
            childRow.put("id", row.get("id"));
            childRow.put("name", row.get("name"));
            childRow.put("category", row.get("category"));
            childRow.put("description", row.get("description"));
            childRow.put("aliases", row.get("aliases"));
            childRow.put("tags", row.get("tags"));
            childRow.put("createdAt", row.get("createdAt"));
            childRow.put("updatedAt", row.get("updatedAt"));
            childRow.put("properties", row.get("properties"));
            nodes.add(mapNodeRow(childRow));

            Map<String, Object> relRow = new LinkedHashMap<>();
            relRow.put("id", row.get("relId"));
            relRow.put("fromNodeId", row.get("fromNodeId"));
            relRow.put("toNodeId", row.get("toNodeId"));
            relRow.put("predicate", row.get("predicate"));
            relRow.put("description", row.get("relDescription"));
            relRow.put("weight", row.get("weight"));
            relRow.put("createdAt", row.get("relCreatedAt"));
            relRow.put("updatedAt", row.get("relUpdatedAt"));
            relRow.put("properties", row.get("relProperties"));
            edges.add(mapRelationshipRow(relRow));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nodes", nodes);
        result.put("edges", edges);
        return result;
    }

    /**
     * 创建节点关系。
     *
     * @param request 关系创建请求
     * @return 新建关系响应
     */
    public KgRelationshipResponse createRelationship(KgRelationshipCreateRequest request, UserPrincipal principal) {
        ensureEnabled();
        if (request.fromNodeId().equals(request.toNodeId())) {
            throw badRequest("关系起点和终点不能相同");
        }
        String now = Instant.now().toString();
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("predicate", normalizeRequiredText(request.predicate(), "关系谓词"));
        putOptionalText(props, "description", request.description());
        if (request.weight() != null) {
            props.put("weight", request.weight());
        }
        props.put("createdAt", now);
        props.put("updatedAt", now);
        putCreatedAndUpdatedOperatorProps(props, principal);
        props.putAll(sanitizeAttributes(request.attributes(), RESERVED_RELATIONSHIP_KEYS));

        String cypher = """
                MATCH (from:KgNode) WHERE id(from) = $fromNodeId
                MATCH (to:KgNode) WHERE id(to) = $toNodeId
                CREATE (from)-[r:KG_REL]->(to)
                SET r += $props
                RETURN id(r) AS id,
                       id(startNode(r)) AS fromNodeId,
                       id(endNode(r)) AS toNodeId,
                       r.predicate AS predicate,
                       r.description AS description,
                       r.weight AS weight,
                       r.createdAt AS createdAt,
                       r.updatedAt AS updatedAt,
                       properties(r) AS properties
                """;
        Map<String, Object> row = neo4jClient.query(cypher)
                .bind(request.fromNodeId()).to("fromNodeId")
                .bind(request.toNodeId()).to("toNodeId")
                .bind(props).to("props")
                .fetch().one()
                .orElseThrow(() -> notFound("实体不存在，无法创建关系"));
        KgRelationshipResponse response = mapRelationshipRow(row);
        recordKgChange("RELATIONSHIP", response.id(), "CREATE", response.predicate(), principal);
        return response;
    }

    /**
     * 查询单个关系详情。
     *
     * @param relationshipId 关系主键
     * @return 关系响应
     */
    public KgRelationshipResponse getRelationship(Long relationshipId) {
        ensureEnabled();
        String cypher = """
                MATCH ()-[r:KG_REL]->()
                WHERE id(r) = $id
                RETURN id(r) AS id,
                       id(startNode(r)) AS fromNodeId,
                       id(endNode(r)) AS toNodeId,
                       r.predicate AS predicate,
                       r.description AS description,
                       r.weight AS weight,
                       r.createdAt AS createdAt,
                       r.updatedAt AS updatedAt,
                       properties(r) AS properties
                """;
        Map<String, Object> row = neo4jClient.query(cypher)
                .bind(relationshipId).to("id")
                .fetch().one()
                .orElseThrow(() -> notFound("关系不存在: " + relationshipId));
        return mapRelationshipRow(row);
    }

    /**
     * 查询节点关联关系列表。
     *
     * @param nodeId 节点主键
     * @param limit 返回条数上限
     * @return 关系列表
     */
    public List<KgRelationshipResponse> listRelationshipsByNode(Long nodeId, Integer limit) {
        ensureEnabled();
        ensureNodeExists(nodeId);
        int safeLimit = normalizeLimit(limit, 50, 1, 300);
        String cypher = """
                MATCH (center:KgNode)
                WHERE id(center) = $nodeId
                MATCH (center)-[r:KG_REL]-(other:KgNode)
                RETURN id(r) AS id,
                       id(startNode(r)) AS fromNodeId,
                       id(endNode(r)) AS toNodeId,
                       r.predicate AS predicate,
                       r.description AS description,
                       r.weight AS weight,
                       r.createdAt AS createdAt,
                       r.updatedAt AS updatedAt,
                       properties(r) AS properties
                ORDER BY id(r) DESC
                LIMIT $limit
                """;
        return neo4jClient.query(cypher)
                .bind(nodeId).to("nodeId")
                .bind(safeLimit).to("limit")
                .fetch().all()
                .stream()
                .map(this::mapRelationshipRow)
                .toList();
    }

    /**
     * 更新关系属性。
     *
     * @param relationshipId 关系主键
     * @param request 关系更新请求
     * @return 更新后的关系响应
     */
    public KgRelationshipResponse updateRelationship(Long relationshipId,
                                                     KgRelationshipUpdateRequest request,
                                                     UserPrincipal principal) {
        ensureEnabled();
        Map<String, Object> props = new LinkedHashMap<>();
        if (request.predicate() != null && request.predicate().isBlank()) {
            throw badRequest("关系谓词不能为空");
        }
        putOptionalText(props, "predicate", request.predicate());
        putOptionalTextAllowNull(props, "description", request.description());
        if (request.weight() != null) {
            props.put("weight", request.weight());
        }
        props.putAll(sanitizeAttributes(request.attributes(), RESERVED_RELATIONSHIP_KEYS));
        props.put("updatedAt", Instant.now().toString());
        putUpdatedOperatorProps(props, principal);

        String cypher = """
                MATCH ()-[r:KG_REL]->()
                WHERE id(r) = $id
                SET r += $props
                RETURN id(r) AS id,
                       id(startNode(r)) AS fromNodeId,
                       id(endNode(r)) AS toNodeId,
                       r.predicate AS predicate,
                       r.description AS description,
                       r.weight AS weight,
                       r.createdAt AS createdAt,
                       r.updatedAt AS updatedAt,
                       properties(r) AS properties
                """;
        Map<String, Object> row = neo4jClient.query(cypher)
                .bind(relationshipId).to("id")
                .bind(props).to("props")
                .fetch().one()
                .orElseThrow(() -> notFound("关系不存在: " + relationshipId));
        KgRelationshipResponse response = mapRelationshipRow(row);
        recordKgChange("RELATIONSHIP", response.id(), "UPDATE", response.predicate(), principal);
        return response;
    }

    /**
     * 删除指定关系。
     *
     * @param relationshipId 关系主键
     */
    public void deleteRelationship(Long relationshipId, UserPrincipal principal) {
        ensureEnabled();
        String cypher = """
                MATCH ()-[r:KG_REL]->()
                WHERE id(r) = $id
                WITH r, id(r) AS deletedId, r.predicate AS targetLabel
                DELETE r
                RETURN deletedId AS id, targetLabel AS targetLabel
                """;
        Map<String, Object> row = neo4jClient.query(cypher)
                .bind(relationshipId).to("id")
                .fetch().one()
                .orElse(null);
        if (row == null) {
            throw notFound("关系不存在: " + relationshipId);
        }
        recordKgChange("RELATIONSHIP", toLong(row.get("id")), "DELETE", asText(row.get("targetLabel")), principal);
    }

    /**
     * 查询问题相关的图谱上下文数据。
     *
     * @param request 上下文检索请求
     * @return 上下文响应
     */
    public KgContextResponse queryContext(KgContextRequest request) {
        ensureEnabled();
        int nodeLimit = normalizeLimit(
                request.maxNodes(),
                appProperties.getKnowledgeGraph().getContextNodeLimit(),
                1,
                20
        );
        int relationshipLimit = normalizeLimit(
                request.maxRelationships(),
                appProperties.getKnowledgeGraph().getContextRelationshipLimit(),
                1,
                200
        );
        return queryContextInternal(request.question(), nodeLimit, relationshipLimit);
    }

    public List<KgChangeLogResponse> listRecentChangeLogs() {
        return kgChangeLogRepository.findTop100ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toChangeLogResponse)
                .toList();
    }

    /**
     * 构造可供大模型直接消费的图谱上下文提示词。
     *
     * @param question 用户问题
     * @return 上下文提示；无结果时返回 null
     */
    public String buildLlmContextPrompt(String question) {
        KgContextResponse context = buildLlmContext(question);
        return context == null ? null : context.promptContext();
    }

    /**
     * 构造包含提示词与来源的图谱上下文。
     *
     * @param question 用户问题
     * @return 图谱上下文响应；无结果时返回 null
     */
    public KgContextResponse buildLlmContext(String question) {
        log.debug("开始构建知识图谱上下文，问题: {}", question);

        if (!appProperties.getKnowledgeGraph().isEnabled()) {
            log.debug("知识图谱功能未启用");
            return null;
        }
        if (neo4jClient == null) {
            log.debug("Neo4j 客户端未初始化");
            return null;
        }
        if (!StringUtils.hasText(question)) {
            log.debug("问题为空");
            return null;
        }
        try {
            KgContextResponse response = queryContextInternal(
                    question,
                    normalizeLimit(
                            appProperties.getKnowledgeGraph().getContextNodeLimit(),
                            6,
                            1,
                            20
                    ),
                    normalizeLimit(
                            appProperties.getKnowledgeGraph().getContextRelationshipLimit(),
                            24,
                            1,
                            200
                    )
            );
            if (response.nodes().isEmpty()) {
                log.debug("知识图谱查询未返回任何节点");
                return null;
            }
            log.debug("知识图谱查询成功，返回 {} 个节点，{} 个关系，{} 个来源",
                    response.nodes().size(),
                    response.relationships().size(),
                    response.sources().size());
            return response;
        } catch (Exception ex) {
            log.warn("知识图谱上下文检索失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private KgContextResponse queryContextInternal(String question, int nodeLimit, int relationshipLimit) {
        log.debug("queryContextInternal 被调用，问题: {}, nodeLimit: {}, relationshipLimit: {}", question, nodeLimit, relationshipLimit);

        List<String> terms = extractTerms(question);
        log.debug("从问题中提取的关键词: {}", terms);

        if (terms.isEmpty()) {
            log.debug("未提取到任何关键词");
            return new KgContextResponse("", List.of(), List.of(), List.of());
        }

        log.debug("准备执行 Cypher 查询，关键词: {}", terms);

        String nodeCypher = """
                MATCH (n:KgNode)
                WHERE any(term IN $terms WHERE
                        toLower(coalesce(n.name, '')) CONTAINS term
                        OR toLower(coalesce(n.category, '')) CONTAINS term
                        OR toLower(coalesce(n.description, '')) CONTAINS term
                        OR any(alias IN coalesce(n.aliases, []) WHERE toLower(alias) CONTAINS term)
                        OR any(tag IN coalesce(n.tags, []) WHERE toLower(tag) CONTAINS term)
                )
                RETURN id(n) AS id,
                       n.name AS name,
                       n.category AS category,
                       n.description AS description,
                       coalesce(n.aliases, []) AS aliases,
                       coalesce(n.tags, []) AS tags,
                       n.createdAt AS createdAt,
                       n.updatedAt AS updatedAt,
                       properties(n) AS properties
                ORDER BY coalesce(n.updatedAt, n.createdAt) DESC, id(n) DESC
                LIMIT $limit
                """;

        log.debug("执行节点查询，绑定参数 terms: {}, limit: {}", terms, nodeLimit);

        List<KgNodeResponse> nodes = neo4jClient.query(nodeCypher)
                .bind(terms).to("terms")
                .bind(nodeLimit).to("limit")
                .fetch().all()
                .stream()
                .map(this::mapNodeRow)
                .toList();

        log.debug("知识图谱节点查询完成，匹配到 {} 个节点", nodes.size());

        if (nodes.isEmpty()) {
            return new KgContextResponse("", List.of(), List.of(), List.of());
        }

        List<Long> nodeIds = nodes.stream().map(KgNodeResponse::id).toList();
        String relationshipCypher = """
                MATCH (a:KgNode)-[r:KG_REL]-(b:KgNode)
                WHERE id(a) IN $nodeIds OR id(b) IN $nodeIds
                WITH DISTINCT r
                RETURN id(r) AS id,
                       id(startNode(r)) AS fromNodeId,
                       id(endNode(r)) AS toNodeId,
                       r.predicate AS predicate,
                       r.description AS description,
                       r.weight AS weight,
                       r.createdAt AS createdAt,
                       r.updatedAt AS updatedAt,
                       properties(r) AS properties
                ORDER BY id(r) DESC
                LIMIT $limit
                """;
        List<KgRelationshipResponse> relationships = neo4jClient.query(relationshipCypher)
                .bind(nodeIds).to("nodeIds")
                .bind(relationshipLimit).to("limit")
                .fetch().all()
                .stream()
                .map(this::mapRelationshipRow)
                .toList();

        log.debug("知识图谱关系查询完成，匹配到 {} 个关系", relationships.size());

        List<KgSourceReference> sources = collectSources(nodes, relationships);
        String promptContext = buildPromptContext(question, nodes, relationships, sources);
        return new KgContextResponse(promptContext, nodes, relationships, sources);
    }

    private String buildPromptContext(String question,
                                      List<KgNodeResponse> nodes,
                                      List<KgRelationshipResponse> relationships,
                                      List<KgSourceReference> sources) {
        if (nodes.isEmpty()) {
            return "";
        }
        Map<Long, String> nodeNames = nodes.stream()
                .collect(Collectors.toMap(KgNodeResponse::id, KgNodeResponse::name, (a, b) -> a));

        StringBuilder sb = new StringBuilder();
        sb.append("以下是从旅游知识图谱检索到的事实。仅在与用户问题直接相关时引用；不确定时请明确说明。\n");
        sb.append("用户问题：").append(question.trim()).append("\n");
        sb.append("实体事实：\n");
        for (KgNodeResponse node : nodes) {
            sb.append("- [").append(node.id()).append("] ")
                    .append(defaultText(node.name(), "未命名实体"));
            if (StringUtils.hasText(node.category())) {
                sb.append("（类别: ").append(node.category()).append("）");
            }
            if (StringUtils.hasText(node.description())) {
                sb.append("：").append(node.description());
            }
            if (!node.tags().isEmpty()) {
                sb.append("；标签=").append(String.join("、", node.tags()));
            }
            sb.append("\n");
        }

        if (!relationships.isEmpty()) {
            sb.append("关系事实：\n");
            for (KgRelationshipResponse rel : relationships) {
                String fromName = nodeNames.getOrDefault(rel.fromNodeId(), "实体" + rel.fromNodeId());
                String toName = nodeNames.getOrDefault(rel.toNodeId(), "实体" + rel.toNodeId());
                sb.append("- ")
                        .append(fromName)
                        .append(" -[")
                        .append(defaultText(rel.predicate(), "相关"))
                        .append("]-> ")
                        .append(toName);
                if (StringUtils.hasText(rel.description())) {
                    sb.append("（").append(rel.description()).append("）");
                }
                sb.append("\n");
            }
        }

        if (!sources.isEmpty()) {
            sb.append("可引用来源：\n");
            int sourceLimit = Math.min(6, sources.size());
            for (int i = 0; i < sourceLimit; i++) {
                KgSourceReference source = sources.get(i);
                sb.append("- [S").append(i + 1).append("] ")
                        .append(defaultText(source.title(), "未命名来源"));
                if (StringUtils.hasText(source.sourceType())) {
                    sb.append("（").append(source.sourceType()).append("）");
                } else if (StringUtils.hasText(source.source())) {
                    sb.append("（").append(source.source()).append("）");
                }
                if (StringUtils.hasText(source.url())) {
                    sb.append(" - ").append(source.url());
                }
                sb.append("\n");
            }
            sb.append("若回答使用了以上知识图谱事实，请在回答末尾附“数据来源：”并列出对应来源。\n");
        }

        int maxChars = 2800;
        if (sb.length() > maxChars) {
            return sb.substring(0, maxChars) + "\n[知识图谱上下文已截断]";
        }
        return sb.toString();
    }

    private List<KgSourceReference> collectSources(List<KgNodeResponse> nodes,
                                                   List<KgRelationshipResponse> relationships) {
        Map<String, KgSourceReference> unique = new LinkedHashMap<>();

        for (KgRelationshipResponse relationship : relationships) {
            addSourceReference(
                    unique,
                    asText(relationship.attributes().get("sourceUrl")),
                    defaultText(relationship.description(), relationship.predicate()),
                    asText(relationship.attributes().get("source")),
                    null,
                    null
            );
        }

        for (KgNodeResponse node : nodes) {
            Map<String, Object> attributes = node.attributes();
            log.debug("节点 {} 的 attributes: {}", node.name(), attributes);
            addSourceReference(
                    unique,
                    asText(attributes.get("sourceUrl")),
                    node.name(),
                    asText(attributes.get("source")),
                    asText(attributes.get("sourceType")),
                    asText(attributes.get("sourceDomain"))
            );
            addSourceReference(
                    unique,
                    asText(attributes.get("wikipediaUrl")),
                    node.name() + "（Wikipedia）",
                    "Wikipedia",
                    "百科",
                    "wikipedia.org"
            );
        }

        log.debug("收集到的来源数量: {}", unique.size());
        return new ArrayList<>(unique.values());
    }

    private void addSourceReference(Map<String, KgSourceReference> unique,
                                    String url,
                                    String title,
                                    String source,
                                    String sourceType,
                                    String domain) {
        String normalizedUrl = url == null ? "" : url.trim();
        if (normalizedUrl.isEmpty()) {
            return;
        }
        unique.putIfAbsent(
                normalizedUrl,
                new KgSourceReference(
                        defaultText(title, "未命名来源"),
                        normalizedUrl,
                        source,
                        sourceType,
                        domain
                )
        );
    }

    private List<String> extractTerms(String question) {
        if (!StringUtils.hasText(question)) {
            return List.of();
        }
        int termLimit = normalizeLimit(
                appProperties.getKnowledgeGraph().getContextTermLimit(),
                8,
                1,
                20
        );
        String lower = question.toLowerCase(Locale.ROOT);
        log.debug("原始问题（小写）: {}", lower);

        LinkedHashSet<String> terms = new LinkedHashSet<>();
        List<SegToken> tokens = SEGMENTER.process(lower, SegMode.SEARCH);
        for (SegToken token : tokens) {
            if (terms.size() >= termLimit) break;
            String word = token.word.trim();
            // 跳过空串、纯空白、纯标点
            if (word.isEmpty() || !word.matches("[\\p{IsHan}a-z0-9]+")) continue;
            // 单字符无意义（含单个汉字和单个字母）
            if (word.length() < 2) continue;
            // 英文/数字词要求至少 3 个字符，避免缩写噪声
            if (word.matches("[a-z0-9]+") && word.length() < 3) continue;
            if (STOP_WORDS.contains(word)) continue;
            log.debug("分词保留词: {}", word);
            terms.add(word);
        }

        if (terms.isEmpty()) {
            String fallback = lower.trim();
            if (!fallback.isEmpty()) {
                log.debug("分词无结果，使用原始问题作为 fallback: {}", fallback);
                terms.add(fallback);
            }
        }
        log.debug("最终提取的关键词列表: {}", terms);
        return new ArrayList<>(terms);
    }

    private void ensureNodeExists(Long nodeId) {
        String cypher = """
                MATCH (n:KgNode)
                WHERE id(n) = $id
                RETURN count(n) > 0 AS exists
                """;
        boolean exists = Boolean.TRUE.equals(
                neo4jClient.query(cypher)
                        .bind(nodeId).to("id")
                        .fetchAs(Boolean.class)
                        .one()
                        .orElse(false)
        );
        if (!exists) {
            throw notFound("实体不存在: " + nodeId);
        }
    }

    private Map<String, Object> fetchNodeRow(Long nodeId) {
        String cypher = """
                MATCH (n:KgNode)
                WHERE id(n) = $id
                RETURN id(n) AS id,
                       n.name AS name,
                       n.category AS category,
                       n.description AS description,
                       coalesce(n.aliases, []) AS aliases,
                       coalesce(n.tags, []) AS tags,
                       n.createdAt AS createdAt,
                       n.updatedAt AS updatedAt,
                       properties(n) AS properties
                """;
        return neo4jClient.query(cypher)
                .bind(nodeId).to("id")
                .fetch().one()
                .orElse(null);
    }

    private KgNodeResponse mapNodeRow(Map<String, Object> row) {
        Map<String, Object> properties = asMap(row.get("properties"));
        Map<String, Object> attributes = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (!RESERVED_NODE_KEYS.contains(entry.getKey())) {
                attributes.put(entry.getKey(), entry.getValue());
            }
        }
        return new KgNodeResponse(
                toLong(row.get("id")),
                asText(row.get("name")),
                asText(row.get("category")),
                asText(row.get("description")),
                asStringList(row.get("aliases")),
                asStringList(row.get("tags")),
                attributes,
                asText(row.get("createdAt")),
                asText(row.get("updatedAt")),
                toLong(properties.get("createdByUserId")),
                asText(properties.get("createdByUsername")),
                asText(properties.get("createdByDisplayName")),
                toLong(properties.get("updatedByUserId")),
                asText(properties.get("updatedByUsername")),
                asText(properties.get("updatedByDisplayName"))
        );
    }

    private KgRelationshipResponse mapRelationshipRow(Map<String, Object> row) {
        Map<String, Object> properties = asMap(row.get("properties"));
        Map<String, Object> attributes = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (!RESERVED_RELATIONSHIP_KEYS.contains(entry.getKey())) {
                attributes.put(entry.getKey(), entry.getValue());
            }
        }
        return new KgRelationshipResponse(
                toLong(row.get("id")),
                toLong(row.get("fromNodeId")),
                toLong(row.get("toNodeId")),
                asText(row.get("predicate")),
                asText(row.get("description")),
                toDouble(row.get("weight")),
                attributes,
                asText(row.get("createdAt")),
                asText(row.get("updatedAt")),
                toLong(properties.get("createdByUserId")),
                asText(properties.get("createdByUsername")),
                asText(properties.get("createdByDisplayName")),
                toLong(properties.get("updatedByUserId")),
                asText(properties.get("updatedByUsername")),
                asText(properties.get("updatedByDisplayName"))
        );
    }

    private void putCreatedAndUpdatedOperatorProps(Map<String, Object> props, UserPrincipal principal) {
        putCreatedOperatorProps(props, principal);
        putUpdatedOperatorProps(props, principal);
    }

    private void putCreatedOperatorProps(Map<String, Object> props, UserPrincipal principal) {
        props.put("createdByUserId", principal == null ? null : principal.getUserId());
        props.put("createdByUsername", principal == null ? null : principal.getUsername());
        props.put("createdByDisplayName", principal == null ? null : principal.getDisplayName());
    }

    private void putUpdatedOperatorProps(Map<String, Object> props, UserPrincipal principal) {
        props.put("updatedByUserId", principal == null ? null : principal.getUserId());
        props.put("updatedByUsername", principal == null ? null : principal.getUsername());
        props.put("updatedByDisplayName", principal == null ? null : principal.getDisplayName());
    }

    private void recordKgChange(String targetType,
                                Long targetId,
                                String action,
                                String targetLabel,
                                UserPrincipal principal) {
        KgChangeLog logEntry = new KgChangeLog();
        logEntry.setTargetType(targetType);
        logEntry.setTargetId(targetId);
        logEntry.setAction(action);
        logEntry.setTargetLabel(limitText(targetLabel, 160));
        if (principal != null) {
            logEntry.setOperatorUserId(principal.getUserId());
            logEntry.setOperatorUsername(principal.getUsername());
            logEntry.setOperatorDisplayName(principal.getDisplayName());
        }
        kgChangeLogRepository.save(logEntry);
    }

    private KgChangeLogResponse toChangeLogResponse(KgChangeLog logEntry) {
        return new KgChangeLogResponse(
                logEntry.getId(),
                logEntry.getTargetType(),
                logEntry.getTargetId(),
                logEntry.getAction(),
                logEntry.getTargetLabel(),
                logEntry.getOperatorUserId(),
                logEntry.getOperatorUsername(),
                logEntry.getOperatorDisplayName(),
                logEntry.getCreatedAt()
        );
    }

    private void putOptionalText(Map<String, Object> target, String key, String value) {
        if (value == null) {
            return;
        }
        String text = value.trim();
        if (!text.isEmpty()) {
            target.put(key, text);
        }
    }

    private String limitText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim();
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private void putOptionalTextAllowNull(Map<String, Object> target, String key, String value) {
        if (value == null) {
            return;
        }
        String text = value.trim();
        target.put(key, text.isEmpty() ? null : text);
    }

    private void putOptionalStringList(Map<String, Object> target, String key, List<String> values) {
        if (values == null) {
            return;
        }
        List<String> normalized = normalizeStringList(values);
        if (!normalized.isEmpty()) {
            target.put(key, normalized);
        }
    }

    private String normalizeRequiredText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw badRequest(fieldName + "不能为空");
        }
        return value.trim();
    }

    private List<String> normalizeStringList(List<String> values) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String item = value.trim();
            if (!item.isEmpty()) {
                set.add(item);
            }
        }
        return new ArrayList<>(set);
    }

    private Map<String, Object> sanitizeAttributes(Map<String, Object> raw, Set<String> reservedKeys) {
        Map<String, Object> cleaned = new LinkedHashMap<>();
        if (raw == null || raw.isEmpty()) {
            return cleaned;
        }
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            String key = entry.getKey().trim();
            if (key.isEmpty() || reservedKeys.contains(key) || !ATTRIBUTE_KEY_PATTERN.matcher(key).matches()) {
                continue;
            }
            Object value = normalizeAttributeValue(entry.getValue());
            if (value != null) {
                cleaned.put(key, value);
            }
        }
        return cleaned;
    }

    private Object normalizeAttributeValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof String str) {
            String text = str.trim();
            return text.isEmpty() ? null : text;
        }
        if (value instanceof List<?> list) {
            List<Object> normalized = new ArrayList<>();
            for (Object item : list) {
                Object scalar = normalizeScalarValue(item);
                if (scalar != null) {
                    normalized.add(scalar);
                }
            }
            return normalized;
        }
        return value.toString();
    }

    private Object normalizeScalarValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof String str) {
            String text = str.trim();
            return text.isEmpty() ? null : text;
        }
        return value.toString();
    }

    private String asText(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private List<String> asStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (item == null) {
                continue;
            }
            String text = item.toString().trim();
            if (!text.isEmpty()) {
                result.add(text);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                result.put(entry.getKey().toString(), entry.getValue());
            }
            return result;
        }
        return Map.of();
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private int normalizeLimit(Integer value, int defaultValue, int min, int max) {
        int resolved = value == null ? defaultValue : value;
        if (resolved < min) {
            return min;
        }
        return Math.min(resolved, max);
    }

    private ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST.value(), message);
    }

    private ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND.value(), message);
    }

    private void ensureEnabled() {
        if (!appProperties.getKnowledgeGraph().isEnabled()) {
            throw badRequest("知识图谱功能未启用");
        }
        if (neo4jClient == null) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    "Neo4j 未就绪，请检查 Neo4j 配置与连接状态"
            );
        }
    }
}
