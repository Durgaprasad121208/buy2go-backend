package com.buy2go.api.controller;

import com.buy2go.api.constants.Constants;
import com.buy2go.api.dto.ApiResponse;
import com.buy2go.api.dto.PagedResponse;
import com.buy2go.api.dto.request.OrderRequestDTO;
import com.buy2go.api.dto.request.OrderStatusUpdateDTO;
import com.buy2go.api.dto.request.ShipmentUpdateDTO;
import com.buy2go.api.dto.response.OrderResponseDTO;
import com.buy2go.api.enums.OrderStatus;
import com.buy2go.api.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Customer order processing and owner fulfillment management")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    @Operation(summary = "Place a new customer order")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> createOrder(@RequestBody OrderRequestDTO request) {
        OrderResponseDTO response = orderService.createOrder(request);
        return ResponseEntity.ok(ApiResponse.success("Order placed successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_STAFF')")
    @Operation(summary = "Get paginated orders list with status and customer search")
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponseDTO>>> getAllOrders(
            @RequestParam(defaultValue = Constants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = Constants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String search) {
        PagedResponse<OrderResponseDTO> response = orderService.getAllOrders(page, size, status, search);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single order details by ID")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> getOrderById(@PathVariable String id) {
        OrderResponseDTO response = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/track/{orderNumber}")
    @Operation(summary = "Public track order status by order number")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> getOrderByOrderNumber(@PathVariable String orderNumber) {
        OrderResponseDTO response = orderService.getOrderByOrderNumber(orderNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_STAFF')")
    @Operation(summary = "Update order status with audit trail logging")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> updateOrderStatus(
            @PathVariable String id,
            @RequestBody OrderStatusUpdateDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : "ADMIN";
        OrderResponseDTO response = orderService.updateOrderStatus(id, request, username);
        return ResponseEntity.ok(ApiResponse.success("Order status updated", response));
    }

    @PutMapping("/{id}/shipment")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_STAFF')")
    @Operation(summary = "Update shipment details (courier, tracking, status, notes)")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> updateShipment(
            @PathVariable String id,
            @RequestBody ShipmentUpdateDTO request) {
        OrderResponseDTO response = orderService.updateShipmentDetails(id, request);
        return ResponseEntity.ok(ApiResponse.success("Shipment updated successfully", response));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_STAFF')")
    @Operation(summary = "Cancel an order")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> cancelOrder(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "Cancelled by admin") String remarks,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : "ADMIN";
        OrderResponseDTO response = orderService.cancelOrder(id, remarks, username);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled", response));
    }

    @GetMapping("/download-excel")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Download orders Excel report (Apache POI)")
    public ResponseEntity<InputStreamResource> downloadOrdersExcel(@RequestParam(required = false) OrderStatus status) {
        ByteArrayInputStream in = orderService.downloadOrdersExcel(status);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=buy2go_orders.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }
}
