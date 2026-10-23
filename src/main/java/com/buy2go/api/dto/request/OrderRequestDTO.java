package com.buy2go.api.dto.request;

import com.buy2go.api.entity.OrderEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequestDTO {

    private OrderEntity.CustomerDetail customer;
    private List<OrderEntity.OrderItemDetail> items;
    private String photo;         // Primary order-level photo URL if provided
    private List<String> photos;  // All order-level photo URLs
    private double totalAmount;
    private String location;

    // Backward compatibility if additionalPhotos array was sent
    public void setAdditionalPhotos(List<String> additionalPhotos) {
        if (additionalPhotos != null && !additionalPhotos.isEmpty()) {
            if (this.photos == null) this.photos = new java.util.ArrayList<>();
            this.photos.addAll(additionalPhotos);
            if (this.photo == null) {
                this.photo = additionalPhotos.get(0);
            }
        }
    }
}
