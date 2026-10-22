package com.buy2go.api.repository;

import com.buy2go.api.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<ProductEntity, String> {

    Optional<ProductEntity> findByIdAndIsDeletedFalse(String id);
    Optional<ProductEntity> findBySlugAndIsDeletedFalse(String slug);

    List<ProductEntity> findByIsLiveTrueAndIsDeletedFalse();

    Page<ProductEntity> findByIsDeletedFalse(Pageable pageable);

    @Query("{ 'isDeleted': false, $or: [ { 'title': { $regex: ?0, $options: 'i' } }, { 'category': { $regex: ?0, $options: 'i' } }, { 'description': { $regex: ?0, $options: 'i' } } ] }")
    Page<ProductEntity> searchProducts(String search, Pageable pageable);

    long countByIsDeletedFalse();
    long countByIsLiveTrueAndIsDeletedFalse();
}
