package com.tourismqa.dto;

import java.time.LocalDate;
import java.util.List;

public record ExtractRouteDraftResponse(
        String destination,
        Integer days,
        LocalDate startDate,
        LocalDate endDate,
        String interests,
        String budget,
        String departure,
        String title,
        String summary,
        List<ExtractedPointDto> points,
        List<String> tips
) {
}
