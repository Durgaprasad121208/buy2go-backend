package com.buy2go.api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductRequestDTO {

    private String title;
    private String category;
    private double price;
    private double startingPrice;
    private String description;
    private String image;
    private boolean isLive = true;
    private int stockQuantity = 100;
}
