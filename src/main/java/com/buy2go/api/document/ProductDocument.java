package com.buy2go.api.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "products")
public class ProductDocument extends BaseDocument {

    private String title;

    @Indexed(unique = true)
    private String slug;

    private String category;
    private double startingPrice;
    private double price;
    private String formattedPrice;
    private String image;
    private String description;

    private boolean isLive = true;
    private int stockQuantity = 100;
}
