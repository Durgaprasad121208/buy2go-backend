package com.buy2go.api.mapper;

import com.buy2go.api.document.OrderDocument;
import com.buy2go.api.document.ProductDocument;
import com.buy2go.api.document.UserDocument;
import com.buy2go.api.dto.request.ProductRequestDTO;
import com.buy2go.api.dto.response.OrderResponseDTO;
import com.buy2go.api.dto.response.ProductResponseDTO;
import com.buy2go.api.dto.response.UserResponseDTO;
import com.buy2go.api.utils.OrderNumberGenerator;

public class ModelMapper {

    public static ProductResponseDTO toProductResponse(ProductDocument doc) {
        if (doc == null) return null;
        return ProductResponseDTO.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .slug(doc.getSlug())
                .category(doc.getCategory())
                .startingPrice(doc.getStartingPrice())
                .price(doc.getPrice())
                .formattedPrice(doc.getFormattedPrice())
                .image(doc.getImage())
                .description(doc.getDescription())
                .isLive(doc.isLive())
                .stockQuantity(doc.getStockQuantity())
                .createdAt(doc.getCreatedAt())
                .build();
    }

    public static ProductDocument toProductDocument(ProductRequestDTO dto) {
        if (dto == null) return null;
        ProductDocument doc = new ProductDocument();
        doc.setTitle(dto.getTitle());
        doc.setSlug(OrderNumberGenerator.generateSlug(dto.getTitle()));
        doc.setCategory(dto.getCategory());
        doc.setStartingPrice(dto.getStartingPrice() > 0 ? dto.getStartingPrice() : dto.getPrice());
        doc.setPrice(dto.getPrice());
        doc.setFormattedPrice("₹" + (int) dto.getPrice());
        doc.setImage(dto.getImage());
        doc.setDescription(dto.getDescription());
        doc.setLive(dto.isLive());
        doc.setStockQuantity(dto.getStockQuantity());
        return doc;
    }

    public static OrderResponseDTO toOrderResponse(OrderDocument doc) {
        if (doc == null) return null;
        return OrderResponseDTO.builder()
                .id(doc.getId())
                .orderNumber(doc.getOrderNumber())
                .customerName(doc.getCustomerName())
                .location(doc.getLocation())
                .itemCount(doc.getItemCount())
                .totalAmount(doc.getTotalAmount())
                .status(doc.getStatus())
                .customer(doc.getCustomer())
                .items(doc.getItems())
                .shipment(doc.getShipment())
                .statusHistory(doc.getStatusHistory())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }

    public static UserResponseDTO toUserResponse(UserDocument doc) {
        if (doc == null) return null;
        return UserResponseDTO.builder()
                .id(doc.getId())
                .firstName(doc.getFirstName())
                .lastName(doc.getLastName())
                .email(doc.getEmail())
                .phone(doc.getPhone())
                .roleName(doc.getRoleName())
                .createdAt(doc.getCreatedAt())
                .build();
    }
}
