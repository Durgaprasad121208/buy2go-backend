package com.buy2go.api.dto.response;

import com.buy2go.api.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardMetricsDTO {

    private long totalOrders;
    private double totalRevenue;
    private long totalProducts;
    private long liveProducts;

    private Map<OrderStatus, Long> statusCounts;
    private List<OrderResponseDTO> recentOrders;
}
