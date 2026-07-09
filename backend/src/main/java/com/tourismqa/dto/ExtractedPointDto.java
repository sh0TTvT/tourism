package com.tourismqa.dto;

public record ExtractedPointDto(
        int day,
        int order,
        String name,
        String description,
        Double latitude,
        Double longitude
) {
}
