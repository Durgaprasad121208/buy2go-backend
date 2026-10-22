package com.buy2go.api.controller;

import com.buy2go.api.constants.Constants;
import com.buy2go.api.dto.ApiResponse;
import com.buy2go.api.dto.PagedResponse;
import com.buy2go.api.dto.request.ItemCustomizationDTO;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Customer order processing, product photo customization, and fulfillment management")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    @Operation(summary = "Place a new customer order with customized product photos and text to print")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> createOrder(@RequestBody OrderRequestDTO request) {
        OrderResponseDTO response = orderService.createOrder(request);
        return ResponseEntity.ok(ApiResponse.success("Order placed successfully", response));
    }

    @PostMapping(value = "/upload-photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload multiple photos (JPG, PNG, PDF - up to 8 files) to MinIO for product printing")
    public ResponseEntity<ApiResponse<List<String>>> uploadPhotos(@RequestParam("files") MultipartFile[] files) {
        List<String> urls = orderService.uploadOrderPhotos(files);
        return ResponseEntity.ok(ApiResponse.success("Photos uploaded successfully", urls));
    }

    @PostMapping(value = "/upload-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload single photo to MinIO for product printing")
    public ResponseEntity<ApiResponse<String>> uploadPhoto(@RequestParam("file") MultipartFile file) {
        String url = orderService.uploadOrderPhoto(file);
        return ResponseEntity.ok(ApiResponse.success("Photo uploaded successfully", url));
    }

    @PutMapping("/{id}/items/{itemId}/customization")
    @Operation(summary = "Edit photo, textToPrint, color, or notes for a product item in an existing order")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> updateItemCustomization(
            @PathVariable String id,
            @PathVariable String itemId,
            @RequestBody ItemCustomizationDTO customization) {
        OrderResponseDTO response = orderService.updateItemCustomization(id, itemId, customization);
        return ResponseEntity.ok(ApiResponse.success("Item customization updated successfully", response));
    }

    @PostMapping("/{id}/items/{itemId}/photos")
    @Operation(summary = "Add more photo URLs to an order item")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> addItemPhotos(
            @PathVariable String id,
            @PathVariable String itemId,
            @RequestBody List<String> photoUrls) {
        OrderResponseDTO response = orderService.addItemPhotos(id, itemId, photoUrls);
        return ResponseEntity.ok(ApiResponse.success("Photos added to item successfully", response));
    }

    @DeleteMapping("/{id}/items/{itemId}/photos")
    @Operation(summary = "Remove a photo URL from an order item")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> removeItemPhoto(
            @PathVariable String id,
            @PathVariable String itemId,
            @RequestParam String photoUrl) {
        OrderResponseDTO response = orderService.removeItemPhoto(id, itemId, photoUrl);
        return ResponseEntity.ok(ApiResponse.success("Photo removed successfully", response));
    }

    @PutMapping("/{id}/additional-photos")
    @Operation(summary = "Update order checkout-level additional photos")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> updateAdditionalPhotos(
            @PathVariable String id,
            @RequestBody List<String> additionalPhotos) {
        OrderResponseDTO response = orderService.updateOrderAdditionalPhotos(id, additionalPhotos);
        return ResponseEntity.ok(ApiResponse.success("Additional photos updated successfully", response));
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
    @Operation(summary = "Get single order details by ID or order number")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> getOrderById(@PathVariable String id) {
        OrderResponseDTO response = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/track/{orderNumber}")
    @Operation(summary = "Public track order status by order number or ID")
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
