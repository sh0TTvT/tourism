package com.tourismqa.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tourismqa.dto.AdminExternalServiceItemResponse;
import com.tourismqa.dto.AdminExternalServiceSaveRequest;
import com.tourismqa.dto.AdminExternalServiceSaveResponse;
import com.tourismqa.dto.AdminExternalServiceTestResponse;
import com.tourismqa.service.ServiceManagementService;

import jakarta.validation.Valid;

/**
 * 管理端服务管理控制器。
 */
@RestController
@RequestMapping("/api/admin/services")
@PreAuthorize("hasRole('ADMIN')")
public class AdminServiceController {

    private final ServiceManagementService serviceManagementService;

    public AdminServiceController(ServiceManagementService serviceManagementService) {
        this.serviceManagementService = serviceManagementService;
    }

    @GetMapping
    public List<AdminExternalServiceItemResponse> listServices() {
        return serviceManagementService.listServicesForAdmin();
    }

    @PutMapping("/{serviceKey}")
    public AdminExternalServiceSaveResponse saveService(@PathVariable String serviceKey,
                                                        @Valid @RequestBody AdminExternalServiceSaveRequest request) {
        return serviceManagementService.saveService(serviceKey, request);
    }

    @PostMapping("/{serviceKey}/test")
    public AdminExternalServiceTestResponse testService(@PathVariable String serviceKey) {
        return serviceManagementService.testService(serviceKey);
    }
}
