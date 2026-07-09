package com.tourismqa.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tourismqa.dto.CreateRoutePlanRequest;
import com.tourismqa.dto.ExtractRouteDraftRequest;
import com.tourismqa.dto.ExtractRouteDraftResponse;
import com.tourismqa.dto.GeocodeRoutePointsRequest;
import com.tourismqa.dto.RoutePlanItemResponse;
import com.tourismqa.dto.RoutePlanRequest;
import com.tourismqa.dto.RoutePlanResponse;
import com.tourismqa.dto.RoutePointDto;
import com.tourismqa.dto.UpdateRoutePlanRequest;
import com.tourismqa.security.UserPrincipal;
import com.tourismqa.service.RoutePlannerService;

import jakarta.validation.Valid;

/**
 * 旅游路线规划控制器。
 * 使用场景：
 * 为前端路线可视化模块提供路线生成入口。
 * 核心职责：
 * 1. 接收路线规划请求并完成参数校验。
 * 2. 注入当前登录用户上下文用于数据归属控制。
 * 3. 转发业务至路线规划服务并返回结构化结果。
 *
 * <p>框架作用：`@RestController` 将业务返回对象自动转换为 JSON 响应。</p>
 */
@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RoutePlannerService routePlannerService;

    public RouteController(RoutePlannerService routePlannerService) {
        this.routePlannerService = routePlannerService;
    }

    /**
     * 生成旅游路线方案。
     *
     * @param request 路线规划请求
     * @param principal 当前登录用户
     * @return 路线规划结果
     */
    @PostMapping("/plan")
    public RoutePlanResponse plan(@Valid @RequestBody RoutePlanRequest request,
                                  @AuthenticationPrincipal UserPrincipal principal) {
        return routePlannerService.plan(request, principal);
    }

    @PostMapping
    public RoutePlanResponse create(@Valid @RequestBody CreateRoutePlanRequest request,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        return routePlannerService.createPlan(request, principal);
    }

    @PostMapping("/extract-draft")
    public ExtractRouteDraftResponse extractDraft(@Valid @RequestBody ExtractRouteDraftRequest request) {
        return routePlannerService.extractDraft(request);
    }

    @PostMapping("/geocode-points")
    public List<RoutePointDto> geocodePoints(@Valid @RequestBody GeocodeRoutePointsRequest request) {
        return routePlannerService.geocodePoints(request);
    }

    @GetMapping
    public List<RoutePlanItemResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return routePlannerService.listPlans(principal);
    }

    @GetMapping("/{routePlanId}")
    public RoutePlanResponse detail(@PathVariable Long routePlanId,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        return routePlannerService.getPlan(routePlanId, principal);
    }

    @PutMapping("/{routePlanId}")
    public RoutePlanResponse update(@PathVariable Long routePlanId,
                                    @Valid @RequestBody UpdateRoutePlanRequest request,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        return routePlannerService.updatePlan(routePlanId, request, principal);
    }

    @DeleteMapping("/{routePlanId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long routePlanId,
                       @AuthenticationPrincipal UserPrincipal principal) {
        routePlannerService.deletePlan(routePlanId, principal);
    }
}
