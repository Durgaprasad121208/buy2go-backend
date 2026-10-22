package com.buy2go.api.dto.request;

import com.buy2go.api.document.OrderDocument;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequestDTO {

    private OrderDocument.CustomerDetail customer;
    private List<OrderDocument.OrderItemDetail> items;
    private double totalAmount;
    private String location;
}
