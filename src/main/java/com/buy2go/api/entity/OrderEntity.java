package com.buy2go.api.entity;

import com.buy2go.api.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "orders")
public class OrderEntity extends BaseEntity {

    @Indexed(unique = true)
    private String orderNumber;

    private String customerName;
    private String location;
    private int itemCount;
    private double totalAmount;

    private OrderStatus status = OrderStatus.New;

    private CustomerDetail customer;
    private List<OrderItemDetail> items = new ArrayList<>();
    private ShipmentDetail shipment = new ShipmentDetail();

    private List<OrderStatusHistory> statusHistory = new ArrayList<>();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CustomerDetail {
        private String name;
        private String phone;
        private String email;
        private String address;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderItemDetail {
        private String id;
        private String title;
        private String variant;
        private int quantity;
        private double price;
        private List<String> photos = new ArrayList<>();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ShipmentDetail {
        private OrderStatus status = OrderStatus.New;
        private String courier;
        private String trackingNumber;
        private String notes;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderStatusHistory {
        private OrderStatus fromStatus;
        private OrderStatus toStatus;
        private String changedBy;
        private Instant changedAt = Instant.now();
        private String remarks;
    }
}
