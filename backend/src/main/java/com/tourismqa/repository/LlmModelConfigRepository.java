package com.tourismqa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tourismqa.entity.LlmModelConfig;

/**
 * LlmModelConfigRepository 数据访问仓储接口。
 * 使用场景：提供聚合根的数据查询与持久化能力，供服务层调用。
 * 核心职责：声明领域对象访问契约并复用 Spring Data 查询机制。
 */
public interface LlmModelConfigRepository extends JpaRepository<LlmModelConfig, Long> {

    List<LlmModelConfig> findAllByOrderByProviderAscModelIdAsc();

    List<LlmModelConfig> findByEnabledTrueOrderByProviderAscModelIdAsc();

    List<LlmModelConfig> findByProviderAndEnabledTrueOrderByDefaultModelDescModelIdAsc(String provider);

    List<LlmModelConfig> findByModelIdAndEnabledTrueOrderByDefaultModelDescProviderAsc(String modelId);

    Optional<LlmModelConfig> findByProviderAndModelIdAndEnabledTrue(String provider, String modelId);

    Optional<LlmModelConfig> findByProviderAndModelId(String provider, String modelId);

    Optional<LlmModelConfig> findFirstByDefaultModelTrueAndEnabledTrue();

    Optional<LlmModelConfig> findFirstByEnabledTrueOrderByProviderAscModelIdAsc();

    List<LlmModelConfig> findByDefaultModelTrue();

    boolean existsByProviderAndModelId(String provider, String modelId);

    boolean existsByProviderAndModelIdAndIdNot(String provider, String modelId, Long id);

    long countByEnabledTrue();
}
