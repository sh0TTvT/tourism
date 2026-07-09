package com.tourismqa.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 受管外部服务初始化器。
 * 使用场景：
 * 应用启动后确保地图与天气服务的默认配置写入数据库。
 */
@Component
public class ManagedExternalServiceInitializer {

    private final ServiceManagementService serviceManagementService;

    public ManagedExternalServiceInitializer(ServiceManagementService serviceManagementService) {
        this.serviceManagementService = serviceManagementService;
    }

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void initializeDefaults() {
        serviceManagementService.initializeDefaults();
    }
}
