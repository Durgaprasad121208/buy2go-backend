package com.buy2go.api.service;

import com.buy2go.api.dto.PagedResponse;
import com.buy2go.api.dto.request.ProductRequestDTO;
import com.buy2go.api.dto.response.ProductResponseDTO;

import java.util.List;

public interface ProductService {
    List<ProductResponseDTO> getLiveProducts();
    PagedResponse<ProductResponseDTO> getAllProducts(int page, int size, String search);
    ProductResponseDTO getProductById(String id);
    ProductResponseDTO getProductBySlug(String slug);
    ProductResponseDTO createProduct(ProductRequestDTO request);
    ProductResponseDTO updateProduct(String id, ProductRequestDTO request);
    ProductResponseDTO toggleLiveStatus(String id);
    void deleteProduct(String id);
}
