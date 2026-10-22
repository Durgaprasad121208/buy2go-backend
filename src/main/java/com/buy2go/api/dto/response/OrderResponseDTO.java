package com.buy2go.api.dto.response;

import com.buy2go.api.document.OrderDocument;
import com.buy2go.api.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponseDTO {

    private String id;
    private String orderNumber;
    private String customerName;
    private String location;
    private int itemCount;
    private double totalAmount;
    private OrderStatus status;

    private OrderDocument.CustomerDetail customer;
    private List<OrderDocument.OrderItemDetail> items;
    private OrderDocument.ShipmentDetail shipment;
    private List<OrderDocument.OrderStatusHistory> statusHistory;

    private Instant createdAt;
    private Instant updatedAt;
}
