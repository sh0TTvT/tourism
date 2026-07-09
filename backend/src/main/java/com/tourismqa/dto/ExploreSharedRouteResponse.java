package com.tourismqa.dto;

import java.util.List;

public record ExploreSharedRouteResponse(
        String title,
        String summary,
        String destination,
        Integer days,
        String interests,
        String budget,
        String departure,
        List<RoutePointDto> points,
        List<String> tips
) {
}
