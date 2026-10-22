package com.buy2go.api.service;

import com.buy2go.api.dto.PagedResponse;
import com.buy2go.api.dto.request.OrderRequestDTO;
import com.buy2go.api.dto.request.OrderStatusUpdateDTO;
import com.buy2go.api.dto.request.ShipmentUpdateDTO;
import com.buy2go.api.dto.response.OrderResponseDTO;
import com.buy2go.api.enums.OrderStatus;

import java.io.ByteArrayInputStream;

public interface OrderService {
    OrderResponseDTO createOrder(OrderRequestDTO request);
    PagedResponse<OrderResponseDTO> getAllOrders(int page, int size, OrderStatus status, String search);
    OrderResponseDTO getOrderById(String id);
    OrderResponseDTO getOrderByOrderNumber(String orderNumber);
    OrderResponseDTO updateOrderStatus(String id, OrderStatusUpdateDTO request, String username);
    OrderResponseDTO updateShipmentDetails(String id, ShipmentUpdateDTO request);
    OrderResponseDTO cancelOrder(String id, String remarks, String username);
    ByteArrayInputStream downloadOrdersExcel(OrderStatus status);
}
