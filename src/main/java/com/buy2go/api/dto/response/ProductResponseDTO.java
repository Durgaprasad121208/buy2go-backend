package com.buy2go.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponseDTO {

    private String id;
    private String title;
    private String slug;
    private String category;
    private double startingPrice;
    private double price;
    private String formattedPrice;
    private String image;
    private String description;
    private boolean isLive;
    private int stockQuantity;
    private Instant createdAt;
}
