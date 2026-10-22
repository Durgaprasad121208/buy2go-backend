package com.buy2go.api.dto.request;

import com.buy2go.api.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderStatusUpdateDTO {

    private OrderStatus status;
    private String remarks;
}
