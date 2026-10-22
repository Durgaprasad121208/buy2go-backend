package com.buy2go.api.repository;

import com.buy2go.api.document.OrderDocument;
import com.buy2go.api.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends MongoRepository<OrderDocument, String> {

    Optional<OrderDocument> findByIdAndIsDeletedFalse(String id);
    Optional<OrderDocument> findByOrderNumberAndIsDeletedFalse(String orderNumber);

    Page<OrderDocument> findByIsDeletedFalse(Pageable pageable);
    Page<OrderDocument> findByStatusAndIsDeletedFalse(OrderStatus status, Pageable pageable);

    @Query("{ 'isDeleted': false, $or: [ { 'orderNumber': { $regex: ?0, $options: 'i' } }, { 'customerName': { $regex: ?0, $options: 'i' } }, { 'location': { $regex: ?0, $options: 'i' } } ] }")
    Page<OrderDocument> searchOrders(String search, Pageable pageable);

    @Query("{ 'isDeleted': false, 'status': ?0, $or: [ { 'orderNumber': { $regex: ?1, $options: 'i' } }, { 'customerName': { $regex: ?1, $options: 'i' } }, { 'location': { $regex: ?1, $options: 'i' } } ] }")
    Page<OrderDocument> searchOrdersWithStatus(OrderStatus status, String search, Pageable pageable);

    long countByStatusAndIsDeletedFalse(OrderStatus status);
    long countByIsDeletedFalse();

    List<OrderDocument> findTop5ByIsDeletedFalseOrderByCreatedAtDesc();

    List<OrderDocument> findByCreatedAtBetweenAndIsDeletedFalse(Instant start, Instant end);
}
