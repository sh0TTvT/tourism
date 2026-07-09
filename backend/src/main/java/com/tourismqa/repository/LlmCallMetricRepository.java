package com.tourismqa.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tourismqa.entity.LlmCallMetric;

public interface LlmCallMetricRepository extends JpaRepository<LlmCallMetric, Long> {
}
