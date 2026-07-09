package com.tourismqa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourismqa.dto.GeocodeRoutePointsRequest;
import com.tourismqa.dto.RoutePointSaveRequest;
import com.tourismqa.entity.RoutePlan;
import com.tourismqa.entity.UserAccount;
import com.tourismqa.entity.UserRole;
import com.tourismqa.exception.ApiException;
import com.tourismqa.repository.ChatConversationRepository;
import com.tourismqa.repository.RoutePlanRepository;
import com.tourismqa.repository.RoutePointRepository;
import com.tourismqa.repository.UserAccountRepository;
import com.tourismqa.security.UserPrincipal;

@ExtendWith(MockitoExtension.class)
class RoutePlannerServiceTest {

    @Mock
    private LlmRouterService llmRouterService;

    @Mock
    private ModelCatalogService modelCatalogService;

    @Mock
    private GeocodingService geocodingService;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private ChatConversationRepository chatConversationRepository;

    @Mock
    private RoutePlanRepository routePlanRepository;

    @Mock
    private RoutePointRepository routePointRepository;

    @Mock
    private KnowledgeGraphService knowledgeGraphService;

    private RoutePlannerService routePlannerService;

    @BeforeEach
    void setUp() {
        routePlannerService = new RoutePlannerService(
                llmRouterService,
                modelCatalogService,
                geocodingService,
                new ObjectMapper(),
                userAccountRepository,
                chatConversationRepository,
                routePlanRepository,
                routePointRepository,
                knowledgeGraphService
        );
    }

    @Test
    void deletePlan_allowsAdminToDeleteAnyRoute() {
        UserAccount admin = user(99L, UserRole.ADMIN);
        UserAccount owner = user(1L, UserRole.USER);
        RoutePlan routePlan = routePlan(10L, owner);

        when(userAccountRepository.findById(99L)).thenReturn(Optional.of(admin));
        when(routePlanRepository.findById(10L)).thenReturn(Optional.of(routePlan));

        routePlannerService.deletePlan(10L, new UserPrincipal(admin));

        verify(routePointRepository).deleteByRoutePlan_Id(10L);
        verify(routePlanRepository).delete(routePlan);
    }

    @Test
    void deletePlan_rejectsNonOwner() {
        UserAccount actor = user(2L, UserRole.USER);
        UserAccount owner = user(1L, UserRole.USER);
        RoutePlan routePlan = routePlan(10L, owner);

        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(actor));
        when(routePlanRepository.findById(10L)).thenReturn(Optional.of(routePlan));

        ApiException ex = assertThrows(ApiException.class,
                () -> routePlannerService.deletePlan(10L, new UserPrincipal(actor)));

        assertEquals(403, ex.getStatus());
        verify(routePointRepository, never()).deleteByRoutePlan_Id(10L);
        verify(routePlanRepository, never()).delete(routePlan);
    }

    @Test
    void extractDraft_geocodesExtractedPoints() {
        when(modelCatalogService.resolveModelSelection(null, null))
                .thenReturn(new LlmModelSelection("mock", "route-model"));
        when(llmRouterService.chatResolved(anyString(), anyString(), anyList(), anyDouble()))
                .thenReturn("""
                        {
                          "destination": "上海",
                          "days": 1,
                          "startDate": null,
                          "endDate": null,
                          "interests": "博物馆",
                          "budget": "舒适型",
                          "departure": "杭州",
                          "title": "上海一日游",
                          "summary": "城市文化路线",
                          "points": [
                            {"day": 1, "order": 1, "name": "上海博物馆", "description": "参观展览"}
                          ],
                          "tips": []
                        }
                        """);
        when(geocodingService.geocode("中国 上海市 上海博物馆")).thenReturn(new double[]{31.2304, 121.4737});

        var response = routePlannerService.extractDraft(new com.tourismqa.dto.ExtractRouteDraftRequest(
                List.of(new com.tourismqa.dto.ChatMessageDto("assistant", "上海一日游：上海博物馆"))
        ));

        assertEquals(1, response.points().size());
        assertEquals(31.2304, response.points().get(0).latitude());
        assertEquals(121.4737, response.points().get(0).longitude());
        verify(geocodingService).geocode("中国 上海市 上海博物馆");
    }

    @Test
    void geocodePoints_normalizesMarkdownAndRoutePhrases() {
        when(geocodingService.geocode("中国 浙江省 杭州市 西湖")).thenReturn(new double[]{30.2431, 120.1412});
        when(geocodingService.geocode("中国 浙江省 杭州市 河坊街")).thenReturn(new double[]{30.2478, 120.1716});

        var response = routePlannerService.geocodePoints(new GeocodeRoutePointsRequest(
                "杭州",
                List.of(
                        new RoutePointSaveRequest(1, 1, "**可以去西湖游览拍照**", "上午安排", null, null),
                        new RoutePointSaveRequest(1, 2, "适合去河坊街逛吃", "晚上安排", null, null)
                )
        ));

        assertEquals("西湖", response.get(0).name());
        assertEquals(30.2431, response.get(0).latitude());
        assertEquals(120.1412, response.get(0).longitude());
        assertEquals("河坊街", response.get(1).name());
        assertEquals(30.2478, response.get(1).latitude());
        assertEquals(120.1716, response.get(1).longitude());
        verify(geocodingService).geocode("中国 浙江省 杭州市 西湖");
        verify(geocodingService).geocode("中国 浙江省 杭州市 河坊街");
    }

    private UserAccount user(Long id, UserRole role) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setUsername("user-" + id);
        user.setDisplayName("User " + id);
        user.setEmail("user" + id + "@example.com");
        user.setPasswordHash("hashed-password");
        user.setRole(role);
        return user;
    }

    private RoutePlan routePlan(Long id, UserAccount owner) {
        RoutePlan routePlan = new RoutePlan();
        routePlan.setId(id);
        routePlan.setUser(owner);
        routePlan.setTitle("route");
        routePlan.setSummary("summary");
        routePlan.setDestination("destination");
        routePlan.setDays(3);
        routePlan.setProvider("provider");
        routePlan.setModel("model");
        return routePlan;
    }
}
