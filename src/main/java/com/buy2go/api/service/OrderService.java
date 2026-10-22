package com.buy2go.api.service;

import com.buy2go.api.dto.PagedResponse;
import com.buy2go.api.dto.request.ItemCustomizationDTO;
import com.buy2go.api.dto.request.OrderRequestDTO;
import com.buy2go.api.dto.request.OrderStatusUpdateDTO;
import com.buy2go.api.dto.request.ShipmentUpdateDTO;
import com.buy2go.api.dto.response.OrderResponseDTO;
import com.buy2go.api.enums.OrderStatus;
import org.springframework.web.multipart.MultipartFile;

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

    // Upload single photo to MinIO for product printing - returns MinIO URL only
    String uploadOrderPhoto(MultipartFile file);

    // Edit and update photo / textToPrint / color customization on an order item
    OrderResponseDTO updateItemCustomization(String orderId, String itemId, ItemCustomizationDTO customization);
    OrderResponseDTO updateItemPhoto(String orderId, String itemId, String photoUrl);
    OrderResponseDTO removeItemPhoto(String orderId, String itemId);
    OrderResponseDTO updateOrderPhoto(String orderId, String photoUrl);
}
