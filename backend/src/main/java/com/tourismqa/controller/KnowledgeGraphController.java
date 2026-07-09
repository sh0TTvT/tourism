package com.tourismqa.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tourismqa.dto.KgContextRequest;
import com.tourismqa.dto.KgContextResponse;
import com.tourismqa.dto.KgChangeLogResponse;
import com.tourismqa.dto.KgNodeCreateRequest;
import com.tourismqa.dto.KgNodeResponse;
import com.tourismqa.dto.KgNodeUpdateRequest;
import com.tourismqa.dto.KgRelationshipCreateRequest;
import com.tourismqa.dto.KgRelationshipResponse;
import com.tourismqa.dto.KgRelationshipUpdateRequest;
import com.tourismqa.security.UserPrincipal;
import com.tourismqa.service.KnowledgeGraphService;

import jakarta.validation.Valid;

/**
 * 知识图谱管理与检索控制器。
 * 使用场景：
 * 为后台维护端提供图谱节点/关系的增删改查以及上下文检索接口。
 * 核心职责：
 * 1. 承接图谱管理请求并执行参数校验。
 * 2. 调用知识图谱服务完成图数据库读写。
 * 3. 通过管理员权限控制图谱写操作入口。
 *
 * <p>框架作用：`@PreAuthorize` 在控制器层统一施加管理员访问约束。</p>
 */
@RestController
@RequestMapping("/api/kg")
@PreAuthorize("hasRole('ADMIN')")
public class KnowledgeGraphController {

    private final KnowledgeGraphService knowledgeGraphService;

    public KnowledgeGraphController(KnowledgeGraphService knowledgeGraphService) {
        this.knowledgeGraphService = knowledgeGraphService;
    }

    /**
     * 创建图谱节点。
     *
     * @param request 节点创建请求
     * @return 新建节点信息
     */
    @PostMapping("/nodes")
    public KgNodeResponse createNode(@Valid @RequestBody KgNodeCreateRequest request,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        return knowledgeGraphService.createNode(request, principal);
    }

    /**
     * 按节点 ID 查询节点详情。
     *
     * @param nodeId 节点主键
     * @return 节点详情
     */
    @GetMapping("/nodes/{nodeId}")
    public KgNodeResponse getNode(@PathVariable Long nodeId) {
        return knowledgeGraphService.getNode(nodeId);
    }

    /**
     * 按关键字检索节点列表。
     *
     * @param keyword 可选关键字
     * @param limit 可选返回上限
     * @param category 可选类别筛选
     * @return 节点结果集
     */
    @GetMapping("/nodes")
    public List<KgNodeResponse> searchNodes(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer limit,
                                            @RequestParam(required = false) String category) {
        // 如果指定了 category 为 "城市"，则调用专门的方法
        if ("城市".equals(category) && keyword == null) {
            return knowledgeGraphService.getCityNodes();
        }
        return knowledgeGraphService.searchNodes(keyword, limit);
    }

    /**
     * 获取所有市级节点。
     *
     * @return 市级节点列表
     */
    @GetMapping("/cities")
    public List<KgNodeResponse> getCityNodes() {
        return knowledgeGraphService.getCityNodes();
    }

    /**
     * 获取城市之间的关系。
     *
     * @return 城市间关系列表
     */
    @GetMapping("/cities/relationships")
    public List<KgRelationshipResponse> getCityRelationships() {
        return knowledgeGraphService.getCityRelationships();
    }

    /**
     * 展开指定市节点，获取其子节点和关系。
     *
     * @param cityId 市节点 ID
     * @return 包含子节点和关系的响应
     */
    @GetMapping("/cities/{cityId}/expand")
    public java.util.Map<String, Object> expandCityNode(@PathVariable Long cityId) {
        return knowledgeGraphService.expandCityNode(cityId);
    }

    /**
     * 按类别获取节点列表，用于多层级图谱概览。
     *
     * @param category 类别名（国家/省份/城市/区县/景点/酒店/美食/名人 等）
     * @return 同类别节点列表
     */
    @GetMapping("/nodes/by-category/{category}")
    public List<KgNodeResponse> getNodesByCategory(@PathVariable String category) {
        return knowledgeGraphService.getNodesByCategory(category);
    }

    /**
     * 按类别获取同层级节点之间的关系。
     *
     * @param category 类别名
     * @return 同类别节点间关系列表
     */
    @GetMapping("/relationships/by-category/{category}")
    public List<KgRelationshipResponse> getRelationshipsByCategory(@PathVariable String category) {
        return knowledgeGraphService.getRelationshipsByCategory(category);
    }

    /**
     * 通用节点展开：获取任意节点的下级（出边）节点和对应关系。
     *
     * @param nodeId 节点 ID
     * @return 包含子节点和关系的响应
     */
    @GetMapping("/nodes/{nodeId}/expand")
    public java.util.Map<String, Object> expandNode(@PathVariable Long nodeId) {
        return knowledgeGraphService.expandNode(nodeId);
    }

    /**
     * 更新指定节点属性。
     *
     * @param nodeId 节点主键
     * @param request 节点更新请求
     * @return 更新后的节点详情
     */
    @PutMapping("/nodes/{nodeId}")
    public KgNodeResponse updateNode(@PathVariable Long nodeId,
                                     @Valid @RequestBody KgNodeUpdateRequest request,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        return knowledgeGraphService.updateNode(nodeId, request, principal);
    }

    /**
     * 删除指定节点。
     *
     * @param nodeId 节点主键
     */
    @DeleteMapping("/nodes/{nodeId}")
    public void deleteNode(@PathVariable Long nodeId,
                           @AuthenticationPrincipal UserPrincipal principal) {
        knowledgeGraphService.deleteNode(nodeId, principal);
    }

    /**
     * 创建节点关系。
     *
     * @param request 关系创建请求
     * @return 新建关系信息
     */
    @PostMapping("/relationships")
    public KgRelationshipResponse createRelationship(@Valid @RequestBody KgRelationshipCreateRequest request,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        return knowledgeGraphService.createRelationship(request, principal);
    }

    /**
     * 按关系 ID 查询关系详情。
     *
     * @param relationshipId 关系主键
     * @return 关系详情
     */
    @GetMapping("/relationships/{relationshipId}")
    public KgRelationshipResponse getRelationship(@PathVariable Long relationshipId) {
        return knowledgeGraphService.getRelationship(relationshipId);
    }

    /**
     * 查询指定节点关联的关系集合。
     *
     * @param nodeId 节点主键
     * @param limit 可选返回上限
     * @return 关系列表
     */
    @GetMapping("/relationships")
    public List<KgRelationshipResponse> listRelationshipsByNode(@RequestParam Long nodeId,
                                                                @RequestParam(required = false) Integer limit) {
        return knowledgeGraphService.listRelationshipsByNode(nodeId, limit);
    }

    /**
     * 更新指定关系属性。
     *
     * @param relationshipId 关系主键
     * @param request 关系更新请求
     * @return 更新后的关系详情
     */
    @PutMapping("/relationships/{relationshipId}")
    public KgRelationshipResponse updateRelationship(@PathVariable Long relationshipId,
                                                     @Valid @RequestBody KgRelationshipUpdateRequest request,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        return knowledgeGraphService.updateRelationship(relationshipId, request, principal);
    }

    /**
     * 删除指定关系。
     *
     * @param relationshipId 关系主键
     */
    @DeleteMapping("/relationships/{relationshipId}")
    public void deleteRelationship(@PathVariable Long relationshipId,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        knowledgeGraphService.deleteRelationship(relationshipId, principal);
    }

    /**
     * 按问题上下文检索图谱相关信息。
     *
     * @param request 上下文检索请求
     * @return 检索结果响应
     */
    @PostMapping("/context")
    public KgContextResponse context(@Valid @RequestBody KgContextRequest request) {
        return knowledgeGraphService.queryContext(request);
    }

    /**
     * 查询最近知识图谱变更日志。
     *
     * @return 最近 100 条变更记录
     */
    @GetMapping("/change-logs")
    public List<KgChangeLogResponse> listRecentChangeLogs() {
        return knowledgeGraphService.listRecentChangeLogs();
    }
}
