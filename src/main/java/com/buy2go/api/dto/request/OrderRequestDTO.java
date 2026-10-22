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
    private double totalAmount;
    private String location;
}
