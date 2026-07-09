package com.tourismqa.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 外部服务心跳监测器。
 * 使用场景：
 * 每 24 小时执行一次真实探活，并在应用启动后补齐首次检测结果。
 */
@Component
public class ServiceHeartbeatMonitor {

    private static final Logger log = LoggerFactory.getLogger(ServiceHeartbeatMonitor.class);

    private final ServiceManagementService serviceManagementService;

    public ServiceHeartbeatMonitor(ServiceManagementService serviceManagementService) {
        this.serviceManagementService = serviceManagementService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void runStartupHeartbeat() {
        try {
            serviceManagementService.runStartupHeartbeatIfMissing();
        } catch (Exception ex) {
            log.warn("外部服务启动心跳检测失败: {}", ex.getMessage());
        }
    }

    @Scheduled(fixedDelay = 86_400_000L, initialDelay = 86_400_000L)
    public void runDailyHeartbeat() {
        try {
            serviceManagementService.runHeartbeatChecks();
        } catch (Exception ex) {
            log.warn("外部服务定时心跳检测失败: {}", ex.getMessage());
        }
    }
}
