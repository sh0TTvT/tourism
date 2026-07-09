package com.tourismqa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tourismqa.entity.RoutePoint;

/**
 * RoutePointRepository 数据访问仓储接口。
 * 使用场景：提供聚合根的数据查询与持久化能力，供服务层调用。
 * 核心职责：声明领域对象访问契约并复用 Spring Data 查询机制。
 */
public interface RoutePointRepository extends JpaRepository<RoutePoint, Long> {

    List<RoutePoint> findByRoutePlan_IdOrderByDayNoAscPointOrderAsc(Long routePlanId);

    void deleteByRoutePlan_Id(Long routePlanId);
}
