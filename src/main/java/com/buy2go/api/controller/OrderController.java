package com.buy2go.api.controller;

import com.buy2go.api.constants.Constants;
import com.buy2go.api.dto.ApiResponse;
import com.buy2go.api.dto.PagedResponse;
import com.buy2go.api.dto.request.ItemCustomizationDTO;
import com.buy2go.api.dto.request.OrderRequestDTO;
import com.buy2go.api.dto.request.OrderStatusUpdateDTO;
import com.buy2go.api.dto.request.ShipmentUpdateDTO;
import com.buy2go.api.dto.response.OrderResponseDTO;
import com.buy2go.api.dto.response.PhotoDetailsDTO;
import com.buy2go.api.entity.OrderEntity;
import com.buy2go.api.enums.OrderStatus;
import com.buy2go.api.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    @PostMapping(value = "/upload-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = {MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Upload single photo to MinIO for product printing - returns MinIO URL string directly")
    public ResponseEntity<String> uploadPhoto(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is required");
        }
        String url = orderService.uploadOrderPhoto(file);
        return ResponseEntity.ok(url);
    }

    @PostMapping(value = "/upload-photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload multiple photos to MinIO for product printing - returns list of MinIO URL strings")
    public ResponseEntity<ApiResponse<List<String>>> uploadPhotos(@RequestParam("files") List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Files are required"));
        }
        List<String> urls = orderService.uploadOrderPhotos(files);
        return ResponseEntity.ok(ApiResponse.success("Photos uploaded successfully", urls));
    }

    @GetMapping("/download-photo")
    @Operation(summary = "Download any order photo from MinIO by Order ID/number, Item ID, Photo ID, or direct URL")
    public ResponseEntity<InputStreamResource> downloadPhotoByUrl(
            @Parameter(description = "Order ID or order number (e.g. 418b00b3 or #418b00b3), item ID, or photo ID")
            @RequestParam(required = false) String id,
            @Parameter(description = "Direct MinIO photo URL or object key")
            @RequestParam(required = false) String url) {
        String target = (id != null && !id.trim().isEmpty()) ? id.trim() : (url != null ? url.trim() : null);
        if (target == null || target.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String resolvedPhotoUrl = orderService.resolvePhotoUrl(target);
        if (resolvedPhotoUrl == null || resolvedPhotoUrl.trim().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        java.io.InputStream is = orderService.downloadPhoto(resolvedPhotoUrl);
        String filename = "order-photo-" + System.currentTimeMillis();
        int slashIndex = resolvedPhotoUrl.lastIndexOf('/');
        if (slashIndex >= 0 && slashIndex < resolvedPhotoUrl.length() - 1) {
            filename = resolvedPhotoUrl.substring(slashIndex + 1);
        }
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            mediaType = MediaType.IMAGE_JPEG;
        } else if (lower.endsWith(".png")) {
            mediaType = MediaType.IMAGE_PNG;
        } else if (lower.endsWith(".webp")) {
            mediaType = MediaType.parseMediaType("image/webp");
        } else if (lower.endsWith(".pdf")) {
            mediaType = MediaType.APPLICATION_PDF;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(mediaType)
                .body(new InputStreamResource(is));
    }

    public ResponseEntity<InputStreamResource> downloadPhotoByUrl(String url) {
        return downloadPhotoByUrl(null, url);
    }

    @GetMapping("/photo-url")
    @Operation(summary = "Get MinIO photo URL and details by Order ID/number, item ID, or photo ID")
    public ResponseEntity<ApiResponse<PhotoDetailsDTO>> getPhotoUrl(
            @Parameter(description = "Order ID/number (e.g. 418b00b3 or #418b00b3), item ID, or photo ID")
            @RequestParam(required = false) String id,
            @Parameter(description = "Direct MinIO photo URL")
            @RequestParam(required = false) String url) {
        String target = (id != null && !id.trim().isEmpty()) ? id.trim() : (url != null ? url.trim() : null);
        if (target == null || target.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Either id or url is required"));
        }
        PhotoDetailsDTO details = orderService.getPhotoDetails(target);
        return ResponseEntity.ok(ApiResponse.success(details));
    }

    @GetMapping("/{id}/download-photo")
    @Operation(summary = "Download primary photo for order")
    public ResponseEntity<InputStreamResource> downloadOrderPhoto(@PathVariable String id) {
        OrderResponseDTO order = orderService.getOrderById(id);
        String photoUrl = order.getPhoto();
        if ((photoUrl == null || photoUrl.isEmpty()) && order.getItems() != null && !order.getItems().isEmpty()) {
            photoUrl = order.getItems().get(0).getPhoto();
        }
        if (photoUrl == null || photoUrl.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return downloadPhotoByUrl(photoUrl);
    }

    @GetMapping("/{id}/items/{itemId}/download-photo")
    @Operation(summary = "Download customized photo for specific order item")
    public ResponseEntity<InputStreamResource> downloadOrderItemPhoto(@PathVariable String id, @PathVariable String itemId) {
        OrderResponseDTO order = orderService.getOrderById(id);
        OrderEntity.OrderItemDetail item = order.getItems().stream()
                .filter(i -> itemId.equalsIgnoreCase(i.getId()))
                .findFirst()
                .orElse(null);
        if (item == null || item.getPhoto() == null || item.getPhoto().isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return downloadPhotoByUrl(item.getPhoto());
    }

    @PostMapping("/create")
    @Operation(summary = "Place a new customer order with customized product photo and text to print")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> createOrder(
            @RequestBody OrderRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (request.getCustomer() == null) {
            request.setCustomer(new OrderEntity.CustomerDetail());
        }
        if ((request.getCustomer().getEmail() == null || request.getCustomer().getEmail().trim().isEmpty()) && userDetails != null) {
            request.getCustomer().setEmail(userDetails.getUsername());
        }
        OrderResponseDTO response = orderService.createOrder(request);
        return ResponseEntity.ok(ApiResponse.success("Order placed successfully", response));
    }

    @PostMapping("/bulk-create")
    @Operation(summary = "Place multiple customer orders in a single request (bulk ordering)")
    public ResponseEntity<ApiResponse<List<OrderResponseDTO>>> createBulkOrders(@RequestBody List<OrderRequestDTO> requests) {
        List<OrderResponseDTO> response = orderService.createBulkOrders(requests);
        return ResponseEntity.ok(ApiResponse.success("Bulk orders placed successfully", response));
    }

    @GetMapping("/customer")
    @Operation(summary = "Get all orders for a customer by email or phone number")
    public ResponseEntity<ApiResponse<List<OrderResponseDTO>>> getOrdersByCustomer(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone) {
        List<OrderResponseDTO> response = orderService.getOrdersByCustomer(email, phone);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my-orders")
    @Operation(summary = "Get all orders for the currently authenticated customer")
    public ResponseEntity<ApiResponse<List<OrderResponseDTO>>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails != null ? userDetails.getUsername() : null;
        List<OrderResponseDTO> response = orderService.getOrdersByCustomer(email, null);
        return ResponseEntity.ok(ApiResponse.success(response));
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

    @PutMapping("/{id}/items/{itemId}/photo")
    @Operation(summary = "Update single photo URL on an order item")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> updateItemPhoto(
            @PathVariable String id,
            @PathVariable String itemId,
            @RequestParam String photoUrl) {
        OrderResponseDTO response = orderService.updateItemPhoto(id, itemId, photoUrl);
        return ResponseEntity.ok(ApiResponse.success("Item photo updated successfully", response));
    }

    @DeleteMapping("/{id}/items/{itemId}/photo")
    @Operation(summary = "Remove photo from an order item")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> removeItemPhoto(
            @PathVariable String id,
            @PathVariable String itemId) {
        OrderResponseDTO response = orderService.removeItemPhoto(id, itemId);
        return ResponseEntity.ok(ApiResponse.success("Item photo removed successfully", response));
    }

    @PutMapping("/{id}/photo")
    @Operation(summary = "Update order-level photo URL")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> updateOrderPhoto(
            @PathVariable String id,
            @RequestParam String photoUrl) {
        OrderResponseDTO response = orderService.updateOrderPhoto(id, photoUrl);
        return ResponseEntity.ok(ApiResponse.success("Order photo updated successfully", response));
    }

    @DeleteMapping("/{id}/photo")
    @Operation(summary = "Remove order-level photo")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> removeOrderPhoto(@PathVariable String id) {
        OrderResponseDTO response = orderService.removeOrderPhoto(id);
        return ResponseEntity.ok(ApiResponse.success("Order photo removed successfully", response));
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
