package com.buy2go.api.dto.request;

import com.buy2go.api.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShipmentUpdateDTO {

    private OrderStatus status;
    private String courier;
    private String trackingNumber;
    private String notes;
}
