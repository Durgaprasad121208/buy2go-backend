package com.buy2go.api.service;

import com.buy2go.api.dto.response.DashboardMetricsDTO;
import com.buy2go.api.enums.FilterType;

import java.util.Map;

public interface DashboardService {
    DashboardMetricsDTO getMetrics();
    Map<String, Object> getRevenueAnalytics(FilterType filterType);
}
