package com.buy2go.api.controller;

import com.buy2go.api.dto.ApiResponse;
import com.buy2go.api.dto.response.DashboardMetricsDTO;
import com.buy2go.api.enums.FilterType;
import com.buy2go.api.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Owner analytics and business metrics")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/metrics")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Get overall business metrics (orders, products, revenue, status breakdown)")
    public ResponseEntity<ApiResponse<DashboardMetricsDTO>> getMetrics() {
        DashboardMetricsDTO response = dashboardService.getMetrics();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/revenue-trend/{filterType}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Get revenue analytics by filter period (DAY, WEEK, MONTH, YEAR)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRevenueTrend(@PathVariable FilterType filterType) {
        Map<String, Object> response = dashboardService.getRevenueAnalytics(filterType);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
