package com.tourismqa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tourismqa.entity.ManagedExternalServiceConfig;

/**
 * 外部服务配置仓储。
 */
public interface ManagedExternalServiceConfigRepository extends JpaRepository<ManagedExternalServiceConfig, Long> {

    Optional<ManagedExternalServiceConfig> findByServiceKey(String serviceKey);

    List<ManagedExternalServiceConfig> findAllByOrderByServiceKeyAsc();
}
