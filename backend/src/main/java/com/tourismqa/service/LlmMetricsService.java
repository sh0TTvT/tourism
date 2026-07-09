package com.tourismqa.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tourismqa.entity.LlmCallMetric;
import com.tourismqa.entity.LlmModelConfig;
import com.tourismqa.repository.LlmCallMetricRepository;
import com.tourismqa.repository.LlmModelConfigRepository;

/**
 * 大模型调用指标记录服务。
 */
@Service
public class LlmMetricsService {

    private final LlmCallMetricRepository metricRepository;
    private final LlmModelConfigRepository modelRepository;

    public LlmMetricsService(LlmCallMetricRepository metricRepository,
                             LlmModelConfigRepository modelRepository) {
        this.metricRepository = metricRepository;
        this.modelRepository = modelRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordCall(LlmModelConfig config, long latencyMs, boolean success, String errorMessage) {
        if (config == null) {
            return;
        }
        long safeLatency = Math.max(0, latencyMs);
        LlmCallMetric metric = new LlmCallMetric();
        metric.setProvider(config.getProvider());
        metric.setModel(config.getModelId());
        metric.setSuccess(success);
        metric.setLatencyMs(safeLatency);
        metric.setErrorMessage(limit(errorMessage, 255));
        metricRepository.save(metric);

        if (config.getId() == null) {
            return;
        }
        modelRepository.findById(config.getId()).ifPresent(model -> {
            long totalCalls = model.getTotalCallCount() + 1;
            long totalLatency = model.getTotalLatencyMs() + safeLatency;
            model.setTotalCallCount(totalCalls);
            model.setTotalLatencyMs(totalLatency);
            model.setAverageLatencyMs(totalLatency / totalCalls);
            model.setLastLatencyMs(safeLatency);
            model.setLastCalledAt(Instant.now());
            if (success) {
                model.setSuccessfulCallCount(model.getSuccessfulCallCount() + 1);
            } else {
                model.setFailedCallCount(model.getFailedCallCount() + 1);
            }
            modelRepository.save(model);
        });
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
