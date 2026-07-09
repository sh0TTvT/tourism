package com.tourismqa.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tourismqa.dto.PublicServiceStatusResponse;
import com.tourismqa.service.ServiceManagementService;

/**
 * 用户端公共服务状态控制器。
 */
@RestController
@RequestMapping("/api/public/services")
public class PublicServiceController {

    private final ServiceManagementService serviceManagementService;

    public PublicServiceController(ServiceManagementService serviceManagementService) {
        this.serviceManagementService = serviceManagementService;
    }

    @GetMapping("/status")
    public PublicServiceStatusResponse status() {
        return serviceManagementService.getPublicServiceStatus();
    }
}
