package com.buy2go.api.service.impl;

import com.buy2go.api.document.ProductDocument;
import com.buy2go.api.dto.PagedResponse;
import com.buy2go.api.dto.request.ProductRequestDTO;
import com.buy2go.api.dto.response.ProductResponseDTO;
import com.buy2go.api.exception.ResourceNotFoundException;
import com.buy2go.api.mapper.ModelMapper;
import com.buy2go.api.repository.ProductRepository;
import com.buy2go.api.service.ProductService;
import com.buy2go.api.utils.OrderNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public List<ProductResponseDTO> getLiveProducts() {
        return productRepository.findByIsLiveTrueAndIsDeletedFalse().stream()
                .map(ModelMapper::toProductResponse)
                .toList();
    }

    @Override
    public PagedResponse<ProductResponseDTO> getAllProducts(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<ProductDocument> productPage;
        if (search != null && !search.trim().isEmpty()) {
            productPage = productRepository.searchProducts(search.trim(), pageable);
        } else {
            productPage = productRepository.findByIsDeletedFalse(pageable);
        }

        List<ProductResponseDTO> content = productPage.getContent().stream()
                .map(ModelMapper::toProductResponse)
                .toList();

        return PagedResponse.<ProductResponseDTO>builder()
                .content(content)
                .pageNumber(productPage.getNumber())
                .pageSize(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .last(productPage.isLast())
                .build();
    }

    @Override
    public ProductResponseDTO getProductById(String id) {
        ProductDocument doc = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        return ModelMapper.toProductResponse(doc);
    }

    @Override
    public ProductResponseDTO getProductBySlug(String slug) {
        ProductDocument doc = productRepository.findBySlugAndIsDeletedFalse(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with slug: " + slug));
        return ModelMapper.toProductResponse(doc);
    }

    @Override
    public ProductResponseDTO createProduct(ProductRequestDTO request) {
        ProductDocument doc = ModelMapper.toProductDocument(request);
        doc = productRepository.save(doc);
        return ModelMapper.toProductResponse(doc);
    }

    @Override
    public ProductResponseDTO updateProduct(String id, ProductRequestDTO request) {
        ProductDocument doc = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        doc.setTitle(request.getTitle());
        doc.setCategory(request.getCategory());
        doc.setPrice(request.getPrice());
        doc.setStartingPrice(request.getStartingPrice() > 0 ? request.getStartingPrice() : request.getPrice());
        doc.setFormattedPrice("₹" + (int) request.getPrice());
        doc.setDescription(request.getDescription());
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            doc.setImage(request.getImage());
        }
        doc.setLive(request.isLive());
        doc.setStockQuantity(request.getStockQuantity());
        doc.setUpdatedAt(Instant.now());

        doc = productRepository.save(doc);
        return ModelMapper.toProductResponse(doc);
    }

    @Override
    public ProductResponseDTO toggleLiveStatus(String id) {
        ProductDocument doc = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        doc.setLive(!doc.isLive());
        doc.setUpdatedAt(Instant.now());
        doc = productRepository.save(doc);
        return ModelMapper.toProductResponse(doc);
    }

    @Override
    public void deleteProduct(String id) {
        ProductDocument doc = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        doc.setDeleted(true);
        doc.setDeletedAt(Instant.now());
        productRepository.save(doc);
    }
}
