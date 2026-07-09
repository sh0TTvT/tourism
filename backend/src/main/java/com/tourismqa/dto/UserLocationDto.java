package com.tourismqa.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

/**
 * 用户端浏览器定位信息。
 */
public record UserLocationDto(
        @DecimalMin(value = "-90.0", message = "纬度不能小于 -90")
        @DecimalMax(value = "90.0", message = "纬度不能大于 90")
        Double latitude,

        @DecimalMin(value = "-180.0", message = "经度不能小于 -180")
        @DecimalMax(value = "180.0", message = "经度不能大于 180")
        Double longitude,

        Double accuracy,
        String label,
        String capturedAt
) {
}
