package com.buy2go.api.entity;

import com.buy2go.api.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "orders")
public class OrderEntity extends BaseEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    private String orderNumber;

    private String customerName;
    private String location;
    private int itemCount;
    private double totalAmount;

    private OrderStatus status = OrderStatus.New;

    private CustomerDetail customer;
    private List<OrderItemDetail> items = new ArrayList<>();
    
    // Primary order photo URL stored in MinIO for product printing
    private String photo;
    private List<String> photos = new ArrayList<>();

    public List<String> getPhotos() {
        if (photos != null && !photos.isEmpty()) {
            return photos;
        }
        return photo != null && !photo.isEmpty() ? List.of(photo) : Collections.emptyList();
    }

    public void setPhotos(List<String> photos) {
        this.photos = photos != null ? new ArrayList<>(photos) : new ArrayList<>();
        if ((this.photo == null || this.photo.isEmpty()) && !this.photos.isEmpty()) {
            this.photo = this.photos.get(0);
        }
    }

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

    public static String extractPhotoId(String url) {
        if (url == null || url.trim().isEmpty()) return null;
        String trimmed = url.trim();
        int slash = trimmed.lastIndexOf('/');
        String filename = slash >= 0 ? trimmed.substring(slash + 1) : trimmed;
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    public String getPhotoId() {
        return extractPhotoId(this.photo);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderItemDetail {
        private String id;
        private String productId;
        private String title;
        private String variant;       // e.g. "Clear", "Black"
        private String color;         // explicit color selected
        private String textToPrint;   // custom text to print on product
        private int quantity;
        private double price;
        
        // Primary photo URL stored in MinIO to be printed
        private String photo;
        private String photoId;
        private List<String> photos = new ArrayList<>();
        
        private String printNotes;    // instructions / placement notes

        public String getPhotoId() {
            if (photoId != null && !photoId.isEmpty()) return photoId;
            return extractPhotoId(photo);
        }

        // Compatibility helpers
        public List<String> getPhotos() {
            if (photos != null && !photos.isEmpty()) {
                return photos;
            }
            return photo != null && !photo.isEmpty() ? List.of(photo) : Collections.emptyList();
        }

        public void setPhotos(List<String> photos) {
            this.photos = photos != null ? new ArrayList<>(photos) : new ArrayList<>();
            if ((this.photo == null || this.photo.isEmpty()) && !this.photos.isEmpty()) {
                this.photo = this.photos.get(0);
            }
            if (this.photoId == null && this.photo != null) {
                this.photoId = extractPhotoId(this.photo);
            }
        }
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
