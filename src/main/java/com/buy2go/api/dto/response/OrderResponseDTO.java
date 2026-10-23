package com.buy2go.api.dto.response;

import com.buy2go.api.entity.OrderEntity;
import com.buy2go.api.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Collections;
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

    private OrderEntity.CustomerDetail customer;
    private List<OrderEntity.OrderItemDetail> items;
    private String photo; // Primary photo URL stored in MinIO
    private List<String> photos; // All photo URLs stored in MinIO
    private OrderEntity.ShipmentDetail shipment;
    private List<OrderEntity.OrderStatusHistory> statusHistory;

    private Instant createdAt;
    private Instant updatedAt;

    // Backward compatibility helper
    public List<String> getAdditionalPhotos() {
        if (photos != null && !photos.isEmpty()) return photos;
        return photo != null && !photo.isEmpty() ? List.of(photo) : Collections.emptyList();
    }
}
