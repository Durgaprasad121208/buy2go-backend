package com.buy2go.api.service.impl;

import com.buy2go.api.document.OrderDocument;
import com.buy2go.api.dto.response.DashboardMetricsDTO;
import com.buy2go.api.dto.response.OrderResponseDTO;
import com.buy2go.api.enums.FilterType;
import com.buy2go.api.enums.OrderStatus;
import com.buy2go.api.mapper.ModelMapper;
import com.buy2go.api.repository.OrderRepository;
import com.buy2go.api.repository.ProductRepository;
import com.buy2go.api.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Override
    public DashboardMetricsDTO getMetrics() {
        long totalOrders = orderRepository.countByIsDeletedFalse();
        long totalProducts = productRepository.countByIsDeletedFalse();
        long liveProducts = productRepository.countByIsLiveTrueAndIsDeletedFalse();

        Map<OrderStatus, Long> statusCounts = new HashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            statusCounts.put(status, orderRepository.countByStatusAndIsDeletedFalse(status));
        }

        List<OrderDocument> allOrders = orderRepository.findByIsDeletedFalse(PageRequest.of(0, 10000)).getContent();
        double totalRevenue = allOrders.stream()
                .filter(o -> o.getStatus() != OrderStatus.Cancelled)
                .mapToDouble(OrderDocument::getTotalAmount)
                .sum();

        List<OrderResponseDTO> recentOrders = orderRepository.findTop5ByIsDeletedFalseOrderByCreatedAtDesc().stream()
                .map(ModelMapper::toOrderResponse)
                .toList();

        return DashboardMetricsDTO.builder()
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .totalProducts(totalProducts)
                .liveProducts(liveProducts)
                .statusCounts(statusCounts)
                .recentOrders(recentOrders)
                .build();
    }

    @Override
    public Map<String, Object> getRevenueAnalytics(FilterType filterType) {
        Instant now = Instant.now();
        Instant start;

        if (filterType == null) filterType = FilterType.MONTH;

        switch (filterType) {
            case DAY -> start = now.minus(24, ChronoUnit.HOURS);
            case WEEK -> start = now.minus(7, ChronoUnit.DAYS);
            case MONTH -> start = now.minus(30, ChronoUnit.DAYS);
            case YEAR -> start = now.minus(365, ChronoUnit.DAYS);
            default -> start = now.minus(30, ChronoUnit.DAYS);
        }

        List<OrderDocument> orders = orderRepository.findByCreatedAtBetweenAndIsDeletedFalse(start, now);
        double periodRevenue = orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.Cancelled)
                .mapToDouble(OrderDocument::getTotalAmount)
                .sum();

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("filterType", filterType.name());
        analytics.put("periodRevenue", periodRevenue);
        analytics.put("orderCount", orders.size());
        analytics.put("averageOrderValue", orders.isEmpty() ? 0 : periodRevenue / orders.size());

        return analytics;
    }
}
