package com.buy2go.api.repository;

import com.buy2go.api.document.ProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<ProductDocument, String> {

    Optional<ProductDocument> findByIdAndIsDeletedFalse(String id);
    Optional<ProductDocument> findBySlugAndIsDeletedFalse(String slug);

    List<ProductDocument> findByIsLiveTrueAndIsDeletedFalse();

    Page<ProductDocument> findByIsDeletedFalse(Pageable pageable);

    @Query("{ 'isDeleted': false, $or: [ { 'title': { $regex: ?0, $options: 'i' } }, { 'category': { $regex: ?0, $options: 'i' } }, { 'description': { $regex: ?0, $options: 'i' } } ] }")
    Page<ProductDocument> searchProducts(String search, Pageable pageable);

    long countByIsDeletedFalse();
    long countByIsLiveTrueAndIsDeletedFalse();
}
