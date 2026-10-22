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
    private String photo;         // Single order-level photo URL if provided
    private double totalAmount;
    private String location;

    // Backward compatibility if additionalPhotos array was sent
    public void setAdditionalPhotos(List<String> additionalPhotos) {
        if (additionalPhotos != null && !additionalPhotos.isEmpty() && this.photo == null) {
            this.photo = additionalPhotos.get(0);
        }
    }
}
